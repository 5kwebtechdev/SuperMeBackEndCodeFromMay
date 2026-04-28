package com.superme.service;

import com.superme.dto.JournalEntryRequest;
import com.superme.dto.JournalEntryResponseDTO;
import com.superme.exception.BusinessException;
import com.superme.model.JournalEntry;
import com.superme.model.User;
import com.superme.repository.JournalEntryRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class JournalEntryService {

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
        entry.setCreationDate(today);
        entry.setCreationTime(now.toLocalTime().withNano(0));

        JournalEntry savedEntry = journalEntryRepository.save(entry);
        return mapEntityToResponseDTO(savedEntry);
    }


    /**
     * Update an existing journal entry
     */
    public Optional<JournalEntryResponseDTO> updateJournalEntry(Long entryId, JournalEntryRequest request, User user) {
        Optional<JournalEntry> existingEntry = journalEntryRepository.findById(entryId);

        if (existingEntry.isPresent() && existingEntry.get().getCreatedBy().getId().equals(user.getId())) {
            JournalEntry entry = existingEntry.get();
            updateEntityFromRequest(entry, request);
            JournalEntry updatedEntry = journalEntryRepository.save(entry);
            return Optional.of(mapEntityToResponseDTO(updatedEntry));
        }
        return Optional.empty();
    }

    /**
     * Delete a journal entry
     */
    public boolean deleteJournalEntry(Long entryId, User user) {
        Optional<JournalEntry> entry = journalEntryRepository.findById(entryId);
        if (entry.isPresent() && entry.get().getCreatedBy().getId().equals(user.getId())) {
            journalEntryRepository.delete(entry.get());
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
        entry.setAttachmentUrls(request.getAttachmentUrls());
        return entry;
    }

    private void updateEntityFromRequest(JournalEntry entry, JournalEntryRequest request) {
        if (request.getEmotions() != null) entry.setEmotions(request.getEmotions());
        if (request.getSleep() != null) entry.setSleep(request.getSleep());
        if (request.getHealth() != null) entry.setHealth(request.getHealth());
        if (request.getHobbies() != null) entry.setHobbies(request.getHobbies());
        if (request.getFood() != null) entry.setFood(request.getFood());
        if (request.getSocial() != null) entry.setSocial(request.getSocial());
        if (request.getSchool() != null) entry.setSchool(request.getSchool());
        if (request.getNotes() != null) entry.setNotes(request.getNotes());
        if (request.getAttachmentUrls() != null) entry.setAttachmentUrls(request.getAttachmentUrls());
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
        dto.setAttachmentUrls(entry.getAttachmentUrls());
        return dto;
    }

    /**
     * Get journal entry counts by user (for admin/statistics)
     */
    public List<Object[]> getJournalEntryCountsByUser() {
        return journalEntryRepository.countJournalEntriesByUser();
    }
}