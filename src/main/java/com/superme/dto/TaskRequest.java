package com.superme.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import com.superme.model.Task;
import lombok.Data;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

@Data
public class TaskRequest {
    // Task fields - all at root level
    private Long taskId;
    private String title;
    private String description;
    private Task.TaskStatus status = Task.TaskStatus.PENDING;
    private Task.Priority priority;  // Changed from String to Task.Priority
    private String routine;    // Changed from String to Task.Routine
    private List<String> tags;

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
    private int rewardCoins = 5;
    private Long forUserId; // For parent/child task actions
    private Boolean sharedWithParent;
}