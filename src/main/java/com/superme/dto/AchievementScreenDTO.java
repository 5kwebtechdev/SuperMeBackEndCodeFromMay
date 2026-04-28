package com.superme.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AchievementScreenDTO {
    private String title;
    private String badgeName;
    private String achievementText;
    private String icon;
    private Long badgeId;
}