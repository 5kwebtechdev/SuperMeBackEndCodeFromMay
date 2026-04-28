package com.superme.dto;

import com.superme.model.User;
import com.superme.service.UserService;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class StreakStatusDto {
    private int currentStreak;
    private int highestStreak;

    private boolean canRestoreStreak;
    private LocalDate restoreDeadline;
    private Integer restoreToStreak;

    private int restoreCost;
}
