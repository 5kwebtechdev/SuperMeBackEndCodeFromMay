package com.superme.service;

import com.superme.dto.AdminHabitViewDTO;
import com.superme.dto.AdminHabitViewDTO.HabitStatistics;
import com.superme.dto.AdminHabitViewDTO.HabitFilterCriteria;
import com.superme.dto.AdminHabitOverviewResponse;
import com.superme.model.User;
import com.superme.model.Habit;
import com.superme.repository.UserRepository;
import com.superme.repository.HabitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced Admin Habit View Service with comprehensive filtering and analytics.
 *
 * Provides habit management functionality for admin dashboard including:
 * - User habit overview with advanced filtering
 * - Comprehensive habit statistics and analytics
 * - Performance tracking and user categorization
 * - Detailed habit distribution analysis
 *
 * @author MindfullB Admin Team
 * @version 2.0
 */
@Service
public class AdminHabitViewService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HabitRepository habitRepository;

    // ============================================================================
    // CORE HABIT VIEW METHODS
    // ============================================================================

    /**
     * Get all user habit views with comprehensive data
     */
    public List<AdminHabitViewDTO> getAllUserHabitViews() {
        try {
            List<User> users = userRepository.findAll();
            List<AdminHabitViewDTO> result = new ArrayList<>();

            for (User user : users) {
                AdminHabitViewDTO dto = createUserHabitViewDTO(user);
                result.add(dto);
            }

            return result;
        } catch (Exception e) {
            System.err.println("Error fetching user habit views: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get filtered and paginated user habit views
     */
    public AdminHabitOverviewResponse getFilteredUserHabitViews(HabitFilterCriteria criteria) {
        try {
            // Get all users first
            List<AdminHabitViewDTO> allUsers = getAllUserHabitViews();
            int originalCount = allUsers.size();

            // Apply filters
            List<AdminHabitViewDTO> filteredUsers = applyFilters(allUsers, criteria);

            // Get global statistics from filtered users
            HabitStatistics globalStats = AdminHabitViewDTO.getHabitStatistics(filteredUsers);

            // Create response
            return new AdminHabitOverviewResponse(filteredUsers, globalStats, criteria, originalCount);

        } catch (Exception e) {
            System.err.println("Error getting filtered habit views: " + e.getMessage());
            return new AdminHabitOverviewResponse(new ArrayList<>(), new HabitStatistics());
        }
    }

    /**
     * Get paginated user habit views with filtering
     */
    public AdminHabitOverviewResponse getPaginatedUserHabitViews(
            HabitFilterCriteria criteria, int page, int size, String sortBy, String sortDirection) {
        try {
            // Get filtered results first
            AdminHabitOverviewResponse response = getFilteredUserHabitViews(criteria);
            List<AdminHabitViewDTO> filteredUsers = response.getUsers();

            // Apply sorting
            List<AdminHabitViewDTO> sortedUsers = applySorting(filteredUsers, sortBy, sortDirection);

            // Apply pagination
            List<AdminHabitViewDTO> paginatedUsers = applyPagination(sortedUsers, page, size);

            // Update response with paginated data
            response.setUsers(paginatedUsers);

            return response;

        } catch (Exception e) {
            System.err.println("Error getting paginated habit views: " + e.getMessage());
            return new AdminHabitOverviewResponse(new ArrayList<>(), new HabitStatistics());
        }
    }

    // ============================================================================
    // FILTERING METHODS
    // ============================================================================

    /**
     * Apply comprehensive filters to user habit list
     */
    private List<AdminHabitViewDTO> applyFilters(List<AdminHabitViewDTO> users, HabitFilterCriteria criteria) {
        if (criteria == null || !criteria.hasFilters()) {
            return users;
        }

        List<AdminHabitViewDTO> filteredUsers = users;

        // Apply search term filter
        if (criteria.getSearchTerm() != null && !criteria.getSearchTerm().trim().isEmpty()) {
            filteredUsers = AdminHabitViewDTO.filterBySearchTerm(filteredUsers, criteria.getSearchTerm());
        }

        // Apply completion rate filter
        filteredUsers = AdminHabitViewDTO.filterByCompletionRate(filteredUsers,
                criteria.getMinCompletionRate(), criteria.getMaxCompletionRate());

        // Apply habit count filter
        filteredUsers = AdminHabitViewDTO.filterByHabitCount(filteredUsers,
                criteria.getMinHabitCount(), criteria.getMaxHabitCount());

        // Apply streak filter
        filteredUsers = AdminHabitViewDTO.filterByStreakLevel(filteredUsers,
                criteria.getMinStreak(), criteria.getMaxStreak());

        // Apply age group filter
        if (criteria.getAgeGroup() != null && !criteria.getAgeGroup().trim().isEmpty()) {
            filteredUsers = AdminHabitViewDTO.filterByAgeGroup(filteredUsers, criteria.getAgeGroup());
        }

        // Apply gender filter
        if (criteria.getGender() != null && !criteria.getGender().trim().isEmpty()) {
            filteredUsers = AdminHabitViewDTO.filterByGender(filteredUsers, criteria.getGender());
        }

        // Apply account status filter
        if (criteria.getAccountStatus() != null && !criteria.getAccountStatus().trim().isEmpty()) {
            filteredUsers = AdminHabitViewDTO.filterByAccountStatus(filteredUsers, criteria.getAccountStatus());
        }

        // Apply hasHabits filter
        if (criteria.getHasHabits() != null) {
            filteredUsers = AdminHabitViewDTO.filterByHabitExistence(filteredUsers, criteria.getHasHabits());
        }

        // Apply isHighPerformer filter
        if (criteria.getIsHighPerformer() != null) {
            filteredUsers = AdminHabitViewDTO.filterByPerformance(filteredUsers, criteria.getIsHighPerformer());
        }

        return filteredUsers;
    }

    // ============================================================================
    // SORTING METHODS
    // ============================================================================

    /**
     * Apply sorting to user habit list
     */
    private List<AdminHabitViewDTO> applySorting(List<AdminHabitViewDTO> users, String sortBy, String sortDirection) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "userId"; // Default sort
        }

        boolean ascending = !"desc".equalsIgnoreCase(sortDirection);

        Comparator<AdminHabitViewDTO> comparator = getComparator(sortBy);
        if (!ascending) {
            comparator = comparator.reversed();
        }

        return users.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    /**
     * Get comparator for sorting field
     */
    private Comparator<AdminHabitViewDTO> getComparator(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "userid", "user_id" ->
                    Comparator.comparing(AdminHabitViewDTO::getUserId, Comparator.nullsLast(Long::compareTo));
            case "email" ->
                    Comparator.comparing(AdminHabitViewDTO::getEmail, Comparator.nullsLast(String::compareTo));
            case "phone" ->
                    Comparator.comparing(AdminHabitViewDTO::getPhone, Comparator.nullsLast(String::compareTo));
            case "gender" ->
                    Comparator.comparing(AdminHabitViewDTO::getGender, Comparator.nullsLast(String::compareTo));
            case "age" ->
                    Comparator.comparing(AdminHabitViewDTO::getAge, Comparator.nullsLast(Integer::compareTo));
            case "agegroup", "age_group" ->
                    Comparator.comparing(AdminHabitViewDTO::getAgeGroup, Comparator.nullsLast(String::compareTo));
            case "accountstatus", "account_status" ->
                    Comparator.comparing(AdminHabitViewDTO::getAccountStatus, Comparator.nullsLast(String::compareTo));
            case "total_habits" ->
                    Comparator.comparing(AdminHabitViewDTO::getTotalHabitsCreated, Comparator.nullsLast(Long::compareTo));
            case "completed_habits" ->
                    Comparator.comparing(AdminHabitViewDTO::getTotalHabitsCompleted, Comparator.nullsLast(Long::compareTo));
            case "completion_rate" ->
                    Comparator.comparing(user -> user.getCompletionRate() != null ? user.getCompletionRate() : 0.0);
            case "current_streak" ->
                    Comparator.comparing(AdminHabitViewDTO::getCurrentStreak, Comparator.nullsLast(Integer::compareTo));
            case "highest_streak" ->
                    Comparator.comparing(AdminHabitViewDTO::getHighestStreak, Comparator.nullsLast(Integer::compareTo));
            case "coins" ->
                    Comparator.comparing(AdminHabitViewDTO::getCoins, Comparator.nullsLast(Integer::compareTo));
            case "last_login" ->
                    Comparator.comparing(AdminHabitViewDTO::getLastLoginDate,
                            Comparator.nullsLast(LocalDateTime::compareTo));
            case "registration_date" ->
                    Comparator.comparing(AdminHabitViewDTO::getRegistrationDate,
                            Comparator.nullsLast(LocalDateTime::compareTo));
            case "last_activity" ->
                    Comparator.comparing(AdminHabitViewDTO::getLastHabitDate,
                            Comparator.nullsLast(LocalDate::compareTo));
            case "first_habit" ->
                    Comparator.comparing(AdminHabitViewDTO::getFirstHabitDate,
                            Comparator.nullsLast(LocalDate::compareTo));
            default -> Comparator.comparing(AdminHabitViewDTO::getUserId, Comparator.nullsLast(Long::compareTo));
        };
    }

    // ============================================================================
    // PAGINATION METHODS
    // ============================================================================

    /**
     * Apply pagination to user list
     */
    private List<AdminHabitViewDTO> applyPagination(List<AdminHabitViewDTO> users, int page, int size) {
        if (size <= 0) {
            return users; // Return all if size is invalid
        }

        int start = Math.max(0, page * size);
        int end = Math.min(users.size(), start + size);

        if (start >= users.size()) {
            return new ArrayList<>(); // Page beyond available data
        }

        return users.subList(start, end);
    }

    // ============================================================================
    // USER HABIT VIEW CREATION
    // ============================================================================

    /**
     * Create comprehensive AdminHabitViewDTO for a user
     */
    private AdminHabitViewDTO createUserHabitViewDTO(User user) {
        try {
            // Get user habits using repository method
            List<Habit> userHabits = getHabitsForUser(user);

            // Calculate habit statistics
            Long totalCreated = (long) userHabits.size();
            Long totalCompleted = userHabits.stream()
                    .mapToLong(habit -> habit.getStatus() == Habit.HabitStatus.COMPLETED ? 1L : 0L)
                    .sum();
            Long totalPending = totalCreated - totalCompleted;

            // Find first and last habit dates
            LocalDate firstHabitDate = calculateFirstHabitDate(userHabits);
            LocalDate lastHabitDate = calculateLastHabitDate(userHabits);

            // Create and return DTO with all demographic data
            return AdminHabitViewDTO.builder()
                    .userId(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .gender(user.getGender())
                    .age(user.getAge())
                    .ageGroup(user.getAgeGroup() != null ? user.getAgeGroup().toString() : "N/A")
                    .accountStatus(user.isEnabled() ? "ACTIVE" : "INACTIVE")
                    .role(user.getRelationship() != null ? user.getRelationship().getDisplayName().toLowerCase() : null)
                    .totalHabitsCreated(totalCreated)
                    .totalHabitsCompleted(totalCompleted)
                    .totalHabitsPending(totalPending)
                    .firstHabitDate(firstHabitDate)
                    .lastHabitDate(lastHabitDate)
                    .currentStreak(user.getCurrentStreak())
                    .highestStreak(user.getHighestStreak())
                    .coins(user.getCoins())
                    .lastLoginDate(user.getLastLoginDate())
                    .registrationDate(user.getCreatedDateTime())
                    .statistics(new HabitStatistics())
                    .build();

        } catch (Exception e) {
            System.err.println("Error creating habit view DTO for user " + user.getId() + ": " + e.getMessage());
            // Return DTO with default values
            return AdminHabitViewDTO.builder()
                    .userId(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .gender(user.getGender())
                    .age(user.getAge())
                    .ageGroup(user.getAgeGroup() != null ? user.getAgeGroup().toString() : "N/A")
                    .accountStatus(user.isEnabled() ? "ACTIVE" : "INACTIVE")
                    .role(user.getRelationship() != null ? user.getRelationship().getDisplayName().toLowerCase() : null)
                    .totalHabitsCreated(0L)
                    .totalHabitsCompleted(0L)
                    .totalHabitsPending(0L)
                    .currentStreak(user.getCurrentStreak())
                    .highestStreak(user.getHighestStreak())
                    .coins(user.getCoins())
                    .lastLoginDate(user.getLastLoginDate())
                    .registrationDate(user.getCreatedDateTime())
                    .statistics(new HabitStatistics())
                    .build();
        }
    }

    /**
     * Calculate first habit date from user habits
     */
    private LocalDate calculateFirstHabitDate(List<Habit> habits) {
        return habits.stream()
                .filter(habit -> habit.getStartDate() != null)
                .map(Habit::getStartDate)
                .min(LocalDate::compareTo)
                .orElse(null);
    }

    /**
     * Calculate last habit date from user habits
     */
    private LocalDate calculateLastHabitDate(List<Habit> habits) {
        return habits.stream()
                .filter(habit -> habit.getStartDate() != null)
                .map(Habit::getStartDate)
                .max(LocalDate::compareTo)
                .orElse(null);
    }

    // ============================================================================
    // HABIT ANALYTICS METHODS
    // ============================================================================

    /**
     * Get habits grouped by priority for a user
     */
    private Map<String, Long> getHabitsByPriority(List<Habit> habits) {
        return habits.stream()
                .collect(Collectors.groupingBy(
                        habit -> {
                            try {
                                return habit.getPriority() != null ? habit.getPriority().toString() : "Not Set";
                            } catch (Exception e) {
                                return "Not Set";
                            }
                        },
                        Collectors.counting()));
    }

    /**
     * Get habits grouped by routine for a user
     */
    private Map<String, Long> getHabitsByRoutine(List<Habit> habits) {
        return habits.stream()
                .collect(Collectors.groupingBy(
                        habit -> {
                            try {
                                return habit.getRoutine() != null ? habit.getRoutine().toString() : "Not Set";
                            } catch (Exception e) {
                                return "Not Set";
                            }
                        },
                        Collectors.counting()));
    }

    /**
     * Calculate recent activity (last 30 days)
     */
    private Long calculateRecentActivity(List<Habit> habits) {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        return habits.stream()
                .filter(habit -> habit.getStartDate() != null && !habit.getStartDate().isBefore(thirtyDaysAgo))
                .count();
    }

    // ============================================================================
    // STATISTICS METHODS
    // ============================================================================

    /**
     * Get comprehensive habit statistics using the updated DTO method
     */
    public HabitStatistics getHabitStatistics() {
        try {
            List<AdminHabitViewDTO> userViews = getAllUserHabitViews();
            return AdminHabitViewDTO.getHabitStatistics(userViews);
        } catch (Exception e) {
            System.err.println("Error calculating habit statistics: " + e.getMessage());
            return new HabitStatistics();
        }
    }

    /**
     * Count users with habits
     */
    private Long countUsersWithHabits() {
        try {
            return habitRepository.findAll().stream()
                    .map(habit -> habit.getCreatedBy() != null ? habit.getCreatedBy().getId() : null)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();
        } catch (Exception e) {
            System.err.println("Error counting users with habits: " + e.getMessage());
            return 0L;
        }
    }

    /**
     * Get optimized habit statistics using repository queries
     */
    public HabitStatistics getOptimizedHabitStatistics() {
        try {
            List<AdminHabitViewDTO> userViews = getAllUserHabitViews();
            return AdminHabitViewDTO.getHabitStatistics(userViews);
        } catch (Exception e) {
            System.err.println("Error calculating optimized habit statistics: " + e.getMessage());
            return new HabitStatistics();
        }
    }

    // ============================================================================
    // DETAILED ANALYTICS METHODS
    // ============================================================================

    /**
     * Get comprehensive habit analytics
     */
    public Map<String, Object> getDetailedHabitAnalytics() {
        try {
            Map<String, Object> analytics = new HashMap<>();

            List<AdminHabitViewDTO> userViews = getAllUserHabitViews();

            // Get statistics from the updated DTO
            HabitStatistics stats = AdminHabitViewDTO.getHabitStatistics(userViews);

            analytics.put("habitStatistics", stats);
            analytics.put("topPerformers", getTopPerformers(userViews, 5));
            analytics.put("bottomPerformers", getBottomPerformers(userViews, 5));
            analytics.put("habitDistribution", getHabitDistribution());
            analytics.put("completionTrends", getCompletionTrends());
            analytics.put("userHabitCounts", getUserHabitCounts(userViews));
            analytics.put("engagementMetrics", getEngagementMetrics(userViews));
            analytics.put("performanceMetrics", getPerformanceMetrics(userViews));

            // Add demographic analytics from the updated DTO
            analytics.put("userDemographics", getUserDemographics(userViews));
            analytics.put("ageGroupDistribution", getAgeGroupDistribution(userViews));
            analytics.put("genderDistribution", getGenderDistribution(userViews));
            analytics.put("accountStatusSummary", getAccountStatusSummary(userViews));

            return analytics;
        } catch (Exception e) {
            System.err.println("Error calculating detailed analytics: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Get top performing users - FIXED NULL HANDLING
     */
    private List<AdminHabitViewDTO> getTopPerformers(List<AdminHabitViewDTO> userViews, int limit) {
        return userViews.stream()
                .filter(user -> user.hasHabits())
                .sorted((u1, u2) -> {
                    Double rate1 = u1.getCompletionRate() != null ? u1.getCompletionRate() : 0.0;
                    Double rate2 = u2.getCompletionRate() != null ? u2.getCompletionRate() : 0.0;
                    return Double.compare(rate2, rate1);
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get bottom performing users (who need attention) - FIXED NULL HANDLING
     */
    private List<AdminHabitViewDTO> getBottomPerformers(List<AdminHabitViewDTO> userViews, int limit) {
        return userViews.stream()
                .filter(user -> user.hasHabits())
                .filter(user -> {
                    Double rate = user.getCompletionRate();
                    return rate != null && rate < 30.0;
                })
                .sorted((u1, u2) -> {
                    Double rate1 = u1.getCompletionRate() != null ? u1.getCompletionRate() : 0.0;
                    Double rate2 = u2.getCompletionRate() != null ? u2.getCompletionRate() : 0.0;
                    return Double.compare(rate1, rate2);
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get habit distribution by priority and routine
     */
    private Map<String, Long> getHabitDistribution() {
        try {
            List<Habit> allHabits = habitRepository.findAll();
            Map<String, Long> distribution = new HashMap<>();

            // Group habits by priority
            Map<String, Long> priorityDist = allHabits.stream()
                    .collect(Collectors.groupingBy(
                            habit -> {
                                String priority = "Not Set";
                                try {
                                    if (habit.getPriority() != null) {
                                        priority = habit.getPriority().toString();
                                    }
                                } catch (Exception e) {
                                    priority = "Not Set";
                                }
                                return priority;
                            },
                            Collectors.counting()));

            distribution.putAll(priorityDist);

            // Group habits by routine
            Map<String, Long> routineDist = allHabits.stream()
                    .collect(Collectors.groupingBy(
                            habit -> {
                                String routine = "Not Set";
                                try {
                                    if (habit.getRoutine() != null) {
                                        routine = habit.getRoutine().toString();
                                    }
                                } catch (Exception e) {
                                    routine = "Not Set";
                                }
                                return routine;
                            },
                            Collectors.counting()));

            // Add routine distribution with prefixes to avoid key conflicts
            routineDist.forEach((key, value) -> distribution.put("routine_" + key, value));

            distribution.put("totalPriorities", (long) priorityDist.size());
            distribution.put("totalRoutines", (long) routineDist.size());

            return distribution;
        } catch (Exception e) {
            System.err.println("Error calculating habit distribution: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Get completion trends and patterns
     */
    private Map<String, Object> getCompletionTrends() {
        try {
            List<Habit> allHabits = habitRepository.findAll();
            Map<String, Object> trends = new HashMap<>();

            // Calculate completion rate by month
            Map<String, Double> monthlyCompletionRates = calculateMonthlyCompletionRates(allHabits);
            trends.put("monthlyCompletionRates", monthlyCompletionRates);

            // Calculate recent habits (last 30 days)
            LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
            long recentHabits = allHabits.stream()
                    .filter(habit -> habit.getStartDate() != null && !habit.getStartDate().isBefore(thirtyDaysAgo))
                    .count();
            trends.put("recentHabitsCount", recentHabits);

            // Calculate habits by completion status
            long completedHabits = allHabits.stream()
                    .mapToLong(habit -> habit.getStatus() == Habit.HabitStatus.COMPLETED ? 1L : 0L)
                    .sum();
            long pendingHabits = allHabits.size() - completedHabits;

            trends.put("completedHabits", completedHabits);
            trends.put("pendingHabits", pendingHabits);
            trends.put("completionRate",
                    !allHabits.isEmpty() ? (double) completedHabits / allHabits.size() * 100 : 0.0);

            // Calculate trend direction
            trends.put("trendDirection", calculateTrendDirection(allHabits));

            return trends;
        } catch (Exception e) {
            System.err.println("Error calculating completion trends: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Calculate monthly completion rates
     */
    private Map<String, Double> calculateMonthlyCompletionRates(List<Habit> habits) {
        Map<String, Double> monthlyRates = new HashMap<>();

        try {
            long totalHabits = habits.size();
            long completedHabits = habits.stream()
                    .mapToLong(habit -> habit.getStatus() == Habit.HabitStatus.COMPLETED ? 1L : 0L)
                    .sum();

            double currentRate = totalHabits > 0 ? ((double) completedHabits / totalHabits) * 100 : 0.0;

            monthlyRates.put("current_month", currentRate);
            monthlyRates.put("overall", currentRate);

            // Calculate recent vs older habits completion rates
            LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);

            List<Habit> recentHabits = habits.stream()
                    .filter(habit -> habit.getStartDate() != null && !habit.getStartDate().isBefore(thirtyDaysAgo))
                    .collect(Collectors.toList());

            List<Habit> olderHabits = habits.stream()
                    .filter(habit -> habit.getStartDate() == null || habit.getStartDate().isBefore(thirtyDaysAgo))
                    .collect(Collectors.toList());

            if (!recentHabits.isEmpty()) {
                double recentCompletionRate = recentHabits.stream()
                        .mapToDouble(habit -> habit.getStatus() == Habit.HabitStatus.COMPLETED ? 100.0 : 0.0)
                        .average()
                        .orElse(0.0);
                monthlyRates.put("recent_30_days", recentCompletionRate);
            }

            if (!olderHabits.isEmpty()) {
                double olderCompletionRate = olderHabits.stream()
                        .mapToDouble(habit -> habit.getStatus() == Habit.HabitStatus.COMPLETED ? 100.0 : 0.0)
                        .average()
                        .orElse(0.0);
                monthlyRates.put("older_than_30_days", olderCompletionRate);
            }

        } catch (Exception e) {
            System.err.println("Error calculating monthly completion rates: " + e.getMessage());
        }

        return monthlyRates;
    }

    /**
     * Calculate trend direction
     */
    private String calculateTrendDirection(List<Habit> habits) {
        try {
            LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
            LocalDate sixtyDaysAgo = LocalDate.now().minusDays(60);

            long recentCompletions = habits.stream()
                    .filter(habit -> habit.getStatus() == Habit.HabitStatus.COMPLETED && habit.getStartDate() != null &&
                            !habit.getStartDate().isBefore(thirtyDaysAgo))
                    .count();

            long previousCompletions = habits.stream()
                    .filter(habit -> habit.getStatus() == Habit.HabitStatus.COMPLETED && habit.getStartDate() != null &&
                            !habit.getStartDate().isBefore(sixtyDaysAgo)
                            && habit.getStartDate().isBefore(thirtyDaysAgo))
                    .count();

            if (recentCompletions > previousCompletions) {
                return "improving";
            } else if (recentCompletions < previousCompletions) {
                return "declining";
            } else {
                return "stable";
            }
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Get user habit counts with analytics
     */
    private Map<String, Object> getUserHabitCounts(List<AdminHabitViewDTO> userViews) {
        try {
            Map<String, Object> result = new HashMap<>();

            Map<Long, Long> userHabitMap = userViews.stream()
                    .filter(user -> user.getTotalHabitsCreated() != null && user.getTotalHabitsCreated() > 0)
                    .collect(Collectors.toMap(
                            AdminHabitViewDTO::getUserId,
                            AdminHabitViewDTO::getTotalHabitsCreated));

            result.put("userHabitCounts", userHabitMap);
            result.put("usersWithHabits", userHabitMap.size());

            // Calculate statistics
            if (!userHabitMap.isEmpty()) {
                double avgHabits = userHabitMap.values().stream()
                        .mapToLong(Long::longValue)
                        .average()
                        .orElse(0.0);
                result.put("averageHabitsPerActiveUser", avgHabits);

                long maxHabits = userHabitMap.values().stream()
                        .mapToLong(Long::longValue)
                        .max()
                        .orElse(0L);
                result.put("maxHabitsPerUser", maxHabits);

                long minHabits = userHabitMap.values().stream()
                        .mapToLong(Long::longValue)
                        .min()
                        .orElse(0L);
                result.put("minHabitsPerUser", minHabits);

                // Calculate distribution
                Map<String, Long> distribution = new HashMap<>();
                distribution.put("1-5_habits", userHabitMap.values().stream()
                        .filter(count -> count >= 1 && count <= 5)
                        .count());
                distribution.put("6-10_habits", userHabitMap.values().stream()
                        .filter(count -> count >= 6 && count <= 10)
                        .count());
                distribution.put("11-20_habits", userHabitMap.values().stream()
                        .filter(count -> count >= 11 && count <= 20)
                        .count());
                distribution.put("20+_habits", userHabitMap.values().stream()
                        .filter(count -> count > 20)
                        .count());

                result.put("habitCountDistribution", distribution);
            }

            return result;
        } catch (Exception e) {
            System.err.println("Error calculating user habit counts: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Get engagement metrics
     */
    private Map<String, Object> getEngagementMetrics(List<AdminHabitViewDTO> userViews) {
        Map<String, Object> metrics = new HashMap<>();

        try {
            long totalUsers = userViews.size();
            long usersWithHabits = userViews.stream()
                    .filter(AdminHabitViewDTO::hasHabits)
                    .count();
            long highlyEngagedUsers = userViews.stream()
                    .filter(AdminHabitViewDTO::isHighlyEngaged)
                    .count();

            // Calculate engagement rates
            double habitAdoptionRate = totalUsers > 0 ? (double) usersWithHabits / totalUsers * 100 : 0.0;
            double highEngagementRate = totalUsers > 0 ? (double) highlyEngagedUsers / totalUsers * 100 : 0.0;

            metrics.put("totalUsers", totalUsers);
            metrics.put("usersWithHabits", usersWithHabits);
            metrics.put("usersWithoutHabits", totalUsers - usersWithHabits);
            metrics.put("highlyEngagedUsers", highlyEngagedUsers);
            metrics.put("habitAdoptionRate", habitAdoptionRate);
            metrics.put("highEngagementRate", highEngagementRate);

            // Calculate recent activity
            LocalDate thirtyDaysAgoDate = LocalDate.now().minusDays(30);
            long recentlyActiveUsers = userViews.stream()
                    .filter(user -> user.getLastHabitDate() != null &&
                            (user.getLastHabitDate().isAfter(thirtyDaysAgoDate)
                                    || user.getLastHabitDate().isEqual(thirtyDaysAgoDate)))
                    .count();

            double recentActivityRate = totalUsers > 0 ? (double) recentlyActiveUsers / totalUsers * 100 : 0.0;

            metrics.put("recentlyActiveUsers", recentlyActiveUsers);
            metrics.put("recentActivityRate", recentActivityRate);

        } catch (Exception e) {
            System.err.println("Error calculating engagement metrics: " + e.getMessage());
        }

        return metrics;
    }

    /**
     * Get performance metrics - FIXED NULL HANDLING
     */
    private Map<String, Object> getPerformanceMetrics(List<AdminHabitViewDTO> userViews) {
        Map<String, Object> metrics = new HashMap<>();

        try {
            List<AdminHabitViewDTO> usersWithHabits = userViews.stream()
                    .filter(AdminHabitViewDTO::hasHabits)
                    .collect(Collectors.toList());

            if (usersWithHabits.isEmpty()) {
                metrics.put("averageCompletionRate", 0.0);
                metrics.put("highPerformers", 0L);
                metrics.put("mediumPerformers", 0L);
                metrics.put("lowPerformers", 0L);
                return metrics;
            }

            // Calculate average completion rate with null safety
            double avgCompletionRate = usersWithHabits.stream()
                    .mapToDouble(user -> {
                        Double rate = user.getCompletionRate();
                        return rate != null ? rate : 0.0;
                    })
                    .average()
                    .orElse(0.0);

            // Categorize users by performance with null safety
            long highPerformers = usersWithHabits.stream()
                    .filter(user -> {
                        Double rate = user.getCompletionRate();
                        return rate != null && rate >= 70.0;
                    })
                    .count();
            long mediumPerformers = usersWithHabits.stream()
                    .filter(user -> {
                        Double rate = user.getCompletionRate();
                        return rate != null && rate >= 30.0 && rate < 70.0;
                    })
                    .count();
            long lowPerformers = usersWithHabits.stream()
                    .filter(user -> {
                        Double rate = user.getCompletionRate();
                        return rate != null && rate < 30.0;
                    })
                    .count();

            metrics.put("averageCompletionRate", avgCompletionRate);
            metrics.put("highPerformers", highPerformers);
            metrics.put("mediumPerformers", mediumPerformers);
            metrics.put("lowPerformers", lowPerformers);

            // Calculate performance rates
            long totalWithHabits = usersWithHabits.size();
            metrics.put("highPerformerRate",
                    totalWithHabits > 0 ? (double) highPerformers / totalWithHabits * 100 : 0.0);
            metrics.put("mediumPerformerRate",
                    totalWithHabits > 0 ? (double) mediumPerformers / totalWithHabits * 100 : 0.0);
            metrics.put("lowPerformerRate", totalWithHabits > 0 ? (double) lowPerformers / totalWithHabits * 100 : 0.0);

        } catch (Exception e) {
            System.err.println("Error calculating performance metrics: " + e.getMessage());
        }

        return metrics;
    }

    /**
     * Get user demographic data from DTO
     */
    private Map<String, Object> getUserDemographics(List<AdminHabitViewDTO> userViews) {
        Map<String, Object> demographics = new HashMap<>();

        try {
            long totalUsers = userViews.size();
            long usersWithEmail = userViews.stream().filter(AdminHabitViewDTO::hasEmail).count();
            long usersWithPhone = userViews.stream().filter(AdminHabitViewDTO::hasPhone).count();
            long activeAccounts = userViews.stream().filter(AdminHabitViewDTO::isAccountActive).count();

            demographics.put("totalUsers", totalUsers);
            demographics.put("usersWithEmail", usersWithEmail);
            demographics.put("usersWithPhone", usersWithPhone);
            demographics.put("activeAccounts", activeAccounts);
            demographics.put("emailCoveragePercentage", totalUsers > 0 ? (double) usersWithEmail / totalUsers * 100 : 0);
            demographics.put("phoneCoveragePercentage", totalUsers > 0 ? (double) usersWithPhone / totalUsers * 100 : 0);
            demographics.put("accountActivationRate", totalUsers > 0 ? (double) activeAccounts / totalUsers * 100 : 0);

        } catch (Exception e) {
            System.err.println("Error calculating user demographics: " + e.getMessage());
        }

        return demographics;
    }

    /**
     * Get age group distribution from DTO
     */
    private Map<String, Long> getAgeGroupDistribution(List<AdminHabitViewDTO> userViews) {
        return userViews.stream()
                .collect(Collectors.groupingBy(
                        AdminHabitViewDTO::getAgeGroup,
                        Collectors.counting()
                ));
    }

    /**
     * Get gender distribution from DTO
     */
    private Map<String, Long> getGenderDistribution(List<AdminHabitViewDTO> userViews) {
        return userViews.stream()
                .collect(Collectors.groupingBy(
                        AdminHabitViewDTO::getGender,
                        Collectors.counting()
                ));
    }

    /**
     * Get account status summary from DTO
     */
    private Map<String, Long> getAccountStatusSummary(List<AdminHabitViewDTO> userViews) {
        return userViews.stream()
                .collect(Collectors.groupingBy(
                        AdminHabitViewDTO::getAccountStatus,
                        Collectors.counting()
                ));
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    /**
     * Get users with habits
     */
    public List<AdminHabitViewDTO> getUsersWithHabits() {
        return getAllUserHabitViews().stream()
                .filter(AdminHabitViewDTO::hasHabits)
                .collect(Collectors.toList());
    }

    /**
     * Get users without habits
     */
    public List<AdminHabitViewDTO> getUsersWithoutHabits() {
        return getAllUserHabitViews().stream()
                .filter(user -> !user.hasHabits())
                .collect(Collectors.toList());
    }

    /**
     * Get high performing users
     */
    public List<AdminHabitViewDTO> getHighPerformingUsers() {
        return getAllUserHabitViews().stream()
                .filter(AdminHabitViewDTO::isHighPerformer)
                .collect(Collectors.toList());
    }

    /**
     * Get users needing attention
     */
    public List<AdminHabitViewDTO> getUsersNeedingAttention() {
        return getAllUserHabitViews().stream()
                .filter(AdminHabitViewDTO::needsAttention)
                .collect(Collectors.toList());
    }

    /**
     * Get highly engaged users
     */
    public List<AdminHabitViewDTO> getHighlyEngagedUsers() {
        return getAllUserHabitViews().stream()
                .filter(AdminHabitViewDTO::isHighlyEngaged)
                .collect(Collectors.toList());
    }

    /**
     * Get quick statistics for dashboard
     */
    public Map<String, Object> getQuickStats() {
        try {
            Map<String, Object> quickStats = new HashMap<>();
            HabitStatistics stats = getHabitStatistics();

            quickStats.put("totalHabits", stats.getTotalHabitsCreated());
            quickStats.put("completedHabits", stats.getTotalHabitsCompleted());
            quickStats.put("pendingHabits", stats.getTotalHabitsPending());
            quickStats.put("activeUsers", stats.getActiveUsers());
            quickStats.put("averageHabitsPerUser", String.format("%.1f", stats.getAverageHabitsPerUser()));
            quickStats.put("totalUsers", stats.getTotalUsers());
            quickStats.put("maleUsers", stats.getMaleUsers());
            quickStats.put("femaleUsers", stats.getFemaleUsers());
            quickStats.put("activeAccounts", stats.getActiveAccounts());

            // Add completion rate
            double completionRate = stats.getTotalHabitsCreated() > 0
                    ? (double) stats.getTotalHabitsCompleted() / stats.getTotalHabitsCreated() * 100
                    : 0.0;
            quickStats.put("completionRate", String.format("%.1f%%", completionRate));

            return quickStats;
        } catch (Exception e) {
            System.err.println("Error calculating quick stats: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Get habits for a specific user
     */
    public List<Habit> getHabitsForUser(User user) {
        try {
            return habitRepository.findByCreatedBy(user);
        } catch (Exception e) {
            System.err.println("Error fetching habits for user " + user.getId() + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get habits for a specific user by ID
     */
    public List<Habit> getHabitsForUserId(Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                return getHabitsForUser(userOpt.get());
            }
            return new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Error fetching habits for user ID " + userId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Search users by various criteria
     */
    public List<AdminHabitViewDTO> searchUsers(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllUserHabitViews();
        }

        HabitFilterCriteria criteria = new HabitFilterCriteria();
        criteria.setSearchTerm(searchTerm);

        return getFilteredUserHabitViews(criteria).getUsers();
    }

    /**
     * Get user by ID with habit data
     */
    public AdminHabitViewDTO getUserHabitView(Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                return createUserHabitViewDTO(userOpt.get());
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error fetching user habit view for ID " + userId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Export user habit data for reports
     */
    public List<Map<String, Object>> exportUserHabitData() {
        try {
            List<AdminHabitViewDTO> users = getAllUserHabitViews();
            return users.stream()
                    .map(user -> {
                        Map<String, Object> data = new HashMap<>();
                        data.put("userId", user.getUserId());
                        data.put("email", user.getEmail());
                        data.put("phone", user.getPhone());
                        data.put("gender", user.getGender());
                        data.put("age", user.getAge());
                        data.put("ageGroup", user.getAgeGroup());
                        data.put("accountStatus", user.getAccountStatus());
                        data.put("totalHabits", user.getTotalHabitsCreated());
                        data.put("completedHabits", user.getTotalHabitsCompleted());
                        data.put("pendingHabits", user.getTotalHabitsPending());
                        data.put("completionRate", String.format("%.1f%%", user.getCompletionRate()));
                        data.put("currentStreak", user.getCurrentStreak());
                        data.put("highestStreak", user.getHighestStreak());
                        data.put("coins", user.getCoins());
                        data.put("lastLogin", user.getLastLoginDate());
                        data.put("registrationDate", user.getRegistrationDate());
                        data.put("hasHabits", user.hasHabits());
                        data.put("isHighPerformer", user.isHighPerformer());
                        data.put("needsAttention", user.needsAttention());
                        data.put("isHighlyEngaged", user.isHighlyEngaged());
                        data.put("firstHabitDate", user.getFirstHabitDate());
                        data.put("lastHabitDate", user.getLastHabitDate());
                        return data;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error exporting user habit data: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}