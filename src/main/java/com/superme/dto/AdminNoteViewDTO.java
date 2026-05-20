package com.superme.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.*;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@Builder
public class AdminNoteViewDTO {
    // Getter for totalNotes for stream/mapToLong compatibility
    public Long getTotalNotes() {
        return totalNotes != null ? totalNotes : 0L;
    }

    // Getter for notesCreatedThisMonth for stream/mapToLong compatibility
    public Long getNotesCreatedThisMonth() {
        return notesCreatedThisMonth != null ? notesCreatedThisMonth : 0L;
    }

    private Long userId;
    private String name;
    private String phone;
    private String email;
    private Integer age;
    private String gender;
    private String role;
    private String accountStatus;           // "active" | "inactive"
    private java.time.LocalDateTime lastActive;
    private Long totalNotes;
    private Long notesCreatedThisMonth;
    private java.time.LocalDate firstNoteDate;
    private java.time.LocalTime firstNoteTime;
    private java.time.LocalDate lastNoteDate;
    private java.time.LocalTime lastNoteTime;
    private NoteStatistics statistics;
    private String highlightedUserId;

    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================

    // Default constructor
    public AdminNoteViewDTO() {
        this.statistics = new NoteStatistics();
    }

    // Constructor with all fields (LocalDate/LocalTime only)
    public AdminNoteViewDTO(Long userId, String email,
            Long totalNotes, Long notesCreatedThisMonth,
            java.time.LocalDate firstNoteDate, java.time.LocalTime firstNoteTime,
            java.time.LocalDate lastNoteDate, java.time.LocalTime lastNoteTime,
            NoteStatistics statistics) {
        this.userId = userId;
        this.email = nullSafeString(email);
        this.totalNotes = totalNotes != null ? totalNotes : 0L;
        this.notesCreatedThisMonth = notesCreatedThisMonth != null ? notesCreatedThisMonth : 0L;
        this.firstNoteDate = firstNoteDate;
        this.firstNoteTime = firstNoteTime;
        this.lastNoteDate = lastNoteDate;
        this.lastNoteTime = lastNoteTime;
        this.statistics = statistics != null ? statistics : new NoteStatistics();

        // Initialize highlighted fields
        this.highlightedUserId = this.userId != null ? this.userId.toString() : "N/A";
    }

    // ============================================================================
    // SEARCH AND FILTER FUNCTIONALITY
    // ============================================================================

    /**
     * Filter users by search term (userId)
     * 
     * @param users      List of AdminNoteViewDTO to filter
     * @param searchTerm The search term to filter by
     * @return Filtered list of users
     */
    public static List<AdminNoteViewDTO> filterBySearchTerm(List<AdminNoteViewDTO> users, String searchTerm) {
        if (users == null || searchTerm == null || searchTerm.trim().isEmpty()) {
            return users != null ? users : new ArrayList<>();
        }

        String lowerSearchTerm = searchTerm.toLowerCase().trim();

        return users.stream()
                .filter(user -> matchesSearchTerm(user, lowerSearchTerm))
                .collect(Collectors.toList());
    }

    /**
     * Check if a user matches the search term
     * 
     * @param user       The user to check
     * @param searchTerm The search term (already lowercase)
     * @return true if user matches search term
     */
    private static boolean matchesSearchTerm(AdminNoteViewDTO user, String searchTerm) {
        if (user.getUserId() != null && user.getUserId().toString().contains(searchTerm)) return true;
        if (user.getName()   != null && user.getName().toLowerCase().contains(searchTerm))  return true;
        if (user.getEmail()  != null && user.getEmail().toLowerCase().contains(searchTerm)) return true;
        if (user.getPhone()  != null && user.getPhone().toLowerCase().contains(searchTerm)) return true;
        return false;
    }

    /**
     * Get highlighted version of this user for search results
     * 
     * @param searchTerm The search term to highlight
     * @return Copy of this user with highlighted fields
     */
    public AdminNoteViewDTO getHighlightedVersion(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return this;
        }

        AdminNoteViewDTO highlighted = new AdminNoteViewDTO();

        // Copy all original fields
        highlighted.userId = this.userId;
        highlighted.email = this.email;
        highlighted.totalNotes = this.totalNotes;
        highlighted.notesCreatedThisMonth = this.notesCreatedThisMonth;
        highlighted.firstNoteDate = this.firstNoteDate;
        highlighted.lastNoteDate = this.lastNoteDate;
        highlighted.statistics = this.statistics;

        // Apply highlighting
        highlighted.highlightedUserId = highlightText(
                this.userId != null ? this.userId.toString() : "N/A",
                searchTerm);

        return highlighted;
    }

    /**
     * Apply highlighting to text by wrapping matches with <mark> tags
     * 
     * @param text       The text to highlight
     * @param searchTerm The term to highlight
     * @return Text with highlighted search terms
     */
    private static String highlightText(String text, String searchTerm) {
        if (text == null || searchTerm == null || searchTerm.trim().isEmpty()) {
            return text;
        }

        // Use case-insensitive replacement with regex
        String pattern = "(?i)" + java.util.regex.Pattern.quote(searchTerm.trim());
        return text.replaceAll(pattern, "<mark>$0</mark>");
    }

    /**
     * Clear search highlighting and return original values
     */
    public void clearHighlighting() {
        this.highlightedUserId = userId != null ? userId.toString() : "N/A";
    }

    /**
     * Check if this user has highlighting applied
     * 
     * @return true if highlighting is applied
     */
    public boolean hasHighlighting() {
        return (highlightedUserId != null && highlightedUserId.contains("<mark>"));
    }

    // ============================================================================
    // FILTER CRITERIA AND STATISTICS
    // ============================================================================

    public static List<AdminNoteViewDTO> filterByNoteCount(List<AdminNoteViewDTO> users,
            Long minNotes,
            Long maxNotes) {
        if (users == null) {
            return new ArrayList<>();
        }

        return users.stream()
                .filter(user -> {
                    Long noteCount = user.getTotalNotes();
                    boolean minCheck = minNotes == null || noteCount >= minNotes;
                    boolean maxCheck = maxNotes == null || noteCount <= maxNotes;
                    return minCheck && maxCheck;
                })
                .collect(Collectors.toList());
    }

    public static List<AdminNoteViewDTO> filterByMonthlyActivity(List<AdminNoteViewDTO> users,
            Long minMonthlyNotes,
            Long maxMonthlyNotes) {
        if (users == null) {
            return new ArrayList<>();
        }

        return users.stream()
                .filter(user -> {
                    Long monthlyNotes = user.getNotesCreatedThisMonth();
                    boolean minCheck = minMonthlyNotes == null || monthlyNotes >= minMonthlyNotes;
                    boolean maxCheck = maxMonthlyNotes == null || monthlyNotes <= maxMonthlyNotes;
                    return minCheck && maxCheck;
                })
                .collect(Collectors.toList());
    }

    public static List<AdminNoteViewDTO> filterByNoteExistence(List<AdminNoteViewDTO> users,
            Boolean hasNotes) {
        if (users == null || hasNotes == null) {
            return users != null ? users : new ArrayList<>();
        }

        return users.stream()
                .filter(user -> user.hasNotes() == hasNotes)
                .collect(Collectors.toList());
    }

    public static List<AdminNoteViewDTO> filterByRecentActivity(List<AdminNoteViewDTO> users,
            Boolean hasRecentActivity) {
        if (users == null || hasRecentActivity == null) {
            return users != null ? users : new ArrayList<>();
        }

        return users.stream()
                .filter(user -> user.hasRecentActivity() == hasRecentActivity)
                .collect(Collectors.toList());
    }

    public static List<AdminNoteViewDTO> filterByActivityScore(List<AdminNoteViewDTO> users,
            Double minActivityScore,
            Double maxActivityScore) {
        if (users == null) {
            return new ArrayList<>();
        }

        return users.stream()
                .filter(user -> {
                    Double score = user.getActivityScore();
                    boolean minCheck = minActivityScore == null || score >= minActivityScore;
                    boolean maxCheck = maxActivityScore == null || score <= maxActivityScore;
                    return minCheck && maxCheck;
                })
                .collect(Collectors.toList());
    }

    public static NoteStatistics getNoteStatistics(List<AdminNoteViewDTO> users) {
        if (users == null || users.isEmpty()) {
            return new NoteStatistics();
        }

        long totalNotes = users.stream()
                .mapToLong(AdminNoteViewDTO::getTotalNotes)
                .sum();

        long totalMonthlyNotes = users.stream()
                .mapToLong(AdminNoteViewDTO::getNotesCreatedThisMonth)
                .sum();

        long usersWithNotes = users.stream()
                .filter(AdminNoteViewDTO::hasNotes)
                .count();

        double averageNotesPerUser = users.size() > 0 ? (double) totalNotes / users.size() : 0.0;

        return new NoteStatistics(totalNotes, (long) users.size(),
                averageNotesPerUser, usersWithNotes,
                totalMonthlyNotes);
    }

    public static List<AdminNoteViewDTO> sortByNoteCount(List<AdminNoteViewDTO> users) {
        if (users == null) {
            return new ArrayList<>();
        }

        return users.stream()
                .sorted((u1, u2) -> Long.compare(u2.getTotalNotes(), u1.getTotalNotes()))
                .collect(Collectors.toList());
    }

    public static List<AdminNoteViewDTO> sortByMonthlyActivity(List<AdminNoteViewDTO> users) {
        if (users == null) {
            return new ArrayList<>();
        }

        return users.stream()
                .sorted((u1, u2) -> Long.compare(u2.getNotesCreatedThisMonth(), u1.getNotesCreatedThisMonth()))
                .collect(Collectors.toList());
    }

    public static List<AdminNoteViewDTO> sortByActivityScore(List<AdminNoteViewDTO> users) {
        if (users == null) {
            return new ArrayList<>();
        }

        return users.stream()
                .sorted((u1, u2) -> Double.compare(u2.getActivityScore(), u1.getActivityScore()))
                .collect(Collectors.toList());
    }

    // ============================================================================
    // SEARCH-RELATED HELPER METHODS
    // ============================================================================

    /**
     * Check if user matches specific search criteria
     * 
     * @param searchTerm   Search term to match
     * @param searchUserId Whether to search in user ID
     * @return true if user matches the criteria
     */
    public boolean matchesSearchCriteria(String searchTerm, boolean searchUserId) {
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

    /**
     * Get search summary for this user
     * 
     * @param searchTerm The search term used
     * @return Summary string for search results
     */
    public String getSearchSummary(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getNoteActivitySummary();
        }

        List<String> matches = new ArrayList<>();
        String lowerSearchTerm = searchTerm.toLowerCase();

        if (userId != null && userId.toString().toLowerCase().contains(lowerSearchTerm)) {
            matches.add("User ID");
        }

        String matchedFields = matches.isEmpty() ? "No matches" : String.join(", ", matches);
        return String.format("Matched: %s | %s", matchedFields, getNoteActivitySummary());
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

    // ============================================================================
    // HELPER METHODS FOR DISPLAY
    // ============================================================================

    public boolean hasNotes() {
        return getTotalNotes() > 0;
    }

    public boolean hasRecentActivity() {
        return getNotesCreatedThisMonth() > 0;
    }

    public boolean isActiveUser() {
        return hasNotes() && hasRecentActivity();
    }

    // Calculate activity score based on total and recent notes
    public Double getActivityScore() {
        if (getTotalNotes() == 0)
            return 0.0;
        return (double) getNotesCreatedThisMonth() / getTotalNotes() * 100;
    }

    public String getFormattedActivityScore() {
        return String.format("%.1f%%", getActivityScore());
    }

    public String getFormattedFirstNoteDate() {
        if (firstNoteDate == null) {
            return "No notes";
        }
        return java.util.Date.from(firstNoteDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()).toString();
    }

    public String getFormattedLastNoteDate() {
        if (lastNoteDate == null) {
            return "No notes";
        }
        return java.util.Date.from(lastNoteDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()).toString();
    }

    // Helper method to get note activity summary
    public String getNoteActivitySummary() {
        return String.format("Total: %d, This Month: %d, Activity: %.1f%%",
                getTotalNotes(), getNotesCreatedThisMonth(), getActivityScore());
    }

    // Helper method to get engagement level
    public String getEngagementLevel() {
        double score = getActivityScore();
        if (!hasNotes())
            return "No Activity";
        if (score >= 80)
            return "Very High";
        if (score >= 60)
            return "High";
        if (score >= 40)
            return "Medium";
        if (score >= 20)
            return "Low";
        return "Very Low";
    }

    // Check if user needs attention (has notes but no recent activity)
    public boolean needsAttention() {
        return hasNotes() && !hasRecentActivity();
    }

    // Check if user is highly engaged (high activity score and recent activity)
    public boolean isHighlyEngaged() {
        return hasRecentActivity() && getActivityScore() >= 60.0;
    }

    // ============================================================================
    // ENHANCED NOTE STATISTICS CLASS
    // ============================================================================

    public static class NoteStatistics {
        private Long totalNotesAllUsers;
        private Long totalUsers;
        private Double averageNotesPerUser;
        private Long usersWithNotes;
        private Long totalMonthlyNotes;
        private Double monthlyGrowthRate;

        public NoteStatistics() {
            this.totalNotesAllUsers = 0L;
            this.totalUsers = 0L;
            this.averageNotesPerUser = 0.0;
            this.usersWithNotes = 0L;
            this.totalMonthlyNotes = 0L;
            this.monthlyGrowthRate = 0.0;
        }

        public NoteStatistics(Long totalNotesAllUsers, Long totalUsers,
                Double averageNotesPerUser, Long usersWithNotes) {
            this.totalNotesAllUsers = totalNotesAllUsers != null ? totalNotesAllUsers : 0L;
            this.totalUsers = totalUsers != null ? totalUsers : 0L;
            this.averageNotesPerUser = averageNotesPerUser != null ? averageNotesPerUser : 0.0;
            this.usersWithNotes = usersWithNotes != null ? usersWithNotes : 0L;
            this.totalMonthlyNotes = 0L;
            this.monthlyGrowthRate = 0.0;
        }

        public NoteStatistics(Long totalNotesAllUsers, Long totalUsers,
                Double averageNotesPerUser, Long usersWithNotes,
                Long totalMonthlyNotes) {
            this.totalNotesAllUsers = totalNotesAllUsers != null ? totalNotesAllUsers : 0L;
            this.totalUsers = totalUsers != null ? totalUsers : 0L;
            this.averageNotesPerUser = averageNotesPerUser != null ? averageNotesPerUser : 0.0;
            this.usersWithNotes = usersWithNotes != null ? usersWithNotes : 0L;
            this.totalMonthlyNotes = totalMonthlyNotes != null ? totalMonthlyNotes : 0L;
            this.monthlyGrowthRate = calculateMonthlyGrowthRate();
        }

        private Double calculateMonthlyGrowthRate() {
            if (totalNotesAllUsers == 0)
                return 0.0;
            return ((double) totalMonthlyNotes / totalNotesAllUsers) * 100;
        }

        // Getters and setters
        public Long getTotalNotesAllUsers() {
            return totalNotesAllUsers;
        }

        public void setTotalNotesAllUsers(Long totalNotesAllUsers) {
            this.totalNotesAllUsers = totalNotesAllUsers != null ? totalNotesAllUsers : 0L;
            this.monthlyGrowthRate = calculateMonthlyGrowthRate();
        }

        public Long getTotalUsers() {
            return totalUsers;
        }

        public void setTotalUsers(Long totalUsers) {
            this.totalUsers = totalUsers != null ? totalUsers : 0L;
        }

        public Double getAverageNotesPerUser() {
            return averageNotesPerUser;
        }

        public void setAverageNotesPerUser(Double averageNotesPerUser) {
            this.averageNotesPerUser = averageNotesPerUser != null ? averageNotesPerUser : 0.0;
        }

        public Long getUsersWithNotes() {
            return usersWithNotes;
        }

        public void setUsersWithNotes(Long usersWithNotes) {
            this.usersWithNotes = usersWithNotes != null ? usersWithNotes : 0L;
        }

        public Long getTotalMonthlyNotes() {
            return totalMonthlyNotes;
        }

        public void setTotalMonthlyNotes(Long totalMonthlyNotes) {
            this.totalMonthlyNotes = totalMonthlyNotes != null ? totalMonthlyNotes : 0L;
            this.monthlyGrowthRate = calculateMonthlyGrowthRate();
        }

        public Double getMonthlyGrowthRate() {
            return monthlyGrowthRate;
        }

        public void setMonthlyGrowthRate(Double monthlyGrowthRate) {
            this.monthlyGrowthRate = monthlyGrowthRate != null ? monthlyGrowthRate : 0.0;
        }

        // Helper methods
        public String getFormattedAverageNotesPerUser() {
            return String.format("%.1f", averageNotesPerUser);
        }

        public Double getEngagementRate() {
            return totalUsers > 0 ? (double) usersWithNotes / totalUsers * 100 : 0.0;
        }

        public String getFormattedEngagementRate() {
            return String.format("%.1f%%", getEngagementRate());
        }

        public String getFormattedMonthlyGrowthRate() {
            return String.format("%.1f%%", monthlyGrowthRate);
        }

        public Long getUsersWithoutNotes() {
            return totalUsers - usersWithNotes;
        }

        public Double getMonthlyNotesPerUser() {
            return totalUsers > 0 ? (double) totalMonthlyNotes / totalUsers : 0.0;
        }

        public String getFormattedMonthlyNotesPerUser() {
            return String.format("%.1f", getMonthlyNotesPerUser());
        }

        @Override
        public String toString() {
            return "NoteStatistics{" +
                    "totalNotesAllUsers=" + totalNotesAllUsers +
                    ", totalUsers=" + totalUsers +
                    ", averageNotesPerUser=" + averageNotesPerUser +
                    ", usersWithNotes=" + usersWithNotes +
                    ", totalMonthlyNotes=" + totalMonthlyNotes +
                    ", monthlyGrowthRate=" + monthlyGrowthRate +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "AdminNoteViewDTO{" +
                "userId=" + userId +
                ", totalNotes=" + getTotalNotes() +
                ", notesCreatedThisMonth=" + getNotesCreatedThisMonth() +
                ", activityScore=" + getActivityScore() +
                '}';
    }
}
