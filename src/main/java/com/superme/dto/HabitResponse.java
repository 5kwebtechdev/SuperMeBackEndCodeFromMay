package com.superme.dto;

import com.superme.model.Habit;
import lombok.Builder;
import lombok.Data;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * DTO for Habit API responses with comprehensive metrics.
 */
@Data
@Builder
public class HabitResponse {
    // Basic habit fields
    private boolean completed;
    private Long id;
    private String title;
    private String description;
    private Habit.HabitStatus status;
    private String priority;
    private String routine;
    private java.time.LocalDate startDate;
    private LocalTime startTime;
    private java.time.LocalDate endDate;
    private LocalTime endTime;
    private Long createdBy;
    private Long assignedTo;
    private Long familyId;
    private Set<DayOfWeek> daysOfWeek;
    private java.time.LocalDate createdAt;
    private java.time.LocalDate updatedAt;
    private boolean isEveryday;
    private boolean isEveryWeekend;
    private int coinReward;
    private Set<String> tags;
    private Boolean sharedWithParent;

    // Individual Habit Metrics (from Metrics Documentation - Section 8-11)
    private int currentHabitStreak; // Current streak for this specific habit
    private int highestHabitStreak; // Best streak ever for this habit
    private int coinsEarned; // Total coins earned from this habit
    private double completionPercentage; // Completion rate for this specific habit
    private int totalScheduledDays; // Total days this habit was scheduled
    private int totalCompletedDays; // Total days this habit was actually completed

    // Limited badges (only 4 as shown in the image)
    private List<BadgeResponseDTO> milestoneBadges;



    public HabitResponse() {
    }

    // Full constructor
    // Full constructor - CORRECTED
    public HabitResponse(boolean completed, Long id, String title, String description, Habit.HabitStatus status,
                         String priority, String routine,
                         java.time.LocalDate startDate, LocalTime startTime, java.time.LocalDate endDate, LocalTime endTime,
                         Long createdBy, Long assignedTo, Long familyId, Set<DayOfWeek> daysOfWeek,
                         java.time.LocalDate createdAt, java.time.LocalDate updatedAt,
                         boolean isEveryday, boolean isEveryWeekend, int coinReward, Set<String> tags,
                         Boolean sharedWithParent, // Changed from boolean to Boolean
                         int currentHabitStreak, int highestHabitStreak, int coinsEarned, double completionPercentage,
                         int totalScheduledDays, int totalCompletedDays, List<BadgeResponseDTO> milestoneBadges) {

        this.completed = completed;
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.routine = routine;
        this.startDate = startDate;
        this.startTime = startTime;
        this.endDate = endDate;
        this.endTime = endTime;
        this.createdBy = createdBy;
        this.assignedTo = assignedTo;
        this.familyId = familyId;
        this.daysOfWeek = daysOfWeek;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isEveryday = isEveryday;
        this.isEveryWeekend = isEveryWeekend;
        this.coinReward = coinReward;
        this.tags = tags;
        this.sharedWithParent = sharedWithParent; // Now accepts null
        this.currentHabitStreak = currentHabitStreak;
        this.highestHabitStreak = highestHabitStreak;
        this.coinsEarned = coinsEarned;
        this.completionPercentage = completionPercentage;
        this.totalScheduledDays = totalScheduledDays;
        this.totalCompletedDays = totalCompletedDays;
        this.milestoneBadges = milestoneBadges;
    }

    // Convenience constructor from Habit entity with metrics calculation
    public HabitResponse(Habit habit) {
        this.id = habit.getId();
        this.title = habit.getTitle();
        this.description = habit.getDescription();
        this.status = habit.getStatus();
        this.priority = habit.getPriority() != null ? habit.getPriority().name() : null;
        this.routine = habit.getRoutine();
        this.startDate = habit.getStartDate();
        this.startTime = habit.getStartTime();
        this.endDate = habit.getEndDate();
        this.endTime = habit.getEndTime();
        this.createdBy = habit.getCreatedBy().getId();
        this.assignedTo = habit.getAssignedTo().getId();
        this.familyId = habit.getFamily() != null ? habit.getFamily().getId() : null;
        this.daysOfWeek = habit.getDaysOfWeek();
        this.createdAt = habit.getCreatedAt();
        this.updatedAt = habit.getUpdatedAt();
        this.isEveryday = habit.isEveryday();
        this.isEveryWeekend = habit.isEveryWeekend();
        this.coinReward = habit.getCoinReward();
        this.tags = habit.getTags();
        this.sharedWithParent = habit.getSharedWithParent();
        this.completed = habit.getStatus() == Habit.HabitStatus.COMPLETED;

        // Initialize metrics with default values
        this.currentHabitStreak = 0;
        this.highestHabitStreak = 0;
        this.coinsEarned = 0;
        this.completionPercentage = 0.0;
        this.totalScheduledDays = 0;
        this.totalCompletedDays = 0;
        this.milestoneBadges = List.of();
    }
}