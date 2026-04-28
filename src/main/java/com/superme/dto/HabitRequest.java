package com.superme.dto;

import com.superme.model.Habit;
import lombok.Data;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Data
public class HabitRequest {
    // Habit fields - all at root level
    private Long id;
    private String title;
    private String description;
    private Habit.HabitStatus status = Habit.HabitStatus.PENDING;
    private Habit.Priority priority;
    private String routine;
    private Set<String> tags;

    // Date and time fields
    private LocalDate startDate;
    private LocalTime startTime;
    private LocalDate endDate;
    private LocalTime endTime;

    // Recurrence settings
    private boolean isEveryday;
    private boolean isEveryWeekend;
    private Set<DayOfWeek> daysOfWeek;

    // Reward and assignment
    private int coinReward = 5;
    private Long forUserId; // For parent/child habit actions
    private Boolean sharedWithParent = true;

    public HabitRequest() {
    }

    // Constructor with all fields
    public HabitRequest(String title, String description, Habit.HabitStatus status, Habit.Priority priority,
                        String routine, Set<String> tags, LocalDate startDate, LocalTime startTime,
                        LocalDate endDate, LocalTime endTime, boolean isEveryday, boolean isEveryWeekend,
                        Set<DayOfWeek> daysOfWeek, int coinReward, Long forUserId) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.routine = routine;
        this.tags = tags;
        this.startDate = startDate;
        this.startTime = startTime;
        this.endDate = endDate;
        this.endTime = endTime;
        this.isEveryday = isEveryday;
        this.isEveryWeekend = isEveryWeekend;
        this.daysOfWeek = daysOfWeek;
        this.coinReward = coinReward;
        this.forUserId = forUserId;
    }
}