package com.superme.dto;

import com.superme.model.HabitCompletion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HabitMonthlyResponse {

    private Long habitId;
    private String habitName;
    private List<DayStatus> monthlyCompletion;

    @Data
    @AllArgsConstructor
    public static class DayStatus {
        private LocalDate date;
        private HabitCompletion.CompletionStatus status;
    }
}

