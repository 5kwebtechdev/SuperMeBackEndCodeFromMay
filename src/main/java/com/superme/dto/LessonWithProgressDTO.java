package com.superme.dto;

import com.superme.enums.LessonStatus;
import com.superme.enums.Status;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonWithProgressDTO {
    private Long id;
    private String title;
    private String description;
    private Integer duration; // in minutes
    private Integer order;
    private String videoUrl;
    private String thumbnailUrl;
    private String content;
    private LessonStatus lessonStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // User progress fields
    private Boolean isCompleted;
    private LocalDateTime completedAt;
    private Integer timeSpentMinutes;
    private LocalDateTime lastAccessedAt;
    private Boolean isLocked;
}