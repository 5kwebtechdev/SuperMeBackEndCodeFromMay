package com.superme.dto;

import com.superme.admin.dto.TutorCategoryMappingDto;
import com.superme.model.Tutor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminTutorDTO {

    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;

    private String name;
    private String headline;
    private Integer age;
    private String gender;
    private String experience;
    private String qualification;
    private String phone;
    private String email;

    private Boolean documentsVerified;
    private Double hourlyRate;

    // High-level location label
    private String location;

    // Granular address section
    private String addressLine;
    private String state;
    private String city;
    private String pincode;

    private List<String> subjects;
    private String entityType;
    private String entityName;

    private Integer studentCount;
    private Integer totalStudents;
    private String status;
    private String verificationStatus;
    private Integer champsLiked;
    private BigDecimal rating;
    private Integer totalReviews;

    // Fields needed for edit form
    private BigDecimal fees;
    private String feeType;
    private String startTime;   // "HH:mm"
    private String endTime;     // "HH:mm"
    private String timePreference;
    private List<String> levels;
    private List<Tutor.ContactMode> contactModes;
    private List<Tutor.Availability> availability;
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
    private String profilePicUrl;
    private String documentsVerificationUrl;
    private Boolean isActive;
    private Boolean isVerified;

    // Category mappings
    private String category;

    // Nested statistics class
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TutorStatistics {
        private long totalTutors;
        private long activeTutors;
        private long verifiedTutors;
        private long unverifiedTutors;
    }
}