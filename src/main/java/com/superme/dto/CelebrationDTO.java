package com.superme.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CelebrationDTO {
    private String title;
    private String message;
    private String shareMessage;
    private List<String> earnedBadges;
    private boolean showCelebration;
}