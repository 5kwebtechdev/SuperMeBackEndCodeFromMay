package com.superme.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserStreakDTO {
    private int rank;
    private String name;
    private int currentStreak;
    private String avatarImageName;
    private String avatarName;
}