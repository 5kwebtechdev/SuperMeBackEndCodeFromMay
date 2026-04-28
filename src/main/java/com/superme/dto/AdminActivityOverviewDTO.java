package com.superme.dto;

import java.util.List;

/**
 * Enhanced DTO for admin activity overview operations.
 * Contains user activity information with search and filtering capabilities.
 */
public class AdminActivityOverviewDTO {
    private Long userId;
    // Removed username field
    private String name;
    private Long totalHabitsCompleted;
    private Long totalTasksCompleted;
    private Long habitsAndTasksSameDay;
    private Long totalEvents; // habits + tasks + events

    // Statistics for stat cards
    private ActivityOverviewStatistics statistics;

    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================

    // Default constructor
    public AdminActivityOverviewDTO() {
        this.statistics = new ActivityOverviewStatistics();
    }

    // Constructor with all fields
    public AdminActivityOverviewDTO(Long userId, String name,
                                   Long totalHabitsCompleted, Long totalTasksCompleted,
                                   Long habitsAndTasksSameDay, Long totalEvents,
                                   ActivityOverviewStatistics statistics) {
        this.userId = userId;
        this.name = nullSafeString(name);
        this.totalHabitsCompleted = totalHabitsCompleted != null ? totalHabitsCompleted : 0L;
        this.totalTasksCompleted = totalTasksCompleted != null ? totalTasksCompleted : 0L;
        this.habitsAndTasksSameDay = habitsAndTasksSameDay != null ? habitsAndTasksSameDay : 0L;
        this.totalEvents = totalEvents != null ? totalEvents : 0L;
        this.statistics = statistics != null ? statistics : new ActivityOverviewStatistics();
    }

    // ============================================================================
    // GETTERS AND SETTERS
    // ============================================================================

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return nullSafeString(name); }
    public void setName(String name) { this.name = nullSafeString(name); }

    public Long getTotalHabitsCompleted() { return totalHabitsCompleted != null ? totalHabitsCompleted : 0L; }
    public void setTotalHabitsCompleted(Long totalHabitsCompleted) { 
        this.totalHabitsCompleted = totalHabitsCompleted != null ? totalHabitsCompleted : 0L;
    }

    public Long getTotalTasksCompleted() { return totalTasksCompleted != null ? totalTasksCompleted : 0L; }
    public void setTotalTasksCompleted(Long totalTasksCompleted) { 
        this.totalTasksCompleted = totalTasksCompleted != null ? totalTasksCompleted : 0L;
    }

    public Long getHabitsAndTasksSameDay() { return habitsAndTasksSameDay != null ? habitsAndTasksSameDay : 0L; }
    public void setHabitsAndTasksSameDay(Long habitsAndTasksSameDay) { 
        this.habitsAndTasksSameDay = habitsAndTasksSameDay != null ? habitsAndTasksSameDay : 0L;
    }

    public Long getTotalEvents() { return totalEvents != null ? totalEvents : 0L; }
    public void setTotalEvents(Long totalEvents) { 
        this.totalEvents = totalEvents != null ? totalEvents : 0L;
    }

    public ActivityOverviewStatistics getStatistics() { return statistics; }
    public void setStatistics(ActivityOverviewStatistics statistics) { 
        this.statistics = statistics != null ? statistics : new ActivityOverviewStatistics(); 
    }

    // ============================================================================
    // BUSINESS LOGIC METHODS
    // ============================================================================

    // Helper methods for display
    public boolean hasActivity() {
        return getTotalEvents() > 0;
    }

    public boolean hasCompletedActivities() {
        return getTotalHabitsCompleted() > 0 || getTotalTasksCompleted() > 0;
    }

    public boolean hasSameDayActivities() {
        return getHabitsAndTasksSameDay() > 0;
    }

    public boolean isActiveUser() {
        return hasActivity() && hasCompletedActivities();
    }

    public boolean isHighlyEngaged() {
        return getActivityScore() >= 80.0;
    }

    public boolean needsAttention() {
        return hasActivity() && !hasCompletedActivities();
    }

    // Calculate total completed activities
    public Long getTotalCompletedActivities() {
        return getTotalHabitsCompleted() + getTotalTasksCompleted();
    }

    // Calculate activity score (0-100)
    public double getActivityScore() {
        if (!hasActivity()) return 0.0;
        
        double baseScore = Math.min(getTotalEvents() * 2.0, 40.0); // Max 40 points for total events
        double completionScore = Math.min(getTotalCompletedActivities() * 3.0, 40.0); // Max 40 points for completed
        double sameDayScore = Math.min(getHabitsAndTasksSameDay() * 4.0, 20.0); // Max 20 points for same day
        
        return baseScore + completionScore + sameDayScore;
    }

    public String getEngagementLevel() {
        double score = getActivityScore();
        if (score >= 80) return "High";
        else if (score >= 40) return "Medium";
        else if (score > 0) return "Low";
        else return "None";
    }

    // ============================================================================
    // SEARCH METHODS
    // ============================================================================

    /**
     * Check if user matches search term (searches userId and name)
     */
    public boolean matchesSearchTerm(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return true;
        }
        
        String term = searchTerm.toLowerCase().trim();
        
        // Search in userId
        if (userId != null && userId.toString().contains(term)) {
            return true;
        }
        
        // Search in name (null-safe)
        if (name != null && !name.equals("N/A") && name.toLowerCase().contains(term)) {
            return true;
        }
        
        return false;
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    // Utility method to handle null/blank strings
    private String nullSafeString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "N/A";
        }
        return value.trim();
    }

    @Override
    public String toString() {
        return "AdminActivityOverviewDTO{" +
                "userId=" + userId +
                ", name='" + getName() + '\'' +
                ", totalHabitsCompleted=" + getTotalHabitsCompleted() +
                ", totalTasksCompleted=" + getTotalTasksCompleted() +
                ", habitsAndTasksSameDay=" + getHabitsAndTasksSameDay() +
                ", totalEvents=" + getTotalEvents() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        AdminActivityOverviewDTO that = (AdminActivityOverviewDTO) o;
        
        if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (totalHabitsCompleted != null ? !totalHabitsCompleted.equals(that.totalHabitsCompleted) : that.totalHabitsCompleted != null)
            return false;
        if (totalTasksCompleted != null ? !totalTasksCompleted.equals(that.totalTasksCompleted) : that.totalTasksCompleted != null)
            return false;
        if (habitsAndTasksSameDay != null ? !habitsAndTasksSameDay.equals(that.habitsAndTasksSameDay) : that.habitsAndTasksSameDay != null)
            return false;
        return totalEvents != null ? totalEvents.equals(that.totalEvents) : that.totalEvents == null;
    }
    
    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (totalHabitsCompleted != null ? totalHabitsCompleted.hashCode() : 0);
        result = 31 * result + (totalTasksCompleted != null ? totalTasksCompleted.hashCode() : 0);
        result = 31 * result + (habitsAndTasksSameDay != null ? habitsAndTasksSameDay.hashCode() : 0);
        result = 31 * result + (totalEvents != null ? totalEvents.hashCode() : 0);
        return result;
    }

    // ============================================================================
    // NESTED CLASSES
    // ============================================================================

    /**
     * Nested class for activity overview filtering criteria
     */
    public static class ActivityFilterCriteria {
        private String searchTerm;
        private Long minTotalEvents;
        private Long maxTotalEvents;
        private Long minHabitsCompleted;
        private Long maxHabitsCompleted;
        private Long minTasksCompleted;
        private Long maxTasksCompleted;
        private Long minSameDayActivities;
        private Long maxSameDayActivities;
        private String engagementLevel;
        private String userType;
        private Boolean isActive;
        
        // ============================================================================
        // CONSTRUCTORS
        // ============================================================================
        
        public ActivityFilterCriteria() {
        }
        
        // ============================================================================
        // GETTERS AND SETTERS
        // ============================================================================
        
        public String getSearchTerm() {
            return searchTerm;
        }
        
        public void setSearchTerm(String searchTerm) {
            this.searchTerm = searchTerm;
        }
        
        public Long getMinTotalEvents() {
            return minTotalEvents;
        }
        
        public void setMinTotalEvents(Long minTotalEvents) {
            this.minTotalEvents = minTotalEvents;
        }
        
        public Long getMaxTotalEvents() {
            return maxTotalEvents;
        }
        
        public void setMaxTotalEvents(Long maxTotalEvents) {
            this.maxTotalEvents = maxTotalEvents;
        }
        
        public Long getMinHabitsCompleted() {
            return minHabitsCompleted;
        }
        
        public void setMinHabitsCompleted(Long minHabitsCompleted) {
            this.minHabitsCompleted = minHabitsCompleted;
        }
        
        public Long getMaxHabitsCompleted() {
            return maxHabitsCompleted;
        }
        
        public void setMaxHabitsCompleted(Long maxHabitsCompleted) {
            this.maxHabitsCompleted = maxHabitsCompleted;
        }
        
        public Long getMinTasksCompleted() {
            return minTasksCompleted;
        }
        
        public void setMinTasksCompleted(Long minTasksCompleted) {
            this.minTasksCompleted = minTasksCompleted;
        }
        
        public Long getMaxTasksCompleted() {
            return maxTasksCompleted;
        }
        
        public void setMaxTasksCompleted(Long maxTasksCompleted) {
            this.maxTasksCompleted = maxTasksCompleted;
        }
        
        public Long getMinSameDayActivities() {
            return minSameDayActivities;
        }
        
        public void setMinSameDayActivities(Long minSameDayActivities) {
            this.minSameDayActivities = minSameDayActivities;
        }
        
        public Long getMaxSameDayActivities() {
            return maxSameDayActivities;
        }
        
        public void setMaxSameDayActivities(Long maxSameDayActivities) {
            this.maxSameDayActivities = maxSameDayActivities;
        }
        
        public String getEngagementLevel() {
            return engagementLevel;
        }
        
        public void setEngagementLevel(String engagementLevel) {
            this.engagementLevel = engagementLevel;
        }
        
        public String getUserType() {
            return userType;
        }
        
        public void setUserType(String userType) {
            this.userType = userType;
        }
        
        public Boolean getIsActive() {
            return isActive;
        }
        
        public void setIsActive(Boolean isActive) {
            this.isActive = isActive;
        }
        
        // ============================================================================
        // UTILITY METHODS
        // ============================================================================
        
        /**
         * Check if any filters are set
         */
        public boolean hasFilters() {
            return (searchTerm != null && !searchTerm.trim().isEmpty()) ||
                   minTotalEvents != null ||
                   maxTotalEvents != null ||
                   minHabitsCompleted != null ||
                   maxHabitsCompleted != null ||
                   minTasksCompleted != null ||
                   maxTasksCompleted != null ||
                   minSameDayActivities != null ||
                   maxSameDayActivities != null ||
                   (engagementLevel != null && !engagementLevel.trim().isEmpty()) ||
                   (userType != null && !userType.trim().isEmpty()) ||
                   isActive != null;
        }
        
        /**
         * Check if search term is set
         */
        public boolean hasSearchTerm() {
            return searchTerm != null && !searchTerm.trim().isEmpty();
        }
        
        /**
         * Check if range filters are set
         */
        public boolean hasRangeFilters() {
            return minTotalEvents != null || maxTotalEvents != null ||
                   minHabitsCompleted != null || maxHabitsCompleted != null ||
                   minTasksCompleted != null || maxTasksCompleted != null ||
                   minSameDayActivities != null || maxSameDayActivities != null;
        }
        
        /**
         * Check if categorical filters are set
         */
        public boolean hasCategoricalFilters() {
            return (engagementLevel != null && !engagementLevel.trim().isEmpty()) ||
                   (userType != null && !userType.trim().isEmpty()) ||
                   isActive != null;
        }
        
        @Override
        public String toString() {
            return "ActivityFilterCriteria{" +
                    "searchTerm='" + searchTerm + '\'' +
                    ", minTotalEvents=" + minTotalEvents +
                    ", maxTotalEvents=" + maxTotalEvents +
                    ", minHabitsCompleted=" + minHabitsCompleted +
                    ", maxHabitsCompleted=" + maxHabitsCompleted +
                    ", minTasksCompleted=" + minTasksCompleted +
                    ", maxTasksCompleted=" + maxTasksCompleted +
                    ", minSameDayActivities=" + minSameDayActivities +
                    ", maxSameDayActivities=" + maxSameDayActivities +
                    ", engagementLevel='" + engagementLevel + '\'' +
                    ", userType='" + userType + '\'' +
                    ", isActive=" + isActive +
                    '}';
        }
    }

    /**
     * Static inner class for activity overview statistics
     */
    public static class ActivityOverviewStatistics {
        private Long totalEvents; // habits + tasks + events
        private Long totalHabits;
        private Long totalTasks;
        private Double averageEventsPerUser;
        private Long activeUsers;
        private Long usersWithActivities;

        // ============================================================================
        // CONSTRUCTORS
        // ============================================================================

        public ActivityOverviewStatistics() {
            this.totalEvents = 0L;
            this.totalHabits = 0L;
            this.totalTasks = 0L;
            this.averageEventsPerUser = 0.0;
            this.activeUsers = 0L;
            this.usersWithActivities = 0L;
        }

        public ActivityOverviewStatistics(Long totalEvents, Long totalHabits, 
                                        Long totalTasks, Double averageEventsPerUser,
                                        Long activeUsers, Long usersWithActivities) {
            this.totalEvents = totalEvents != null ? totalEvents : 0L;
            this.totalHabits = totalHabits != null ? totalHabits : 0L;
            this.totalTasks = totalTasks != null ? totalTasks : 0L;
            this.averageEventsPerUser = averageEventsPerUser != null ? averageEventsPerUser : 0.0;
            this.activeUsers = activeUsers != null ? activeUsers : 0L;
            this.usersWithActivities = usersWithActivities != null ? usersWithActivities : 0L;
        }

        // ============================================================================
        // GETTERS AND SETTERS
        // ============================================================================

        public Long getTotalEvents() { return totalEvents; }
        public void setTotalEvents(Long totalEvents) { 
            this.totalEvents = totalEvents != null ? totalEvents : 0L;
        }

        public Long getTotalHabits() { return totalHabits; }
        public void setTotalHabits(Long totalHabits) { 
            this.totalHabits = totalHabits != null ? totalHabits : 0L;
        }

        public Long getTotalTasks() { return totalTasks; }
        public void setTotalTasks(Long totalTasks) { 
            this.totalTasks = totalTasks != null ? totalTasks : 0L;
        }

        public Double getAverageEventsPerUser() { return averageEventsPerUser; }
        public void setAverageEventsPerUser(Double averageEventsPerUser) { 
            this.averageEventsPerUser = averageEventsPerUser != null ? averageEventsPerUser : 0.0;
        }

        public Long getActiveUsers() { return activeUsers; }
        public void setActiveUsers(Long activeUsers) { 
            this.activeUsers = activeUsers != null ? activeUsers : 0L;
        }

        public Long getUsersWithActivities() { return usersWithActivities; }
        public void setUsersWithActivities(Long usersWithActivities) { 
            this.usersWithActivities = usersWithActivities != null ? usersWithActivities : 0L;
        }

        // ============================================================================
        // DERIVED GETTERS
        // ============================================================================

        public Long getTotalCompletedActivities() {
            return totalHabits + totalTasks;
        }

        public Double getEngagementRate() {
            return usersWithActivities > 0 ? 
                (double) activeUsers / usersWithActivities * 100 : 0.0;
        }

        // Helper methods
        public String getFormattedAverageEventsPerUser() {
            return String.format("%.1f", averageEventsPerUser);
        }

        public String getFormattedEngagementRate() {
            return String.format("%.1f%%", getEngagementRate());
        }

        @Override
        public String toString() {
            return "ActivityOverviewStatistics{" +
                    "totalEvents=" + totalEvents +
                    ", totalHabits=" + totalHabits +
                    ", totalTasks=" + totalTasks +
                    ", averageEventsPerUser=" + averageEventsPerUser +
                    ", activeUsers=" + activeUsers +
                    ", usersWithActivities=" + usersWithActivities +
                    '}';
        }
    }

    /**
     * Nested class for activity overview response
     */
    public static class ActivityOverviewResponse {
        private List<AdminActivityOverviewDTO> users;
        private ActivityOverviewStatistics statistics;
        private ActivityFilterCriteria appliedFilters;
        private int totalFilteredCount;
        private int originalCount;
        
        // ============================================================================
        // CONSTRUCTORS
        // ============================================================================
        
        public ActivityOverviewResponse() {
        }
        
        public ActivityOverviewResponse(List<AdminActivityOverviewDTO> users,
                                      ActivityOverviewStatistics statistics) {
            this.users = users;
            this.statistics = statistics;
            this.totalFilteredCount = users != null ? users.size() : 0;
            this.originalCount = totalFilteredCount;
        }
        
        public ActivityOverviewResponse(List<AdminActivityOverviewDTO> users,
                                      ActivityOverviewStatistics statistics, 
                                      ActivityFilterCriteria appliedFilters,
                                      int originalCount) {
            this.users = users;
            this.statistics = statistics;
            this.appliedFilters = appliedFilters;
            this.totalFilteredCount = users != null ? users.size() : 0;
            this.originalCount = originalCount;
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
        
        public ActivityOverviewStatistics getStatistics() {
            return statistics;
        }
        
        public void setStatistics(ActivityOverviewStatistics statistics) {
            this.statistics = statistics;
        }
        
        public ActivityFilterCriteria getAppliedFilters() {
            return appliedFilters;
        }
        
        public void setAppliedFilters(ActivityFilterCriteria appliedFilters) {
            this.appliedFilters = appliedFilters;
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
        
        // ============================================================================
        // DERIVED GETTERS
        // ============================================================================
        
        public boolean hasFiltersApplied() {
            return appliedFilters != null && appliedFilters.hasFilters();
        }
        
        public boolean hasSearchApplied() {
            return appliedFilters != null && appliedFilters.hasSearchTerm();
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
        
        // ============================================================================
        // UTILITY METHODS
        // ============================================================================
        
        @Override
        public String toString() {
            return "ActivityOverviewResponse{" +
                    "usersCount=" + (users != null ? users.size() : 0) +
                    ", statistics=" + statistics +
                    ", appliedFilters=" + appliedFilters +
                    ", totalFilteredCount=" + totalFilteredCount +
                    ", originalCount=" + originalCount +
                    '}';
        }
    }
}