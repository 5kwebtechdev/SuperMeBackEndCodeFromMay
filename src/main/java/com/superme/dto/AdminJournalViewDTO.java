package com.superme.dto;

import java.util.List;

/**
 * DTO for admin journal entry view operations.
 * Contains user information and their journal entry statistics.
 */
public class AdminJournalViewDTO {
    
    // User basic information
    private Long userId;
    // private String username; // Removed username
    private String name; // Added name
    
    // Journal entry statistics
    private Long totalJournalEntries;
    private Long journalEntriesCreatedThisMonth;
    
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    public AdminJournalViewDTO() {
    }
    
    public AdminJournalViewDTO(Long userId, String name, Long totalJournalEntries, Long journalEntriesCreatedThisMonth) {
        this.userId = userId;
        this.name = name;
        this.totalJournalEntries = totalJournalEntries;
        this.journalEntriesCreatedThisMonth = journalEntriesCreatedThisMonth;
    }
    
    // ============================================================================
    // GETTERS AND SETTERS
    // ============================================================================
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Long getTotalJournalEntries() {
        return totalJournalEntries;
    }
    
    public void setTotalJournalEntries(Long totalJournalEntries) {
        this.totalJournalEntries = totalJournalEntries;
    }
    
    public Long getJournalEntriesCreatedThisMonth() {
        return journalEntriesCreatedThisMonth;
    }
    
    public void setJournalEntriesCreatedThisMonth(Long journalEntriesCreatedThisMonth) {
        this.journalEntriesCreatedThisMonth = journalEntriesCreatedThisMonth;
    }
    
    // ============================================================================
    // BUSINESS LOGIC METHODS
    // ============================================================================
    
    public boolean hasJournalEntries() {
        return totalJournalEntries != null && totalJournalEntries > 0;
    }
    
    public boolean hasRecentActivity() {
        return journalEntriesCreatedThisMonth != null && journalEntriesCreatedThisMonth > 0;
    }
    
    public boolean isActiveUser() {
        return hasJournalEntries() && hasRecentActivity();
    }
    
    public boolean isHighlyEngaged() {
        return getActivityScore() >= 80.0;
    }
    
    public boolean needsAttention() {
        return hasJournalEntries() && !hasRecentActivity();
    }
    
    public double getActivityScore() {
        if (!hasJournalEntries()) return 0.0;
        
        double baseScore = Math.min(totalJournalEntries * 5.0, 50.0); // Max 50 points for total
        double recentScore = Math.min(journalEntriesCreatedThisMonth * 10.0, 50.0); // Max 50 points for recent
        
        return baseScore + recentScore;
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
        
        // Search in name
        if (name != null && name.toLowerCase().contains(term)) {
            return true;
        }
        
        return false;
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================
    
    @Override
    public String toString() {
        return "AdminJournalViewDTO{" +
                "userId=" + userId +
                ", name='" + name + '\'' +
                ", totalJournalEntries=" + totalJournalEntries +
                ", journalEntriesCreatedThisMonth=" + journalEntriesCreatedThisMonth +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        AdminJournalViewDTO that = (AdminJournalViewDTO) o;
        
        if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (totalJournalEntries != null ? !totalJournalEntries.equals(that.totalJournalEntries) : that.totalJournalEntries != null)
            return false;
        return journalEntriesCreatedThisMonth != null ? journalEntriesCreatedThisMonth.equals(that.journalEntriesCreatedThisMonth) : that.journalEntriesCreatedThisMonth == null;
    }
    
    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (totalJournalEntries != null ? totalJournalEntries.hashCode() : 0);
        result = 31 * result + (journalEntriesCreatedThisMonth != null ? journalEntriesCreatedThisMonth.hashCode() : 0);
        return result;
    }

    /**
     * Nested class for journal filtering criteria
     */
    public static class JournalFilterCriteria {
        private String searchTerm;
        private Long minJournalEntries;
        private Long maxJournalEntries;
        private Long minRecentEntries;
        private Long maxRecentEntries;
        private String engagementLevel;
        private String userType;
        private Boolean isActive;
        
        // ============================================================================
        // CONSTRUCTORS
        // ============================================================================
        
        public JournalFilterCriteria() {
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
        
        public Long getMinJournalEntries() {
            return minJournalEntries;
        }
        
        public void setMinJournalEntries(Long minJournalEntries) {
            this.minJournalEntries = minJournalEntries;
        }
        
        public Long getMaxJournalEntries() {
            return maxJournalEntries;
        }
        
        public void setMaxJournalEntries(Long maxJournalEntries) {
            this.maxJournalEntries = maxJournalEntries;
        }
        
        public Long getMinRecentEntries() {
            return minRecentEntries;
        }
        
        public void setMinRecentEntries(Long minRecentEntries) {
            this.minRecentEntries = minRecentEntries;
        }
        
        public Long getMaxRecentEntries() {
            return maxRecentEntries;
        }
        
        public void setMaxRecentEntries(Long maxRecentEntries) {
            this.maxRecentEntries = maxRecentEntries;
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
                   minJournalEntries != null ||
                   maxJournalEntries != null ||
                   minRecentEntries != null ||
                   maxRecentEntries != null ||
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
            return minJournalEntries != null || maxJournalEntries != null ||
                   minRecentEntries != null || maxRecentEntries != null;
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
            return "JournalFilterCriteria{" +
                    "searchTerm='" + searchTerm + '\'' +
                    ", minJournalEntries=" + minJournalEntries +
                    ", maxJournalEntries=" + maxJournalEntries +
                    ", minRecentEntries=" + minRecentEntries +
                    ", maxRecentEntries=" + maxRecentEntries +
                    ", engagementLevel='" + engagementLevel + '\'' +
                    ", userType='" + userType + '\'' +
                    ", isActive=" + isActive +
                    '}';
        }
    }

    /**
     * Nested class for global journal entry statistics
     */
    public static class JournalStatistics {
        private Long totalJournalEntriesAllUsers;
        private Long totalUsers;
        private Double averageJournalEntriesPerUser;
        private Long usersWithJournalEntries;
        
        // ============================================================================
        // CONSTRUCTORS
        // ============================================================================
        
        public JournalStatistics() {
        }
        
        public JournalStatistics(Long totalJournalEntriesAllUsers, Long totalUsers, 
                               Double averageJournalEntriesPerUser, Long usersWithJournalEntries) {
            this.totalJournalEntriesAllUsers = totalJournalEntriesAllUsers;
            this.totalUsers = totalUsers;
            this.averageJournalEntriesPerUser = averageJournalEntriesPerUser;
            this.usersWithJournalEntries = usersWithJournalEntries;
        }
        
        // ============================================================================
        // GETTERS AND SETTERS
        // ============================================================================
        
        public Long getTotalJournalEntriesAllUsers() {
            return totalJournalEntriesAllUsers;
        }
        
        public void setTotalJournalEntriesAllUsers(Long totalJournalEntriesAllUsers) {
            this.totalJournalEntriesAllUsers = totalJournalEntriesAllUsers;
        }
        
        public Long getTotalUsers() {
            return totalUsers;
        }
        
        public void setTotalUsers(Long totalUsers) {
            this.totalUsers = totalUsers;
        }
        
        public Double getAverageJournalEntriesPerUser() {
            return averageJournalEntriesPerUser;
        }
        
        public void setAverageJournalEntriesPerUser(Double averageJournalEntriesPerUser) {
            this.averageJournalEntriesPerUser = averageJournalEntriesPerUser;
        }
        
        public Long getUsersWithJournalEntries() {
            return usersWithJournalEntries;
        }
        
        public void setUsersWithJournalEntries(Long usersWithJournalEntries) {
            this.usersWithJournalEntries = usersWithJournalEntries;
        }
        
        // ============================================================================
        // DERIVED GETTERS
        // ============================================================================
        
        public Long getUsersWithoutJournalEntries() {
            return totalUsers != null && usersWithJournalEntries != null ? 
                totalUsers - usersWithJournalEntries : 0L;
        }
        
        public Double getEngagementRate() {
            return totalUsers != null && totalUsers > 0 ? 
                (double) usersWithJournalEntries / totalUsers * 100 : 0.0;
        }
        
        public String getFormattedAverageJournalEntriesPerUser() {
            return averageJournalEntriesPerUser != null ? 
                String.format("%.1f", averageJournalEntriesPerUser) : "0.0";
        }
        
        public String getFormattedEngagementRate() {
            return String.format("%.1f%%", getEngagementRate());
        }
        
        // ============================================================================
        // UTILITY METHODS
        // ============================================================================
        
        @Override
        public String toString() {
            return "JournalStatistics{" +
                    "totalJournalEntriesAllUsers=" + totalJournalEntriesAllUsers +
                    ", totalUsers=" + totalUsers +
                    ", averageJournalEntriesPerUser=" + averageJournalEntriesPerUser +
                    ", usersWithJournalEntries=" + usersWithJournalEntries +
                    '}';
        }
    }

    /**
     * Nested class for journal overview response
     */
    public static class JournalOverviewResponse {
        private List<AdminJournalViewDTO> users;
        private JournalStatistics statistics;
        private JournalFilterCriteria appliedFilters;
        private int totalFilteredCount;
        private int originalCount;
        
        // ============================================================================
        // CONSTRUCTORS
        // ============================================================================
        
        public JournalOverviewResponse() {
        }
        
        public JournalOverviewResponse(List<AdminJournalViewDTO> users,
                                       JournalStatistics statistics) {
            this.users = users;
            this.statistics = statistics;
            this.totalFilteredCount = users != null ? users.size() : 0;
            this.originalCount = totalFilteredCount;
        }
        
        public JournalOverviewResponse(List<AdminJournalViewDTO> users,
                                     JournalStatistics statistics, 
                                     JournalFilterCriteria appliedFilters,
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
        
        public List<AdminJournalViewDTO> getUsers() {
            return users;
        }
        
        public void setUsers(List<AdminJournalViewDTO> users) {
            this.users = users;
            this.totalFilteredCount = users != null ? users.size() : 0;
        }
        
        public JournalStatistics getStatistics() {
            return statistics;
        }
        
        public void setStatistics(JournalStatistics statistics) {
            this.statistics = statistics;
        }
        
        public JournalFilterCriteria getAppliedFilters() {
            return appliedFilters;
        }
        
        public void setAppliedFilters(JournalFilterCriteria appliedFilters) {
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
            return "JournalOverviewResponse{" +
                    "usersCount=" + (users != null ? users.size() : 0) +
                    ", statistics=" + statistics +
                    ", appliedFilters=" + appliedFilters +
                    ", totalFilteredCount=" + totalFilteredCount +
                    ", originalCount=" + originalCount +
                    '}';
        }
    }
}
