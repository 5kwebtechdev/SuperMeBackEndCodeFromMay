package com.superme.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeResultDTO {
    private boolean success;
    private String celebrationText;
    private int correctCount;
    private int totalQuestions;
    private int coinsEarned;
    private int trophiesEarned;
    private int totalCoins;
    private int totalTrophies;
    private List<BadgeResponseDTO> badges; // Now using your BadgeResponseDTO
    private boolean challengeCompleted;
    private double percentageScore;
    private Map<Long, Boolean> questionResults;
    private List<QuestionFeedbackDTO> questionFeedbacks;
    private String overallFeedback;
    private boolean firstCompletion;

    // Additional badge-related information
    private boolean earnedNewBadge;
    private List<String> newlyEarnedBadges;
    private String badgeCelebrationMessage;
}
