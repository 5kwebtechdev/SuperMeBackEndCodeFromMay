package com.superme.admin.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for tutor's expertise in a specific category.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TutorCategoryExpertiseDto {

    private Integer categoryId;
    private String categoryName;

    // Expertise metrics
    private Integer studentCount;
    private BigDecimal categoryRating;
    private Integer totalReviews;
    private Integer yearsOfExperience;
    private String expertiseLevel;

    // Availability in this category
    private Boolean isAcceptingStudents;
    private Integer maxStudentsPerBatch;
    private Integer currentStudents;

    // Rates
    private BigDecimal categoryHourlyRate;
    private BigDecimal categoryBatchRate;

    // Activity
    private LocalDateTime lastClassTaught;
    private Integer classesCompleted;
    private Integer coursesCompleted;
}

