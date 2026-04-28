package com.superme.admin.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTO for adding/updating category mapping for a tutor.
 * Used when tutor wants to add a new category to their profile.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TutorCategoryMappingDto {

    // Category to add
    private Integer categoryId;

    // Field options selected by tutor for this category
    // Example: { "classes": ["CLASS_9", "CLASS_10"], "subjects": ["MATHEMATICS", "PHYSICS"] }
    private Map<String, List<String>> fieldOptions;

    // Expertise information
    private String expertiseLevel;
    private Integer yearsOfExperience;

    // Availability
    private Boolean isAcceptingStudents;
    private Integer maxStudentsPerBatch;

    // Pricing
    private BigDecimal categoryHourlyRate;
    private BigDecimal categoryBatchRate;

    // Description of why they are expert in this
    private String description;
}
