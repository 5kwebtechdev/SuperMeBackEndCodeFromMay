package com.superme.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NonNull;

@Data
public class FeedbackRequestDto {

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 4")
    @Max(value = 4, message = "Rating must be between 1 and 4")
    private Integer rating;

    @Size(max = 500, message = "App experience cannot exceed 500 characters")
    private String appExperience;

    @Size(max = 500, message = "Feature request cannot exceed 500 characters")
    private String featureRequest;
}
