package com.superme.dto;

import com.superme.model.Habit;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HabitDTO {
    private Long id;
    private String title;
    private String description;
    private LocalDate completionDate;

    // Use the inner enums from Task class
    private Habit.Priority priority;
    private String routine;

    private LocalDate startDate;
    private LocalTime startTime;
    private LocalDate endDate;
    private LocalTime endTime;
    private Set<DayOfWeek> daysOfWeek;
    private LocalDate createdAt;
    private LocalDate updatedAt;

    // Use Task.TaskStatus
    private String status;

    private Integer rewardCoins;
    private boolean sharedWithParents;
    private boolean isEveryday;
    private boolean isEveryWeekend;
    private Set<String> tags;

    // Simplified user info (to avoid circular reference)
    private Long createdBy;
    private Long assignedTo;
    private Integer currentHabitStreak;
    private Integer highestHabitStreak;
}
