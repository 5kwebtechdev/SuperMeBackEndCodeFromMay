package com.superme.dto;

import java.util.*;
import java.util.stream.Collectors;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTaskViewDTO {
    private Long userId;
    private String name;
    private String phone;
    private String email;
    private Integer age;
    private String gender;
    private String role;
    private String accountStatus;
    private LocalDateTime lastActive;
    private Long totalTasksCreated;
    private Long totalTasksCompleted;
    private Long totalTasksPending;
    private Long totalTasksOverdue;
    private Long firstTaskDate;
    private Long lastTaskDate;
    private LocalDate firstTaskLocalDate;
    private LocalTime firstTaskLocalTime;
    private LocalDate lastTaskLocalDate;
    private LocalTime lastTaskLocalTime;
    private Double completionRate;
    private TaskStatistics statistics;
    private String highlightedUserId;

    // Getter for userId (explicitly, in case Lombok isn't working)
    public Long getUserId() {
        return userId;
    }

    // ============================================================================
    // SEARCH AND FILTER FUNCTIONALITY
    // ============================================================================

    /**
     * Returns a highlighted version of this DTO for the search term.
     */
    public AdminTaskViewDTO getHighlightedVersion(String searchTerm) {
        AdminTaskViewDTO copy = this; // Shallow copy, for true copy use a copy constructor
        copy.setHighlightedUserId(searchTerm != null && userId != null && userId.toString().contains(searchTerm)
                ? userId.toString()
                : null);
        return copy;
    }

    /**
     * Filter users by search term (userId only)
     */
    public static List<AdminTaskViewDTO> filterBySearchTerm(List<AdminTaskViewDTO> users, String searchTerm) {
        if (users == null || searchTerm == null || searchTerm.trim().isEmpty()) {
            return users != null ? users : new ArrayList<>();
        }

        String lowerSearchTerm = searchTerm.toLowerCase().trim();

        return users.stream()
                .filter(user -> user.getUserId() != null
                        && user.getUserId().toString().toLowerCase().contains(lowerSearchTerm))
                .collect(Collectors.toList());
    }

    /**
     * Filters by completion rate.
     */
    public static List<AdminTaskViewDTO> filterByCompletionRate(List<AdminTaskViewDTO> users, Double min, Double max) {
        return users.stream()
                .filter(user -> {
                    double rate = user.getCompletionRate();
                    boolean matchesMin = min == null || rate >= min;
                    boolean matchesMax = max == null || rate <= max;
                    return matchesMin && matchesMax;
                })
                .collect(Collectors.toList());
    }

    /**
     * Filters by total task count.
     */
    public static List<AdminTaskViewDTO> filterByTaskCount(List<AdminTaskViewDTO> users, Long min, Long max) {
        return users.stream()
                .filter(user -> {
                    long count = user.getTotalTasksCreated();
                    boolean matchesMin = min == null || count >= min;
                    boolean matchesMax = max == null || count <= max;
                    return matchesMin && matchesMax;
                })
                .collect(Collectors.toList());
    }

    /**
     * Filters by overdue status.
     */
    public static List<AdminTaskViewDTO> filterByOverdueStatus(List<AdminTaskViewDTO> users, Boolean hasOverdue) {
        if (hasOverdue == null)
            return users;
        return users.stream()
                .filter(user -> user.hasOverdueTasks() == hasOverdue)
                .collect(Collectors.toList());
    }

    /**
     * Filters by existence of tasks.
     */
    public static List<AdminTaskViewDTO> filterByTaskExistence(List<AdminTaskViewDTO> users, Boolean hasTasks) {
        if (hasTasks == null)
            return users;
        return users.stream()
                .filter(user -> user.hasTasks() == hasTasks)
                .collect(Collectors.toList());
    }

    /**
     * Sorts by completion rate descending.
     */
    public static List<AdminTaskViewDTO> sortByCompletionRate(List<AdminTaskViewDTO> users) {
        return users.stream()
                .sorted((u1, u2) -> Double.compare(u2.getCompletionRate(), u1.getCompletionRate()))
                .collect(Collectors.toList());
    }

    /**
     * Sorts by total task count descending.
     */
    public static List<AdminTaskViewDTO> sortByTaskCount(List<AdminTaskViewDTO> users) {
        return users.stream()
                .sorted((u1, u2) -> Long.compare(u2.getTotalTasksCreated(), u1.getTotalTasksCreated()))
                .collect(Collectors.toList());
    }

    /**
     * Get aggregate statistics for a list of DTOs.
     */
    public static TaskStatistics getTaskStatistics(List<AdminTaskViewDTO> users) {
        long totalCreated = users.stream().mapToLong(AdminTaskViewDTO::getTotalTasksCreated).sum();
        long totalCompleted = users.stream().mapToLong(AdminTaskViewDTO::getTotalTasksCompleted).sum();
        long totalPending = users.stream().mapToLong(AdminTaskViewDTO::getTotalTasksPending).sum();
        long totalOverdue = users.stream().mapToLong(AdminTaskViewDTO::getTotalTasksOverdue).sum();
        double averageTasksPerUser = users.size() > 0 ? (double) totalCreated / users.size() : 0.0;
        long activeUsers = users.stream().filter(AdminTaskViewDTO::hasTasks).count();

        return new TaskStatistics(
                totalCreated,
                totalCompleted,
                totalPending,
                totalOverdue,
                averageTasksPerUser,
                activeUsers);
    }

    // Completion rate calculation logic
    private Double calculateCompletionRate() {
        if (totalTasksCreated == null || totalTasksCreated == 0)
            return 0.0;
        return ((double) getTotalTasksCompleted() / totalTasksCreated) * 100;
    }

    public void setTotalTasksCreated(Long totalTasksCreated) {
        this.totalTasksCreated = totalTasksCreated != null ? totalTasksCreated : 0L;
        this.completionRate = calculateCompletionRate();
    }

    public Long getTotalTasksCreated() {
        return totalTasksCreated != null ? totalTasksCreated : 0L;
    }

    public Long getTotalTasksCompleted() {
        return totalTasksCompleted != null ? totalTasksCompleted : 0L;
    }

    public void setTotalTasksCompleted(Long totalTasksCompleted) {
        this.totalTasksCompleted = totalTasksCompleted != null ? totalTasksCompleted : 0L;
        this.completionRate = calculateCompletionRate();
    }

    public Long getTotalTasksPending() {
        return totalTasksPending != null ? totalTasksPending : 0L;
    }

    public void setTotalTasksPending(Long totalTasksPending) {
        this.totalTasksPending = totalTasksPending != null ? totalTasksPending : 0L;
    }

    public Long getTotalTasksOverdue() {
        return totalTasksOverdue != null ? totalTasksOverdue : 0L;
    }

    public void setTotalTasksOverdue(Long totalTasksOverdue) {
        this.totalTasksOverdue = totalTasksOverdue != null ? totalTasksOverdue : 0L;
    }

    public Long getFirstTaskDate() {
        return firstTaskDate;
    }

    public void setFirstTaskDate(Long firstTaskDate) {
        this.firstTaskDate = firstTaskDate;
        if (firstTaskDate != null) {
            Instant instant = Instant.ofEpochMilli(firstTaskDate);
            this.firstTaskLocalDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
            this.firstTaskLocalTime = instant.atZone(ZoneId.systemDefault()).toLocalTime();
        } else {
            this.firstTaskLocalDate = null;
            this.firstTaskLocalTime = null;
        }
    }

    public Long getLastTaskDate() {
        return lastTaskDate;
    }

    public void setLastTaskDate(Long lastTaskDate) {
        this.lastTaskDate = lastTaskDate;
        if (lastTaskDate != null) {
            Instant instant = Instant.ofEpochMilli(lastTaskDate);
            this.lastTaskLocalDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
            this.lastTaskLocalTime = instant.atZone(ZoneId.systemDefault()).toLocalTime();
        } else {
            this.lastTaskLocalDate = null;
            this.lastTaskLocalTime = null;
        }
    }

    public Double getCompletionRate() {
        return completionRate != null ? completionRate : 0.0;
    }

    public void setCompletionRate(Double completionRate) {
        this.completionRate = completionRate != null ? completionRate : 0.0;
    }

    public TaskStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(TaskStatistics statistics) {
        this.statistics = statistics != null ? statistics : new TaskStatistics();
    }

    // Highlighted field getters
    public String getHighlightedUserId() {
        return highlightedUserId != null ? highlightedUserId : (userId != null ? userId.toString() : "N/A");
    }

    public void setHighlightedUserId(String highlightedUserId) {
        this.highlightedUserId = highlightedUserId;
    }

    // ============================================================================
    // HELPER METHODS FOR DISPLAY
    // ============================================================================

    public String getFormattedFirstTaskDate() {
        if (firstTaskLocalDate == null) {
            return "No tasks";
        }
        return firstTaskLocalDate.toString()
                + (firstTaskLocalTime != null ? (" " + firstTaskLocalTime.toString()) : "");
    }

    public String getFormattedLastTaskDate() {
        if (lastTaskLocalDate == null) {
            return "No tasks";
        }
        return lastTaskLocalDate.toString() + (lastTaskLocalTime != null ? (" " + lastTaskLocalTime.toString()) : "");
    }

    public String getFormattedCompletionRate() {
        return String.format("%.1f%%", getCompletionRate());
    }

    public boolean hasTasks() {
        return getTotalTasksCreated() > 0;
    }

    public boolean hasOverdueTasks() {
        return getTotalTasksOverdue() > 0;
    }

    // Calculate overdue rate
    public Double getOverdueRate() {
        if (totalTasksCreated == null || totalTasksCreated == 0) {
            return 0.0;
        }
        return ((double) getTotalTasksOverdue() / totalTasksCreated) * 100;
    }

    public String getFormattedOverdueRate() {
        return String.format("%.1f%%", getOverdueRate());
    }

    // Helper method to get task status summary
    public String getTaskStatusSummary() {
        return String.format("Created: %d, Completed: %d, Pending: %d, Overdue: %d",
                getTotalTasksCreated(), getTotalTasksCompleted(),
                getTotalTasksPending(), getTotalTasksOverdue());
    }

    // Helper method to get performance level
    public String getPerformanceLevel() {
        double rate = getCompletionRate();
        if (rate >= 80)
            return "Excellent";
        if (rate >= 60)
            return "Good";
        if (rate >= 40)
            return "Average";
        if (rate >= 20)
            return "Below Average";
        return "Needs Attention";
    }

    // Check if user needs attention (high overdue rate or low completion rate)
    public boolean needsAttention() {
        return getOverdueRate() > 30.0 || (hasTasks() && getCompletionRate() < 40.0);
    }

    // ============================================================================
    // SEARCH-RELATED HELPER METHODS
    // ============================================================================

    /**
     * Get search summary for this user
     * 
     * @param searchTerm The search term used
     * @return Summary string for search results
     */
    public String getSearchSummary(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getTaskStatusSummary();
        }

        List<String> matches = new ArrayList<>();
        String lowerSearchTerm = searchTerm.toLowerCase();

        if (userId != null && userId.toString().toLowerCase().contains(lowerSearchTerm)) {
            matches.add("User ID");
        }
        String matchedFields = matches.isEmpty() ? "No matches" : String.join(", ", matches);
        return String.format("Matched: %s | %s", matchedFields, getTaskStatusSummary());
    }

    /**
     * Check if user matches specific search criteria
     * 
     * @param searchTerm     Search term to match
     * @param searchUserId   Whether to search in user ID
     * @param searchUsername Whether to search in username
     * @return true if user matches the criteria
     */
    public boolean matchesSearchCriteria(String searchTerm, boolean searchUserId, boolean searchUsername) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return true;
        }

        String lowerSearchTerm = searchTerm.toLowerCase().trim();

        if (searchUserId && userId != null &&
                userId.toString().toLowerCase().contains(lowerSearchTerm)) {
            return true;
        }

        return false;
    }

    // ============================================================================
    // STATIC INNER CLASS FOR TASK STATISTICS
    // ============================================================================

    public static class TaskStatistics {
        private Long totalTasksCreated;
        private Long totalTasksCompleted;
        private Long totalTasksPending;
        private Long totalTasksOverdue;
        private Double averageTasksPerUser;
        private Long activeUsers; // Users with at least one task
        private Double systemCompletionRate;
        private Double systemOverdueRate;

        public TaskStatistics() {
            this.totalTasksCreated = 0L;
            this.totalTasksCompleted = 0L;
            this.totalTasksPending = 0L;
            this.totalTasksOverdue = 0L;
            this.averageTasksPerUser = 0.0;
            this.activeUsers = 0L;
            this.systemCompletionRate = 0.0;
            this.systemOverdueRate = 0.0;
        }

        public TaskStatistics(Long totalTasksCreated, Long totalTasksCompleted,
                Long totalTasksPending, Long totalTasksOverdue,
                Double averageTasksPerUser, Long activeUsers) {
            this.totalTasksCreated = totalTasksCreated != null ? totalTasksCreated : 0L;
            this.totalTasksCompleted = totalTasksCompleted != null ? totalTasksCompleted : 0L;
            this.totalTasksPending = totalTasksPending != null ? totalTasksPending : 0L;
            this.totalTasksOverdue = totalTasksOverdue != null ? totalTasksOverdue : 0L;
            this.averageTasksPerUser = averageTasksPerUser != null ? averageTasksPerUser : 0.0;
            this.activeUsers = activeUsers != null ? activeUsers : 0L;
            this.systemCompletionRate = calculateSystemCompletionRate();
            this.systemOverdueRate = calculateSystemOverdueRate();
        }

        private Double calculateSystemCompletionRate() {
            if (totalTasksCreated == 0) {
                return 0.0;
            }
            return ((double) totalTasksCompleted / totalTasksCreated) * 100;
        }

        private Double calculateSystemOverdueRate() {
            if (totalTasksCreated == 0) {
                return 0.0;
            }
            return ((double) totalTasksOverdue / totalTasksCreated) * 100;
        }

        // Getters and setters
        public Long getTotalTasksCreated() {
            return totalTasksCreated;
        }

        public void setTotalTasksCreated(Long totalTasksCreated) {
            this.totalTasksCreated = totalTasksCreated != null ? totalTasksCreated : 0L;
            this.systemCompletionRate = calculateSystemCompletionRate();
            this.systemOverdueRate = calculateSystemOverdueRate();
        }

        public Long getTotalTasksCompleted() {
            return totalTasksCompleted;
        }

        public void setTotalTasksCompleted(Long totalTasksCompleted) {
            this.totalTasksCompleted = totalTasksCompleted != null ? totalTasksCompleted : 0L;
            this.systemCompletionRate = calculateSystemCompletionRate();
        }

        public Long getTotalTasksPending() {
            return totalTasksPending;
        }

        public void setTotalTasksPending(Long totalTasksPending) {
            this.totalTasksPending = totalTasksPending != null ? totalTasksPending : 0L;
        }

        public Long getTotalTasksOverdue() {
            return totalTasksOverdue;
        }

        public void setTotalTasksOverdue(Long totalTasksOverdue) {
            this.totalTasksOverdue = totalTasksOverdue != null ? totalTasksOverdue : 0L;
            this.systemOverdueRate = calculateSystemOverdueRate();
        }

        public Double getAverageTasksPerUser() {
            return averageTasksPerUser;
        }

        public void setAverageTasksPerUser(Double averageTasksPerUser) {
            this.averageTasksPerUser = averageTasksPerUser != null ? averageTasksPerUser : 0.0;
        }

        public Long getActiveUsers() {
            return activeUsers;
        }

        public void setActiveUsers(Long activeUsers) {
            this.activeUsers = activeUsers != null ? activeUsers : 0L;
        }

        public Double getSystemCompletionRate() {
            return systemCompletionRate;
        }

        public void setSystemCompletionRate(Double systemCompletionRate) {
            this.systemCompletionRate = systemCompletionRate != null ? systemCompletionRate : 0.0;
        }

        public Double getSystemOverdueRate() {
            return systemOverdueRate;
        }

        public void setSystemOverdueRate(Double systemOverdueRate) {
            this.systemOverdueRate = systemOverdueRate != null ? systemOverdueRate : 0.0;
        }

        // Helper methods
        public String getFormattedSystemCompletionRate() {
            return String.format("%.1f%%", systemCompletionRate);
        }

        public String getFormattedSystemOverdueRate() {
            return String.format("%.1f%%", systemOverdueRate);
        }

        @Override
        public String toString() {
            return "TaskStatistics{" +
                    "totalTasksCreated=" + totalTasksCreated +
                    ", totalTasksCompleted=" + totalTasksCompleted +
                    ", totalTasksPending=" + totalTasksPending +
                    ", totalTasksOverdue=" + totalTasksOverdue +
                    ", averageTasksPerUser=" + averageTasksPerUser +
                    ", activeUsers=" + activeUsers +
                    ", systemCompletionRate=" + systemCompletionRate +
                    ", systemOverdueRate=" + systemOverdueRate +
                    '}';
        }
    }

    // Explicit getter for email, in case Lombok isn't working
    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return "AdminTaskViewDTO{" +
                "userId=" + getUserId() +
                ", email='" + getEmail() + '\'' +
                ", totalTasksCreated=" + getTotalTasksCreated() +
                ", totalTasksCompleted=" + getTotalTasksCompleted() +
                ", totalTasksOverdue=" + getTotalTasksOverdue() +
                ", completionRate=" + getCompletionRate() +
                '}';
    }
}