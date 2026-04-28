package com.superme.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.List;

import com.superme.model.Task;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private boolean completed;
    private Long id;
    private String title;
    private String description;
    private Task.TaskStatus status;
    private String priority;
    private String routine;
    private LocalDate startDate;
    private LocalTime startTime;
    private LocalDate endDate;
    private LocalTime endTime;
    private Long createdBy;
    private Long assignedTo;
    private Long familyId;
    private Set<DayOfWeek> daysOfWeek;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer rewardCoins;
    private Boolean sharedWithParent;
    private boolean isEveryday;
    private boolean isEveryWeekend;
    private List<String> tags;

    private int currentTaskStreak;
    private int highestTaskStreak;
    private int coinsEarned;
    private double completionPercentage;
    private int totalScheduledDays;
    private int totalCompletedDays;

    // Limited badges (only 4 as shown in the image)
    private List<BadgeResponseDTO> milestoneBadges;
}