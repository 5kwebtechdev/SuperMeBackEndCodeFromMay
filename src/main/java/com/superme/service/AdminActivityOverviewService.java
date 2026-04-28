package com.superme.service;

import com.superme.dto.AdminActivityOverviewDTO;
import com.superme.dto.AdminActivityOverviewDTO.ActivityOverviewStatistics;
import com.superme.model.User;
import com.superme.model.Task;
import com.superme.model.Habit;
import com.superme.model.CalendarEvent;
import com.superme.repository.UserRepository;
import com.superme.repository.TaskRepository;
import com.superme.repository.HabitRepository;
import com.superme.repository.CalendarEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing admin activity overview operations.
 * 
 * Provides comprehensive analytics across habits, tasks, and calendar events
 * for administrative dashboard functionality.
 * 
 * Updated with improved error handling, null safety, and better fallback
 * mechanisms.
 */
@Service
public class AdminActivityOverviewService {

    // ============================================================================
    // DEPENDENCY INJECTION
    // ============================================================================

    @Autowired(required = false)
    private UserRepository userRepository;

    @Autowired(required = false)
    private TaskRepository taskRepository;

    @Autowired(required = false)
    private HabitRepository habitRepository;

    @Autowired(required = false)
    private CalendarEventRepository calendarEventRepository;

    // ============================================================================
    // REPOSITORY AVAILABILITY CHECKS
    // ============================================================================

    private boolean isUserRepositoryAvailable() {
        return userRepository != null;
    }

    private boolean isTaskRepositoryAvailable() {
        return taskRepository != null;
    }

    private boolean isHabitRepositoryAvailable() {
        return habitRepository != null;
    }

    private boolean isCalendarEventRepositoryAvailable() {
        return calendarEventRepository != null;
    }

    private boolean areAllRepositoriesAvailable() {
        return isUserRepositoryAvailable() &&
                isTaskRepositoryAvailable() &&
                isHabitRepositoryAvailable() &&
                isCalendarEventRepositoryAvailable();
    }

    // ============================================================================
    // PUBLIC API METHODS
    // ============================================================================

    /**
     * Retrieves comprehensive activity overview for all users.
     * Includes habits completed, tasks completed, same-day activities, and total
     * events.
     * 
     * @return List of AdminActivityOverviewDTO with user activity data
     */
    public List<AdminActivityOverviewDTO> getAllUserActivityOverviews() {
        if (!isUserRepositoryAvailable()) {
            return new ArrayList<>();
        }

        try {
            List<User> users = userRepository.findAll();
            List<AdminActivityOverviewDTO> result = new ArrayList<>();

            for (User user : users) {
                try {
                    AdminActivityOverviewDTO dto = createUserActivityOverviewDTO(user);
                    if (dto != null) {
                        result.add(dto);
                    }
                } catch (Exception e) {
                    System.err.println("Error creating overview for user " + user.getId() + ": " + e.getMessage());
                }
            }

            return result;
        } catch (Exception e) {
            System.err.println("Error fetching user activity overviews: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Calculates global activity overview statistics across all users.
     */
    public AdminActivityOverviewDTO.ActivityOverviewStatistics getActivityOverviewStatistics() {
        try {
            // Get basic statistics with null safety
            Long totalEvents = getTotalEventsCount();
            Long totalHabits = getTotalHabitsCount();
            Long totalTasks = getTotalTasksCount();
            Long totalUsers = getTotalUsersCount();

            // Calculate average events per user with null safety
            Double averageEventsPerUser = (totalUsers != null && totalUsers > 0) ? (double) totalEvents / totalUsers
                    : 0.0;

            // Calculate active users and users with activities
            Long activeUsers = 0L;
            Long usersWithActivities = 0L;

            try {
                List<AdminActivityOverviewDTO> allUsers = getAllUserActivityOverviews();
                activeUsers = allUsers.stream()
                        .filter(user -> user != null && user.isActiveUser())
                        .count();
                usersWithActivities = allUsers.stream()
                        .filter(user -> user != null && user.hasActivity())
                        .count();
            } catch (Exception e) {
                activeUsers = totalUsers;
                usersWithActivities = totalUsers;
            }

            // Create ActivityOverviewStatistics with proper null safety
            return new AdminActivityOverviewDTO.ActivityOverviewStatistics(
                    totalEvents != null ? totalEvents : 0L,
                    totalHabits != null ? totalHabits : 0L,
                    totalTasks != null ? totalTasks : 0L,
                    averageEventsPerUser != null ? averageEventsPerUser : 0.0,
                    activeUsers != null ? activeUsers : 0L,
                    usersWithActivities != null ? usersWithActivities : 0L);

        } catch (Exception e) {
            System.err.println("Error calculating activity overview statistics: " + e.getMessage());
            // Return safe default statistics
            return new AdminActivityOverviewDTO.ActivityOverviewStatistics(
                    0L, 0L, 0L, 0.0, 0L, 0L);
        }
    }

    /**
     * Gets quick statistics for dashboard cards.
     * 
     * @return Map containing key metrics for dashboard display
     */
    public Map<String, Object> getQuickStats() {
        Map<String, Object> quickStats = new HashMap<>();

        try {
            ActivityOverviewStatistics stats = getActivityOverviewStatistics();

            quickStats.put("totalEvents", stats.getTotalEvents());
            quickStats.put("totalHabits", stats.getTotalHabits());
            quickStats.put("totalTasks", stats.getTotalTasks());
            quickStats.put("averageEventsPerUser", stats.getAverageEventsPerUser());
            quickStats.put("activeUsers", stats.getActiveUsers());
            quickStats.put("usersWithActivities", stats.getUsersWithActivities());

            // Additional quick metrics with null safety
            quickStats.put("usersWithActivity", safeGetListSize(getUsersWithActivity()));
            quickStats.put("usersWithSameDayActivities", safeGetListSize(getUsersWithSameDayActivities()));
            quickStats.put("totalUsers", getTotalUsersCount());
            quickStats.put("serviceStatus", areAllRepositoriesAvailable() ? "healthy" : "degraded");

        } catch (Exception e) {
            System.err.println("Error calculating quick stats: " + e.getMessage());
            // Provide minimal fallback data
            quickStats.put("totalEvents", 0L);
            quickStats.put("totalHabits", 0L);
            quickStats.put("totalTasks", 0L);
            quickStats.put("averageEventsPerUser", 0.0);
            quickStats.put("serviceStatus", "error");
            quickStats.put("error", e.getMessage());
        }

        return quickStats;
    }

    /**
     * Gets detailed analytics including top performers and trends.
     * 
     * @return Map containing comprehensive analytics data
     */
    public Map<String, Object> getDetailedAnalytics() {
        Map<String, Object> analytics = new HashMap<>();

        try {
            ActivityOverviewStatistics stats = getActivityOverviewStatistics();
            List<AdminActivityOverviewDTO> allUsers = getAllUserActivityOverviews();

            analytics.put("statistics", stats);
            analytics.put("topUsersByEvents", getTopUsersByTotalEvents(5));
            analytics.put("topUsersByCompleted", getTopUsersByCompletedActivities(5));
            analytics.put("usersWithSameDayActivities", safeGetListSize(getUsersWithSameDayActivities()));
            analytics.put("totalUsersWithActivity", safeGetListSize(getUsersWithActivity()));
            analytics.put("totalUsers", allUsers.size());
            analytics.put("engagementRate", calculateEngagementRate(allUsers));
            analytics.put("averageCompletionRate", calculateAverageCompletionRate(allUsers));
            analytics.put("serviceHealth", areAllRepositoriesAvailable() ? "healthy" : "degraded");

        } catch (Exception e) {
            System.err.println("Error calculating detailed analytics: " + e.getMessage());
            analytics.put("error", e.getMessage());
            analytics.put("serviceHealth", "error");
        }

        return analytics;
    }

    // ============================================================================
    // FILTERING AND ANALYSIS METHODS
    // ============================================================================

    /**
     * Gets list of users who have at least one activity.
     * 
     * @return List of users with any activity (habits, tasks, or events)
     */
    public List<AdminActivityOverviewDTO> getUsersWithActivity() {
        try {
            return getAllUserActivityOverviews().stream()
                    .filter(user -> user != null && user.hasActivity())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error getting users with activity: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Gets list of users who have no activities.
     * 
     * @return List of users without any activities
     */
    public List<AdminActivityOverviewDTO> getUsersWithoutActivity() {
        try {
            return getAllUserActivityOverviews().stream()
                    .filter(user -> user != null && !user.hasActivity())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error getting users without activity: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Gets list of users who have both habits and tasks on the same day.
     * 
     * @return List of users with same-day habit and task activities
     */
    public List<AdminActivityOverviewDTO> getUsersWithSameDayActivities() {
        try {
            return getAllUserActivityOverviews().stream()
                    .filter(user -> user != null && user.hasSameDayActivities())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error getting users with same day activities: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Gets top users by total number of events (habits + tasks + calendar events).
     * 
     * @param limit Maximum number of users to return
     * @return List of top users sorted by total events descending
     */
    public List<AdminActivityOverviewDTO> getTopUsersByTotalEvents(int limit) {
        try {
            return getAllUserActivityOverviews().stream()
                    .filter(user -> user != null && user.hasActivity())
                    .sorted((u1, u2) -> {
                        Long events1 = u1.getTotalEvents() != null ? u1.getTotalEvents() : 0L;
                        Long events2 = u2.getTotalEvents() != null ? u2.getTotalEvents() : 0L;
                        return Long.compare(events2, events1);
                    })
                    .limit(Math.max(0, limit))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error getting top users by events: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Gets top users by total number of completed activities.
     * 
     * @param limit Maximum number of users to return
     * @return List of top users sorted by completed activities descending
     */
    public List<AdminActivityOverviewDTO> getTopUsersByCompletedActivities(int limit) {
        try {
            return getAllUserActivityOverviews().stream()
                    .filter(user -> user != null && user.hasCompletedActivities())
                    .sorted((u1, u2) -> {
                        Long completed1 = u1.getTotalCompletedActivities() != null ? u1.getTotalCompletedActivities()
                                : 0L;
                        Long completed2 = u2.getTotalCompletedActivities() != null ? u2.getTotalCompletedActivities()
                                : 0L;
                        return Long.compare(completed2, completed1);
                    })
                    .limit(Math.max(0, limit))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error getting top users by completed activities: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ============================================================================
    // PRIVATE HELPER METHODS
    // ============================================================================

    /**
     * Creates activity overview DTO for a single user.
     * Uses name instead of username.
     * 
     * @param user The user to create overview for
     * @return AdminActivityOverviewDTO with user's activity data, or null if user
     *         is invalid
     */
    private AdminActivityOverviewDTO createUserActivityOverviewDTO(User user) {
        if (user == null || user.getId() == null) {
            return null;
        }

        AdminActivityOverviewDTO dto = new AdminActivityOverviewDTO();
        dto.setUserId(user.getId());
        // Use name instead of username
        dto.setName(user.getName() != null ? user.getName() : "Unknown User");

        try {
            List<Habit> userHabits = getHabitsForUser(user);
            List<Task> userTasks = getTasksForUser(user);
            List<CalendarEvent> userEvents = getEventsForUser(user);

            Long totalHabitsCompleted = userHabits.stream()
                    .filter(Objects::nonNull)
                    .mapToLong(habit -> habit.getStatus() == Habit.HabitStatus.COMPLETED ? 1L : 0L)
                    .sum();

            Long totalTasksCompleted = userTasks.stream()
                    .filter(Objects::nonNull)
                    .mapToLong(task -> task.getStatus() == Task.TaskStatus.COMPLETED ? 1L : 0L)
                    .sum();

            Long habitsAndTasksSameDay = calculateSameDayActivities(userHabits, userTasks);
            Long totalEvents = (long) (userHabits.size() + userTasks.size() + userEvents.size());

            dto.setTotalHabitsCompleted(totalHabitsCompleted != null ? totalHabitsCompleted : 0L);
            dto.setTotalTasksCompleted(totalTasksCompleted != null ? totalTasksCompleted : 0L);
            dto.setHabitsAndTasksSameDay(habitsAndTasksSameDay != null ? habitsAndTasksSameDay : 0L);
            dto.setTotalEvents(totalEvents != null ? totalEvents : 0L);

        } catch (Exception e) {
            System.err.println("Error calculating activity overview for user " + user.getId() + ": " + e.getMessage());
            // Set safe defaults on error
            dto.setTotalHabitsCompleted(0L);
            dto.setTotalTasksCompleted(0L);
            dto.setHabitsAndTasksSameDay(0L);
            dto.setTotalEvents(0L);
        }

        return dto;
    }

    /**
     * Retrieves habits for a specific user with comprehensive error handling.
     * 
     * @param user The user to get habits for
     * @return List of habits for the user, empty list if error occurs or repository
     *         unavailable
     */
    private List<Habit> getHabitsForUser(User user) {
        if (user == null || !isHabitRepositoryAvailable()) {
            return new ArrayList<>();
        }
        try {
            return habitRepository.findByCreatedBy(user);
        } catch (Exception e) {
            try {
                return habitRepository.findByCreatedById(user.getId());
            } catch (Exception ex) {
                return new ArrayList<>();
            }
        }
    }

    /**
     * Retrieves tasks for a specific user with multiple fallback methods.
     * 
     * @param user The user to get tasks for
     * @return List of tasks for the user, empty list if error occurs or repository
     *         unavailable
     */
    private List<Task> getTasksForUser(User user) {
        if (user == null || !isTaskRepositoryAvailable()) {
            return new ArrayList<>();
        }
        try {
            List<Task> createdTasks = new ArrayList<>();
            List<Task> assignedTasks = new ArrayList<>();
            try {
                createdTasks = taskRepository.findByCreatedBy(user);
            } catch (Exception e) {
                // fallback: ignore
            }
            try {
                // If repository supports findByAssignedTo, use it; otherwise, filter all tasks
                if (taskRepository.getClass().getDeclaredMethods().length > 0) {
                    assignedTasks = taskRepository.findByAssignedTo(user);
                } else {
                    assignedTasks = taskRepository.findAll().stream()
                            .filter(task -> task.getAssignedTo() != null && task.getAssignedTo().equals(user))
                            .collect(Collectors.toList());
                }
            } catch (Exception e) {
                // fallback: ignore
            }
            List<Task> allTasks = new ArrayList<>(createdTasks);
            allTasks.addAll(assignedTasks);
            return allTasks;
        } catch (Exception exc) {
            return new ArrayList<>();
        }
    }

    /**
     * Retrieves calendar events for a specific user with multiple fallback methods.
     * 
     * @param user The user to get events for
     * @return List of calendar events for the user, empty list if error occurs or
     *         repository unavailable
     */
    private List<CalendarEvent> getEventsForUser(User user) {
        if (user == null || !isCalendarEventRepositoryAvailable()) {
            return new ArrayList<>();
        }
        try {
            try {
                return calendarEventRepository.findByUser(user);
            } catch (Exception ex) {
                try {
                    return calendarEventRepository.findByCreatedBy(user);
                } catch (Exception exc) {
                    return new ArrayList<>();
                }
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Calculates the number of days where a user has both habits and tasks.
     * This indicates coordinated activity planning.
     * 
     * @param habits List of user's habits
     * @param tasks  List of user's tasks
     * @return Number of days with both habits and tasks
     */
    private Long calculateSameDayActivities(List<Habit> habits, List<Task> tasks) {
        try {
            if (habits == null || tasks == null || habits.isEmpty() || tasks.isEmpty()) {
                return 0L;
            }

            // Extract dates from habits with null safety
            Set<LocalDate> habitDates = habits.stream()
                    .filter(Objects::nonNull)
                    .map(Habit::getStartDate)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // Extract dates from tasks with null safety
            Set<LocalDate> taskDates = tasks.stream()
                    .filter(Objects::nonNull)
                    .map(task -> {
                        // Task.getCreatedAt() returns LocalDateTime, convert to LocalDate
                        LocalDateTime ldt = null;
                        try {
                            java.lang.reflect.Method m = task.getClass().getMethod("getCreatedAt");
                            Object val = m.invoke(task);
                            if (val instanceof LocalDateTime) {
                                ldt = (LocalDateTime) val;
                            }
                        } catch (Exception e) {
                            // fallback: skip
                        }
                        return ldt != null ? ldt.toLocalDate() : null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // Find intersection of habit and task dates
            habitDates.retainAll(taskDates);
            return (long) habitDates.size();

        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Calculates overall engagement rate (users with activities / total users).
     * 
     * @param allUsers List of all user activity overviews
     * @return Engagement rate as percentage
     */
    private Double calculateEngagementRate(List<AdminActivityOverviewDTO> allUsers) {
        if (allUsers == null || allUsers.isEmpty()) {
            return 0.0;
        }
        try {
            long usersWithActivity = allUsers.stream()
                    .filter(Objects::nonNull)
                    .filter(AdminActivityOverviewDTO::hasActivity)
                    .count();
            return ((double) usersWithActivity / allUsers.size()) * 100;
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Calculates average completion rate across all users.
     * 
     * @param allUsers List of all user activity overviews
     * @return Average completion rate as percentage
     */
    private Double calculateAverageCompletionRate(List<AdminActivityOverviewDTO> allUsers) {
        if (allUsers == null || allUsers.isEmpty()) {
            return 0.0;
        }
        try {
            List<AdminActivityOverviewDTO> usersWithActivities = allUsers.stream()
                    .filter(Objects::nonNull)
                    .filter(AdminActivityOverviewDTO::hasActivity)
                    .collect(Collectors.toList());

            if (usersWithActivities.isEmpty()) {
                return 0.0;
            }

            double totalCompletionRate = usersWithActivities.stream()
                    .mapToDouble(user -> {
                        try {
                            Long totalCompletable = (user.getTotalHabitsCompleted() != null
                                    ? user.getTotalHabitsCompleted()
                                    : 0L) +
                                    (user.getTotalTasksCompleted() != null ? user.getTotalTasksCompleted() : 0L);
                            Long totalEvents = user.getTotalEvents() != null ? user.getTotalEvents() : 0L;

                            if (totalEvents == 0) {
                                return 0.0;
                            }
                            return ((double) totalCompletable / totalEvents) * 100;
                        } catch (Exception e) {
                            return 0.0;
                        }
                    })
                    .sum();

            return totalCompletionRate / usersWithActivities.size();
        } catch (Exception e) {
            return 0.0;
        }
    }

    // ============================================================================
    // UTILITY METHODS FOR SPECIFIC ANALYTICS
    // ============================================================================

    /**
     * Gets users who are highly engaged (above average activity).
     * 
     * @return List of highly engaged users
     */
    public List<AdminActivityOverviewDTO> getHighlyEngagedUsers() {
        try {
            List<AdminActivityOverviewDTO> allUsers = getAllUserActivityOverviews();
            if (allUsers.isEmpty()) {
                return new ArrayList<>();
            }

            double averageEvents = allUsers.stream()
                    .filter(Objects::nonNull)
                    .mapToLong(user -> user.getTotalEvents() != null ? user.getTotalEvents() : 0L)
                    .average()
                    .orElse(0.0);

            return allUsers.stream()
                    .filter(Objects::nonNull)
                    .filter(user -> (user.getTotalEvents() != null ? user.getTotalEvents() : 0L) > averageEvents)
                    .sorted((u1, u2) -> {
                        Long events1 = u1.getTotalEvents() != null ? u1.getTotalEvents() : 0L;
                        Long events2 = u2.getTotalEvents() != null ? u2.getTotalEvents() : 0L;
                        return Long.compare(events2, events1);
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Gets users who need attention (low activity or completion rates).
     * 
     * @return List of users who may need support or encouragement
     */
    public List<AdminActivityOverviewDTO> getUsersNeedingAttention() {
        try {
            List<AdminActivityOverviewDTO> allUsers = getAllUserActivityOverviews();
            return allUsers.stream()
                    .filter(Objects::nonNull)
                    .filter(user -> {
                        Long totalEvents = user.getTotalEvents() != null ? user.getTotalEvents() : 0L;
                        Long completedActivities = user.getTotalCompletedActivities() != null
                                ? user.getTotalCompletedActivities()
                                : 0L;
                        boolean lowActivity = totalEvents <= 2;
                        boolean poorCompletion = completedActivities == 0 && totalEvents > 0;
                        return lowActivity || poorCompletion;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Gets activity distribution summary.
     * 
     * @return Map with activity type distribution percentages
     */
    public Map<String, Object> getActivityDistribution() {
        Map<String, Object> distribution = new HashMap<>();

        try {
            ActivityOverviewStatistics stats = getActivityOverviewStatistics();

            Long totalHabits = stats.getTotalHabits() != null ? stats.getTotalHabits() : 0L;
            Long totalTasks = stats.getTotalTasks() != null ? stats.getTotalTasks() : 0L;
            Long totalEvents = stats.getTotalEvents() != null ? stats.getTotalEvents() : 0L;
            Long totalCalendarEvents = totalEvents - totalHabits - totalTasks;

            if (totalEvents > 0) {
                distribution.put("habitsPercentage", ((double) totalHabits / totalEvents) * 100);
                distribution.put("tasksPercentage", ((double) totalTasks / totalEvents) * 100);
                distribution.put("eventsPercentage", ((double) totalCalendarEvents / totalEvents) * 100);
            } else {
                distribution.put("habitsPercentage", 0.0);
                distribution.put("tasksPercentage", 0.0);
                distribution.put("eventsPercentage", 0.0);
            }

            distribution.put("totalHabits", totalHabits);
            distribution.put("totalTasks", totalTasks);
            distribution.put("totalCalendarEvents", totalCalendarEvents);
            distribution.put("totalEvents", totalEvents);

        } catch (Exception e) {
            // Return safe defaults
            distribution.put("habitsPercentage", 0.0);
            distribution.put("tasksPercentage", 0.0);
            distribution.put("eventsPercentage", 0.0);
            distribution.put("totalHabits", 0L);
            distribution.put("totalTasks", 0L);
            distribution.put("totalCalendarEvents", 0L);
            distribution.put("totalEvents", 0L);
            distribution.put("error", e.getMessage());
        }

        return distribution;
    }

    // ============================================================================
    // SAFE COUNT METHODS
    // ============================================================================

    /**
     * Gets total number of events across all repositories (habits + tasks +
     * calendar events).
     * 
     * @return Total count of all events in the system
     */
    private Long getTotalEventsCount() {
        try {
            Long totalHabits = getTotalHabitsCount();
            Long totalTasks = getTotalTasksCount();
            Long totalCalendarEvents = getTotalCalendarEventsCount();
            return totalHabits + totalTasks + totalCalendarEvents;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Gets total number of habits in the system.
     * 
     * @return Total count of habits
     */
    private Long getTotalHabitsCount() {
        if (!isHabitRepositoryAvailable()) {
            return 0L;
        }
        try {
            return habitRepository.count();
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Gets total number of tasks in the system.
     * 
     * @return Total count of tasks
     */
    private Long getTotalTasksCount() {
        if (!isTaskRepositoryAvailable()) {
            return 0L;
        }
        try {
            return taskRepository.count();
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Gets total number of calendar events in the system.
     * 
     * @return Total count of calendar events
     */
    private Long getTotalCalendarEventsCount() {
        if (!isCalendarEventRepositoryAvailable()) {
            return 0L;
        }
        try {
            return calendarEventRepository.count();
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Gets total number of users in the system.
     * 
     * @return Total count of users
     */
    private Long getTotalUsersCount() {
        if (!isUserRepositoryAvailable()) {
            return 0L;
        }
        try {
            return userRepository.count();
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Safely gets the size of a list, returning 0 if null or on error.
     * 
     * @param list The list to get size for
     * @return Size of list or 0 if null/error
     */
    private int safeGetListSize(List<?> list) {
        try {
            return list != null ? list.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
