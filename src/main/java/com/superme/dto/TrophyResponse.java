package com.superme.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrophyResponse {
    private String title;
    private Integer trophyCount;
    private String earnedDate;
    private String formattedEarnedDate; // e.g., "Earned on 2nd Mar"
    private String contentType;
    private String difficultyLevel;
    private String challengeId;
    private String categoryIconUrl;

    public static TrophyResponse fromEntity(com.superme.model.Trophy trophy) {
        DateTimeFormatter shortFormatter = DateTimeFormatter.ofPattern("d MMM");
        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("d'%s' MMM");

        String day = trophy.getEarnedDate().getDayOfMonth() + "";
        String suffix = getDayOfMonthSuffix(trophy.getEarnedDate().getDayOfMonth());
        String formattedDate = String.format("Earned on %s%s %s",
                day, suffix, trophy.getEarnedDate().format(DateTimeFormatter.ofPattern("MMM")));

        return TrophyResponse.builder()
                .title(trophy.getTitle())
                .trophyCount(trophy.getTrophyCount())
                .earnedDate(trophy.getEarnedDate().format(shortFormatter))
                .formattedEarnedDate(formattedDate)
                .contentType(trophy.getCategory().getDisplayName())
                .difficultyLevel(trophy.getDifficulty().getDisplayName())
                .challengeId(trophy.getChallengeId())
                .categoryIconUrl(trophy.getCategoryIconUrl())
                .build();
    }

    private static String getDayOfMonthSuffix(int day) {
        if (day >= 11 && day <= 13) {
            return "th";
        }
        switch (day % 10) {
            case 1: return "st";
            case 2: return "nd";
            case 3: return "rd";
            default: return "th";
        }
    }
}