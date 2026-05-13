package com.superme.dto;

import com.superme.admin.dto.TutorCategoryMappingDto;
import com.superme.model.Tutor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<String> levels;
    private List<Tutor.ContactMode> contactModes;
    private List<Tutor.Availability> availability;
    private String profilePicUrl;
    private String documentsVerificationUrl;
    private Boolean isActive;
    private Boolean isVerified;

    // Category mappings
//    private List<TutorCategoryMappingDto> categories;
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