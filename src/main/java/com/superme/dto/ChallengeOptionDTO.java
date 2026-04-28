package com.superme.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChallengeOptionDTO {
    private Long id;
    private String optionText;
    private String optionImageUrl; // New field for image-based options
    private Integer optionOrder;
    private Boolean isCorrect;
    private String hint; // Optional: Hint for this specific option
    private String explanation; // Optional: Explanation for why this option is correct/incorrect

    // Helper method to get display content
    public String getDisplayContent() {
        if (optionImageUrl != null && !optionImageUrl.trim().isEmpty()) {
            return optionImageUrl;
        }
        return optionText != null ? optionText : "";
    }

    // Check if this is an image option
    public boolean isImageOption() {
        return optionImageUrl != null && !optionImageUrl.trim().isEmpty();
    }

    // Check if this is a text option
    public boolean isTextOption() {
        return optionText != null && !optionText.trim().isEmpty()
                && (optionImageUrl == null || optionImageUrl.trim().isEmpty());
    }

    // Check if this option has both text and image
    public boolean hasBothContent() {
        return optionText != null && !optionText.trim().isEmpty()
                && optionImageUrl != null && !optionImageUrl.trim().isEmpty();
    }

    // Safe getters
    public String getOptionText() {
        return optionText != null ? optionText : "";
    }

    public String getOptionImageUrl() {
        return optionImageUrl != null ? optionImageUrl : "";
    }

    public Boolean getIsCorrect() {
        return isCorrect != null ? isCorrect : false;
    }

    public String getHint() {
        return hint != null ? hint : "";
    }

    public String getExplanation() {
        return explanation != null ? explanation : "";
    }
}