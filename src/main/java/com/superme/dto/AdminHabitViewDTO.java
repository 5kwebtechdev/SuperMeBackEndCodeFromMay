package com.superme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data Transfer Object for Admin Habit View operations.
 *
 * Provides comprehensive habit analytics and overview functionality
 * for administrative dashboard with advanced search and filtering capabilities.
 *
 * @author MindfullB Admin Team
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminHabitViewDTO {
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String gender;
    private Integer age;
    private String ageGroup;
    private String accountStatus;
    private String role;
    private Long totalHabitsCreated;
    private Long totalHabitsCompleted;
    private Long totalHabitsPending;
    private LocalDate firstHabitDate;
    private LocalDate lastHabitDate;
    private Double completionRate;

    // Engagement metrics
    private Integer currentStreak;
    private Integer highestStreak;
    private Integer coins;
    private LocalDateTime lastLoginDate;
    private LocalDateTime registrationDate;

    // Statistics for stat cards
    private HabitStatistics statistics;

    // Search highlighting support
    private String highlightedUserId;

    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================

    /**
     * Constructor with all fields
     */
    public AdminHabitViewDTO(Long userId, String email, String phone,
                             String gender, Integer age, String ageGroup,
                             String accountStatus, Long totalHabitsCreated,
                             Long totalHabitsCompleted, Long totalHabitsPending,
                             LocalDate firstHabitDate, LocalDate lastHabitDate,
                             Integer currentStreak, Integer highestStreak, Integer coins,
                             LocalDateTime lastLoginDate, LocalDateTime registrationDate,
                             HabitStatistics statistics) {
        this.userId = userId;
        this.email = nullSafeString(email);
        this.phone = nullSafeString(phone);
        this.gender = nullSafeString(gender);
        this.age = age != null ? age : 0;
        this.ageGroup = nullSafeString(ageGroup);
        this.accountStatus = nullSafeString(accountStatus);
        this.totalHabitsCreated = totalHabitsCreated != null ? totalHabitsCreated : 0L;
        this.totalHabitsCompleted = totalHabitsCompleted != null ? totalHabitsCompleted : 0L;
        this.totalHabitsPending = totalHabitsPending != null ? totalHabitsPending : 0L;
        this.firstHabitDate = firstHabitDate;
        this.lastHabitDate = lastHabitDate;
        this.currentStreak = currentStreak != null ? currentStreak : 0;
        this.highestStreak = highestStreak != null ? highestStreak : 0;
        this.coins = coins != null ? coins : 0;
        this.lastLoginDate = lastLoginDate;
        this.registrationDate = registrationDate;
        this.statistics = statistics != null ? statistics : new HabitStatistics();

        // Ensure completionRate is always calculated and not null
        this.completionRate = calculateCompletionRate();
        if (this.completionRate == null) {
            this.completionRate = 0.0;
        }
    }

    // ============================================================================
    // SEARCH AND FILTER METHODS
    // ============================================================================

    /**
     * Filters list of users by search term (userId, email, or phone)
     */
    public static List<AdminHabitViewDTO> filterBySearchTerm(List<AdminHabitViewDTO> users, String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return users;
        }

        String lowerSearchTerm = searchTerm.toLowerCase().trim();

        return users.stream()
                .filter(user -> user.matchesSearchTerm(lowerSearchTerm))
                .collect(Collectors.toList());
    }

    /**
     * Filters users by age group
     */
    public static List<AdminHabitViewDTO> filterByAgeGroup(List<AdminHabitViewDTO> users, String ageGroup) {
        if (ageGroup == null || ageGroup.trim().isEmpty()) {
            return users;
        }

        return users.stream()
                .filter(user -> ageGroup.equalsIgnoreCase(user.getAgeGroup()))
                .collect(Collectors.toList());
    }

    /**
     * Filters users by gender
     */
    public static List<AdminHabitViewDTO> filterByGender(List<AdminHabitViewDTO> users, String gender) {
        if (gender == null || gender.trim().isEmpty()) {
            return users;
        }

        return users.stream()
                .filter(user -> gender.equalsIgnoreCase(user.getGender()))
                .collect(Collectors.toList());
    }

    /**
     * Filters users by account status
     */
    public static List<AdminHabitViewDTO> filterByAccountStatus(List<AdminHabitViewDTO> users, String status) {
        if (status == null || status.trim().isEmpty()) {
            return users;
        }

        return users.stream()
                .filter(user -> status.equalsIgnoreCase(user.getAccountStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Filters users by completion rate range
     */
    public static List<AdminHabitViewDTO> filterByCompletionRate(List<AdminHabitViewDTO> users,
                                                                 Double minRate, Double maxRate) {
        return users.stream()
                .filter(user -> {
                    double rate = user.getCompletionRate();
                    boolean minCheck = minRate == null || rate >= minRate;
                    boolean maxCheck = maxRate == null || rate <= maxRate;
                    return minCheck && maxCheck;
                })
                .collect(Collectors.toList());
    }

    /**
     * Filters users by habit count range
     */
    public static List<AdminHabitViewDTO> filterByHabitCount(List<AdminHabitViewDTO> users,
                                                             Long minCount, Long maxCount) {
        return users.stream()
                .filter(user -> {
                    long count = user.getTotalHabitsCreated();
                    boolean minCheck = minCount == null || count >= minCount;
                    boolean maxCheck = maxCount == null || count <= maxCount;
                    return minCheck && maxCheck;
                })
                .collect(Collectors.toList());
    }

    /**
     * Filters users by streak level
     */
    public static List<AdminHabitViewDTO> filterByStreakLevel(List<AdminHabitViewDTO> users,
                                                              Integer minStreak, Integer maxStreak) {
        return users.stream()
                .filter(user -> {
                    int streak = user.getCurrentStreak();
                    boolean minCheck = minStreak == null || streak >= minStreak;
                    boolean maxCheck = maxStreak == null || streak <= maxStreak;
                    return minCheck && maxCheck;
                })
                .collect(Collectors.toList());
    }

    /**
     * Filters users by habit existence
     */
    public static List<AdminHabitViewDTO> filterByHabitExistence(List<AdminHabitViewDTO> users, boolean hasHabits) {
        return users.stream()
                .filter(user -> user.hasHabits() == hasHabits)
                .collect(Collectors.toList());
    }

    /**
     * Filters users by high performance (completion rate >= 70%)
     */
    public static List<AdminHabitViewDTO> filterByPerformance(List<AdminHabitViewDTO> users, boolean isHighPerformer) {
        return users.stream()
                .filter(user -> user.isHighPerformer() == isHighPerformer)
                .collect(Collectors.toList());
    }

    /**
     * Sorts users by completion rate in descending order
     */
    public static List<AdminHabitViewDTO> sortByCompletionRate(List<AdminHabitViewDTO> users) {
        return users.stream()
                .sorted((u1, u2) -> Double.compare(u2.getCompletionRate(), u1.getCompletionRate()))
                .collect(Collectors.toList());
    }

    /**
     * Sorts users by habit count in descending order
     */
    public static List<AdminHabitViewDTO> sortByHabitCount(List<AdminHabitViewDTO> users) {
        return users.stream()
                .sorted((u1, u2) -> Long.compare(u2.getTotalHabitsCreated(), u1.getTotalHabitsCreated()))
                .collect(Collectors.toList());
    }

    /**
     * Gets habit statistics for a list of users
     */
    public static HabitStatistics getHabitStatistics(List<AdminHabitViewDTO> users) {
        if (users == null || users.isEmpty()) {
            return new HabitStatistics();
        }

        long totalCreated = users.stream().mapToLong(AdminHabitViewDTO::getTotalHabitsCreated).sum();
        long totalCompleted = users.stream().mapToLong(AdminHabitViewDTO::getTotalHabitsCompleted).sum();
        long totalPending = totalCreated - totalCompleted;
        double averagePerUser = users.size() > 0 ? (double) totalCreated / users.size() : 0.0;
        long activeUsers = users.stream().filter(AdminHabitViewDTO::hasHabits).count();
        long totalUsers = users.size();

        // Calculate demographic statistics
        long maleCount = users.stream().filter(u -> "MALE".equalsIgnoreCase(u.getGender())).count();
        long femaleCount = users.stream().filter(u -> "FEMALE".equalsIgnoreCase(u.getGender())).count();
        long activeAccounts = users.stream().filter(u -> u.isAccountActive()).count();
        long usersWithEmail = users.stream().filter(AdminHabitViewDTO::hasEmail).count();
        long usersWithPhone = users.stream().filter(AdminHabitViewDTO::hasPhone).count();

        return new HabitStatistics(totalCreated, totalCompleted, totalPending,
                averagePerUser, activeUsers, totalUsers,
                maleCount, femaleCount, activeAccounts,
                usersWithEmail, usersWithPhone);
    }

    // ============================================================================
    // SEARCH HELPER METHODS
    // ============================================================================

    /**
     * Checks if this user matches the search term
     */
    public boolean matchesSearchTerm(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return true;
        }

        // Search by userId
        if (userId != null && userId.toString().toLowerCase().contains(searchTerm)) {
            return true;
        }

        // Search by email
        if (email != null && email.toLowerCase().contains(searchTerm)) {
            return true;
        }

        // Search by phone
        if (phone != null && phone.toLowerCase().contains(searchTerm)) {
            return true;
        }

        return false;
    }

    /**
     * Creates a copy of this DTO with search term highlighting
     */
    public AdminHabitViewDTO getHighlightedVersion(String searchTerm) {
        AdminHabitViewDTO highlighted = AdminHabitViewDTO.builder()
                .userId(this.userId)
                .email(this.email)
                .phone(this.phone)
                .gender(this.gender)
                .age(this.age)
                .ageGroup(this.ageGroup)
                .accountStatus(this.accountStatus)
                .totalHabitsCreated(this.totalHabitsCreated)
                .totalHabitsCompleted(this.totalHabitsCompleted)
                .totalHabitsPending(this.totalHabitsPending)
                .firstHabitDate(this.firstHabitDate)
                .lastHabitDate(this.lastHabitDate)
                .completionRate(this.getCompletionRate()) // Use getter to ensure not null
                .currentStreak(this.currentStreak)
                .highestStreak(this.highestStreak)
                .coins(this.coins)
                .lastLoginDate(this.lastLoginDate)
                .registrationDate(this.registrationDate)
                .statistics(this.statistics != null ? this.statistics : new HabitStatistics())
                .build();

        // Add highlighting
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            highlighted.highlightedUserId = highlightText(this.userId != null ? this.userId.toString() : "",
                    searchTerm);
        }

        return highlighted;
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    /**
     * Utility method to handle null/blank strings
     */
    private String nullSafeString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "N/A";
        }
        return value.trim();
    }

    /**
     * Calculate completion rate - UPDATED VERSION
     */
    private Double calculateCompletionRate() {
        try {
            if (totalHabitsCreated == null || totalHabitsCreated == 0) {
                return 0.0;
            }
            Long completed = totalHabitsCompleted != null ? totalHabitsCompleted : 0L;
            Long created = totalHabitsCreated != null ? totalHabitsCreated : 1L; // Avoid division by zero

            if (created == 0) {
                return 0.0;
            }

            double rate = ((double) completed / created) * 100;
            return Double.isFinite(rate) ? rate : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String highlightText(String text, String searchTerm) {
        if (text == null || searchTerm == null || searchTerm.trim().isEmpty()) {
            return text;
        }

        String pattern = "(?i)" + java.util.regex.Pattern.quote(searchTerm);
        return text.replaceAll(pattern, "<mark>$0</mark>");
    }

    // ============================================================================
    // HELPER METHODS FOR DISPLAY AND ANALYSIS
    // ============================================================================

    public String getFormattedFirstHabitDate() {
        if (firstHabitDate == null) {
            return "No habits";
        }
        return firstHabitDate.toString();
    }

    public String getFormattedCompletionRate() {
        return String.format("%.1f%%", getCompletionRate());
    }

    public Double getCompletionRate() {
        if (completionRate == null) {
            completionRate = calculateCompletionRate();
        }
        return completionRate != null ? completionRate : 0.0;
    }

    public Long getTotalHabitsCreated() {
        return totalHabitsCreated != null ? totalHabitsCreated : 0L;
    }

    public Long getTotalHabitsCompleted() {
        return totalHabitsCompleted != null ? totalHabitsCompleted : 0L;
    }

    public Long getTotalHabitsPending() {
        return totalHabitsPending != null ? totalHabitsPending : 0L;
    }

    public Integer getCurrentStreak() {
        return currentStreak != null ? currentStreak : 0;
    }

    public Integer getHighestStreak() {
        return highestStreak != null ? highestStreak : 0;
    }

    public Integer getCoins() {
        return coins != null ? coins : 0;
    }

    public boolean hasHabits() {
        return getTotalHabitsCreated() > 0;
    }

    public boolean isActiveUser() {
        return hasHabits() && getTotalHabitsCompleted() > 0;
    }

    public boolean isHighPerformer() {
        return hasHabits() && getCompletionRate() >= 70.0;
    }

    public boolean needsAttention() {
        return hasHabits() && getCompletionRate() < 30.0;
    }

    public boolean isHighlyEngaged() {
        return hasHabits() && getTotalHabitsCreated() >= 10 && getCompletionRate() >= 50.0;
    }

    public Double getEngagementScore() {
        if (!hasHabits())
            return 0.0;

        // Base score from completion rate
        double score = getCompletionRate() * 0.6;

        // Bonus for habit volume (up to 40 points)
        double volumeBonus = Math.min(40.0, getTotalHabitsCreated() * 2.0);
        score += volumeBonus * 0.4;

        // Bonus for streak (up to 20 points)
        double streakBonus = Math.min(20.0, getCurrentStreak() * 2.0);
        score += streakBonus;

        return Math.min(100.0, score);
    }

    public String getPerformanceLevel() {
        if (!hasHabits())
            return "No habits";

        double rate = getCompletionRate();
        if (rate >= 80)
            return "Excellent";
        else if (rate >= 60)
            return "Good";
        else if (rate >= 40)
            return "Fair";
        else if (rate >= 20)
            return "Poor";
        else
            return "Very Poor";
    }

    public String getEngagementLevel() {
        if (!hasHabits())
            return "Not engaged";

        double score = getEngagementScore();
        if (score >= 80)
            return "Highly engaged";
        else if (score >= 60)
            return "Well engaged";
        else if (score >= 40)
            return "Moderately engaged";
        else if (score >= 20)
            return "Poorly engaged";
        else
            return "Barely engaged";
    }

    public boolean isAccountActive() {
        return "ACTIVE".equalsIgnoreCase(accountStatus) || "ENABLED".equalsIgnoreCase(accountStatus);
    }

    public boolean hasEmail() {
        return email != null && !email.equals("N/A") && !email.trim().isEmpty();
    }

    public boolean hasPhone() {
        return phone != null && !phone.equals("N/A") && !phone.trim().isEmpty();
    }

    public long getDaysSinceRegistration() {
        if (registrationDate == null) {
            return 0;
        }
        return java.time.Duration.between(registrationDate, LocalDateTime.now()).toDays();
    }

    public long getDaysSinceLastLogin() {
        if (lastLoginDate == null) {
            return Long.MAX_VALUE;
        }
        return java.time.Duration.between(lastLoginDate, LocalDateTime.now()).toDays();
    }

    public String getActivityStatus() {
        long daysSinceLogin = getDaysSinceLastLogin();
        if (daysSinceLogin <= 1) return "Very Active";
        if (daysSinceLogin <= 7) return "Active";
        if (daysSinceLogin <= 30) return "Less Active";
        return "Inactive";
    }

    // ============================================================================
    // STATIC INNER CLASSES
    // ============================================================================

    /**
     * Filter criteria for habit users
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HabitFilterCriteria {
        private String searchTerm;
        private Double minCompletionRate;
        private Double maxCompletionRate;
        private Long minHabitCount;
        private Long maxHabitCount;
        private Boolean hasHabits;
        private Boolean isHighPerformer;
        private String ageGroup;
        private String gender;
        private String accountStatus;
        private Integer minStreak;
        private Integer maxStreak;

        public boolean hasFilters() {
            return (searchTerm != null && !searchTerm.trim().isEmpty()) ||
                    minCompletionRate != null ||
                    maxCompletionRate != null ||
                    minHabitCount != null ||
                    maxHabitCount != null ||
                    hasHabits != null ||
                    isHighPerformer != null ||
                    ageGroup != null ||
                    gender != null ||
                    accountStatus != null ||
                    minStreak != null ||
                    maxStreak != null;
        }

        public String getSummary() {
            List<String> filters = new ArrayList<>();

            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                filters.add("Search: '" + searchTerm + "'");
            }
            if (minCompletionRate != null || maxCompletionRate != null) {
                String range = "";
                if (minCompletionRate != null)
                    range += minCompletionRate + "%";
                if (maxCompletionRate != null)
                    range += (minCompletionRate != null ? "-" : "≤") + maxCompletionRate + "%";
                filters.add("Completion Rate: " + range);
            }
            if (minHabitCount != null || maxHabitCount != null) {
                String range = "";
                if (minHabitCount != null)
                    range += minHabitCount;
                if (maxHabitCount != null)
                    range += (minHabitCount != null ? "-" : "≤") + maxHabitCount;
                filters.add("Habit Count: " + range);
            }
            if (hasHabits != null) {
                filters.add(hasHabits ? "Has habits" : "No habits");
            }
            if (isHighPerformer != null) {
                filters.add(isHighPerformer ? "High performer" : "Not high performer");
            }
            if (ageGroup != null) {
                filters.add("Age Group: " + ageGroup);
            }
            if (gender != null) {
                filters.add("Gender: " + gender);
            }
            if (accountStatus != null) {
                filters.add("Status: " + accountStatus);
            }
            if (minStreak != null || maxStreak != null) {
                String range = "";
                if (minStreak != null)
                    range += minStreak;
                if (maxStreak != null)
                    range += (minStreak != null ? "-" : "≤") + maxStreak;
                filters.add("Streak: " + range);
            }

            return filters.isEmpty() ? "No filters applied" : String.join(" | ", filters);
        }
    }

    /**
     * Static inner class for habit statistics
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class HabitStatistics {
        private Long totalHabitsCreated;
        private Long totalHabitsCompleted;
        private Long totalHabitsPending;
        private Double averageHabitsPerUser;
        private Long activeUsers; // Users with at least one habit
        private Long totalUsers;
        private Long maleUsers;
        private Long femaleUsers;
        private Long activeAccounts;
        private Long usersWithEmail;
        private Long usersWithPhone;
        private Double systemCompletionRate;

        public HabitStatistics() {
            this.totalHabitsCreated = 0L;
            this.totalHabitsCompleted = 0L;
            this.totalHabitsPending = 0L;
            this.averageHabitsPerUser = 0.0;
            this.activeUsers = 0L;
            this.totalUsers = 0L;
            this.maleUsers = 0L;
            this.femaleUsers = 0L;
            this.activeAccounts = 0L;
            this.usersWithEmail = 0L;
            this.usersWithPhone = 0L;
            this.systemCompletionRate = 0.0;
        }

        public HabitStatistics(Long totalHabitsCreated, Long totalHabitsCompleted,
                               Long totalHabitsPending, Double averageHabitsPerUser,
                               Long activeUsers, Long totalUsers, Long maleUsers,
                               Long femaleUsers, Long activeAccounts, Long usersWithEmail,
                               Long usersWithPhone) {
            this.totalHabitsCreated = totalHabitsCreated != null ? totalHabitsCreated : 0L;
            this.totalHabitsCompleted = totalHabitsCompleted != null ? totalHabitsCompleted : 0L;
            this.totalHabitsPending = totalHabitsPending != null ? totalHabitsPending : 0L;
            this.averageHabitsPerUser = averageHabitsPerUser != null ? averageHabitsPerUser : 0.0;
            this.activeUsers = activeUsers != null ? activeUsers : 0L;
            this.totalUsers = totalUsers != null ? totalUsers : 0L;
            this.maleUsers = maleUsers != null ? maleUsers : 0L;
            this.femaleUsers = femaleUsers != null ? femaleUsers : 0L;
            this.activeAccounts = activeAccounts != null ? activeAccounts : 0L;
            this.usersWithEmail = usersWithEmail != null ? usersWithEmail : 0L;
            this.usersWithPhone = usersWithPhone != null ? usersWithPhone : 0L;
            this.systemCompletionRate = calculateSystemCompletionRate();
        }

        private Double calculateSystemCompletionRate() {
            if (totalHabitsCreated == null || totalHabitsCreated == 0) {
                return 0.0;
            }
            return ((double) (totalHabitsCompleted != null ? totalHabitsCompleted : 0L) / totalHabitsCreated) * 100;
        }

        // Safe getters
        public Long getTotalHabitsCreated() {
            return totalHabitsCreated != null ? totalHabitsCreated : 0L;
        }

        public Long getTotalHabitsCompleted() {
            return totalHabitsCompleted != null ? totalHabitsCompleted : 0L;
        }

        public Long getTotalHabitsPending() {
            return totalHabitsPending != null ? totalHabitsPending : 0L;
        }

        public Double getAverageHabitsPerUser() {
            return averageHabitsPerUser != null ? averageHabitsPerUser : 0.0;
        }

        public Long getActiveUsers() {
            return activeUsers != null ? activeUsers : 0L;
        }

        public Long getTotalUsers() {
            return totalUsers != null ? totalUsers : 0L;
        }

        public Long getMaleUsers() {
            return maleUsers != null ? maleUsers : 0L;
        }

        public Long getFemaleUsers() {
            return femaleUsers != null ? femaleUsers : 0L;
        }

        public Long getActiveAccounts() {
            return activeAccounts != null ? activeAccounts : 0L;
        }

        public Long getUsersWithEmail() {
            return usersWithEmail != null ? usersWithEmail : 0L;
        }

        public Long getUsersWithPhone() {
            return usersWithPhone != null ? usersWithPhone : 0L;
        }

        public Double getSystemCompletionRate() {
            return systemCompletionRate != null ? systemCompletionRate : 0.0;
        }

        // Additional helper methods
        public Long getUsersWithoutHabits() {
            Long total = getTotalUsers();
            Long active = getActiveUsers();
            return total - active;
        }

        public Double getHabitAdoptionRate() {
            Long total = getTotalUsers();
            if (total == 0) {
                return 0.0;
            }
            return (double) getActiveUsers() / total * 100;
        }

        public Double getGenderRatio() {
            Long male = getMaleUsers();
            Long female = getFemaleUsers();
            long totalGender = male + female;
            return totalGender > 0 ? (double) male / totalGender * 100 : 0.0;
        }

        public Double getEmailCoverage() {
            Long total = getTotalUsers();
            if (total == 0) {
                return 0.0;
            }
            return (double) getUsersWithEmail() / total * 100;
        }

        public Double getPhoneCoverage() {
            Long total = getTotalUsers();
            if (total == 0) {
                return 0.0;
            }
            return (double) getUsersWithPhone() / total * 100;
        }

        public Double getAccountActivationRate() {
            Long total = getTotalUsers();
            if (total == 0) {
                return 0.0;
            }
            return (double) getActiveAccounts() / total * 100;
        }

        public String getFormattedSystemCompletionRate() {
            return String.format("%.1f%%", getSystemCompletionRate());
        }

        public String getFormattedAverageHabitsPerUser() {
            return String.format("%.1f", getAverageHabitsPerUser());
        }

        public String getFormattedHabitAdoptionRate() {
            return String.format("%.1f%%", getHabitAdoptionRate());
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("totalHabitsCreated", getTotalHabitsCreated());
            map.put("totalHabitsCompleted", getTotalHabitsCompleted());
            map.put("totalHabitsPending", getTotalHabitsPending());
            map.put("averageHabitsPerUser", getAverageHabitsPerUser());
            map.put("activeUsers", getActiveUsers());
            map.put("totalUsers", getTotalUsers());
            map.put("maleUsers", getMaleUsers());
            map.put("femaleUsers", getFemaleUsers());
            map.put("activeAccounts", getActiveAccounts());
            map.put("usersWithEmail", getUsersWithEmail());
            map.put("usersWithPhone", getUsersWithPhone());
            map.put("systemCompletionRate", getSystemCompletionRate());
            map.put("habitAdoptionRate", getHabitAdoptionRate());
            map.put("emailCoverage", getEmailCoverage());
            map.put("phoneCoverage", getPhoneCoverage());
            map.put("accountActivationRate", getAccountActivationRate());
            return map;
        }

        @Override
        public String toString() {
            return "HabitStatistics{" +
                    "totalHabitsCreated=" + getTotalHabitsCreated() +
                    ", totalHabitsCompleted=" + getTotalHabitsCompleted() +
                    ", totalHabitsPending=" + getTotalHabitsPending() +
                    ", averageHabitsPerUser=" + getAverageHabitsPerUser() +
                    ", activeUsers=" + getActiveUsers() +
                    ", totalUsers=" + getTotalUsers() +
                    ", systemCompletionRate=" + getSystemCompletionRate() +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "AdminHabitViewDTO{" +
                "userId=" + userId +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", gender='" + gender + '\'' +
                ", age=" + age +
                ", ageGroup='" + ageGroup + '\'' +
                ", totalHabitsCreated=" + getTotalHabitsCreated() +
                ", totalHabitsCompleted=" + getTotalHabitsCompleted() +
                ", completionRate=" + getCompletionRate() +
                '}';
    }
}