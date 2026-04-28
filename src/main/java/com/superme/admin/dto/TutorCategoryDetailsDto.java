package com.superme.admin.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO containing category details and selected field options for a tutor.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TutorCategoryDetailsDto {

    // Category Information
    private Integer categoryId;
    private String categoryName;
    private String categoryKey;
    private String categoryDescription;
    private Integer categoryDisplayOrder;

    // Selected Field Options by Field Key
    // Example: { "classes": ["CLASS_9", "CLASS_10"], "subjects": ["MATHEMATICS", "PHYSICS"] }
    private Map<String, List<String>> selectedFieldOptions;

    // Expert level in this category
    private String expertiseLevel;

    // Whether tutor is currently accepting students in this category
    private Boolean isAcceptingStudents;

    // Experience years in this category
    private Integer yearsOfExperience;

    // Hourly rate for this category
    private BigDecimal categoryHourlyRate;

    // Batch rate for this category
    private BigDecimal categoryBatchRate;

    // Description
    private String description;

    // When tutor was added to this category
    private LocalDateTime addedAt;

    // Stats
    private Integer studentCount;
    private BigDecimal categoryRating;
    private Integer totalReviews;
    private Integer classesCompleted;
}
