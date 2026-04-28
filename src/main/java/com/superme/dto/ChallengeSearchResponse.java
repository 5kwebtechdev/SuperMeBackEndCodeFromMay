package com.superme.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeSearchResponse {
    private List<ChallengeDTO> challenges;
    private long totalChallenges;
    private long completedChallenges;
    private double completionRate;
    private long totalCoins;
    private long totalTrophies;
}