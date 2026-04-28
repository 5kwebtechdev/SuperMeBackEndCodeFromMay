package com.superme.dto;

import com.superme.enums.FeeType;
import com.superme.model.Tutor;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorsResponse {

    private Long id;
    private String name;
    private String headline;
    private Integer age;
    private String phone;
    private String email;

    private Tutor.Gender gender;
    private String qualification;
    private Tutor.Experience experience;

    private Tutor.EntityType entityType;
    private String entityName;

    private List<Tutor.Subject> subjects;

    private String location;
    private String addressLine;
    private String state;
    private String city;
    private String pincode;

    private String profilePicUrl;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private FeeType feeType;
    private BigDecimal fees;

    private List<String> levels;

    private String documentsVerificationUrl;

    private BigDecimal hourlyRate;

    private List<Tutor.Availability> availability;
    private List<Tutor.ContactMode> contactModes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Boolean isActive;
    private Boolean isVerified;

    private String verificationNotes;
    private String adminNotes;

    private LocalDateTime lastLoginAt;

    private Integer totalStudents;
    private BigDecimal rating;
    private Integer totalReviews;

    // Category related
    private List<?> categoryMappings;
    private List<Integer> categoryIds;

    // Like related
    private Integer champsLiked;
    private Boolean likedByUser;

    // Derived flags (frontend convenience)
    private Boolean active;
    private Boolean verified;
    private Boolean validForVerification;
}
