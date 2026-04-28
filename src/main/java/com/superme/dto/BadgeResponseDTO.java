package com.superme.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BadgeResponseDTO {
    private String title;
    private String levelText;
    private String description;
    private String achievementScreenText;
    private String icon;
    private Integer progress;
    private Integer target;
    private Boolean isEarned;
    private Boolean isMaxLevel;
    private Integer currentLevel;
    private Integer totalLevels;

    // For celebration card
    private String celebrationTitle;
    private String celebrationMessage;
    private String shareMessage;
}