package com.superme.admin.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.superme.dto.AdminActivityOverviewDTO;
import com.superme.service.AdminActivityOverviewService;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/**
 * AdminActivityController handles all activity-related administrative
 * operations.
 * 
 * This controller provides comprehensive activity analytics functionality
 * including:
 * - Unified activity overview (habits, tasks, and calendar events)
 * - Advanced search and filtering capabilities
 * - Activity engagement metrics and analytics
 * - Export functionality for activity data
 * - User coordination and completion trend analysis
 * 
 * All endpoints require admin authentication and return JSON responses
 * suitable for admin dashboard consumption.
 * 
 * Updated with improved error handling and null safety for missing DTOs.
 * 
 * @author MindfullB Admin Team
 * @version 2.2
 */
@RestController
@RequestMapping("/admin/activity-overview")
public class AdminActivityController {

    // ============================================================================
    // DEPENDENCY INJECTION
    // ============================================================================

    @Autowired(required = false)
    private AdminActivityOverviewService adminActivityOverviewService;

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    /**
     * Check if service is available and return appropriate response
     */
    private boolean isServiceAvailable() {
        return adminActivityOverviewService != null;
    }

    /**
     * Get default empty response when service is not available or DTOs are missing
     */
    private Map<String, Object> getEmptyResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("users", new ArrayList<>());
        response.put("statistics", createEmptyStatistics());
        response.put("filterCriteria", new HashMap<>());
        response.put("totalCount", 0);
        response.put("serviceStatus", "unavailable");
        return response;
    }

    /**
     * Create empty statistics object as Map to avoid DTO dependency issues
     */
    private Map<String, Object> createEmptyStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEvents", 0L);
        stats.put("totalHabits", 0L);
        stats.put("totalTasks", 0L);
        stats.put("averageEventsPerUser", 0.0);
        stats.put("activeUsers", 0L);
        stats.put("usersWithActivities", 0L);
        return stats;
    }

    /**
     * Safely convert DTO to Map to avoid class dependency issues
     */
    private Map<String, Object> dtoToMap(AdminActivityOverviewDTO dto) {
        if (dto == null) {
            return new HashMap<>();
        }

        Map<String, Object> map = new HashMap<>();
        try {
            map.put("userId", safeGetValue(() -> dto.getUserId()));
            // Removed username, use name instead
            map.put("name", safeGetValue(() -> dto.getName()));
            map.put("totalEvents", safeGetValue(() -> dto.getTotalEvents()));
            map.put("totalHabitsCompleted", safeGetValue(() -> dto.getTotalHabitsCompleted()));
            map.put("totalTasksCompleted", safeGetValue(() -> dto.getTotalTasksCompleted()));
            map.put("habitsAndTasksSameDay", safeGetValue(() -> dto.getHabitsAndTasksSameDay()));

            // Try to get engagement level and other computed properties safely
            try {
                map.put("engagementLevel", dto.getEngagementLevel());
            } catch (Exception e) {
                map.put("engagementLevel", "unknown");
            }

            try {
                map.put("isActiveUser", dto.isActiveUser());
            } catch (Exception e) {
                map.put("isActiveUser", false);
            }

            try {
                map.put("hasActivity", dto.hasActivity());
            } catch (Exception e) {
                map.put("hasActivity", false);
            }

        } catch (Exception e) {
            System.err.println("Error converting DTO to Map: " + e.getMessage());
        }

        return map;
    }

    /**
     * Safely execute a supplier and return value or null on error
     */
    private <T> T safeGetValue(java.util.function.Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Filter users based on search term with null-safe implementation
     */
    private List<Map<String, Object>> filterUsers(List<Map<String, Object>> users, String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return users;
        }

        String lowerSearch = searchTerm.toLowerCase();
        return users.stream()
                .filter(user -> {
                    try {
                        Object userId = user.get("userId");
                        Object name = user.get("name");

                        return (userId != null && userId.toString().toLowerCase().contains(lowerSearch)) ||
                                (name != null && name.toString().toLowerCase().contains(lowerSearch));
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Apply numeric filters to users list
     */
    private List<Map<String, Object>> applyNumericFilters(List<Map<String, Object>> users,
            Long minTotalEvents, Long maxTotalEvents,
            Long minHabitsCompleted, Long maxHabitsCompleted,
            Long minTasksCompleted, Long maxTasksCompleted,
            Long minSameDayActivities, Long maxSameDayActivities) {

        return users.stream()
                .filter(user -> {
                    try {
                        // Filter by total events
                        if (minTotalEvents != null) {
                            Object totalEvents = user.get("totalEvents");
                            if (totalEvents == null || ((Number) totalEvents).longValue() < minTotalEvents) {
                                return false;
                            }
                        }

                        if (maxTotalEvents != null) {
                            Object totalEvents = user.get("totalEvents");
                            if (totalEvents == null || ((Number) totalEvents).longValue() > maxTotalEvents) {
                                return false;
                            }
                        }

                        // Filter by habits completed
                        if (minHabitsCompleted != null) {
                            Object habitsCompleted = user.get("totalHabitsCompleted");
                            if (habitsCompleted == null
                                    || ((Number) habitsCompleted).longValue() < minHabitsCompleted) {
                                return false;
                            }
                        }

                        if (maxHabitsCompleted != null) {
                            Object habitsCompleted = user.get("totalHabitsCompleted");
                            if (habitsCompleted == null
                                    || ((Number) habitsCompleted).longValue() > maxHabitsCompleted) {
                                return false;
                            }
                        }

                        // Filter by tasks completed
                        if (minTasksCompleted != null) {
                            Object tasksCompleted = user.get("totalTasksCompleted");
                            if (tasksCompleted == null || ((Number) tasksCompleted).longValue() < minTasksCompleted) {
                                return false;
                            }
                        }

                        if (maxTasksCompleted != null) {
                            Object tasksCompleted = user.get("totalTasksCompleted");
                            if (tasksCompleted == null || ((Number) tasksCompleted).longValue() > maxTasksCompleted) {
                                return false;
                            }
                        }

                        // Filter by same day activities
                        if (minSameDayActivities != null) {
                            Object sameDayActivities = user.get("habitsAndTasksSameDay");
                            if (sameDayActivities == null
                                    || ((Number) sameDayActivities).longValue() < minSameDayActivities) {
                                return false;
                            }
                        }

                        if (maxSameDayActivities != null) {
                            Object sameDayActivities = user.get("habitsAndTasksSameDay");
                            if (sameDayActivities == null
                                    || ((Number) sameDayActivities).longValue() > maxSameDayActivities) {
                                return false;
                            }
                        }

                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    // ============================================================================
    // ENHANCED ACTIVITY OVERVIEW ENDPOINTS WITH SEARCH AND FILTERING
    // ============================================================================

    /**
     * GET /admin/activity-overview
     * 
     * Enhanced activity overview with comprehensive filtering support.
     * Returns Map-based response to avoid DTO dependency issues.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUserActivityOverviews(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long minTotalEvents,
            @RequestParam(required = false) Long maxTotalEvents,
            @RequestParam(required = false) Long minHabitsCompleted,
            @RequestParam(required = false) Long maxHabitsCompleted,
            @RequestParam(required = false) Long minTasksCompleted,
            @RequestParam(required = false) Long maxTasksCompleted,
            @RequestParam(required = false) Long minSameDayActivities,
            @RequestParam(required = false) Long maxSameDayActivities,
            @RequestParam(required = false) String engagementLevel,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) Boolean isActive) {

        if (!isServiceAvailable()) {
            return ResponseEntity.ok(getEmptyResponse());
        }

        try {
            List<AdminActivityOverviewDTO> userDTOs = adminActivityOverviewService.getAllUserActivityOverviews();

            // Convert DTOs to Maps to avoid class dependency issues
            List<Map<String, Object>> users = userDTOs.stream()
                    .map(this::dtoToMap)
                    .collect(Collectors.toList());

            // Apply search filter
            if (search != null && !search.trim().isEmpty()) {
                users = filterUsers(users, search);
            }

            // Apply numeric filters
            users = applyNumericFilters(users, minTotalEvents, maxTotalEvents,
                    minHabitsCompleted, maxHabitsCompleted, minTasksCompleted, maxTasksCompleted,
                    minSameDayActivities, maxSameDayActivities);

            // Apply engagement level filter
            if (engagementLevel != null && !engagementLevel.trim().isEmpty()) {
                users = users.stream()
                        .filter(user -> {
                            Object userEngagement = user.get("engagementLevel");
                            return userEngagement != null &&
                                    userEngagement.toString().equalsIgnoreCase(engagementLevel);
                        })
                        .collect(Collectors.toList());
            }

            // Apply active status filter
            if (isActive != null) {
                users = users.stream()
                        .filter(user -> {
                            Object userActive = user.get("isActiveUser");
                            return userActive != null && userActive.equals(isActive);
                        })
                        .collect(Collectors.toList());
            }

            // Get statistics
            Map<String, Object> stats;
            try {
                Object statsObject = adminActivityOverviewService.getActivityOverviewStatistics();
                stats = createStatsMap(statsObject);
            } catch (Exception e) {
                stats = createEmptyStatistics();
            }

            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("users", users);
            response.put("statistics", stats);
            response.put("filterCriteria", createFilterCriteriaMap(search, minTotalEvents, maxTotalEvents,
                    minHabitsCompleted, maxHabitsCompleted, minTasksCompleted, maxTasksCompleted,
                    minSameDayActivities, maxSameDayActivities, engagementLevel, userType, isActive));
            response.put("totalCount", userDTOs.size());
            response.put("filteredCount", users.size());
            response.put("serviceStatus", "healthy");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error in getAllUserActivityOverviews: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = getEmptyResponse();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("serviceStatus", "error");
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Create statistics map from various possible statistics objects
     */
    private Map<String, Object> createStatsMap(Object statsObject) {
        Map<String, Object> stats = new HashMap<>();

        if (statsObject == null) {
            return createEmptyStatistics();
        }

        try {
            // Try to extract statistics using reflection or method calls
            if (statsObject instanceof Map) {
                return (Map<String, Object>) statsObject;
            }

            // Try common getter methods, catch all reflection exceptions
            try {
                stats.put("totalEvents", safeGetValue(() -> {
                    try {
                        return (Long) statsObject.getClass().getMethod("getTotalEvents").invoke(statsObject);
                    } catch (NoSuchMethodException | IllegalAccessException
                            | java.lang.reflect.InvocationTargetException e) {
                        return 0L;
                    }
                }));
            } catch (Exception e) {
                stats.put("totalEvents", 0L);
            }

            try {
                stats.put("totalHabits", safeGetValue(() -> {
                    try {
                        return (Long) statsObject.getClass().getMethod("getTotalHabits").invoke(statsObject);
                    } catch (NoSuchMethodException | IllegalAccessException
                            | java.lang.reflect.InvocationTargetException e) {
                        return 0L;
                    }
                }));
            } catch (Exception e) {
                stats.put("totalHabits", 0L);
            }

            try {
                stats.put("totalTasks", safeGetValue(() -> {
                    try {
                        return (Long) statsObject.getClass().getMethod("getTotalTasks").invoke(statsObject);
                    } catch (NoSuchMethodException | IllegalAccessException
                            | java.lang.reflect.InvocationTargetException e) {
                        return 0L;
                    }
                }));
            } catch (Exception e) {
                stats.put("totalTasks", 0L);
            }

            try {
                stats.put("averageEventsPerUser", safeGetValue(() -> {
                    try {
                        return (Double) statsObject.getClass().getMethod("getAverageEventsPerUser").invoke(statsObject);
                    } catch (NoSuchMethodException | IllegalAccessException
                            | java.lang.reflect.InvocationTargetException e) {
                        return 0.0;
                    }
                }));
            } catch (Exception e) {
                stats.put("averageEventsPerUser", 0.0);
            }

            try {
                stats.put("activeUsers", safeGetValue(() -> {
                    try {
                        return (Long) statsObject.getClass().getMethod("getActiveUsers").invoke(statsObject);
                    } catch (NoSuchMethodException | IllegalAccessException
                            | java.lang.reflect.InvocationTargetException e) {
                        return 0L;
                    }
                }));
            } catch (Exception e) {
                stats.put("activeUsers", 0L);
            }

            try {
                stats.put("usersWithActivities", safeGetValue(() -> {
                    try {
                        return (Long) statsObject.getClass().getMethod("getUsersWithActivities").invoke(statsObject);
                    } catch (NoSuchMethodException | IllegalAccessException
                            | java.lang.reflect.InvocationTargetException e) {
                        return 0L;
                    }
                }));
            } catch (Exception e) {
                stats.put("usersWithActivities", 0L);
            }

        } catch (Exception e) {
            System.err.println("Error creating stats map: " + e.getMessage());
            return createEmptyStatistics();
        }

        // Fill in any missing values with defaults
        stats.putIfAbsent("totalEvents", 0L);
        stats.putIfAbsent("totalHabits", 0L);
        stats.putIfAbsent("totalTasks", 0L);
        stats.putIfAbsent("averageEventsPerUser", 0.0);
        stats.putIfAbsent("activeUsers", 0L);
        stats.putIfAbsent("usersWithActivities", 0L);

        return stats;
    }

    /**
     * Create filter criteria map
     */
    private Map<String, Object> createFilterCriteriaMap(String search, Long minTotalEvents, Long maxTotalEvents,
            Long minHabitsCompleted, Long maxHabitsCompleted, Long minTasksCompleted, Long maxTasksCompleted,
            Long minSameDayActivities, Long maxSameDayActivities, String engagementLevel, String userType,
            Boolean isActive) {

        Map<String, Object> criteria = new HashMap<>();
        criteria.put("searchTerm", search);
        criteria.put("minTotalEvents", minTotalEvents);
        criteria.put("maxTotalEvents", maxTotalEvents);
        criteria.put("minHabitsCompleted", minHabitsCompleted);
        criteria.put("maxHabitsCompleted", maxHabitsCompleted);
        criteria.put("minTasksCompleted", minTasksCompleted);
        criteria.put("maxTasksCompleted", maxTasksCompleted);
        criteria.put("minSameDayActivities", minSameDayActivities);
        criteria.put("maxSameDayActivities", maxSameDayActivities);
        criteria.put("engagementLevel", engagementLevel);
        criteria.put("userType", userType);
        criteria.put("isActive", isActive);
        return criteria;
    }

    /**
     * GET /admin/activity-overview/search
     * 
     * Enhanced activity overview search with pagination support.
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchActivityUsers(
            @RequestParam("q") String q,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        Map<String, Object> response = new HashMap<>();

        if (!isServiceAvailable()) {
            response.put("users", new ArrayList<>());
            response.put("totalCount", 0);
            response.put("displayedCount", 0);
            response.put("limit", limit);
            response.put("offset", offset);
            response.put("hasMore", false);
            response.put("searchTerm", q);
            response.put("error", "Service not available");
            return ResponseEntity.ok(response);
        }

        try {
            List<AdminActivityOverviewDTO> userDTOs = adminActivityOverviewService.getAllUserActivityOverviews();

            // Convert to Maps
            List<Map<String, Object>> allUsers = userDTOs.stream()
                    .map(this::dtoToMap)
                    .collect(Collectors.toList());

            List<Map<String, Object>> searchResults = filterUsers(allUsers, q);

            // Apply pagination manually
            int start = Math.max(0, offset);
            int end = Math.min(searchResults.size(), start + limit);
            List<Map<String, Object>> paginatedResults = start < searchResults.size()
                    ? searchResults.subList(start, end)
                    : new ArrayList<>();

            response.put("users", paginatedResults);
            response.put("totalCount", searchResults.size());
            response.put("displayedCount", paginatedResults.size());
            response.put("limit", limit);
            response.put("offset", offset);
            response.put("hasMore", (offset + limit) < searchResults.size());
            response.put("searchTerm", q);
            response.put("searchFields", "userId, name");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Search failed: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * GET /admin/activity-overview/filter-options
     * 
     * Get available filter options for activity overview filtering UI
     */
    @GetMapping("/filter-options")
    public ResponseEntity<Map<String, Object>> getActivityFilterOptions() {
        Map<String, Object> options = new HashMap<>();

        // User type options
        options.put("userTypes", List.of(
                "active_users", "highly_engaged", "needs_attention",
                "with_activities", "coordinated_users", "inactive_users"));

        // Engagement level options
        options.put("engagementLevels", List.of("high", "medium", "low", "none"));

        if (isServiceAvailable()) {
            try {
                Object statsObject = adminActivityOverviewService.getActivityOverviewStatistics();
                Map<String, Object> stats = createStatsMap(statsObject);
                options.putAll(stats);
            } catch (Exception e) {
                options.put("error", "Failed to load statistics: " + e.getMessage());
                options.putAll(createEmptyStatistics());
            }
        } else {
            options.putAll(createEmptyStatistics());
            options.put("serviceStatus", "unavailable");
        }

        return ResponseEntity.ok(options);
    }

    /**
     * POST /admin/activity-overview/advanced-filter
     * 
     * Advanced activity overview filtering with request body for complex criteria
     */
    @PostMapping("/advanced-filter")
    public ResponseEntity<Map<String, Object>> advancedActivityFilter(
            @RequestBody Map<String, Object> criteria) {

        if (!isServiceAvailable()) {
            return ResponseEntity.ok(getEmptyResponse());
        }

        try {
            List<AdminActivityOverviewDTO> userDTOs = adminActivityOverviewService.getAllUserActivityOverviews();

            // Convert to Maps
            List<Map<String, Object>> users = userDTOs.stream()
                    .map(this::dtoToMap)
                    .collect(Collectors.toList());

            // Apply filtering based on criteria map
            String searchTerm = (String) criteria.get("searchTerm");
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                users = filterUsers(users, searchTerm);
            }

            // Get statistics
            Map<String, Object> stats;
            try {
                Object statsObject = adminActivityOverviewService.getActivityOverviewStatistics();
                stats = createStatsMap(statsObject);
            } catch (Exception e) {
                stats = createEmptyStatistics();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("users", users);
            response.put("statistics", stats);
            response.put("filterCriteria", criteria);
            response.put("totalCount", userDTOs.size());
            response.put("filteredCount", users.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = getEmptyResponse();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.ok(errorResponse);
        }
    }

    // ============================================================================
    // QUICK STATS AND ANALYTICS ENDPOINTS
    // ============================================================================

    /**
     * GET /admin/activity-overview/quick-stats
     * 
     * Provides quick statistical overview for dashboard cards.
     */
    @GetMapping("/quick-stats")
    public ResponseEntity<Map<String, Object>> getActivityOverviewQuickStats() {
        Map<String, Object> quickStats = new HashMap<>();

        if (!isServiceAvailable()) {
            quickStats.putAll(createEmptyStatistics());
            quickStats.put("status", "service_unavailable");
            return ResponseEntity.ok(quickStats);
        }

        try {
            Object statsObject = adminActivityOverviewService.getActivityOverviewStatistics();
            Map<String, Object> stats = createStatsMap(statsObject);
            quickStats.putAll(stats);
            quickStats.put("status", "active");
        } catch (Exception e) {
            quickStats.putAll(createEmptyStatistics());
            quickStats.put("error", "Failed to load stats: " + e.getMessage());
            quickStats.put("status", "error");
        }

        return ResponseEntity.ok(quickStats);
    }

    /**
     * GET /admin/activity-overview/analytics
     * 
     * Enhanced activity overview analytics with optional filtering support.
     */
    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getActivityOverviewAnalytics(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long minTotalEvents,
            @RequestParam(required = false) Long maxTotalEvents,
            @RequestParam(required = false) String engagementLevel,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) Boolean isActive) {

        Map<String, Object> analytics = new HashMap<>();

        if (!isServiceAvailable()) {
            analytics.put("error", "Service not available");
            analytics.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(analytics);
        }

        try {
            Object statsObject = adminActivityOverviewService.getActivityOverviewStatistics();
            Map<String, Object> stats = createStatsMap(statsObject);

            analytics.put("globalStatistics", stats);
            analytics.put("timestamp", System.currentTimeMillis());
            analytics.put("filters", Map.of(
                    "search", search != null ? search : "",
                    "minTotalEvents", minTotalEvents != null ? minTotalEvents : 0,
                    "maxTotalEvents", maxTotalEvents != null ? maxTotalEvents : 0,
                    "engagementLevel", engagementLevel != null ? engagementLevel : "",
                    "userType", userType != null ? userType : "",
                    "isActive", isActive != null ? isActive : false));
        } catch (Exception e) {
            analytics.put("error", "Analytics failed: " + e.getMessage());
            analytics.put("timestamp", System.currentTimeMillis());
        }

        return ResponseEntity.ok(analytics);
    }

    /**
     * GET /admin/activity-overview/engagement-metrics
     * 
     * Get user engagement metrics across all activity types
     */
    @GetMapping("/engagement-metrics")
    public ResponseEntity<Map<String, Object>> getActivityEngagementMetrics() {
        Map<String, Object> analytics = new HashMap<>();

        if (!isServiceAvailable()) {
            analytics.put("error", "Service not available");
            return ResponseEntity.ok(analytics);
        }

        try {
            List<AdminActivityOverviewDTO> userDTOs = adminActivityOverviewService.getAllUserActivityOverviews();

            // Convert to Maps for safer processing
            List<Map<String, Object>> users = userDTOs.stream()
                    .map(this::dtoToMap)
                    .collect(Collectors.toList());

            long highlyEngaged = users.stream()
                    .filter(user -> {
                        Object engagement = user.get("engagementLevel");
                        return engagement != null && "high".equalsIgnoreCase(engagement.toString());
                    })
                    .count();

            long activeUsers = users.stream()
                    .filter(user -> {
                        Object isActive = user.get("isActiveUser");
                        return isActive != null && (Boolean) isActive;
                    })
                    .count();

            long needingAttention = users.stream()
                    .filter(user -> {
                        Object totalEvents = user.get("totalEvents");
                        return totalEvents == null || ((Number) totalEvents).longValue() <= 2;
                    })
                    .count();

            Map<String, Object> engagementAnalysis = new HashMap<>();
            engagementAnalysis.put("highlyEngaged", highlyEngaged);
            engagementAnalysis.put("activeUsers", activeUsers);
            engagementAnalysis.put("needingAttention", needingAttention);
            engagementAnalysis.put("totalUsers", users.size());

            analytics.put("engagementAnalysis", engagementAnalysis);
        } catch (Exception e) {
            analytics.put("error", "Engagement metrics failed: " + e.getMessage());
        }

        return ResponseEntity.ok(analytics);
    }

    // ============================================================================
    // SIMPLE ENDPOINTS FOR BASIC FUNCTIONALITY
    // ============================================================================

    /**
     * GET /admin/activity-overview/status
     * 
     * Check service status and basic health
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("controller", "AdminActivityController");
        status.put("serviceAvailable", isServiceAvailable());
        status.put("timestamp", System.currentTimeMillis());

        if (isServiceAvailable()) {
            try {
                Object statsObject = adminActivityOverviewService.getActivityOverviewStatistics();
                Map<String, Object> stats = createStatsMap(statsObject);
                status.put("status", "healthy");
                status.put("userCount", stats.get("activeUsers"));
                status.put("statistics", stats);
            } catch (Exception e) {
                status.put("status", "service_error");
                status.put("error", e.getMessage());
            }
        } else {
            status.put("status", "service_unavailable");
            status.put("message", "AdminActivityOverviewService not available");
        }

        return ResponseEntity.ok(status);
    }

    /**
     * GET /admin/activity-overview/users-with-activity
     * 
     * Retrieves list of users who have at least one activity
     */
    @GetMapping("/users-with-activity")
    public ResponseEntity<List<Map<String, Object>>> getUsersWithActivity() {
        if (!isServiceAvailable()) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        try {
            List<AdminActivityOverviewDTO> userDTOs = adminActivityOverviewService.getAllUserActivityOverviews();

            List<Map<String, Object>> activeUsers = userDTOs.stream()
                    .map(this::dtoToMap)
                    .filter(user -> {
                        Object hasActivity = user.get("hasActivity");
                        Object totalEvents = user.get("totalEvents");
                        return (hasActivity != null && (Boolean) hasActivity) ||
                                (totalEvents != null && ((Number) totalEvents).longValue() > 0);
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(activeUsers);
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * GET /admin/activity-overview/top-by-events
     * 
     * Retrieves top users ranked by total number of events
     */
    @GetMapping("/top-by-events")
    public ResponseEntity<List<Map<String, Object>>> getTopUsersByEvents(
            @RequestParam(defaultValue = "10") int limit) {

        if (!isServiceAvailable()) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        try {
            List<AdminActivityOverviewDTO> userDTOs = adminActivityOverviewService.getAllUserActivityOverviews();

            List<Map<String, Object>> topUsers = userDTOs.stream()
                    .map(this::dtoToMap)
                    .filter(user -> {
                        Object totalEvents = user.get("totalEvents");
                        return totalEvents != null && ((Number) totalEvents).longValue() > 0;
                    })
                    .sorted((a, b) -> {
                        Long eventsA = ((Number) a.get("totalEvents")).longValue();
                        Long eventsB = ((Number) b.get("totalEvents")).longValue();
                        return Long.compare(eventsB, eventsA);
                    })
                    .limit(Math.max(0, limit))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(topUsers);
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * GET /admin/activity-overview/user/{id}
     * 
     * Get detailed activity view for a specific user
     */
    @GetMapping("/user/{id}")
    public ResponseEntity<Map<String, Object>> getUserActivityView(@PathVariable Long id) {
        if (!isServiceAvailable()) {
            return ResponseEntity.notFound().build();
        }

        try {
            List<AdminActivityOverviewDTO> userDTOs = adminActivityOverviewService.getAllUserActivityOverviews();

            Map<String, Object> userView = userDTOs.stream()
                    .filter(dto -> {
                        try {
                            return dto.getUserId() != null && dto.getUserId().equals(id);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .map(this::dtoToMap)
                    .findFirst()
                    .orElse(null);

            if (userView == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(userView);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /admin/activity-overview/summary
     * 
     * Get overall summary statistics
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getActivitySummary() {
        Map<String, Object> summary = new HashMap<>();

        if (!isServiceAvailable()) {
            summary.putAll(createEmptyStatistics());
            summary.put("serviceStatus", "unavailable");
            return ResponseEntity.ok(summary);
        }

        try {
            Object statsObject = adminActivityOverviewService.getActivityOverviewStatistics();
            Map<String, Object> stats = createStatsMap(statsObject);

            List<AdminActivityOverviewDTO> userDTOs = adminActivityOverviewService.getAllUserActivityOverviews();

            summary.putAll(stats);
            summary.put("totalUsers", userDTOs.size());
            summary.put("usersWithActivity", userDTOs.stream()
                    .map(this::dtoToMap)
                    .filter(user -> {
                        Object hasActivity = user.get("hasActivity");
                        Object totalEvents = user.get("totalEvents");
                        return (hasActivity != null && (Boolean) hasActivity) ||
                                (totalEvents != null && ((Number) totalEvents).longValue() > 0);
                    })
                    .count());
            summary.put("serviceStatus", "healthy");
            summary.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            summary.putAll(createEmptyStatistics());
            summary.put("error", e.getMessage());
            summary.put("serviceStatus", "error");
        }

        return ResponseEntity.ok(summary);
    }
}
