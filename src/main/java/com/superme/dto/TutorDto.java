package com.superme.dto;

import com.superme.admin.dto.TutorCategoryDetailsDto;
import com.superme.admin.dto.TutorCategoryExpertiseDto;
import com.superme.admin.dto.TutorCategoryMappingDto;
import com.superme.enums.FeeType;
import com.superme.model.Tutor.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for Tutor entity.
 * Used to expose relevant tutor information to API consumers.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TutorDto {

    private Long id;
    private String name;
    private String headline;
    private Integer age;
    private String phone;
    private String email;
    private Gender gender;
    private String qualification;
    private Experience experience;
    private EntityType entityType;
    private String entityName;
    private List<Subject> subjects;

    /**
     * High-level location label (kept for backward compatibility / search).
     */
    private String location;

    /**
     * Address section matching UI:
     * Address* (House No, Street, Landmark),
     * State*, City*, Pincode*.
     */
    private String addressLine;
    private String state;
    private String city;
    private String pincode;

    private String profilePicUrl;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<String> levels;
    private String documentsVerificationUrl;
    private BigDecimal hourlyRate;

    // Added fields
    private FeeType feeType;
    private BigDecimal fees;

    private List<Availability> availability;
    private List<ContactMode> contactModes;
    private Boolean isActive;
    private Boolean isVerified;
    private String verificationNotes;
    private String adminNotes;
    private Integer totalStudents;
    private BigDecimal rating;
    private Integer totalReviews;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;

    // Additional computed property (not persisted)
    private Integer champsLikedCount;

    private List<TutorCategoryMappingDto> categories;

    private String timePreference;
    private List<String> languages;
    private List<String> boards;
    private List<String> classes;
    private List<String> degrees;
    private List<String> years;
    private List<String> languagesOffered;
    private List<String> proficiencyLevels;
    private List<String> skills;
    private List<String> hobbyProficiency;
    private List<String> ageGroups;
    private List<String> targetExams;
    private List<String> activities;
    private List<String> otherSkills;
    private List<String> otherLevels;
}
