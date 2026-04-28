package com.superme.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class HabitSearchResponse {
    private List<HabitResponse> habits; // Changed from List<Habit> to List<HabitResponse>
    private long totalHabits;
    private long totalHabitsCompleted;
    private double habitCompletionRate;

    // Updated constructor to accept List<HabitResponse>
    public HabitSearchResponse(List<HabitResponse> habits, long totalHabitsCompleted, long totalHabits) {
        this.habits = habits;
        this.totalHabits = totalHabits;
        this.totalHabitsCompleted = totalHabitsCompleted;
        this.habitCompletionRate = totalHabits == 0 ? 0.0 : ((double) totalHabitsCompleted / totalHabits) * 100.0;
    }
}