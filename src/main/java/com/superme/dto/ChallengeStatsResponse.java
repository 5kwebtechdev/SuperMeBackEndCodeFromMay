package com.superme.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeStatsResponse {
    private long totalChallenges;
    private long totalChallengesPublished;
    private long totalDrafts;
    private long totalUnderReview;
    private double averageCompletionRate;
}
