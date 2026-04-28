package com.superme.dto;

import java.util.List;

/**
 * Response wrapper for reels overview data including reels list, statistics,
 * and filter information.
 */
public class AdminReelsOverviewResponse {
    private List<AdminReelsDTO> reels;
    private AdminReelsDTO.ReelsStatistics statistics;
    private AdminReelsDTO.ReelsFilterCriteria filterCriteria;
    private int totalCount;
    private int filteredCount;
    private boolean hasMore;
    private String message;
    private boolean success;

    // Username field and logic have been removed from DTOs and responses

    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================

    public AdminReelsOverviewResponse() {
        this.success = true;
        this.hasMore = false;
    }

    public AdminReelsOverviewResponse(List<AdminReelsDTO> reels,
            AdminReelsDTO.ReelsStatistics statistics) {
        this.reels = reels;
        this.statistics = statistics;
        this.totalCount = reels != null ? reels.size() : 0;
        this.filteredCount = this.totalCount;
        this.hasMore = false;
        this.success = true;
    }

    public AdminReelsOverviewResponse(List<AdminReelsDTO> reels,
            AdminReelsDTO.ReelsStatistics statistics,
            AdminReelsDTO.ReelsFilterCriteria filterCriteria) {
        this.reels = reels;
        this.statistics = statistics;
        this.filterCriteria = filterCriteria;
        this.totalCount = reels != null ? reels.size() : 0;
        this.filteredCount = this.totalCount;
        this.hasMore = false;
        this.success = true;
    }

    // ============================================================================
    // GETTERS AND SETTERS
    // ============================================================================

    public List<AdminReelsDTO> getReels() {
        return reels;
    }

    public void setReels(List<AdminReelsDTO> reels) {
        this.reels = reels;
        this.filteredCount = reels != null ? reels.size() : 0;
    }

    public AdminReelsDTO.ReelsStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(AdminReelsDTO.ReelsStatistics statistics) {
        this.statistics = statistics;
    }

    public AdminReelsDTO.ReelsFilterCriteria getFilterCriteria() {
        return filterCriteria;
    }

    public void setFilterCriteria(AdminReelsDTO.ReelsFilterCriteria filterCriteria) {
        this.filterCriteria = filterCriteria;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getFilteredCount() {
        return filteredCount;
    }

    public void setFilteredCount(int filteredCount) {
        this.filteredCount = filteredCount;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    public boolean hasFiltersApplied() {
        return filterCriteria != null && filterCriteria.hasFilters();
    }

    public int getFilteredOutCount() {
        return totalCount - filteredCount;
    }

    public boolean hasResults() {
        return reels != null && !reels.isEmpty();
    }

    public boolean isFiltered() {
        return filteredCount != totalCount;
    }

    @Override
    public String toString() {
        return "AdminReelsOverviewResponse{" +
                "reelsCount=" + (reels != null ? reels.size() : 0) +
                ", totalCount=" + totalCount +
                ", filteredCount=" + filteredCount +
                ", hasFilters=" + hasFiltersApplied() +
                ", success=" + success +
                '}';
    }
}
