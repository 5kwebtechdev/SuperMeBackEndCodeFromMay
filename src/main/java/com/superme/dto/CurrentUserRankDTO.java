package com.superme.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CurrentUserRankDTO {
    private String avatarImageName;
    private int rank;
    private int currentStreak;
    private int highestStreak;
    private Integer totalActiveDays;
    private boolean hasStreak;
    private String petMessage;
    private String name;
    private String avatarName;
}
