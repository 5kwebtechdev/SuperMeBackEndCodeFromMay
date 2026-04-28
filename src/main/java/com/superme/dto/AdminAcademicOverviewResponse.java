package com.superme.dto;

import com.superme.enums.AgeGroup;
import com.superme.enums.Status;
import lombok.Data;

import java.util.List;

/**
 * Response DTO for Academic Overview with data table and statistics.
 */
@Data
public class AdminAcademicOverviewResponse {
    
    private List<AdminCourseDTO> courses;
    private AcademicStatistics statistics;
    private AcademicFilterCriteria filterCriteria;
    private Integer totalCount;
    private Integer filteredCount;
    private Boolean hasMore;
    private Boolean success;


    public AdminAcademicOverviewResponse(List<AdminCourseDTO> courses, AcademicStatistics statistics, AcademicFilterCriteria filterCriteria) {
        this.courses = courses;
        this.statistics = statistics;
        this.filterCriteria = filterCriteria;
        this.success = true;
    }

    // ============================================================================
    // NESTED CLASSES
    // ============================================================================


    @Data
    public static class AcademicFilterCriteria {
        private String searchTerm;
        private String difficulty;
        private AgeGroup ageGroup;
        private Status status;
        private String category;
    }
}
