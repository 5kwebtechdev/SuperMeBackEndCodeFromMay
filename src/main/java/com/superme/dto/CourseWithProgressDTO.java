package com.superme.dto;

import com.superme.enums.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseWithProgressDTO {
    private Long id;
    private String courseName;
    private String description;
    private String category;
    private String difficulty;
    private Integer noOfLessons;
    private List<AgeGroup> ageGroups;
    private Integer duration;
    private Format format;
    private Integer totalCoins;
    private String thumbnailUrl;
    private Status status;

    // Progress fields
    private Integer completedLessons;
    private Double completionPercentage;
    private Boolean isCourseCompleted;
    private String progressStatus; // "NOT_STARTED", "IN_PROGRESS", "COMPLETED"
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    // Lessons with user progress
    private List<LessonWithProgressDTO> lessons;

    public String getProgressStatus() {
        if (Boolean.TRUE.equals(isCourseCompleted)) {
            return "COMPLETED";
        } else if (completedLessons != null && completedLessons > 0) {
            return "IN_PROGRESS";
        } else {
            return "NOT_STARTED";
        }
    }
}