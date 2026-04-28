package com.superme.dto;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Response DTO for Admin Habit Overview operations.
 * 
 * Provides comprehensive habit overview data with advanced search and filtering
 * capabilities.
 * Includes habit statistics, filter criteria, and metadata for admin dashboard
 * consumption.
 * 
 * @author MindfullB Admin Team
 * @version 2.0
 */
public class AdminHabitOverviewResponse {
    private List<AdminHabitViewDTO> users;
    private AdminHabitViewDTO.HabitStatistics globalStatistics;
    private AdminHabitViewDTO.HabitFilterCriteria filterCriteria;
    private FilterStatistics filterStatistics;
    private Long timestamp;
    private Integer totalUsers;
    private Integer filteredUsers;
    private Integer originalUserCount;
    private Boolean hasFilters;
    private String filterSummary;
    private Double filterEfficiency;

    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================

    /**
     * Default constructor
     */
    public AdminHabitOverviewResponse() {
        this.timestamp = System.currentTimeMillis();
        this.hasFilters = false;
        this.filterEfficiency = 100.0;
    }

    /**
     * Constructor with users and statistics (basic)
     */
    public AdminHabitOverviewResponse(List<AdminHabitViewDTO> users,
            AdminHabitViewDTO.HabitStatistics globalStatistics) {
        this.users = users;
        this.globalStatistics = globalStatistics;
        this.timestamp = System.currentTimeMillis();
        this.totalUsers = users != null ? users.size() : 0;
        this.filteredUsers = this.totalUsers;
        this.originalUserCount = this.totalUsers;
        this.hasFilters = false;
        this.filterSummary = "No filters applied";
        this.filterEfficiency = 100.0;
        this.filterStatistics = new FilterStatistics(users);
    }

    /**
     * Constructor with filter criteria (enhanced)
     */
    public AdminHabitOverviewResponse(List<AdminHabitViewDTO> users,
            AdminHabitViewDTO.HabitStatistics globalStatistics,
            AdminHabitViewDTO.HabitFilterCriteria filterCriteria,
            int originalUserCount) {
        this.users = users;
        this.globalStatistics = globalStatistics;
        this.filterCriteria = filterCriteria;
        this.timestamp = System.currentTimeMillis();
        this.totalUsers = users != null ? users.size() : 0;
        this.filteredUsers = this.totalUsers;
        this.originalUserCount = originalUserCount;
        this.hasFilters = filterCriteria != null && filterCriteria.hasFilters();
        this.filterSummary = filterCriteria != null ? filterCriteria.getSummary() : "No filters applied";
        this.filterEfficiency = originalUserCount > 0 ? (double) this.totalUsers / originalUserCount * 100 : 100.0;
        this.filterStatistics = new FilterStatistics(users);
    }

    // ============================================================================
    // ENHANCED HELPER METHODS
    // ============================================================================

    /**
     * Get count of users matching the current filters
     */
    public int getUserCount() {
        return users != null ? users.size() : 0;
    }

    /**
     * Check if response has any users
     */
    public boolean hasUsers() {
        return users != null && !users.isEmpty();
    }

    /**
     * Check if filters are applied
     */
    public boolean hasActiveFilters() {
        return hasFilters != null && hasFilters;
    }

    /**
     * Get filter effectiveness percentage
     */
    public double getFilterEffectiveness() {
        return filterEfficiency != null ? filterEfficiency : 100.0;
    }

    /**
     * Get users with habits
     */
    public List<AdminHabitViewDTO> getUsersWithHabits() {
        if (users == null)
            return List.of();
        return users.stream()
                .filter(AdminHabitViewDTO::hasHabits)
                .collect(Collectors.toList());
    }

    /**
     * Get users without habits
     */
    public List<AdminHabitViewDTO> getUsersWithoutHabits() {
        if (users == null)
            return List.of();
        return users.stream()
                .filter(user -> !user.hasHabits())
                .collect(Collectors.toList());
    }

    /**
     * Get high performing users (completion rate >= 70%)
     */
    public List<AdminHabitViewDTO> getHighPerformers() {
        if (users == null)
            return List.of();
        return users.stream()
                .filter(AdminHabitViewDTO::isHighPerformer)
                .collect(Collectors.toList());
    }

    /**
     * Get users needing attention (completion rate < 30%)
     */
    public List<AdminHabitViewDTO> getUsersNeedingAttention() {
        if (users == null)
            return List.of();
        return users.stream()
                .filter(AdminHabitViewDTO::needsAttention)
                .collect(Collectors.toList());
    }

    /**
     * Get highly engaged users
     */
    public List<AdminHabitViewDTO> getHighlyEngagedUsers() {
        if (users == null)
            return List.of();
        return users.stream()
                .filter(AdminHabitViewDTO::isHighlyEngaged)
                .collect(Collectors.toList());
    }

    /**
     * Get summary statistics about the current user set
     */
    public Map<String, Object> getSummaryStats() {
        Map<String, Object> summary = new HashMap<>();

        if (users == null || users.isEmpty()) {
            summary.put("isEmpty", true);
            return summary;
        }

        long usersWithHabits = users.stream().filter(AdminHabitViewDTO::hasHabits).count();
        long highPerformers = users.stream().filter(AdminHabitViewDTO::isHighPerformer).count();
        long needingAttention = users.stream().filter(AdminHabitViewDTO::needsAttention).count();
        long highlyEngaged = users.stream().filter(AdminHabitViewDTO::isHighlyEngaged).count();

        double avgCompletionRate = users.stream()
                .filter(AdminHabitViewDTO::hasHabits)
                .mapToDouble(AdminHabitViewDTO::getCompletionRate)
                .average()
                .orElse(0.0);

        summary.put("isEmpty", false);
        summary.put("totalUsers", users.size());
        summary.put("usersWithHabits", usersWithHabits);
        summary.put("usersWithoutHabits", users.size() - usersWithHabits);
        summary.put("highPerformers", highPerformers);
        summary.put("usersNeedingAttention", needingAttention);
        summary.put("highlyEngagedUsers", highlyEngaged);
        summary.put("averageCompletionRate", avgCompletionRate);
        summary.put("habitAdoptionRate", !users.isEmpty() ? (double) usersWithHabits / users.size() * 100 : 0.0);
        summary.put("highPerformerRate", !users.isEmpty() ? (double) highPerformers / users.size() * 100 : 0.0);
        summary.put("engagementRate", !users.isEmpty() ? (double) highlyEngaged / users.size() * 100 : 0.0);

        return summary;
    }

    /**
     * Get metadata about the response
     */
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        metadata.put("timestamp", timestamp);
        metadata.put("hasFilters", hasActiveFilters());
        metadata.put("filterSummary", filterSummary);
        metadata.put("filterEfficiency", getFilterEffectiveness());
        metadata.put("originalUserCount", originalUserCount != null ? originalUserCount : 0);
        metadata.put("filteredUserCount", getUserCount());
        metadata.put("usersReduced", (originalUserCount != null ? originalUserCount : 0) - getUserCount());
        metadata.put("responseType", "habit_overview");
        metadata.put("version", "2.0");

        return metadata;
    }

    // ============================================================================
    // GETTERS AND SETTERS
    // ============================================================================

    public List<AdminHabitViewDTO> getUsers() {
        return users;
    }

    public void setUsers(List<AdminHabitViewDTO> users) {
        this.users = users;
        this.totalUsers = users != null ? users.size() : 0;
        this.filteredUsers = this.totalUsers;
        if (this.filterStatistics == null) {
            this.filterStatistics = new FilterStatistics(users);
        }
    }

    public AdminHabitViewDTO.HabitStatistics getGlobalStatistics() {
        return globalStatistics;
    }

    public void setGlobalStatistics(AdminHabitViewDTO.HabitStatistics globalStatistics) {
        this.globalStatistics = globalStatistics;
    }

    public AdminHabitViewDTO.HabitFilterCriteria getFilterCriteria() {
        return filterCriteria;
    }

    public void setFilterCriteria(AdminHabitViewDTO.HabitFilterCriteria filterCriteria) {
        this.filterCriteria = filterCriteria;
        this.hasFilters = filterCriteria != null && filterCriteria.hasFilters();
        this.filterSummary = filterCriteria != null ? filterCriteria.getSummary() : "No filters applied";
    }

    public FilterStatistics getFilterStatistics() {
        return filterStatistics;
    }

    public void setFilterStatistics(FilterStatistics filterStatistics) {
        this.filterStatistics = filterStatistics;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Integer totalUsers) {
        this.totalUsers = totalUsers;
    }

    public Integer getFilteredUsers() {
        return filteredUsers;
    }

    public void setFilteredUsers(Integer filteredUsers) {
        this.filteredUsers = filteredUsers;
    }

    public Integer getOriginalUserCount() {
        return originalUserCount;
    }

    public void setOriginalUserCount(Integer originalUserCount) {
        this.originalUserCount = originalUserCount;
        this.filterEfficiency = originalUserCount != null && originalUserCount > 0 && totalUsers != null
                ? (double) totalUsers / originalUserCount * 100
                : 100.0;
    }

    public Boolean getHasFilters() {
        return hasFilters;
    }

    public void setHasFilters(Boolean hasFilters) {
        this.hasFilters = hasFilters;
    }

    public String getFilterSummary() {
        return filterSummary;
    }

    public void setFilterSummary(String filterSummary) {
        this.filterSummary = filterSummary;
    }

    public Double getFilterEfficiency() {
        return filterEfficiency;
    }

    public void setFilterEfficiency(Double filterEfficiency) {
        this.filterEfficiency = filterEfficiency;
    }

    // ============================================================================
    // STATIC INNER CLASSES
    // ============================================================================

    /**
     * Filter statistics for habit data
     */
    public static class FilterStatistics {
        private Long totalUsers;
        private Long usersWithHabits;
        private Long usersWithoutHabits;
        private Long highPerformers;
        private Long usersNeedingAttention;
        private Long highlyEngagedUsers;
        private Double averageCompletionRate;
        private Double habitAdoptionRate;
        private Double highPerformerRate;
        private Double engagementRate;

        public FilterStatistics() {
            this.totalUsers = 0L;
            this.usersWithHabits = 0L;
            this.usersWithoutHabits = 0L;
            this.highPerformers = 0L;
            this.usersNeedingAttention = 0L;
            this.highlyEngagedUsers = 0L;
            this.averageCompletionRate = 0.0;
            this.habitAdoptionRate = 0.0;
            this.highPerformerRate = 0.0;
            this.engagementRate = 0.0;
        }

        public FilterStatistics(List<AdminHabitViewDTO> users) {
            // Initialize with default values first
            this.totalUsers = 0L;
            this.usersWithHabits = 0L;
            this.usersWithoutHabits = 0L;
            this.highPerformers = 0L;
            this.usersNeedingAttention = 0L;
            this.highlyEngagedUsers = 0L;
            this.averageCompletionRate = 0.0;
            this.habitAdoptionRate = 0.0;
            this.highPerformerRate = 0.0;
            this.engagementRate = 0.0;

            // If users is null or empty, keep default values
            if (users == null || users.isEmpty()) {
                return;
            }

            // Calculate actual values
            this.totalUsers = (long) users.size();
            this.usersWithHabits = users.stream().filter(AdminHabitViewDTO::hasHabits).count();
            this.usersWithoutHabits = this.totalUsers - this.usersWithHabits;
            this.highPerformers = users.stream().filter(AdminHabitViewDTO::isHighPerformer).count();
            this.usersNeedingAttention = users.stream().filter(AdminHabitViewDTO::needsAttention).count();
            this.highlyEngagedUsers = users.stream().filter(AdminHabitViewDTO::isHighlyEngaged).count();

            this.averageCompletionRate = users.stream()
                    .filter(AdminHabitViewDTO::hasHabits)
                    .mapToDouble(AdminHabitViewDTO::getCompletionRate)
                    .average()
                    .orElse(0.0);

            this.habitAdoptionRate = this.totalUsers > 0 ? (double) this.usersWithHabits / this.totalUsers * 100 : 0.0;
            this.highPerformerRate = this.totalUsers > 0 ? (double) this.highPerformers / this.totalUsers * 100 : 0.0;
            this.engagementRate = this.totalUsers > 0 ? (double) this.highlyEngagedUsers / this.totalUsers * 100 : 0.0;
        }

        // Helper methods
        public String getFormattedAverageCompletionRate() {
            return String.format("%.1f%%", averageCompletionRate);
        }

        public String getFormattedHabitAdoptionRate() {
            return String.format("%.1f%%", habitAdoptionRate);
        }

        public String getFormattedHighPerformerRate() {
            return String.format("%.1f%%", highPerformerRate);
        }

        public String getFormattedEngagementRate() {
            return String.format("%.1f%%", engagementRate);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("totalUsers", totalUsers);
            map.put("usersWithHabits", usersWithHabits);
            map.put("usersWithoutHabits", usersWithoutHabits);
            map.put("highPerformers", highPerformers);
            map.put("usersNeedingAttention", usersNeedingAttention);
            map.put("highlyEngagedUsers", highlyEngagedUsers);
            map.put("averageCompletionRate", averageCompletionRate);
            map.put("habitAdoptionRate", habitAdoptionRate);
            map.put("highPerformerRate", highPerformerRate);
            map.put("engagementRate", engagementRate);
            return map;
        }

        // Getters and setters
        public Long getTotalUsers() {
            return totalUsers;
        }

        public void setTotalUsers(Long totalUsers) {
            this.totalUsers = totalUsers;
        }

        public Long getUsersWithHabits() {
            return usersWithHabits;
        }

        public void setUsersWithHabits(Long usersWithHabits) {
            this.usersWithHabits = usersWithHabits;
        }

        public Long getUsersWithoutHabits() {
            return usersWithoutHabits;
        }

        public void setUsersWithoutHabits(Long usersWithoutHabits) {
            this.usersWithoutHabits = usersWithoutHabits;
        }

        public Long getHighPerformers() {
            return highPerformers;
        }

        public void setHighPerformers(Long highPerformers) {
            this.highPerformers = highPerformers;
        }

        public Long getUsersNeedingAttention() {
            return usersNeedingAttention;
        }

        public void setUsersNeedingAttention(Long usersNeedingAttention) {
            this.usersNeedingAttention = usersNeedingAttention;
        }

        public Long getHighlyEngagedUsers() {
            return highlyEngagedUsers;
        }

        public void setHighlyEngagedUsers(Long highlyEngagedUsers) {
            this.highlyEngagedUsers = highlyEngagedUsers;
        }

        public Double getAverageCompletionRate() {
            return averageCompletionRate;
        }

        public void setAverageCompletionRate(Double averageCompletionRate) {
            this.averageCompletionRate = averageCompletionRate;
        }

        public Double getHabitAdoptionRate() {
            return habitAdoptionRate;
        }

        public void setHabitAdoptionRate(Double habitAdoptionRate) {
            this.habitAdoptionRate = habitAdoptionRate;
        }

        public Double getHighPerformerRate() {
            return highPerformerRate;
        }

        public void setHighPerformerRate(Double highPerformerRate) {
            this.highPerformerRate = highPerformerRate;
        }

        public Double getEngagementRate() {
            return engagementRate;
        }

        public void setEngagementRate(Double engagementRate) {
            this.engagementRate = engagementRate;
        }

        @Override
        public String toString() {
            return "FilterStatistics{" +
                    "totalUsers=" + totalUsers +
                    ", usersWithHabits=" + usersWithHabits +
                    ", usersWithoutHabits=" + usersWithoutHabits +
                    ", highPerformers=" + highPerformers +
                    ", usersNeedingAttention=" + usersNeedingAttention +
                    ", highlyEngagedUsers=" + highlyEngagedUsers +
                    ", averageCompletionRate=" + averageCompletionRate +
                    ", habitAdoptionRate=" + habitAdoptionRate +
                    ", highPerformerRate=" + highPerformerRate +
                    ", engagementRate=" + engagementRate +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "AdminHabitOverviewResponse{" +
                "userCount=" + getUserCount() +
                ", hasFilters=" + hasActiveFilters() +
                ", filterSummary='" + filterSummary + '\'' +
                ", filterEfficiency=" + getFilterEffectiveness() +
                ", timestamp=" + timestamp +
                '}';
    }
}
