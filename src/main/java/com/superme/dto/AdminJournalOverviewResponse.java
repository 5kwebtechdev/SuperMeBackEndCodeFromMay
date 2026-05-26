package com.superme.dto;

import java.util.List;

/**
 * Enhanced response DTO for admin journal overview operations.
 * Contains user journal data, global statistics,
 * filter metadata, and search information.
 */
public class AdminJournalOverviewResponse {
    
    private List<AdminJournalViewDTO> users;
    private AdminJournalViewDTO.JournalStatistics statistics;
    private AdminJournalViewDTO.JournalFilterCriteria appliedFilters;
    private int totalFilteredCount;
    private int originalCount;
    private String searchTerm;
    private boolean hasFiltersApplied;

    // Pagination
    private int currentPage;
    private int pageSize;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
    
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    public AdminJournalOverviewResponse() {
    }
    
    public AdminJournalOverviewResponse(List<AdminJournalViewDTO> users, AdminJournalViewDTO.JournalStatistics statistics) {
        this.users = users;
        this.statistics = statistics;
        this.totalFilteredCount = users != null ? users.size() : 0;
        this.originalCount = totalFilteredCount;
        this.hasFiltersApplied = false;
    }
    
    public AdminJournalOverviewResponse(List<AdminJournalViewDTO> users, 
                                      AdminJournalViewDTO.JournalStatistics statistics,
                                      AdminJournalViewDTO.JournalFilterCriteria appliedFilters,
                                      int originalCount) {
        this.users = users;
        this.statistics = statistics;
        this.appliedFilters = appliedFilters;
        this.totalFilteredCount = users != null ? users.size() : 0;
        this.originalCount = originalCount;
        this.hasFiltersApplied = appliedFilters != null && appliedFilters.hasFilters();
        this.searchTerm = appliedFilters != null ? appliedFilters.getSearchTerm() : null;
    }
    
    // ============================================================================
    // GETTERS AND SETTERS
    // ============================================================================
    
    public List<AdminJournalViewDTO> getUsers() {
        return users;
    }
    
    public void setUsers(List<AdminJournalViewDTO> users) {
        this.users = users;
        this.totalFilteredCount = users != null ? users.size() : 0;
    }
    
    public AdminJournalViewDTO.JournalStatistics getStatistics() {
        return statistics;
    }
    
    public void setStatistics(AdminJournalViewDTO.JournalStatistics statistics) {
        this.statistics = statistics;
    }
    
    public AdminJournalViewDTO.JournalFilterCriteria getAppliedFilters() {
        return appliedFilters;
    }
    
    public void setAppliedFilters(AdminJournalViewDTO.JournalFilterCriteria appliedFilters) {
        this.appliedFilters = appliedFilters;
        this.hasFiltersApplied = appliedFilters != null && appliedFilters.hasFilters();
        this.searchTerm = appliedFilters != null ? appliedFilters.getSearchTerm() : null;
    }
    
    public int getTotalFilteredCount() {
        return totalFilteredCount;
    }
    
    public void setTotalFilteredCount(int totalFilteredCount) {
        this.totalFilteredCount = totalFilteredCount;
    }
    
    public int getOriginalCount() {
        return originalCount;
    }
    
    public void setOriginalCount(int originalCount) {
        this.originalCount = originalCount;
    }
    
    public String getSearchTerm() {
        return searchTerm;
    }
    
    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }
    
    public boolean isHasFiltersApplied() {
        return hasFiltersApplied;
    }
    
    public void setHasFiltersApplied(boolean hasFiltersApplied) {
        this.hasFiltersApplied = hasFiltersApplied;
    }
    
    // ============================================================================
    // DERIVED GETTERS
    // ============================================================================
    
    public boolean hasSearchApplied() {
        return searchTerm != null && !searchTerm.trim().isEmpty();
    }
    
    public int getFilteredOutCount() {
        return originalCount - totalFilteredCount;
    }
    
    public double getFilterRetentionRate() {
        return originalCount > 0 ? (double) totalFilteredCount / originalCount * 100 : 100.0;
    }
    
    public String getFormattedFilterRetentionRate() {
        return String.format("%.1f%%", getFilterRetentionRate());
    }
    
    public boolean hasUsers() {
        return users != null && !users.isEmpty();
    }
    
    public int getUserCount() {
        return users != null ? users.size() : 0;
    }
    
    // ============================================================================
    // SEARCH AND FILTER METADATA
    // ============================================================================
    
    public String getFilterSummary() {
        if (!hasFiltersApplied) {
            return "No filters applied";
        }
        
        StringBuilder summary = new StringBuilder();
        
        if (hasSearchApplied()) {
            summary.append("Search: '").append(searchTerm).append("' ");
        }
        
        if (appliedFilters != null) {
            if (appliedFilters.getMinJournalEntries() != null || appliedFilters.getMaxJournalEntries() != null) {
                summary.append("Journal entries range: ");
                if (appliedFilters.getMinJournalEntries() != null) {
                    summary.append("≥").append(appliedFilters.getMinJournalEntries());
                }
                if (appliedFilters.getMaxJournalEntries() != null) {
                    if (appliedFilters.getMinJournalEntries() != null) summary.append(" and ");
                    summary.append("≤").append(appliedFilters.getMaxJournalEntries());
                }
                summary.append(" ");
            }
            
            if (appliedFilters.getEngagementLevel() != null) {
                summary.append("Engagement: ").append(appliedFilters.getEngagementLevel()).append(" ");
            }
            
            if (appliedFilters.getUserType() != null) {
                summary.append("Type: ").append(appliedFilters.getUserType()).append(" ");
            }
            
            if (appliedFilters.getIsActive() != null) {
                summary.append("Active: ").append(appliedFilters.getIsActive()).append(" ");
            }
        }
        
        return summary.toString().trim();
    }
    
    public String getResultSummary() {
        StringBuilder summary = new StringBuilder();
        
        summary.append("Showing ").append(totalFilteredCount).append(" of ").append(originalCount).append(" users");
        
        if (hasFiltersApplied) {
            summary.append(" (").append(getFilteredOutCount()).append(" filtered out)");
        }
        
        return summary.toString();
    }
    
    // ============================================================================
    // ANALYTICS METHODS
    // ============================================================================
    
    public String getEngagementDistribution() {
        if (!hasUsers()) {
            return "No users to analyze";
        }
        
        long highEngagement = users.stream().filter(AdminJournalViewDTO::isHighlyEngaged).count();
        long activeUsers = users.stream().filter(AdminJournalViewDTO::isActiveUser).count();
        long usersWithEntries = users.stream().filter(AdminJournalViewDTO::hasJournalEntries).count();
        long usersNeedingAttention = users.stream().filter(AdminJournalViewDTO::needsAttention).count();
        
        return String.format("High engagement: %d, Active: %d, With entries: %d, Need attention: %d", 
                           highEngagement, activeUsers, usersWithEntries, usersNeedingAttention);
    }
    
    public double getAverageActivityScore() {
        if (!hasUsers()) {
            return 0.0;
        }
        
        return users.stream()
                .mapToDouble(AdminJournalViewDTO::getActivityScore)
                .average()
                .orElse(0.0);
    }
    
    public String getFormattedAverageActivityScore() {
        return String.format("%.1f", getAverageActivityScore());
    }
    
    // ============================================================================
    // SEARCH RESULT METADATA
    // ============================================================================
    
    public boolean isSearchResult() {
        return hasSearchApplied();
    }
    
    public String getSearchResultSummary() {
        if (!isSearchResult()) {
            return "All users displayed";
        }
        
        return String.format("Search results for '%s': %d users found", searchTerm, totalFilteredCount);
    }
    
    public boolean hasSearchResults() {
        return isSearchResult() && hasUsers();
    }
    
    public boolean isEmptySearchResult() {
        return isSearchResult() && !hasUsers();
    }
    
    public String getSearchFields() {
        return "userId, name";
    }
    
    // ============================================================================
    // PAGINATION GETTERS AND SETTERS
    // ============================================================================

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public boolean isHasNext() { return hasNext; }
    public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }

    public boolean isHasPrevious() { return hasPrevious; }
    public void setHasPrevious(boolean hasPrevious) { this.hasPrevious = hasPrevious; }

    // ============================================================================
    // PAGINATION SUPPORT
    // ============================================================================

    public boolean supportsPagination() {
        return totalFilteredCount > 20; // Default page size threshold
    }
    
    public int getRecommendedPageSize() {
        if (totalFilteredCount <= 20) return totalFilteredCount;
        if (totalFilteredCount <= 50) return 20;
        if (totalFilteredCount <= 100) return 25;
        return 50;
    }
    
    public String getPaginationRecommendation() {
        if (!supportsPagination()) {
            return "No pagination needed";
        }
        
        return String.format("Recommend using pagination with page size %d for %d users", 
                           getRecommendedPageSize(), totalFilteredCount);
    }
    
    // ============================================================================
    // UTILITY METHODS
    // ============================================================================
    
    @Override
    public String toString() {
        return "AdminJournalOverviewResponse{" +
                "usersCount=" + getUserCount() +
                ", originalCount=" + originalCount +
                ", filteredCount=" + totalFilteredCount +
                ", hasFilters=" + hasFiltersApplied +
                ", searchTerm='" + searchTerm + '\'' +
                ", statistics=" + statistics +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        AdminJournalOverviewResponse that = (AdminJournalOverviewResponse) o;
        
        if (totalFilteredCount != that.totalFilteredCount) return false;
        if (originalCount != that.originalCount) return false;
        if (hasFiltersApplied != that.hasFiltersApplied) return false;
        if (users != null ? !users.equals(that.users) : that.users != null) return false;
        if (statistics != null ? !statistics.equals(that.statistics) : that.statistics != null) return false;
        if (appliedFilters != null ? !appliedFilters.equals(that.appliedFilters) : that.appliedFilters != null)
            return false;
        return searchTerm != null ? searchTerm.equals(that.searchTerm) : that.searchTerm == null;
    }
    
    @Override
    public int hashCode() {
        int result = users != null ? users.hashCode() : 0;
        result = 31 * result + (statistics != null ? statistics.hashCode() : 0);
        result = 31 * result + (appliedFilters != null ? appliedFilters.hashCode() : 0);
        result = 31 * result + totalFilteredCount;
        result = 31 * result + originalCount;
        result = 31 * result + (searchTerm != null ? searchTerm.hashCode() : 0);
        result = 31 * result + (hasFiltersApplied ? 1 : 0);
        return result;
    }
    
    // ============================================================================
    // BUILDER PATTERN SUPPORT
    // ============================================================================
    
    public static class Builder {
        private List<AdminJournalViewDTO> users;
        private AdminJournalViewDTO.JournalStatistics statistics;
        private AdminJournalViewDTO.JournalFilterCriteria appliedFilters;
        private int originalCount;
        
        public Builder users(List<AdminJournalViewDTO> users) {
            this.users = users;
            return this;
        }
        
        public Builder statistics(AdminJournalViewDTO.JournalStatistics statistics) {
            this.statistics = statistics;
            return this;
        }
        
        public Builder appliedFilters(AdminJournalViewDTO.JournalFilterCriteria appliedFilters) {
            this.appliedFilters = appliedFilters;
            return this;
        }
        
        public Builder originalCount(int originalCount) {
            this.originalCount = originalCount;
            return this;
        }
        
        public AdminJournalOverviewResponse build() {
            return new AdminJournalOverviewResponse(users, statistics, appliedFilters, originalCount);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}
