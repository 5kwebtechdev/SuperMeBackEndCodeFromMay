package com.superme.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class LeaderboardResponse {
    private CurrentUserRankDTO currentUser;
    private List<UserStreakDTO> topUsers;
}
