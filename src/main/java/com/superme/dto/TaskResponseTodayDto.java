package com.superme.dto;

import lombok.Builder;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class TaskResponseTodayDto {

    // Habit info
    private Long id;
    private Long assignedTo;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String title;
    private String description;
    private String priority;
    private String routine;
    private List<String> tags;

    private LocalDate startDate;
    private LocalTime startTime;
    private LocalDate endDate;
    private LocalTime endTime;

    private Set<DayOfWeek> daysOfWeek;

    private Integer coinReward;
    private Boolean sharedWithParent;

    // Today's completion info
    private LocalDate completionDate;
    private LocalTime completionTime;
    private boolean isEveryday;
    private boolean isEveryWeekend;
    private String status;
    private boolean completed;
    private int coinsEarned;
    int currentTaskStreak;
    int highestTaskStreak;
    double completionPercentage;
    int totalScheduledDays;
    int totalCompletedDays;
    List<BadgeResponseDTO> milestoneBadges;
}
