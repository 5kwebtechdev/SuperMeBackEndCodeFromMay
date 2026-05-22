package com.superme.service;

import com.superme.dto.AdminTaskViewDTO;
import com.superme.dto.AdminTaskViewDTO.TaskStatistics;
import com.superme.dto.AdminTaskOverviewResponse;
import com.superme.dto.AdminTaskOverviewResponse.TaskFilterCriteria;
import com.superme.model.User;
import com.superme.model.Task;
import com.superme.model.Task.Priority;
import com.superme.model.Task.TaskStatus;
import com.superme.repository.UserRepository;
import com.superme.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing admin task view operations.
 * 
 * Provides comprehensive task analytics and overview functionality
 * for administrative dashboard based on the Task model structure.
 * Includes advanced search, filtering, and analytics capabilities.
 * 
 * @author MindfullB Admin Team
 * @version 2.0
 */
@Service
public class AdminTaskViewService {

    // ============================================================================
    // DEPENDENCY INJECTION
    // ============================================================================

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    // ============================================================================
    // PUBLIC API METHODS
    // ============================================================================

    /**
     * Retrieves comprehensive task overview for all users.
     * 
     * @return List of AdminTaskViewDTO with user task data
     */
    public List<AdminTaskViewDTO> getAllUserTaskViews() {
        try {
            List<User> users = userRepository.findAll();
            List<AdminTaskViewDTO> result = new ArrayList<>();

            for (User user : users) {
                AdminTaskViewDTO dto = createUserTaskViewDTO(user);
                result.add(dto);
            }

            return result;
        } catch (Exception e) {
            System.err.println("Error fetching user task views: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Calculates global task statistics across all users.
     * 
     * @return TaskStatistics with system-wide task metrics
     */
    public TaskStatistics getTaskStatistics() {
        try {
            // Get all tasks
            List<Task> allTasks = taskRepository.findAll();

            // Calculate basic statistics
            Long totalTasksCreated = (long) allTasks.size();
            Long totalTasksCompleted = allTasks.stream()
                    .mapToLong(task -> task.getStatus() == Task.TaskStatus.COMPLETED ? 1L : 0L)
                    .sum();
            Long totalTasksOverdue = allTasks.stream()
                    .mapToLong(task -> isTaskOverdue(task) ? 1L : 0L)
                    .sum();
            Long totalTasksPending = totalTasksCreated - totalTasksCompleted - totalTasksOverdue;

            // Calculate users with tasks
            Long activeUsers = allTasks.stream()
                    .map(task -> {
                        try {
                            User createdBy = task.getCreatedBy();
                            if (createdBy != null) {
                                return createdBy.getId();
                            }

                            User assignedTo = task.getAssignedTo();
                            if (assignedTo != null) {
                                return assignedTo.getId();
                            }
                        } catch (Exception e) {
                            // Ignore errors
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();

            // Calculate average tasks per user
            Long totalUsers = userRepository.count();
            Double averageTasksPerUser = totalUsers > 0 ? (double) totalTasksCreated / totalUsers : 0.0;

            return new TaskStatistics(
                    totalTasksCreated,
                    totalTasksCompleted,
                    totalTasksPending,
                    totalTasksOverdue,
                    averageTasksPerUser,
                    activeUsers);

        } catch (Exception e) {
            System.err.println("Error calculating task statistics: " + e.getMessage());
            e.printStackTrace();
            return new TaskStatistics();
        }
    }

    /**
     * Filter users by search term (userId only)
     * 
     * @param searchTerm The search term to filter by
     * @return Filtered list of users with highlighting
     */
    public List<AdminTaskViewDTO> filterUsersBySearchTerm(String searchTerm) {
        List<AdminTaskViewDTO> users = getAllUserTaskViews();

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return users;
        }

        // Filter and apply highlighting (username removed)
        return AdminTaskViewDTO.filterBySearchTerm(users, searchTerm).stream()
                .map(user -> user.getHighlightedVersion(searchTerm))
                .collect(Collectors.toList());
    }

    /**
     * Filter users by comprehensive criteria
     * 
     * @param criteria The filter criteria to apply
     * @return Filtered list of users
     */
    public List<AdminTaskViewDTO> filterUsers(TaskFilterCriteria criteria) {
        List<AdminTaskViewDTO> users = getAllUserTaskViews();

        if (criteria == null || !criteria.hasFilters()) {
            return users;
        }

        // Apply search term filter
        if (criteria.getSearchTerm() != null && !criteria.getSearchTerm().trim().isEmpty()) {
            users = AdminTaskViewDTO.filterBySearchTerm(users, criteria.getSearchTerm());
        }

        // Apply completion rate filter
        if (criteria.getMinCompletionRate() != null || criteria.getMaxCompletionRate() != null) {
            users = AdminTaskViewDTO.filterByCompletionRate(users,
                    criteria.getMinCompletionRate(), criteria.getMaxCompletionRate());
        }

        // Apply task count filter
        if (criteria.getMinTaskCount() != null || criteria.getMaxTaskCount() != null) {
            users = AdminTaskViewDTO.filterByTaskCount(users,
                    criteria.getMinTaskCount(), criteria.getMaxTaskCount());
        }

        // Apply overdue status filter
        if (criteria.getHasOverdueTasks() != null) {
            users = AdminTaskViewDTO.filterByOverdueStatus(users, criteria.getHasOverdueTasks());
        }

        // Apply task existence filter
        if (criteria.getHasTasks() != null) {
            users = AdminTaskViewDTO.filterByTaskExistence(users, criteria.getHasTasks());
        }

        // Apply highlighting if search term exists
        if (criteria.getSearchTerm() != null && !criteria.getSearchTerm().trim().isEmpty()) {
            users = users.stream()
                    .map(user -> user.getHighlightedVersion(criteria.getSearchTerm()))
                    .collect(Collectors.toList());
        }

        return users;
    }

    /**
     * Filter users with pagination support
     * 
     * @param criteria Filter criteria
     * @param limit    Maximum number of results
     * @param offset   Starting position
     * @return Paginated filtered list of users
     */
    public List<AdminTaskViewDTO> filterUsersWithPagination(TaskFilterCriteria criteria, int limit, int offset) {
        List<AdminTaskViewDTO> filteredUsers = filterUsers(criteria);

        return filteredUsers.stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get count of filtered results
     * 
     * @param criteria Filter criteria
     * @return Number of users matching the criteria
     */
    public long getFilterResultsCount(TaskFilterCriteria criteria) {
        return filterUsers(criteria).size();
    }

    /**
     * Get available filter options for the frontend
     * 
     * @return Map of available filter options
     */
    public Map<String, Object> getAvailableFilterOptions() {
        Map<String, Object> options = new HashMap<>();

        try {
            List<AdminTaskViewDTO> users = getAllUserTaskViews();

            // Completion rate ranges
            List<String> completionRateRanges = Arrays.asList(
                    "0-25%", "25-50%", "50-75%", "75-100%");

            // Task count ranges
            List<String> taskCountRanges = Arrays.asList(
                    "1-5", "6-10", "11-20", "21+");

            // Filter statistics
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalUsers", users.size());
            statistics.put("usersWithTasks", users.stream().filter(AdminTaskViewDTO::hasTasks).count());
            statistics.put("usersWithoutTasks", users.stream().filter(user -> !user.hasTasks()).count());
            statistics.put("usersWithOverdueTasks", users.stream().filter(AdminTaskViewDTO::hasOverdueTasks).count());

            options.put("completionRateRanges", completionRateRanges);
            options.put("taskCountRanges", taskCountRanges);
            options.put("filterStatistics", statistics);
            options.put("searchFields", Arrays.asList("userId", "username"));

        } catch (Exception e) {
            System.err.println("Error getting filter options: " + e.getMessage());
        }

        return options;
    }

    /**
     * Get filter statistics for current user base
     * 
     * @return Filter statistics
     */
    public AdminTaskOverviewResponse.FilterStatistics getFilterStatistics() {
        List<AdminTaskViewDTO> users = getAllUserTaskViews();
        return new AdminTaskOverviewResponse.FilterStatistics(users);
    }

    /**
     * Get detailed analytics with filter support
     * 
     * @param criteria Filter criteria to apply
     * @return Map containing detailed analytics for filtered data
     */
    public Map<String, Object> getDetailedAnalyticsWithFilters(TaskFilterCriteria criteria) {
        List<AdminTaskViewDTO> users = filterUsers(criteria);
        Map<String, Object> analytics = new HashMap<>();

        try {
            // Basic statistics
            TaskStatistics globalStats = getTaskStatistics();
            TaskStatistics filteredStats = AdminTaskViewDTO.getTaskStatistics(users);

            analytics.put("globalStatistics", globalStats);
            analytics.put("filteredStatistics", filteredStats);
            analytics.put("filterCriteria", criteria);
            analytics.put("totalUsers", getAllUserTaskViews().size());
            analytics.put("filteredUsers", users.size());
            analytics.put("filterEfficiency",
                    users.size() > 0 ? (double) users.size() / getAllUserTaskViews().size() * 100 : 0.0);

            // Performance breakdown
            Map<String, Object> performanceBreakdown = new HashMap<>();
            long highPerformers = users.stream()
                    .filter(user -> user.hasTasks() && user.getCompletionRate() >= 80.0)
                    .count();
            long mediumPerformers = users.stream()
                    .filter(user -> user.hasTasks() && user.getCompletionRate() >= 40.0
                            && user.getCompletionRate() < 80.0)
                    .count();
            long lowPerformers = users.stream()
                    .filter(user -> user.hasTasks() && user.getCompletionRate() < 40.0)
                    .count();

            performanceBreakdown.put("highPerformers", highPerformers);
            performanceBreakdown.put("mediumPerformers", mediumPerformers);
            performanceBreakdown.put("lowPerformers", lowPerformers);
            performanceBreakdown.put("highPerformerPercentage",
                    users.size() > 0 ? (double) highPerformers / users.size() * 100 : 0.0);

            analytics.put("performanceBreakdown", performanceBreakdown);

            // Task activity breakdown
            Map<String, Object> activityBreakdown = new HashMap<>();
            long usersWithTasks = users.stream().filter(AdminTaskViewDTO::hasTasks).count();
            long usersWithoutTasks = users.size() - usersWithTasks;
            long usersWithOverdue = users.stream().filter(AdminTaskViewDTO::hasOverdueTasks).count();

            activityBreakdown.put("usersWithTasks", usersWithTasks);
            activityBreakdown.put("usersWithoutTasks", usersWithoutTasks);
            activityBreakdown.put("usersWithOverdueTasks", usersWithOverdue);
            activityBreakdown.put("taskAdoptionRate",
                    users.size() > 0 ? (double) usersWithTasks / users.size() * 100 : 0.0);

            analytics.put("activityBreakdown", activityBreakdown);

            // Top performers
            analytics.put("topPerformers", AdminTaskViewDTO.sortByCompletionRate(users)
                    .stream().limit(10).collect(Collectors.toList()));

            // Users needing attention
            analytics.put("usersNeedingAttention", users.stream()
                    .filter(AdminTaskViewDTO::needsAttention)
                    .collect(Collectors.toList()));

        } catch (Exception e) {
            System.err.println("Error calculating detailed analytics: " + e.getMessage());
            analytics.put("error", "Error calculating analytics: " + e.getMessage());
        }

        return analytics;
    }

    /**
     * Gets quick statistics for dashboard cards.
     * 
     * @return Map containing key task metrics for dashboard display
     */
    public Map<String, Object> getQuickStats() {
        try {
            Map<String, Object> quickStats = new HashMap<>();
            TaskStatistics stats = getTaskStatistics();

            quickStats.put("totalTasks", stats.getTotalTasksCreated());
            quickStats.put("completedTasks", stats.getTotalTasksCompleted());
            quickStats.put("pendingTasks", stats.getTotalTasksPending());
            quickStats.put("overdueTasks", stats.getTotalTasksOverdue());
            quickStats.put("activeUsers", stats.getActiveUsers());
            quickStats.put("averageTasksPerUser", String.format("%.1f", stats.getAverageTasksPerUser()));
            quickStats.put("completionRate", stats.getFormattedSystemCompletionRate());
            quickStats.put("overdueRate", stats.getFormattedSystemOverdueRate());

            // Additional quick metrics based on Task model
            quickStats.put("highPriorityTasks", getTasksByPriority(Priority.HIGH).size());
            quickStats.put("morningTasks", getTasksByRoutine("MORNING").size());
            quickStats.put("inProgressTasks", getTasksByStatus(TaskStatus.ONGOING).size());

            return quickStats;
        } catch (Exception e) {
            System.err.println("Error calculating quick stats: " + e.getMessage());
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    /**
     * Gets detailed task analytics including trends and distributions.
     * 
     * @return Map containing comprehensive task analytics data
     */
    public Map<String, Object> getDetailedTaskAnalytics() {
        try {
            Map<String, Object> analytics = new HashMap<>();

            TaskStatistics stats = getTaskStatistics();
            List<AdminTaskViewDTO> userViews = getAllUserTaskViews();

            analytics.put("taskStatistics", stats);
            analytics.put("topPerformers", getTopPerformers(userViews, 5));
            analytics.put("taskDistribution", getTaskDistribution());
            analytics.put("priorityDistribution", getPriorityDistribution());
            analytics.put("routineDistribution", getRoutineDistribution());
            analytics.put("statusDistribution", getStatusDistribution());
            analytics.put("completionTrends", getCompletionTrends());
            analytics.put("overdueTrends", getOverdueTrends());
            analytics.put("userTaskCounts", getUserTaskCounts());
            analytics.put("rewardAnalysis", getRewardAnalysis());
            analytics.put("familyTaskAnalysis", getFamilyTaskAnalysis());

            return analytics;
        } catch (Exception e) {
            System.err.println("Error calculating detailed task analytics: " + e.getMessage());
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    // ============================================================================
    // FILTERING AND ANALYSIS METHODS
    // ============================================================================

    /**
     * Gets list of users who have created at least one task.
     * 
     * @return List of users with tasks
     */
    public List<AdminTaskViewDTO> getUsersWithTasks() {
        return getAllUserTaskViews().stream()
                .filter(AdminTaskViewDTO::hasTasks)
                .collect(Collectors.toList());
    }

    /**
     * Gets list of users who have not created any tasks.
     * 
     * @return List of users without tasks
     */
    public List<AdminTaskViewDTO> getUsersWithoutTasks() {
        return getAllUserTaskViews().stream()
                .filter(user -> !user.hasTasks())
                .collect(Collectors.toList());
    }

    /**
     * Gets list of users who have overdue tasks.
     * 
     * @return List of users with overdue tasks
     */
    public List<AdminTaskViewDTO> getUsersWithOverdueTasks() {
        return getAllUserTaskViews().stream()
                .filter(AdminTaskViewDTO::hasOverdueTasks)
                .collect(Collectors.toList());
    }

    /**
     * Gets top task performers by completion rate.
     * 
     * @param limit Maximum number of users to return
     * @return List of top performing users
     */
    public List<AdminTaskViewDTO> getTopTaskPerformers(int limit) {
        return AdminTaskViewDTO.sortByCompletionRate(getAllUserTaskViews())
                .stream()
                .filter(AdminTaskViewDTO::hasTasks)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Gets users who need attention (low completion rate or high overdue rate).
     * 
     * @return List of users needing attention
     */
    public List<AdminTaskViewDTO> getUsersNeedingAttention() {
        return getAllUserTaskViews().stream()
                .filter(AdminTaskViewDTO::needsAttention)
                .collect(Collectors.toList());
    }

    /**
     * Gets highly engaged users with good task completion patterns.
     * 
     * @return List of highly engaged users
     */
    public List<AdminTaskViewDTO> getHighlyEngagedUsers() {
        return getAllUserTaskViews().stream()
                .filter(user -> user.hasTasks() &&
                        user.getCompletionRate() >= 70.0 &&
                        user.getTotalTasksCreated() >= 5)
                .sorted((u1, u2) -> Double.compare(u2.getCompletionRate(), u1.getCompletionRate()))
                .collect(Collectors.toList());
    }

    /**
     * Gets users with most tasks created.
     * 
     * @param limit Maximum number of users to return
     * @return List of most active task creators
     */
    public List<AdminTaskViewDTO> getMostActiveTaskCreators(int limit) {
        return AdminTaskViewDTO.sortByTaskCount(getAllUserTaskViews())
                .stream()
                .filter(AdminTaskViewDTO::hasTasks)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Gets most active users this month by tasks created.
     * 
     * @param limit Maximum number of users to return
     * @return List of most active users this month
     */
    public List<AdminTaskViewDTO> getMostActiveUsersThisMonth(int limit) {
        // This would need task creation dates to be more accurate
        // For now, return most active overall
        return getMostActiveTaskCreators(limit);
    }

    /**
     * Gets tasks filtered by priority level.
     * 
     * @param priority The priority level to filter by
     * @return List of tasks with the specified priority
     */
    public List<Task> getTasksByPriority(Priority priority) {
        try {
            return taskRepository.findAll().stream()
                    .filter(task -> {
                        try {
                            return priority.equals(task.getPriority());
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error fetching tasks by priority: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Gets tasks filtered by routine time.
     * 
     * @param routine The routine time to filter by
     * @return List of tasks with the specified routine
     */
    public List<Task> getTasksByRoutine(String routine) {
        try {
            return taskRepository.findAll().stream()
                    .filter(task -> {
                        try {
                            return routine.equals(task.getRoutine());
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error fetching tasks by routine: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Gets tasks filtered by status.
     * 
     * @param status The task status to filter by
     * @return List of tasks with the specified status
     */
    public List<Task> getTasksByStatus(TaskStatus status) {
        try {
            return taskRepository.findAll().stream()
                    .filter(task -> {
                        try {
                            return status.equals(task.getStatus());
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error fetching tasks by status: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ============================================================================
    // SEARCH HELPER METHODS
    // ============================================================================

    /**
     * Search users by multiple criteria with flexible matching
     * 
     * @param searchTerm     Search term
     * @param searchUserId   Whether to search in user ID
     * @param searchUsername Whether to search in username
     * @return List of matching users with highlighting
     */
    public List<AdminTaskViewDTO> searchUsersWithCriteria(String searchTerm,
            boolean searchUserId,
            boolean searchUsername) {
        List<AdminTaskViewDTO> users = getAllUserTaskViews();

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return users;
        }

        // Username search removed
        if (!searchUserId) {
            return new ArrayList<>();
        }

        return users.stream()
                .filter(user -> user.matchesSearchCriteria(searchTerm, searchUserId, false))
                .map(user -> user.getHighlightedVersion(searchTerm))
                .collect(Collectors.toList());
    }

    /**
     * Get search suggestions based on partial input
     * 
     * @param partialInput Partial search input
     * @param limit        Maximum number of suggestions
     * @return List of search suggestions
     */
    public List<String> getSearchSuggestions(String partialInput, int limit) {
        if (partialInput == null || partialInput.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<AdminTaskViewDTO> users = getAllUserTaskViews();
        Set<String> suggestions = new HashSet<>();
        String lowerInput = partialInput.toLowerCase();

        // Username suggestions removed

        // Add matching user IDs (as strings)
        users.stream()
                .map(user -> user.getUserId().toString())
                .filter(userId -> userId.toLowerCase().startsWith(lowerInput))
                .forEach(suggestions::add);

        return suggestions.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ============================================================================
    // PRIVATE HELPER METHODS
    // ============================================================================

    /**
     * Creates task view DTO for a single user.
     * 
     * @param user The user to create task view for
     * @return AdminTaskViewDTO with user's task data
     */
    private AdminTaskViewDTO createUserTaskViewDTO(User user) {
        AdminTaskViewDTO dto = new AdminTaskViewDTO();
        dto.setUserId(user.getId());
        dto.setName(user.getName());
        dto.setPhone(user.getPhone());
        dto.setEmail(user.getEmail());
        dto.setAge(user.getAge());
        dto.setGender(user.getGender());
        dto.setRole(user.getRelationship() != null ? user.getRelationship().getDisplayName().toLowerCase() : null);
        dto.setAccountStatus(user.isEnabled() ? "ACTIVE" : "INACTIVE");
        dto.setLastActive(user.getLastLoginDate());

        try {
            // Get user tasks using the correct repository method
            List<Task> userTasks = getTasksForUser(user);

            // Calculate task statistics
            Long totalCreated = (long) userTasks.size();
            Long totalCompleted = userTasks.stream()
                    .mapToLong(task -> task.getStatus() == Task.TaskStatus.COMPLETED ? 1L : 0L)
                    .sum();
            Long totalPending = userTasks.stream()
                    .mapToLong(task -> task.getStatus() != Task.TaskStatus.COMPLETED && !isTaskOverdue(task) ? 1L : 0L)
                    .sum();
            Long totalOverdue = userTasks.stream()
                    .mapToLong(task -> isTaskOverdue(task) ? 1L : 0L)
                    .sum();

            // Find first and last task dates using reflection for createdAt
            Long firstTaskDate = userTasks.stream()
                    .map(task -> {
                        try {
                            java.lang.reflect.Field createdAtField = task.getClass().getDeclaredField("createdAt");
                            createdAtField.setAccessible(true);
                            Object createdAtObj = createdAtField.get(task);
                            if (createdAtObj != null && createdAtObj instanceof java.time.LocalDateTime) {
                                java.time.LocalDateTime createdAt = (java.time.LocalDateTime) createdAtObj;
                                return createdAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                            }
                        } catch (Exception ignore) {
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .mapToLong(Long::longValue)
                    .min()
                    .orElse(0L);
            firstTaskDate = firstTaskDate == 0L ? null : firstTaskDate;

            Long lastTaskDate = userTasks.stream()
                    .map(task -> {
                        try {
                            java.lang.reflect.Field createdAtField = task.getClass().getDeclaredField("createdAt");
                            createdAtField.setAccessible(true);
                            Object createdAtObj = createdAtField.get(task);
                            if (createdAtObj != null && createdAtObj instanceof java.time.LocalDateTime) {
                                java.time.LocalDateTime createdAt = (java.time.LocalDateTime) createdAtObj;
                                return createdAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                            }
                        } catch (Exception ignore) {
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .mapToLong(Long::longValue)
                    .max()
                    .orElse(0L);
            lastTaskDate = lastTaskDate == 0L ? null : lastTaskDate;

            dto.setTotalTasksCreated(totalCreated);
            dto.setTotalTasksCompleted(totalCompleted);
            dto.setTotalTasksPending(totalPending);
            dto.setTotalTasksOverdue(totalOverdue);
            dto.setFirstTaskDate(firstTaskDate);
            dto.setLastTaskDate(lastTaskDate);

        } catch (Exception e) {
            System.err.println("Error calculating task stats for user " + user.getId() + ": " + e.getMessage());
            e.printStackTrace();
            dto.setTotalTasksCreated(0L);
            dto.setTotalTasksCompleted(0L);
            dto.setTotalTasksPending(0L);
            dto.setTotalTasksOverdue(0L);
            dto.setFirstTaskDate(null);
            dto.setLastTaskDate(null);
        }

        return dto;
    }

    /**
     * Gets user tasks with fallback options based on Task model relationships.
     * 
     * @param user The user to get tasks for
     * @return List of tasks for the user
     */
    private List<Task> getTasksForUser(User user) {
        try {
            // Try findByCreatedBy first (primary relationship in Task model)
            try {
                return taskRepository.findByCreatedBy(user);
            } catch (Exception ex) {
                // Fallback to findByAssignedTo (Task model has assignedTo field)
                try {
                    return taskRepository.findByAssignedTo(user);
                } catch (Exception exc) {
                    // Fallback to findByUserId if available
                    try {
                        return taskRepository.findByUserId(user.getId());
                    } catch (Exception excc) {
                        // Last resort: filter from all tasks
                        return taskRepository.findAll().stream()
                                .filter(task -> {
                                    try {
                                        User createdBy = task.getCreatedBy();
                                        if (createdBy != null && createdBy.getId().equals(user.getId())) {
                                            return true;
                                        }

                                        User assignedTo = task.getAssignedTo();
                                        if (assignedTo != null && assignedTo.getId().equals(user.getId())) {
                                            return true;
                                        }
                                    } catch (Exception ignore) {
                                    }
                                    return false;
                                })
                                .collect(Collectors.toList());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching tasks for user " + user.getId() + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Checks if task is overdue based on endDate from Task model.
     * 
     * @param task The task to check
     * @return true if task is overdue, false otherwise
     */
    private boolean isTaskOverdue(Task task) {
        try {
            if (task.getStatus() == Task.TaskStatus.COMPLETED) {
                return false; // Completed tasks are not overdue
            }

            java.time.LocalDate endDate = task.getEndDate();
            if (endDate == null) {
                return false; // No end date means not overdue
            }
            // Consider overdue if endDate is before today
            return endDate.isBefore(java.time.LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets top performing users by completion rate.
     * 
     * @param userViews List of user views to analyze
     * @param limit     Maximum number of users to return
     * @return List of top performers
     */
    private List<AdminTaskViewDTO> getTopPerformers(List<AdminTaskViewDTO> userViews, int limit) {
        return userViews.stream()
                .filter(AdminTaskViewDTO::hasTasks)
                .sorted((u1, u2) -> Double.compare(u2.getCompletionRate(), u1.getCompletionRate()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Gets task distribution by various categories.
     * 
     * @return Map containing task distribution data
     */
    private Map<String, Object> getTaskDistribution() {
        try {
            List<Task> allTasks = taskRepository.findAll();
            Map<String, Object> distribution = new HashMap<>();

            // Status distribution
            Map<String, Long> statusDist = allTasks.stream()
                    .collect(Collectors.groupingBy(
                            task -> {
                                if (task.getStatus() == Task.TaskStatus.COMPLETED) {
                                    return "Completed";
                                } else if (isTaskOverdue(task)) {
                                    return "Overdue";
                                } else {
                                    return "Pending";
                                }
                            },
                            Collectors.counting()));

            distribution.put("byStatus", statusDist);
            distribution.put("totalTasks", (long) allTasks.size());

            return distribution;
        } catch (Exception e) {
            System.err.println("Error calculating task distribution: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Gets priority distribution based on Task.Priority enum.
     * 
     * @return Map containing priority distribution data
     */
    private Map<String, Object> getPriorityDistribution() {
        try {
            List<Task> allTasks = taskRepository.findAll();
            Map<String, Object> distribution = new HashMap<>();

            Map<String, Long> priorityDist = allTasks.stream()
                    .collect(Collectors.groupingBy(
                            task -> {
                                try {
                                    Priority priority = task.getPriority();
                                    return priority != null ? priority.toString() : "Not Set";
                                } catch (Exception e) {
                                    return "Not Set";
                                }
                            },
                            Collectors.counting()));

            distribution.put("byPriority", priorityDist);
            distribution.put("highPriorityCount", priorityDist.getOrDefault("HIGH", 0L));
            distribution.put("mediumPriorityCount", priorityDist.getOrDefault("MEDIUM", 0L));
            distribution.put("lowPriorityCount", priorityDist.getOrDefault("LOW", 0L));

            return distribution;
        } catch (Exception e) {
            System.err.println("Error calculating priority distribution: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Gets routine distribution based on Task.Routine enum.
     * 
     * @return Map containing routine time distribution data
     */
    private Map<String, Object> getRoutineDistribution() {
        try {
            List<Task> allTasks = taskRepository.findAll();
            Map<String, Object> distribution = new HashMap<>();

            Map<String, Long> routineDist = allTasks.stream()
                    .collect(Collectors.groupingBy(
                            task -> {
                                try {
                                    String routine = task.getRoutine();
                                    return routine != null ? routine.toString() : "Not Set";
                                } catch (Exception e) {
                                    return "Not Set";
                                }
                            },
                            Collectors.counting()));

            distribution.put("byRoutine", routineDist);
            distribution.put("morningCount", routineDist.getOrDefault("MORNING", 0L));
            distribution.put("afternoonCount", routineDist.getOrDefault("AFTERNOON", 0L));
            distribution.put("nightCount", routineDist.getOrDefault("NIGHT", 0L));

            return distribution;
        } catch (Exception e) {
            System.err.println("Error calculating routine distribution: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Gets status distribution based on Task.TaskStatus enum.
     * 
     * @return Map containing status distribution data
     */
    private Map<String, Object> getStatusDistribution() {
        try {
            List<Task> allTasks = taskRepository.findAll();
            Map<String, Object> distribution = new HashMap<>();

            Map<String, Long> statusDist = allTasks.stream()
                    .collect(Collectors.groupingBy(
                            task -> {
                                try {
                                    TaskStatus status = task.getStatus();
                                    return status != null ? status.toString() : "Not Set";
                                } catch (Exception e) {
                                    return "Not Set";
                                }
                            },
                            Collectors.counting()));

            distribution.put("byTaskStatus", statusDist);
            distribution.put("inProgressCount", statusDist.getOrDefault("ONGOING", 0L));
            distribution.put("completedCount", statusDist.getOrDefault("COMPLETED", 0L));

            return distribution;
        } catch (Exception e) {
            System.err.println("Error calculating status distribution: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Gets completion trends over time.
     * 
     * @return Map containing completion trend data
     */
    private Map<String, Object> getCompletionTrends() {
        try {
            List<Task> allTasks = taskRepository.findAll();
            Map<String, Object> trends = new HashMap<>();

            // Calculate recent tasks (last 30 days)

            long thirtyDaysAgoMillis = System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L);
            java.time.LocalDateTime thirtyDaysAgo = java.time.Instant.ofEpochMilli(thirtyDaysAgoMillis)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
            long recentTasks = allTasks.stream()
                    .map(task -> {
                        try {
                            java.lang.reflect.Field createdAtField = task.getClass().getDeclaredField("createdAt");
                            createdAtField.setAccessible(true);
                            Object createdAtObj = createdAtField.get(task);
                            if (createdAtObj != null && createdAtObj instanceof java.time.LocalDateTime) {
                                java.time.LocalDateTime createdAt = (java.time.LocalDateTime) createdAtObj;
                                return createdAt;
                            }
                        } catch (Exception ignore) {
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .filter(createdAt -> ((java.time.LocalDateTime) createdAt).isAfter(thirtyDaysAgo)
                            || ((java.time.LocalDateTime) createdAt).isEqual(thirtyDaysAgo))
                    .count();
            trends.put("recentTasksCount", recentTasks);

            // Calculate completion metrics
            long completedTasks = allTasks.stream()
                    .mapToLong(task -> task.getStatus() == Task.TaskStatus.COMPLETED ? 1L : 0L)
                    .sum();

            long overdueTasks = allTasks.stream()
                    .mapToLong(task -> isTaskOverdue(task) ? 1L : 0L)
                    .sum();
            long pendingTasks = allTasks.size() - completedTasks - overdueTasks;

            trends.put("completedTasks", completedTasks);
            trends.put("overdueTasks", overdueTasks);
            trends.put("pendingTasks", pendingTasks);
            trends.put("completionRate", !allTasks.isEmpty() ? (double) completedTasks / allTasks.size() * 100 : 0.0);
            trends.put("overdueRate", !allTasks.isEmpty() ? (double) overdueTasks / allTasks.size() * 100 : 0.0);

            return trends;
        } catch (Exception e) {
            System.err.println("Error calculating completion trends: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Gets overdue task trends and analysis.
     * 
     * @return Map containing overdue trend data
     */
    private Map<String, Object> getOverdueTrends() {
        Map<String, Object> trends = new HashMap<>();
        try {
            List<Task> allTasks = taskRepository.findAll();

            // Calculate overdue tasks by priority
            Map<String, Long> overduePriorityDist = allTasks.stream()
                    .filter(this::isTaskOverdue)
                    .collect(Collectors.groupingBy(
                            task -> {
                                Priority priority = task.getPriority();
                                return priority != null ? priority.toString() : "Not Set";
                            },
                            Collectors.counting()
                    ));

            trends.put("overdueByPriority", overduePriorityDist);

            // Calculate average overdue days
            double avgOverdueDays = allTasks.stream()
                    .filter(this::isTaskOverdue)
                    .mapToDouble(task -> {
                        LocalDate endDate = task.getEndDate();
                        if (endDate != null) {
                            // Using Period to calculate days difference
                            return Period.between(endDate, LocalDate.now()).getDays();
                        }
                        return 0.0;
                    })
                    .average()
                    .orElse(0.0);

            trends.put("averageOverdueDays", avgOverdueDays);

        } catch (Exception e) {
            System.err.println("Error calculating overdue trends: " + e.getMessage());
            return new HashMap<>();
        }

        return trends;
    }

    /**
     * Gets user task count analysis.
     * quickStats.put("inProgressTasks",
     * getTasksByStatus(TaskStatus.ONGOING).size());
     * 
     * @return Map containing user task count data
     */
    private Map<String, Object> getUserTaskCounts() {
        try {
            List<AdminTaskViewDTO> userViews = getAllUserTaskViews();
            Map<String, Object> result = new HashMap<>();

            Map<Long, Long> userTaskMap = userViews.stream()
                    .filter(AdminTaskViewDTO::hasTasks)
                    .collect(Collectors.toMap(
                            AdminTaskViewDTO::getUserId,
                            AdminTaskViewDTO::getTotalTasksCreated));

            result.put("userTaskCounts", userTaskMap);
            result.put("usersWithTasks", userTaskMap.size());

            // Calculate statistics
            if (!userTaskMap.isEmpty()) {
                double avgTasks = userTaskMap.values().stream()
                        .mapToLong(Long::longValue)
                        .average()
                        .orElse(0.0);
                result.put("averageTasksPerActiveUser", avgTasks);

                long maxTasks = userTaskMap.values().stream()
                        .mapToLong(Long::longValue)
                        .max()
                        .orElse(0L);
                result.put("maxTasksPerUser", maxTasks);
            }

            return result;
        } catch (Exception e) {
            System.err.println("Error calculating user task counts: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Analyzes reward distribution and effectiveness based on Task.rewardCoins.
     * 
     * @return Map containing reward analysis data
     */
    private Map<String, Object> getRewardAnalysis() {
        try {
            List<Task> allTasks = taskRepository.findAll();
            Map<String, Object> analysis = new HashMap<>();

            // Calculate total rewards
            int totalRewards = allTasks.stream()
                    .filter(task -> task.getRewardCoins() != null)
                    .mapToInt(Task::getRewardCoins)
                    .sum();

            // Calculate average reward per task
            double avgReward = allTasks.stream()
                    .filter(task -> task.getRewardCoins() != null)
                    .mapToInt(Task::getRewardCoins)
                    .average()
                    .orElse(0.0);

            // Tasks with rewards vs without
            long tasksWithRewards = allTasks.stream()
                    .filter(task -> task.getRewardCoins() != null && task.getRewardCoins() > 0)
                    .count();

            analysis.put("totalRewards", totalRewards);
            analysis.put("averageRewardPerTask", avgReward);
            analysis.put("tasksWithRewards", tasksWithRewards);
            analysis.put("tasksWithoutRewards", allTasks.size() - tasksWithRewards);
            analysis.put("rewardAdoptionRate",
                    allTasks.size() > 0 ? (double) tasksWithRewards / allTasks.size() * 100 : 0.0);

            return analysis;
        } catch (Exception e) {
            System.err.println("Error calculating reward analysis: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Analyzes family-related tasks based on Task.family and
     * Task.sharedWithParents.
     * 
     * @return Map containing family task analysis data
     */
    private Map<String, Object> getFamilyTaskAnalysis() {
        try {
            List<Task> allTasks = taskRepository.findAll();
            Map<String, Object> analysis = new HashMap<>();

            // Count family tasks
            long familyTasks = allTasks.stream()
                    .filter(task -> {
                        try {
                            return task.getFamily() != null;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .count();

            // Count tasks shared with parents
            long sharedTasks = allTasks.stream()
                    .filter(task -> {
                        try {
                            return task.getSharedWithParent();
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .count();

            analysis.put("familyTasks", familyTasks);
            analysis.put("sharedWithParentsTasks", sharedTasks);
            analysis.put("individualTasks", allTasks.size() - familyTasks);
            analysis.put("familyTasksPercentage",
                    allTasks.size() > 0 ? (double) familyTasks / allTasks.size() * 100 : 0.0);
            analysis.put("parentSharingRate", allTasks.size() > 0 ? (double) sharedTasks / allTasks.size() * 100 : 0.0);

            return analysis;
        } catch (Exception e) {
            System.err.println("Error calculating family task analysis: " + e.getMessage());
            return new HashMap<>();
        }
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    /**
     * Gets tasks for a specific user by userId.
     * 
     * @param userId The user ID
     * @return List of tasks for the user
     */
    public List<Task> getTasksForUserId(Long userId) {
        try {
            return taskRepository.findByUserId(userId);
        } catch (Exception e) {
            System.err.println("Error fetching tasks for user ID " + userId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Gets tasks scheduled for specific days of the week.
     * 
     * @param dayOfWeek The day to filter by
     * @return List of tasks scheduled for the specified day
     */
    public List<Task> getTasksByDayOfWeek(DayOfWeek dayOfWeek) {
        try {
            return taskRepository.findAll().stream()
                    .filter(task -> {
                        try {
                            Set<DayOfWeek> daysOfWeek = task.getDaysOfWeek();
                            return daysOfWeek != null && daysOfWeek.contains(dayOfWeek);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error fetching tasks by day of week: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Gets tasks within a specific date range.
     * 
     * @param startDate Start date (epoch millis)
     * @param endDate   End date (epoch millis)
     * @return List of tasks within the date range
     */
    public List<Task> getTasksInDateRange(Long startDate, Long endDate) {
        try {
            return taskRepository.findAll().stream()
                    .filter(task -> {
                        try {
                            java.time.LocalDate taskStartDate = task.getStartDate();
                            if (taskStartDate == null)
                                return false;
                            java.time.LocalDate start = java.time.Instant.ofEpochMilli(startDate)
                                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                            java.time.LocalDate end = java.time.Instant.ofEpochMilli(endDate)
                                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                            return (taskStartDate.isEqual(start) || taskStartDate.isAfter(start)) &&
                                    (taskStartDate.isEqual(end) || taskStartDate.isBefore(end));
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error fetching tasks in date range: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
