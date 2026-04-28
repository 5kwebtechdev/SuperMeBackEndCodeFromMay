package com.superme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionOptionResponseDTO {

    private Long optionId;
    private String optionText;
    private String optionImageUrl;
    private Integer optionOrder;
    private Boolean isCorrect;
    private String explanation;

    // Audit fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isImageOption() {
        return optionImageUrl != null && !optionImageUrl.trim().isEmpty();
    }
}