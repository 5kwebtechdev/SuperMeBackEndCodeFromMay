package com.superme.dto;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for admin tutor overview containing data table, statistics, and
 * metadata
 */
public class AdminTutorOverviewResponseDTO {
    private List<AdminTutorDTO> tutors;
    private Map<String, Object> stats;
    private Map<String, Object> filterOptions;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;

    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================

    public AdminTutorOverviewResponseDTO() {
    }

    public AdminTutorOverviewResponseDTO(List<AdminTutorDTO> tutors,
            Map<String, Object> stats) {
        this.tutors = tutors;
        this.stats = stats;
        this.totalElements = tutors != null ? tutors.size() : 0;
        this.pageSize = 10; // default page size
        this.currentPage = 0; // default to first page
        updatePaginationInfo();
    }

    public AdminTutorOverviewResponseDTO(List<AdminTutorDTO> tutors,
            Map<String, Object> stats,
            Map<String, Object> filterOptions,
            int totalElements,
            int currentPage,
            int pageSize) {
        this.tutors = tutors;
        this.stats = stats;
        this.filterOptions = filterOptions;
        this.totalElements = totalElements;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        updatePaginationInfo();
    }

    // ============================================================================
    // GETTERS AND SETTERS
    // ============================================================================

    public List<AdminTutorDTO> getTutors() {
        return tutors;
    }

    public void setTutors(List<AdminTutorDTO> tutors) {
        this.tutors = tutors;
        this.totalElements = tutors != null ? tutors.size() : 0;
        updatePaginationInfo();
    }

    public Map<String, Object> getStats() {
        return stats;
    }

    public void setStats(Map<String, Object> stats) {
        this.stats = stats;
    }

    public Map<String, Object> getFilterOptions() {
        return filterOptions;
    }

    public void setFilterOptions(Map<String, Object> filterOptions) {
        this.filterOptions = filterOptions;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
        updatePaginationInfo();
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
        updatePaginationInfo();
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
        updatePaginationInfo();
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }

    // ============================================================================
    // DERIVED GETTERS
    // ============================================================================

    private void updatePaginationInfo() {
        this.totalPages = (int) Math.ceil((double) totalElements / pageSize);
        this.hasNext = currentPage < totalPages - 1;
        this.hasPrevious = currentPage > 0;
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    @Override
    public String toString() {
        return "AdminTutorOverviewResponseDTO{" +
                "tutorsCount=" + (tutors != null ? tutors.size() : 0) +
                ", stats=" + stats +
                ", filterOptions=" + filterOptions +
                ", totalElements=" + totalElements +
                ", totalPages=" + totalPages +
                ", currentPage=" + currentPage +
                ", pageSize=" + pageSize +
                ", hasNext=" + hasNext +
                ", hasPrevious=" + hasPrevious +
                '}';
    }
}
