
package com.superme.service;

import com.superme.config.FileStorageConfig;
import com.superme.dto.LikeResponse;
import com.superme.dto.TutorsResponse;
import com.superme.enums.FeeType;
import com.superme.exception.ResourceNotFoundException;
import com.superme.model.Like;
import com.superme.model.Tutor;
import com.superme.repository.LikeRepository;
import com.superme.repository.TutorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TutorService {

    private final TutorRepository tutorRepository;
    private final LikeRepository likeRepository;

    public TutorService(TutorRepository tutorRepository,
                        LikeRepository likeRepository) {
        this.tutorRepository = tutorRepository;
        this.likeRepository = likeRepository;
    }





    public List<TutorsResponse> getTutors(
            Long userId,
            String subject,
            String mode,
            String standard,
            String location,
            LocalDateTime start,
            LocalDateTime end,
            List<String> levels) {

        Tutor.Subject subjectEnum = parseEnum(subject, Tutor.Subject.class);
        Tutor.ContactMode modeEnum = parseEnum(mode, Tutor.ContactMode.class);

        if (levels != null && levels.isEmpty()) {
            levels = null;
        }

        return tutorRepository.findTutorsWithAllFilters(
                        subjectEnum,
                        modeEnum,
                        standard,
                        levels,
                        location,
                        start,
                        end
                )
                .stream()
                .map(t -> mapToTutorsResponse(t, userId))
                .toList();
    }






    // ================= FILTER API =================

    public List<TutorsResponse> getTutorsWithFilters(
            Long userId,
            String subject,
            String mode,
            String standard,
            String location) {

        Tutor.Subject subjectEnum = parseEnum(subject, Tutor.Subject.class);
        Tutor.ContactMode modeEnum = parseEnum(mode, Tutor.ContactMode.class);

        return tutorRepository.findTutorsWithFilters(
                        subjectEnum,
                        modeEnum,
                        standard,
                        location
                )
                .stream()
                .map(t -> mapToTutorsResponse(t, userId))
                .toList();
    }

    // ================= GET BY ID =================

    public Tutor getTutorById(Long id) {
        return tutorRepository.findById(id).orElse(null); // keeping your behavior
    }

    // ================= LIKE / UNLIKE =================

    public LikeResponse likeTutor(Long tutorId, Long userId) {

        Tutor tutor = tutorRepository.findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Tutor not found"));

        Optional<Like> existingLike =
                likeRepository.findByUserIdAndTutor(userId, tutor);

        boolean liked;

        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            liked = false;
        } else {
            Like like = new Like();
            like.setTutor(tutor);
            like.setUserId(userId);
            likeRepository.save(like);
            liked = true;
        }

        int totalLikes = likeRepository.countByTutor(tutor);

        return new LikeResponse(liked, totalLikes);
    }

    // ================= TIME FILTER =================

    public List<TutorsResponse> getTutorsByTimeRange(
            Long userId,
            LocalDateTime start,
            LocalDateTime end) {

        return tutorRepository.findByStartTimeBetween(start, end)
                .stream()
                .map(t -> mapToTutorsResponse(t, userId))
                .toList();
    }

    // ================= LEVEL FILTER =================

    public List<Tutor> getTutorsByLevel(String level) {
        return tutorRepository.findByLevel(level);
    }

    public List<TutorsResponse> getTutorsByLevels(Long userId, List<String> levels) {

        if (levels == null || levels.isEmpty()) {
            return List.of();
        }

        return tutorRepository.findByLevelsIn(levels)
                .stream()
                .map(t -> mapToTutorsResponse(t, userId))
                .toList();
    }

    // ================= COMBINED FILTER =================

    public List<TutorsResponse> getTutorsByTimeAndLevels(
            Long userId,
            LocalDateTime start,
            LocalDateTime end,
            List<String> levels) {

        List<Tutor> byTime = tutorRepository.findByStartTimeBetween(start, end);
        List<Tutor> byLevels = tutorRepository.findByLevelsIn(levels);

        Set<Long> levelIds = byLevels.stream()
                .map(Tutor::getId)
                .collect(Collectors.toSet());

        return byTime.stream()
                .filter(t -> levelIds.contains(t.getId()))
                .map(t -> mapToTutorsResponse(t, userId))
                .toList();
    }

    // ================= FEES =================

    public List<Tutor> getTutorsByFeeType(FeeType feeType) {
        return tutorRepository.findByFeeType(feeType);
    }

    public List<Tutor> getTutorsByFeesRange(BigDecimal minFees, BigDecimal maxFees) {
        return tutorRepository.findByFeesBetween(minFees, maxFees);
    }

    public List<Tutor> getTutorsByMaxFees(BigDecimal maxFees) {
        return tutorRepository.findByFeesLessThanEqual(maxFees);
    }

    public List<Tutor> getTutorsByMinFees(BigDecimal minFees) {
        return tutorRepository.findByFeesGreaterThanEqual(minFees);
    }

    // ================= MAPPER =================

    public TutorsResponse mapToTutorsResponse(Tutor tutor, Long userId) {

        boolean likedByUser =
                userId != null &&
                        likeRepository.existsByUserIdAndTutor(userId, tutor);

        return TutorsResponse.builder()
                .id(tutor.getId())
                .name(tutor.getName())
                .headline(tutor.getHeadline())
                .age(tutor.getAge())
                .phone(tutor.getPhone())
                .email(tutor.getEmail())
                .gender(tutor.getGender())
                .qualification(tutor.getQualification())
                .experience(tutor.getExperience())
                .entityType(tutor.getEntityType())
                .entityName(tutor.getEntityName())
                .subjects(tutor.getSubjects())
                .location(tutor.getLocation())
                .addressLine(tutor.getAddressLine())
                .state(tutor.getState())
                .city(tutor.getCity())
                .pincode(tutor.getPincode())
//                .profilePicUrl(tutor.getProfilePicUrl())
                .profilePicUrl((buildFileUrl(tutor.getProfilePicUrl())))
                .startTime(tutor.getStartTime())
                .endTime(tutor.getEndTime())
                .feeType(tutor.getFeeType())
                .fees(tutor.getFees())
                .levels(tutor.getLevels())
                .documentsVerificationUrl(tutor.getDocumentsVerificationUrl())
                .hourlyRate(tutor.getHourlyRate())
                .availability(tutor.getAvailability())
                .contactModes(tutor.getContactModes())
                .createdAt(tutor.getCreatedAt())
                .updatedAt(tutor.getUpdatedAt())
                .isActive(tutor.isActive())
                .isVerified(tutor.isVerified())
                .verificationNotes(tutor.getVerificationNotes())
                .adminNotes(tutor.getAdminNotes())
                .lastLoginAt(tutor.getLastLoginAt())
                .totalStudents(tutor.getTotalStudents())
                .rating(tutor.getRating())
                .totalReviews(tutor.getTotalReviews())
                .categoryMappings(tutor.getCategoryMappings())
                .categoryIds(tutor.getCategoryIds())
                .champsLiked(tutor.getChampsLiked())
                .likedByUser(likedByUser)
                .active(tutor.isActive())
                .verified(tutor.isVerified())
                .validForVerification(tutor.isValidForVerification())
                .build();
    }

    // ================= HELPER =================

    private <T extends Enum<T>> T parseEnum(String value, Class<T> enumClass) {
        try {
            return value != null ? Enum.valueOf(enumClass, value.toUpperCase()) : null;
        } catch (Exception e) {
            return null; // keeps your behavior safe instead of crashing
        }
    }



    @Autowired
    private FileStorageConfig fileStorageConfig;

    private String buildFileUrl(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }

        // extract filename from /uploads/tempimg.png
        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        return fileStorageConfig.getBaseUrl() + "/v1/tutors/download/" + fileName;
//        return fileStorageConfig.getBaseUrl() + "/api/files/download/" + fileName;
    }

}