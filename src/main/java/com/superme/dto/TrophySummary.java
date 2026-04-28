package com.superme.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrophySummary {
    private Integer totalTrophies;
    private List<TrophyResponse> recentTrophies;
    private String message; // e.g., "See wins + Know dates + Keep going"
}