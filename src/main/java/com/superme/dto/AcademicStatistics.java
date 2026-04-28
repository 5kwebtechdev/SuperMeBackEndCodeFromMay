package com.superme.dto;

import lombok.Data;

@Data
public class AcademicStatistics {
    private Long totalCourses;
    private Long totalLessons;
    private Long totalCategories;
    private Double averageDuration;
    private Long coursesInDraft;
    private Long coursesPublished;
    private Long coursesPending;
}
