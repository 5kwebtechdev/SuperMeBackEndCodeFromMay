package com.superme.service;

import com.superme.dto.JournalEntryRequest;
import com.superme.dto.JournalEntryResponseDTO;
import com.superme.exception.BusinessException;
import com.superme.model.JournalAttachment;
import com.superme.model.JournalEntry;
import com.superme.model.User;
import com.superme.repository.JournalEntryRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class JournalEntryService {
    @Value("${file.base-url}")
    private String baseUrl;
    @Autowired
    private JournalEntryRepository journalEntryRepository;

    /**
     * Create a new journal entry from request DTO
     */
    @Transactional
    public JournalEntryResponseDTO createJournalEntry(
            JournalEntryRequest request,
            User user) {

        ZoneId istZone = ZoneId.of("Asia/Kolkata");
        LocalDateTime now = LocalDateTime.now(istZone);
        LocalDate today = now.toLocalDate();

        // 🚫 Allow only one journal per day
        if (journalEntryRepository.existsByCreatedByAndCreationDate(user, today)) {
            throw new BusinessException("You can add only one journal entry per day");
        }

        JournalEntry entry = mapRequestToEntity(request, user);

        List<JournalAttachment> attachments =
                saveFiles(request.getFiles(), entry);

        entry.setAttachments(attachments);
        entry.setCreationDate(today);
        entry.setCreationTime(now.toLocalTime().withNano(0));

        JournalEntry savedEntry = journalEntryRepository.save(entry);
        return mapEntityToResponseDTO(savedEntry);
    }
    @Transactional
    public Optional<JournalEntryResponseDTO> updateJournalEntry(
            Long entryId,
            JournalEntryRequest request,
            User user) {

        Optional<JournalEntry> optionalEntry = journalEntryRepository.findById(entryId);

        if (optionalEntry.isPresent() &&
                optionalEntry.get().getCreatedBy().getId().equals(user.getId())) {

            JournalEntry entry = optionalEntry.get();

            // ✅ Update fields
            updateEntityFromRequest(entry, request);

            // ✅ DELETE old files from disk
            deleteOldFiles(entry.getAttachments());

            // ✅ IMPORTANT: clear existing list (DO NOT replace it)
            entry.getAttachments().clear();

            // ✅ Add new attachments to SAME list
            List<JournalAttachment> newAttachments = saveFiles(request.getFiles(), entry);

            for (JournalAttachment att : newAttachments) {
                entry.getAttachments().add(att); // 🔥 THIS IS THE FIX
            }

            JournalEntry updatedEntry = journalEntryRepository.save(entry);

            return Optional.of(mapEntityToResponseDTO(updatedEntry));
        }

        return Optional.empty();
    }





    private void deleteOldFiles(List<JournalAttachment> attachments) {

        if (attachments == null || attachments.isEmpty()) return;

        for (JournalAttachment att : attachments) {
            try {
                String fileName = att.getFilePath()
                        .substring(att.getFilePath().lastIndexOf("/") + 1);

//                Path path = Paths.get(UPLOAD_DIR + fileName);
                Path path = Paths.get(uploadDir + "/journal/" + fileName);
                Files.deleteIfExists(path);

            } catch (Exception e) {
                System.out.println("Failed to delete file: " + att.getFilePath());
            }
        }
    }
//    /**
//     * Update an existing journal entry
//     */
//    public Optional<JournalEntryResponseDTO> updateJournalEntry(Long entryId, JournalEntryRequest request, User user) {
//        Optional<JournalEntry> existingEntry = journalEntryRepository.findById(entryId);
//
//        if (existingEntry.isPresent() && existingEntry.get().getCreatedBy().getId().equals(user.getId())) {
//            JournalEntry entry = existingEntry.get();
//            updateEntityFromRequest(entry, request);
//            JournalEntry updatedEntry = journalEntryRepository.save(entry);
//            return Optional.of(mapEntityToResponseDTO(updatedEntry));
//        }
//        return Optional.empty();
//    }

    /**
     * Delete a journal entry
     */
//    public boolean deleteJournalEntry(Long entryId, User user) {
//        Optional<JournalEntry> entry = journalEntryRepository.findById(entryId);
//        if (entry.isPresent() && entry.get().getCreatedBy().getId().equals(user.getId())) {
//            journalEntryRepository.delete(entry.get());
//            return true;
//        }
//        return false;
//    }
    public boolean deleteJournalEntry(Long entryId, User user) {

        Optional<JournalEntry> optionalEntry = journalEntryRepository.findById(entryId);

        if (optionalEntry.isPresent() &&
                optionalEntry.get().getCreatedBy().getId().equals(user.getId())) {

            JournalEntry entry = optionalEntry.get();

            // ✅ DELETE FILES FROM DISK FIRST
            deleteOldFiles(entry.getAttachments());

            // ✅ THEN DELETE DB RECORD
            journalEntryRepository.delete(entry);

            return true;
        }

        return false;
    }
    /**
     * Get all journal entries for a user
     */
    public List<JournalEntryResponseDTO> getJournalEntries(
            User user,
            String type,
            Integer month,
            Integer year) {

        List<JournalEntry> entries;

        LocalDate today = LocalDate.now();

        switch (type.toUpperCase()) {

            case "MONTH" -> {
                YearMonth yearMonth = (month != null && year != null)
                        ? YearMonth.of(year, month)
                        : YearMonth.from(today);

                LocalDate start = yearMonth.atDay(1);
                LocalDate end = yearMonth.atEndOfMonth();

                entries = journalEntryRepository
                        .findByCreatedByAndCreationDateBetween(user, start, end);
            }

            case "TODAY" -> {
                entries = journalEntryRepository
                        .findByCreatedByAndCreationDate(user, today);
            }

            default -> throw new BusinessException("Invalid type. Use TODAY or MONTH");
        }

        return entries.stream()
                .map(this::mapEntityToResponseDTO)
                .toList();
    }



    /**
     * Get a single journal entry by ID
     */
    public Optional<JournalEntryResponseDTO> getJournalEntryById(Long entryId, User user) {
        Optional<JournalEntry> entry = journalEntryRepository.findById(entryId);
        if (entry.isPresent() && entry.get().getCreatedBy().getId().equals(user.getId())) {
            return Optional.of(mapEntityToResponseDTO(entry.get()));
        }
        return Optional.empty();
    }

    // Helper methods for mapping

    private JournalEntry mapRequestToEntity(JournalEntryRequest request, User user) {
        JournalEntry entry = new JournalEntry();

        entry.setCreatedBy(user);
        entry.setEmotions(request.getEmotions());
        entry.setSleep(request.getSleep());
        entry.setHealth(request.getHealth());
        entry.setHobbies(request.getHobbies());
        entry.setFood(request.getFood());
        entry.setSocial(request.getSocial());
        entry.setSchool(request.getSchool());
        entry.setNotes(request.getNotes());

        return entry;
    }

//    private JournalEntry mapRequestToEntity(JournalEntryRequest request, User user) {
//        JournalEntry entry = new JournalEntry();
//        entry.setCreatedBy(user);
//        entry.setEmotions(request.getEmotions());
//        entry.setSleep(request.getSleep());
//        entry.setHealth(request.getHealth());
//        entry.setHobbies(request.getHobbies());
//        entry.setFood(request.getFood());
//        entry.setSocial(request.getSocial());
//        entry.setSchool(request.getSchool());
//        entry.setNotes(request.getNotes());
//        entry.setAttachmentUrls(request.getAttachmentUrls());
//        return entry;
//    }

    private void updateEntityFromRequest(JournalEntry entry, JournalEntryRequest request) {
        if (request.getEmotions() != null) entry.setEmotions(request.getEmotions());
        if (request.getSleep() != null) entry.setSleep(request.getSleep());
        if (request.getHealth() != null) entry.setHealth(request.getHealth());
        if (request.getHobbies() != null) entry.setHobbies(request.getHobbies());
        if (request.getFood() != null) entry.setFood(request.getFood());
        if (request.getSocial() != null) entry.setSocial(request.getSocial());
        if (request.getSchool() != null) entry.setSchool(request.getSchool());
        if (request.getNotes() != null) entry.setNotes(request.getNotes());
//        if (request.getAttachments() != null) entry.setAttachmentUrls(request.getAttachmentUrls());
    }

    private JournalEntryResponseDTO mapEntityToResponseDTO(JournalEntry entry) {
        JournalEntryResponseDTO dto = new JournalEntryResponseDTO();
        dto.setId(entry.getId());
        dto.setCreatedBy(entry.getCreatedBy().getId());
        dto.setCreationDate(entry.getCreationDate());
        dto.setCreationTime(entry.getCreationTime());
        dto.setEmotions(entry.getEmotions());
        dto.setSleep(entry.getSleep());
        dto.setHealth(entry.getHealth());
        dto.setHobbies(entry.getHobbies());
        dto.setFood(entry.getFood());
        dto.setSocial(entry.getSocial());
        dto.setSchool(entry.getSchool());
        dto.setNotes(entry.getNotes());
//        dto.setAttachmentUrls(entry.getAttachmentUrls());
        dto.setAttachments(
                entry.getAttachments() != null
                        ? entry.getAttachments().stream()
                          .map(att -> baseUrl + "/v1/journal/download/image/" +
                                  att.getFilePath().substring(att.getFilePath().lastIndexOf("/") + 1))
                          .toList()
                        : null
        );
        return dto;
    }

    /**
     * Get journal entry counts by user (for admin/statistics)
     */
    public List<Object[]> getJournalEntryCountsByUser() {
        return journalEntryRepository.countJournalEntriesByUser();
    }



    @Value("${file.upload-dir}")
    private String uploadDir;

 //
//    private static final String UPLOAD_DIR =
//            "src/main/resources/static/images/journal/";

    private List<JournalAttachment> saveFiles(
            List<MultipartFile> files,
            JournalEntry entry) {

        List<JournalAttachment> attachments = new ArrayList<>();

        if (files == null || files.isEmpty()) return attachments;

        for (MultipartFile file : files) {

            try {
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

                // ✅ correct unified path
                Path path = Paths.get(uploadDir + "/journal/" + fileName);

                Files.createDirectories(path.getParent());
                Files.write(path, file.getBytes());

                JournalAttachment att = new JournalAttachment();

                // ✅ store relative path only
                att.setFilePath("journal/" + fileName);
                att.setFileType(file.getContentType());
                att.setJournalEntry(entry);

                attachments.add(att);

            } catch (Exception e) {
                throw new RuntimeException("Failed to store file", e);
            }
        }

        return attachments;
    }




}