package com.superme.dto;

import com.superme.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.List;

/**
 * Request DTO for creating a new course with lessons.
 */
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateCourseRequest {

    private Long id;
    
    @NotBlank(message = "Course name is required")
    private String courseName;
    
    private String description;
    
    @NotBlank(message = "Category is required")
    private CourseCategory category;
    
    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer duration;
    
    @NotNull(message = "Number of lessons is required")
    @Min(value = 1, message = "Must have at least 1 lesson")
    private Integer noOfLessons;
    
    @NotBlank(message = "Difficulty is required")
    private CourseDifficulty difficulty;
    
    @NotBlank(message = "Format is required")
    private Format format;

    @NotBlank(message = "total coins are required")
    private Integer totalCoins;
    
    @NotBlank(message = "Age group is required")
    private AgeGroup ageGroup;
    
    private String thumbnailUrl;
    
    @NotBlank(message = "Status is required")
    private Status status; // DRAFT or VERIFICATION_PENDING
    
    private List<LessonRequest> lessons;

    public List<LessonRequest> getLessons() { return lessons; }

    // ============================================================================
    // NESTED CLASS
    // ============================================================================

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LessonRequest {

        private Long id;

        @NotBlank(message = "Lesson title is required")
        private String lessonTitle;
        
        private String lessonDescription;
        
        @NotBlank(message = "Format is required")
        private Format format;

        @NotBlank(message = "coins are required")
        private Integer coins;
        
        @NotNull(message = "Duration is required")
        @Min(value = 1, message = "Duration must be at least 1 minute")
        private Integer duration;
        
        private String thumbnailUrl;
        
        @NotBlank(message = "Content is required")
        private String content;
        
        private Integer lessonOrder;
    }
}
