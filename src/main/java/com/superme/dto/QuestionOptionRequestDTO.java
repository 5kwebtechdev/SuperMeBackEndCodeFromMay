package com.superme.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating/updating question options
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionOptionRequestDTO {

    private String optionText;
    private String optionImageUrl;

    @NotNull(message = "Option order is required")
    private Integer optionOrder;

    @Builder.Default
    private Boolean isCorrect = false;

    private String explanation;

    public boolean isImageOption() {
        return optionImageUrl != null && !optionImageUrl.trim().isEmpty();
    }
}