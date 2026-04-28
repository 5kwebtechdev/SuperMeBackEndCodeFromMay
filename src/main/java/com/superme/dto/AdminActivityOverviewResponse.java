package com.superme.dto;

import java.util.List;
import java.time.LocalDateTime;

/**
 * Enhanced response DTO for admin activity overview operations.
 * Contains user activity data, global statistics, filter metadata, and search
 * information.
 */
public class AdminActivityOverviewResponse {
    private List<AdminActivityOverviewDTO> users;
    private AdminActivityOverviewDTO.ActivityOverviewStatistics globalStatistics;
    private AdminActivityOverviewDTO.ActivityFilterCriteria appliedFilters;
    private int totalFilteredCount;
    private int originalCount;
    private String searchTerm;
    private boolean hasFiltersApplied;
    private LocalDateTime timestamp;

    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================

    public AdminActivityOverviewResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public AdminActivityOverviewResponse(List<AdminActivityOverviewDTO> users,
            AdminActivityOverviewDTO.ActivityOverviewStatistics globalStatistics) {
        this.users = users;
        this.globalStatistics = globalStatistics;
        this.totalFilteredCount = users != null ? users.size() : 0;
        this.originalCount = totalFilteredCount;
        this.hasFiltersApplied = false;
        this.timestamp = LocalDateTime.now();
    }

    public AdminActivityOverviewResponse(List<AdminActivityOverviewDTO> users,
            AdminActivityOverviewDTO.ActivityOverviewStatistics globalStatistics,
            AdminActivityOverviewDTO.ActivityFilterCriteria appliedFilters,
            int originalCount) {
        this.users = users;
        this.globalStatistics = globalStatistics;
        this.appliedFilters = appliedFilters;
        this.totalFilteredCount = users != null ? users.size() : 0;
        this.originalCount = originalCount;
        this.hasFiltersApplied = appliedFilters != null && appliedFilters.hasFilters();
        this.searchTerm = appliedFilters != null ? appliedFilters.getSearchTerm() : null;
        this.timestamp = LocalDateTime.now();
    }

    // ============================================================================
    // GETTERS AND SETTERS
    // ============================================================================

    public List<AdminActivityOverviewDTO> getUsers() {
        return users;
    }

    public void setUsers(List<AdminActivityOverviewDTO> users) {
        this.users = users;
        this.totalFilteredCount = users != null ? users.size() : 0;
    }

    public AdminActivityOverviewDTO.ActivityOverviewStatistics getGlobalStatistics() {
        return globalStatistics;
    }

    public void setGlobalStatistics(AdminActivityOverviewDTO.ActivityOverviewStatistics globalStatistics) {
        this.globalStatistics = globalStatistics;
    }

    public AdminActivityOverviewDTO.ActivityFilterCriteria getAppliedFilters() {
        return appliedFilters;
    }

    public void setAppliedFilters(AdminActivityOverviewDTO.ActivityFilterCriteria appliedFilters) {
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    // ============================================================================
    // EXISTING HELPER METHODS (ENHANCED)
    // ============================================================================

    public int getUserCount() {
        return users != null ? users.size() : 0;
    }

    public boolean hasUsers() {
        return users != null && !users.isEmpty();
    }

    public int getUsersWithActivity() {
        if (users == null)
            return 0;
        return (int) users.stream().filter(AdminActivityOverviewDTO::hasActivity).count();
    }

    public int getUsersWithSameDayActivities() {
        if (users == null)
            return 0;
        return (int) users.stream().filter(AdminActivityOverviewDTO::hasSameDayActivities).count();
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

    public int getActiveUsersCount() {
        if (users == null)
            return 0;
        return (int) users.stream().filter(AdminActivityOverviewDTO::isActiveUser).count();
    }

    public int getHighlyEngagedUsersCount() {
        if (users == null)
            return 0;
        return (int) users.stream().filter(AdminActivityOverviewDTO::isHighlyEngaged).count();
    }

    public int getUsersNeedingAttentionCount() {
        if (users == null)
            return 0;
        return (int) users.stream().filter(AdminActivityOverviewDTO::needsAttention).count();
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
            if (appliedFilters.getMinTotalEvents() != null || appliedFilters.getMaxTotalEvents() != null) {
                summary.append("Total events range: ");
                if (appliedFilters.getMinTotalEvents() != null) {
                    summary.append("≥").append(appliedFilters.getMinTotalEvents());
                }
                if (appliedFilters.getMaxTotalEvents() != null) {
                    if (appliedFilters.getMinTotalEvents() != null)
                        summary.append(" and ");
                    summary.append("≤").append(appliedFilters.getMaxTotalEvents());
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

        long highEngagement = users.stream().filter(AdminActivityOverviewDTO::isHighlyEngaged).count();
        long activeUsers = users.stream().filter(AdminActivityOverviewDTO::isActiveUser).count();
        long usersWithActivities = users.stream().filter(AdminActivityOverviewDTO::hasActivity).count();
        long usersNeedingAttention = users.stream().filter(AdminActivityOverviewDTO::needsAttention).count();

        return String.format("High engagement: %d, Active: %d, With activities: %d, Need attention: %d",
                highEngagement, activeUsers, usersWithActivities, usersNeedingAttention);
    }

    public double getAverageActivityScore() {
        if (!hasUsers()) {
            return 0.0;
        }

        return users.stream()
                .mapToDouble(AdminActivityOverviewDTO::getActivityScore)
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
    // PAGINATION SUPPORT
    // ============================================================================

    public boolean supportsPagination() {
        return totalFilteredCount > 20; // Default page size threshold
    }

    public int getRecommendedPageSize() {
        if (totalFilteredCount <= 20)
            return totalFilteredCount;
        if (totalFilteredCount <= 50)
            return 20;
        if (totalFilteredCount <= 100)
            return 25;
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
    // ENHANCED STATISTICS METHODS
    // ============================================================================

    public double getCompletionRate() {
        if (!hasUsers()) {
            return 0.0;
        }

        long totalActivities = users.stream()
                .mapToLong(user -> user.getTotalEvents() != null ? user.getTotalEvents() : 0L)
                .sum();

        long completedActivities = users.stream()
                .mapToLong(AdminActivityOverviewDTO::getTotalCompletedActivities)
                .sum();

        return totalActivities > 0 ? (double) completedActivities / totalActivities * 100 : 0.0;
    }

    public String getFormattedCompletionRate() {
        return String.format("%.1f%%", getCompletionRate());
    }

    public double getSameDayActivityRate() {
        if (!hasUsers()) {
            return 0.0;
        }

        long usersWithSameDay = users.stream()
                .filter(AdminActivityOverviewDTO::hasSameDayActivities)
                .count();

        return (double) usersWithSameDay / users.size() * 100;
    }

    public String getFormattedSameDayActivityRate() {
        return String.format("%.1f%%", getSameDayActivityRate());
    }

    // ============================================================================
    // ACTIVITY BREAKDOWN
    // ============================================================================

    public long getTotalHabitsCompleted() {
        if (!hasUsers()) {
            return 0L;
        }

        return users.stream()
                .mapToLong(user -> user.getTotalHabitsCompleted() != null ? user.getTotalHabitsCompleted() : 0L)
                .sum();
    }

    public long getTotalTasksCompleted() {
        if (!hasUsers()) {
            return 0L;
        }

        return users.stream()
                .mapToLong(user -> user.getTotalTasksCompleted() != null ? user.getTotalTasksCompleted() : 0L)
                .sum();
    }

    public long getTotalSameDayActivities() {
        if (!hasUsers()) {
            return 0L;
        }

        return users.stream()
                .mapToLong(user -> user.getHabitsAndTasksSameDay() != null ? user.getHabitsAndTasksSameDay() : 0L)
                .sum();
    }

    public long getTotalAllEvents() {
        if (!hasUsers()) {
            return 0L;
        }

        return users.stream()
                .mapToLong(user -> user.getTotalEvents() != null ? user.getTotalEvents() : 0L)
                .sum();
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    @Override
    public String toString() {
        return "AdminActivityOverviewResponse{" +
                "usersCount=" + getUserCount() +
                ", originalCount=" + originalCount +
                ", filteredCount=" + totalFilteredCount +
                ", hasFilters=" + hasFiltersApplied +
                ", searchTerm='" + searchTerm + '\'' +
                ", timestamp=" + timestamp +
                ", globalStatistics=" + globalStatistics +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        AdminActivityOverviewResponse that = (AdminActivityOverviewResponse) o;

        if (totalFilteredCount != that.totalFilteredCount)
            return false;
        if (originalCount != that.originalCount)
            return false;
        if (hasFiltersApplied != that.hasFiltersApplied)
            return false;
        if (users != null ? !users.equals(that.users) : that.users != null)
            return false;
        if (globalStatistics != null ? !globalStatistics.equals(that.globalStatistics) : that.globalStatistics != null)
            return false;
        if (appliedFilters != null ? !appliedFilters.equals(that.appliedFilters) : that.appliedFilters != null)
            return false;
        if (searchTerm != null ? !searchTerm.equals(that.searchTerm) : that.searchTerm != null)
            return false;
        return timestamp != null ? timestamp.equals(that.timestamp) : that.timestamp == null;
    }

    @Override
    public int hashCode() {
        int result = users != null ? users.hashCode() : 0;
        result = 31 * result + (globalStatistics != null ? globalStatistics.hashCode() : 0);
        result = 31 * result + (appliedFilters != null ? appliedFilters.hashCode() : 0);
        result = 31 * result + totalFilteredCount;
        result = 31 * result + originalCount;
        result = 31 * result + (searchTerm != null ? searchTerm.hashCode() : 0);
        result = 31 * result + (hasFiltersApplied ? 1 : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        return result;
    }

    // ============================================================================
    // BUILDER PATTERN SUPPORT
    // ============================================================================

    public static class Builder {
        private List<AdminActivityOverviewDTO> users;
        private AdminActivityOverviewDTO.ActivityOverviewStatistics globalStatistics;
        private AdminActivityOverviewDTO.ActivityFilterCriteria appliedFilters;
        private int originalCount;

        public Builder users(List<AdminActivityOverviewDTO> users) {
            this.users = users;
            return this;
        }

        public Builder globalStatistics(AdminActivityOverviewDTO.ActivityOverviewStatistics globalStatistics) {
            this.globalStatistics = globalStatistics;
            return this;
        }

        public Builder appliedFilters(AdminActivityOverviewDTO.ActivityFilterCriteria appliedFilters) {
            this.appliedFilters = appliedFilters;
            return this;
        }

        public Builder originalCount(int originalCount) {
            this.originalCount = originalCount;
            return this;
        }

        public AdminActivityOverviewResponse build() {
            return new AdminActivityOverviewResponse(users, globalStatistics, appliedFilters, originalCount);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
