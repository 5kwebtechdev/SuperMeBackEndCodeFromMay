package com.superme.dto;

import com.superme.model.Task;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDTO {
    private Long id;
    private String title;
    private String description;
    private LocalDate completionDate;

    // Use the inner enums from Task class
    private Task.Priority priority;
    private String routine;

    private LocalDate startDate;
    private LocalTime startTime;
    private LocalDate endDate;
    private LocalTime endTime;
    private Set<DayOfWeek> daysOfWeek;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Use Task.TaskStatus
    private String status;

    private Integer rewardCoins;
    private Boolean sharedWithParent;
    private boolean isEveryday;
    private boolean isEveryWeekend;
    private List<String> tags;

    // Simplified user info (to avoid circular reference)
    private Long createdBy;
    private Long assignedTo;
    private Integer currentTaskStreak;
    private Integer highestTaskStreak;
}