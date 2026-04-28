package com.superme.dto;

import java.util.List;

/**
 * Response DTO for admin article overview containing data table, statistics, and filter options
 */
public class AdminArticleOverviewResponseDTO {
    // Data Table Content
    private List<AdminArticleDTO> articles;
    
    // Pagination Information
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;
    
    // Statistics for Stat Cards
    private ArticleStatistics statistics;
    
    // Filter Options for UI
    private FilterOptions filterOptions;

    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================

    public AdminArticleOverviewResponseDTO() {
        this.statistics = new ArticleStatistics();
        this.filterOptions = new FilterOptions();
    }

    // ============================================================================
    // DATA TABLE GETTERS AND SETTERS
    // ============================================================================

    public List<AdminArticleDTO> getArticles() { return articles; }
    public void setArticles(List<AdminArticleDTO> articles) { this.articles = articles; }

    // ============================================================================
    // PAGINATION GETTERS AND SETTERS
    // ============================================================================

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public boolean isHasNext() { return hasNext; }
    public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }

    public boolean isHasPrevious() { return hasPrevious; }
    public void setHasPrevious(boolean hasPrevious) { this.hasPrevious = hasPrevious; }

    // ============================================================================
    // STATISTICS GETTERS AND SETTERS
    // ============================================================================

    public ArticleStatistics getStatistics() { return statistics; }
    public void setStatistics(ArticleStatistics statistics) {
        this.statistics = statistics != null ? statistics : new ArticleStatistics();
    }

    // ============================================================================
    // FILTER OPTIONS GETTERS AND SETTERS
    // ============================================================================

    public FilterOptions getFilterOptions() { return filterOptions; }
    public void setFilterOptions(FilterOptions filterOptions) { 
        this.filterOptions = filterOptions != null ? filterOptions : new FilterOptions(); 
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    public boolean isEmpty() {
        return articles == null || articles.isEmpty();
    }

    public int getArticleCount() {
        return articles != null ? articles.size() : 0;
    }

    public boolean isFirstPage() {
        return currentPage == 0;
    }

    public boolean isLastPage() {
        return currentPage >= totalPages - 1;
    }

    // ============================================================================
    // NESTED CLASSES
    // ============================================================================

    /**
     * Filter options available for the admin panel
     */
    public static class FilterOptions {
        private List<String> ageGroups;
        private List<String> durationCategories;
        private List<String> statuses;

        public FilterOptions() {}

        public FilterOptions(List<String> ageGroups, List<String> durationCategories, List<String> statuses) {
            this.ageGroups = ageGroups;
            this.durationCategories = durationCategories;
            this.statuses = statuses;
        }

        public List<String> getAgeGroups() { return ageGroups; }
        public void setAgeGroups(List<String> ageGroups) { this.ageGroups = ageGroups; }

        public List<String> getDurationCategories() { return  durationCategories; }
        public void setDurationCategories(List<String> durationCategories) { this.durationCategories = durationCategories; }

        public List<String> getStatuses() { return statuses; }
        public void setStatuses(List<String> statuses) { this.statuses = statuses; }
    }

    @Override
    public String toString() {
        return "AdminArticleOverviewResponseDTO{" +
                "articleCount=" + getArticleCount() +
                ", totalElements=" + totalElements +
                ", currentPage=" + currentPage +
                ", totalPages=" + totalPages +
                ", hasNext=" + hasNext +
                ", hasPrevious=" + hasPrevious +
                '}';
    }
}