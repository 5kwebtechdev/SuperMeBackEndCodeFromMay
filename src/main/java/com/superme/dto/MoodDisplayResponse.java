package com.superme.dto;

import com.superme.enums.Relationship;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoodDisplayResponse {

    private Long assignedTo;
    private String userName;

    private Relationship relationship;
    private String avatarImageName;

    private String mood; // e.g., "VERY_HAPPY"
    private String audience; // e.g., "SELF" or "PARENT"

    private String title; // e.g., "You're glowing with happiness!"
    private String description; // Final computed description with counts
    private String emptyStateDescription; // If no tasks/habits
    private String completedStateDescription; // If all completed

    private int totalHabits; // total habits for the day
    private int completedHabits; // completed habits for the day

    private int totalTasks; // total tasks for the day
    private int completedTasks; // completed tasks for the day

    private double progressPercentage; // e.g., 75.0

    private int coins;
    private int trophies;
    private int streak;
    private int badges;

    // 🆕 Parent-only fields
    private Double completionRate;      // Percentage (e.g., 80.0)
    private Double consistencyRate;     // Percentage (e.g., 60.0)
    private Double improvementRate;     // Percentage (e.g., 45.0, can be negative)
    private Integer appScore;           // 0-100 score (e.g., 74)

}
