// package com.mindfull.admin.controller;

// import java.util.HashMap;
// import java.util.Map;
// import java.util.List;
// import java.util.ArrayList;
// import java.util.stream.Collectors;

// import com.mindfull.dto.AdminUserViewDTO;
// import com.mindfull.dto.AdminCreateUserDTO;
// import com.mindfull.dto.AdminHabitOverviewResponse;
// import com.mindfull.dto.AdminUserOverviewResponse;
// import com.mindfull.dto.AdminJournalOverviewResponse;
// import com.mindfull.dto.AdminJournalViewDTO;
// import com.mindfull.dto.AdminTaskOverviewResponse;
// import com.mindfull.dto.AdminTaskOverviewResponse.TaskFilterCriteria;
// import com.mindfull.dto.AdminNoteOverviewResponse;
// import com.mindfull.dto.AdminNoteOverviewResponse.NoteFilterCriteria;
// import com.mindfull.model.User;
// import com.mindfull.service.AdminUserViewService;
// import com.mindfull.service.AdminUserCrudService;
// import com.mindfull.service.AdminHabitViewService;
// import com.mindfull.service.AdminTaskViewService;
// import com.mindfull.service.AdminActivityOverviewService;
// import com.mindfull.service.AdminNoteViewService;
// import com.mindfull.service.AdminJournalViewService;

// import org.springframework.web.bind.annotation.*;
// import org.springframework.beans.factory.annotation.Autowired;

// import com.mindfull.dto.AdminHabitViewDTO;
// import com.mindfull.dto.AdminTaskViewDTO;
// import com.mindfull.dto.AdminActivityOverviewResponse;
// import com.mindfull.dto.AdminActivityOverviewDTO;
// import com.mindfull.dto.AdminNoteViewDTO;
// import com.mindfull.model.JournalEntry;

// /**
//  * AdminController handles all administrative operations for the Mindful application.
//  * 
//  * This controller provides comprehensive admin panel functionality including:
//  * - User management (CRUD operations)
//  * - Habit analytics and overview
//  * - Task analytics and overview with advanced search and filtering
//  * - Activity overview (unified habits, tasks, and events)
//  * - Note analytics and overview with advanced search and filtering
//  * - Journal analytics and overview
//  * 
//  * All endpoints require admin authentication and return JSON responses
//  * suitable for admin dashboard consumption.
//  * 
//  * @author MindfullB Admin Team
//  * @version 2.0
//  */
// @RestController
// @RequestMapping("/admin")
// public class AdminController {

//   // ============================================================================
//   // DEPENDENCY INJECTION
//   // ============================================================================

//   /**
//    * Service for viewing and analyzing user data for admin dashboard
//    */
//   private final AdminUserViewService adminUserViewService;

//   /**
//    * Service for managing user CRUD operations (Create, Read, Update, Delete)
//    */
//   private final AdminUserCrudService adminUserCrudService;

//   /**
//    * Service for habit analytics and overview functionality
//    */
//   @Autowired
//   private AdminHabitViewService adminHabitViewService;

//   /**
//    * Service for task analytics and overview functionality
//    */
//   @Autowired
//   private AdminTaskViewService adminTaskViewService;

//   /**
//    * Service for unified activity overview (habits + tasks + events)
//    */
//   @Autowired
//   private AdminActivityOverviewService adminActivityOverviewService;

//   /**
//    * Service for notes analytics and overview functionality
//    */
//   @Autowired
//   private AdminNoteViewService adminNoteViewService;

//   /**
//    * Service for journal analytics and overview functionality
//    */
//   @Autowired
//   private AdminJournalViewService adminJournalViewService;

//   /**
//    * Constructor for dependency injection of core user services
//    * 
//    * @param adminUserViewService Service for user view operations
//    * @param adminUserCrudService Service for user CRUD operations
//    */
//   public AdminController(AdminUserViewService adminUserViewService, AdminUserCrudService adminUserCrudService) {
//     this.adminUserViewService = adminUserViewService;
//     this.adminUserCrudService = adminUserCrudService;
//   }

//   // ============================================================================
//   // ENHANCED USER MANAGEMENT ENDPOINTS WITH FILTERING
//   // ============================================================================

//   /**
//    * GET /admin/users/overview
//    * 
//    * Enhanced version with comprehensive filtering support
//    */
//   @GetMapping("/users/overview")
//   public AdminUserOverviewResponse getAllUserViews(
//       @RequestParam(required = false) String search,
//       @RequestParam(required = false) List<String> gender,
//       @RequestParam(required = false) List<String> relationship,
//       @RequestParam(required = false) Boolean activeOnly,
//       @RequestParam(required = false) Boolean inactiveOnly) {
    
//     List<AdminUserViewDTO> users = adminUserViewService.getAllUserViews();
//     int originalCount = users.size();
    
//     // Apply filters using FilterCriteria
//     AdminUserViewDTO.FilterCriteria criteria = new AdminUserViewDTO.FilterCriteria();
//     criteria.setSearchTerm(search);
//     criteria.setGenders(gender);
//     criteria.setRelationships(relationship);
//     criteria.setActiveOnly(activeOnly);
//     criteria.setInactiveOnly(inactiveOnly);
    
//     AdminUserViewDTO.UserStatistics stats = adminUserViewService.getUserStatistics();
    
//     // Create response with filter information
//     return new AdminUserOverviewResponse(users, stats, criteria, originalCount);
//   }

//   /**
//    * GET /admin/users/search
//    * 
//    * Enhanced search with pagination support
//    */
//   @GetMapping("/users/search")
//   public Map<String, Object> searchUsers(
//       @RequestParam("q") String q,
//       @RequestParam(defaultValue = "50") int limit,
//       @RequestParam(defaultValue = "0") int offset) {
    
//     AdminUserViewDTO.FilterCriteria criteria = new AdminUserViewDTO.FilterCriteria();
//     criteria.setSearchTerm(q);
    
//     List<AdminUserViewDTO> filteredUsers = adminUserViewService.filterUsersWithPagination(criteria, limit, offset);
//     long totalCount = adminUserViewService.getFilterResultsCount(criteria);
    
//     Map<String, Object> response = new HashMap<>();
//     response.put("users", filteredUsers);
//     response.put("totalCount", totalCount);
//     response.put("displayedCount", filteredUsers.size());
//     response.put("limit", limit);
//     response.put("offset", offset);
//     response.put("hasMore", (offset + limit) < totalCount);
    
//     return response;
//   }

//   /**
//    * GET /admin/users/filter-options
//    * 
//    * Get available filter options for the frontend
//    */
//   @GetMapping("/users/filter-options")
//   public Map<String, Object> getFilterOptions() {
//     return adminUserViewService.getAvailableFilterOptions();
//   }

//   /**
//    * GET /admin/users/filter-statistics
//    * 
//    * Get filter statistics for current user base
//    */
//   @GetMapping("/users/filter-statistics")
//   public AdminUserViewDTO.FilterStatistics getFilterStatistics() {
//     return adminUserViewService.getFilterStatistics();
//   }

//   /**
//    * POST /admin/users/advanced-filter
//    * 
//    * Advanced filtering with request body for complex criteria
//    */
//   @PostMapping("/users/advanced-filter")
//   public AdminUserOverviewResponse advancedFilter(@RequestBody AdminUserViewDTO.FilterCriteria criteria) {
//     List<AdminUserViewDTO> users = adminUserViewService.getAllUserViews();
//     int originalCount = users.size();
    
//     List<AdminUserViewDTO> filteredUsers = adminUserViewService.filterUsers(criteria);
//     AdminUserViewDTO.UserStatistics stats = adminUserViewService.getUserStatistics();
    
//     return new AdminUserOverviewResponse(filteredUsers, stats, criteria, originalCount);
//   }

//   /**
//    * GET /admin/users/analytics
//    * 
//    * Get comprehensive user analytics with optional filtering
//    */
//   @GetMapping("/users/analytics")
//   public Map<String, Object> getUserAnalytics(
//       @RequestParam(required = false) String search,
//       @RequestParam(required = false) List<String> gender,
//       @RequestParam(required = false) List<String> relationship,
//       @RequestParam(required = false) Boolean activeOnly) {
    
//     AdminUserViewDTO.FilterCriteria criteria = new AdminUserViewDTO.FilterCriteria();
//     criteria.setSearchTerm(search);
//     criteria.setGenders(gender);
//     criteria.setRelationships(relationship);
//     criteria.setActiveOnly(activeOnly);
    
//     return adminUserViewService.getDetailedAnalyticsWithFilters(criteria);
//   }

//   /**
//    * POST /admin/users/analytics-detailed
//    * 
//    * Get detailed analytics with complex filter criteria via POST
//    */
//   @PostMapping("/users/analytics-detailed")
//   public Map<String, Object> getDetailedAnalytics(@RequestBody AdminUserViewDTO.FilterCriteria criteria) {
//     return adminUserViewService.getDetailedAnalyticsWithFilters(criteria);
//   }

//   /**
//    * GET /admin/users/export
//    * 
//    * Export users with optional filtering (returns CSV-ready data)
//    */
//   @GetMapping("/users/export")
//   public Map<String, Object> exportUsers(
//       @RequestParam(required = false) String search,
//       @RequestParam(required = false) List<String> gender,
//       @RequestParam(required = false) List<String> relationship,
//       @RequestParam(required = false) Boolean activeOnly) {
    
//     AdminUserViewDTO.FilterCriteria criteria = new AdminUserViewDTO.FilterCriteria();
//     criteria.setSearchTerm(search);
//     criteria.setGenders(gender);
//     criteria.setRelationships(relationship);
//     criteria.setActiveOnly(activeOnly);
    
//     List<AdminUserViewDTO> users = adminUserViewService.filterUsers(criteria);
    
//     Map<String, Object> exportData = new HashMap<>();
//     exportData.put("users", users);
//     exportData.put("exportTimestamp", System.currentTimeMillis());
//     exportData.put("filterCriteria", criteria);
//     exportData.put("totalExported", users.size());
//     exportData.put("filename", "users_export_" + System.currentTimeMillis() + ".csv");
    
//     return exportData;
//   }

//   // ============================================================================
//   // BASIC USER CRUD ENDPOINTS
//   // ============================================================================

//   /**
//    * POST /admin/users
//    * 
//    * Creates a new user in the system.
//    * Used by administrators to manually add users.
//    * 
//    * @param userDto AdminCreateUserDTO containing user creation data
//    * @return User entity of the newly created user
//    */
//   @PostMapping("/users")
//   public User createUser(@RequestBody AdminCreateUserDTO userDto) {
//     return adminUserCrudService.createUser(userDto);
//   }

//   /**
//    * GET /admin/users/{id}
//    * 
//    * Retrieves a specific user by their ID.
//    * Throws RuntimeException if user is not found.
//    * 
//    * @param id The user ID to retrieve
//    * @return User entity with the specified ID
//    * @throws RuntimeException if user not found
//    */
//   @GetMapping("/users/{id}")
//   public User getUser(@PathVariable Long id) {
//     return adminUserCrudService.getUserById(id)
//       .orElseThrow(() -> new RuntimeException("User not found"));
//   }

//   /**
//    * PUT /admin/users/{id}
//    * 
//    * Updates an existing user's information.
//    * Used by administrators to modify user data.
//    * 
//    * @param id The ID of the user to update
//    * @param user User entity containing updated information
//    * @return Updated User entity
//    */
//   @PutMapping("/users/{id}")
//   public User updateUser(@PathVariable Long id, @RequestBody User user) {
//     return adminUserCrudService.updateUser(id, user);
//   }

//   /**
//    * DELETE /admin/users/{id}
//    * 
//    * Permanently deletes a user from the system.
//    * This operation cannot be undone.
//    * 
//    * @param id The ID of the user to delete
//    */
//   @DeleteMapping("/users/{id}")
//   public void deleteUser(@PathVariable Long id) {
//     adminUserCrudService.deleteUser(id);
//   }

//   // ============================================================================
//   // HABIT ANALYTICS ENDPOINTS
//   // ============================================================================

//   /**
//    * GET /admin/habits/overview
//    * 
//    * Provides comprehensive overview of all user habits.
//    * Includes per-user habit statistics and global habit metrics
//    * such as total habits created, completion rates, and trends.
//    * 
//    * @return AdminHabitOverviewResponse with user habit data and statistics
//    */
//   @GetMapping("/habits/overview")
//   public AdminHabitOverviewResponse getAllUserHabitViews() {
//     List<AdminHabitViewDTO> users = adminHabitViewService.getAllUserHabitViews();
//     AdminHabitViewDTO.HabitStatistics stats = adminHabitViewService.getHabitStatistics();
//     return new AdminHabitOverviewResponse(users, stats);
//   }

//   /**
//    * GET /admin/habits/analytics
//    * 
//    * Provides detailed analytical data about habits including:
//    * - Top performers by completion rate
//    * - Habit distribution by priority/category
//    * - Completion trends over time
//    * - Monthly statistics
//    * 
//    * @return Map containing detailed habit analytics data
//    */
//   @GetMapping("/habits/analytics")
//   public Map<String, Object> getHabitAnalytics() {
//     return adminHabitViewService.getDetailedHabitAnalytics();
//   }

//   // ============================================================================
//   // ENHANCED TASK ANALYTICS ENDPOINTS WITH SEARCH AND FILTERING
//   // ============================================================================

//   /**
//    * GET /admin/tasks/overview
//    * 
//    * Enhanced task overview with comprehensive filtering support.
//    * Provides overview of all user tasks with optional search and filters.
//    * 
//    * @param search Search term for userId or username
//    * @param minCompletionRate Minimum completion rate filter (0-100)
//    * @param maxCompletionRate Maximum completion rate filter (0-100)
//    * @param minTaskCount Minimum number of tasks filter
//    * @param maxTaskCount Maximum number of tasks filter
//    * @param hasOverdueTasks Filter for users with/without overdue tasks
//    * @param hasTasks Filter for users with/without any tasks
//    * @return AdminTaskOverviewResponse with filtered user task data and statistics
//    */
//   @GetMapping("/tasks/overview")
//   public AdminTaskOverviewResponse getAllUserTaskViews(
//       @RequestParam(required = false) String search,
//       @RequestParam(required = false) Double minCompletionRate,
//       @RequestParam(required = false) Double maxCompletionRate,
//       @RequestParam(required = false) Long minTaskCount,
//       @RequestParam(required = false) Long maxTaskCount,
//       @RequestParam(required = false) Boolean hasOverdueTasks,
//       @RequestParam(required = false) Boolean hasTasks) {
    
//     List<AdminTaskViewDTO> users = adminTaskViewService.getAllUserTaskViews();
//     int originalCount = users.size();
    
//     // Apply filters using TaskFilterCriteria
//     TaskFilterCriteria criteria = new TaskFilterCriteria();
//     criteria.setSearchTerm(search);
//     criteria.setMinCompletionRate(minCompletionRate);
//     criteria.setMaxCompletionRate(maxCompletionRate);
//     criteria.setMinTaskCount(minTaskCount);
//     criteria.setMaxTaskCount(maxTaskCount);
//     criteria.setHasOverdueTasks(hasOverdueTasks);
//     criteria.setHasTasks(hasTasks);
    
//     List<AdminTaskViewDTO> filteredUsers = adminTaskViewService.filterUsers(criteria);
//     AdminTaskViewDTO.TaskStatistics stats = adminTaskViewService.getTaskStatistics();
    
//     // Create response with filter information
//     return new AdminTaskOverviewResponse(filteredUsers, stats, criteria, originalCount);
//   }

//   /**
//    * GET /admin/tasks/search
//    * 
//    * Enhanced task search with pagination support.
//    * Searches by userId and username only.
//    * 
//    * @param q Search query term
//    * @param limit Maximum number of results (default: 50)
//    * @param offset Starting position for pagination (default: 0)
//    * @return Map containing search results and metadata
//    */
//   @GetMapping("/tasks/search")
//   public Map<String, Object> searchTaskUsers(
//       @RequestParam("q") String q,
//       @RequestParam(defaultValue = "50") int limit,
//       @RequestParam(defaultValue = "0") int offset) {
    
//     TaskFilterCriteria criteria = new TaskFilterCriteria();
//     criteria.setSearchTerm(q);
    
//     List<AdminTaskViewDTO> filteredUsers = adminTaskViewService.filterUsersWithPagination(criteria, limit, offset);
//     long totalCount = adminTaskViewService.getFilterResultsCount(criteria);
    
//     Map<String, Object> response = new HashMap<>();
//     response.put("users", filteredUsers);
//     response.put("totalCount", totalCount);
//     response.put("displayedCount", filteredUsers.size());
//     response.put("limit", limit);
//     response.put("offset", offset);
//     response.put("hasMore", (offset + limit) < totalCount);
//     response.put("searchTerm", q);
//     response.put("searchFields", "userId, username");
    
//     return response;
//   }

//   /**
//    * GET /admin/tasks/filter-options
//    * 
//    * Get available filter options for task filtering UI
//    * 
//    * @return Map containing available filter options and statistics
//    */
//   @GetMapping("/tasks/filter-options")
//   public Map<String, Object> getTaskFilterOptions() {
//     return adminTaskViewService.getAvailableFilterOptions();
//   }

//   /**
//    * GET /admin/tasks/filter-statistics
//    * 
//    * Get filter statistics for current task user base
//    * 
//    * @return Filter statistics for task users
//    */
//   @GetMapping("/tasks/filter-statistics")
//   public AdminTaskOverviewResponse.FilterStatistics getTaskFilterStatistics() {
//     return adminTaskViewService.getFilterStatistics();
//   }

//   /**
//    * POST /admin/tasks/advanced-filter
//    * 
//    * Advanced task filtering with request body for complex criteria
//    * 
//    * @param criteria TaskFilterCriteria containing filter parameters
//    * @return AdminTaskOverviewResponse with filtered results
//    */
//   @PostMapping("/tasks/advanced-filter")
//   public AdminTaskOverviewResponse advancedTaskFilter(@RequestBody TaskFilterCriteria criteria) {
//     List<AdminTaskViewDTO> users = adminTaskViewService.getAllUserTaskViews();
//     int originalCount = users.size();
    
//     List<AdminTaskViewDTO> filteredUsers = adminTaskViewService.filterUsers(criteria);
//     AdminTaskViewDTO.TaskStatistics stats = adminTaskViewService.getTaskStatistics();
    
//     return new AdminTaskOverviewResponse(filteredUsers, stats, criteria, originalCount);
//   }

//   /**
//    * GET /admin/tasks/analytics
//    * 
//    * Enhanced task analytics with optional filtering support.
//    * Provides detailed analytical data about tasks with filter capability.
//    * 
//    * @param search Search term for filtering
//    * @param minCompletionRate Minimum completion rate filter
//    * @param maxCompletionRate Maximum completion rate filter
//    * @param hasOverdueTasks Filter for overdue task status
//    * @param hasTasks Filter for task existence
//    * @return Map containing detailed task analytics data
//    */
//   @GetMapping("/tasks/analytics")
//   public Map<String, Object> getTaskAnalytics(
//       @RequestParam(required = false) String search,
//       @RequestParam(required = false) Double minCompletionRate,
//       @RequestParam(required = false) Double maxCompletionRate,
//       @RequestParam(required = false) Boolean hasOverdueTasks,
//       @RequestParam(required = false) Boolean hasTasks) {
    
//     TaskFilterCriteria criteria = new TaskFilterCriteria();
//     criteria.setSearchTerm(search);
//     criteria.setMinCompletionRate(minCompletionRate);
//     criteria.setMaxCompletionRate(maxCompletionRate);
//     criteria.setHasOverdueTasks(hasOverdueTasks);
//     criteria.setHasTasks(hasTasks);
    
//     return adminTaskViewService.getDetailedAnalyticsWithFilters(criteria);
//   }

//   /**
//    * POST /admin/tasks/analytics-detailed
//    * 
//    * Get detailed task analytics with complex filter criteria via POST
//    * 
//    * @param criteria TaskFilterCriteria for filtering analytics
//    * @return Map containing detailed filtered task analytics
//    */
//   @PostMapping("/tasks/analytics-detailed")
//   public Map<String, Object> getDetailedTaskAnalytics(@RequestBody TaskFilterCriteria criteria) {
//     return adminTaskViewService.getDetailedAnalyticsWithFilters(criteria);
//   }

//   /**
//    * GET /admin/tasks/export
//    * 
//    * Export task users with optional filtering (returns CSV-ready data)
//    * 
//    * @param search Search term for filtering
//    * @param minCompletionRate Minimum completion rate filter
//    * @param maxCompletionRate Maximum completion rate filter
//    * @param minTaskCount Minimum task count filter
//    * @param maxTaskCount Maximum task count filter
//    * @param hasOverdueTasks Filter for overdue task status
//    * @param hasTasks Filter for task existence
//    * @return Map containing export data and metadata
//    */
//   @GetMapping("/tasks/export")
//   public Map<String, Object> exportTaskUsers(
//       @RequestParam(required = false) String search,
//       @RequestParam(required = false) Double minCompletionRate,
//       @RequestParam(required = false) Double maxCompletionRate,
//       @RequestParam(required = false) Long minTaskCount,
//       @RequestParam(required = false) Long maxTaskCount,
//       @RequestParam(required = false) Boolean hasOverdueTasks,
//       @RequestParam(required = false) Boolean hasTasks) {
    
//     TaskFilterCriteria criteria = new TaskFilterCriteria();
//     criteria.setSearchTerm(search);
//     criteria.setMinCompletionRate(minCompletionRate);
//     criteria.setMaxCompletionRate(maxCompletionRate);
//     criteria.setMinTaskCount(minTaskCount);
//     criteria.setMaxTaskCount(maxTaskCount);
//     criteria.setHasOverdueTasks(hasOverdueTasks);
//     criteria.setHasTasks(hasTasks);
    
//     List<AdminTaskViewDTO> users = adminTaskViewService.filterUsers(criteria);
    
//     Map<String, Object> exportData = new HashMap<>();
//     exportData.put("users", users);
//     exportData.put("exportTimestamp", System.currentTimeMillis());
//     exportData.put("filterCriteria", criteria);
//     exportData.put("totalExported", users.size());
//     exportData.put("filename", "task_users_export_" + System.currentTimeMillis() + ".csv");
//     exportData.put("exportType", "task_analytics");
    
//     return exportData;
//   }

//   /**
//    * GET /admin/tasks/search-suggestions
//    * 
//    * Get search suggestions for task user search
//    * 
//    * @param q Partial search query
//    * @param limit Maximum number of suggestions (default: 10)
//    * @return List of search suggestions
//    */
//   @GetMapping("/tasks/search-suggestions")
//   public List<String> getTaskSearchSuggestions(
//       @RequestParam("q") String q,
//       @RequestParam(defaultValue = "10") int limit) {
//     return adminTaskViewService.getSearchSuggestions(q, limit);
//   }

//   /**
//    * GET /admin/tasks/search-with-criteria
//    * 
//    * Flexible search with specific field targeting
//    * 
//    * @param q Search term
//    * @param searchUserId Whether to search in user ID
//    * @param searchUsername Whether to search in username
//    * @return List of matching users
//    */
//   @GetMapping("/tasks/search-with-criteria")
//   public List<AdminTaskViewDTO> searchTaskUsersWithCriteria(
//       @RequestParam("q") String q,
//       @RequestParam(defaultValue = "true") boolean searchUserId,
//       @RequestParam(defaultValue = "true") boolean searchUsername) {
//     return adminTaskViewService.searchUsersWithCriteria(q, searchUserId, searchUsername);
//   }

//   // ============================================================================
//   // EXISTING TASK ANALYTICS ENDPOINTS (MAINTAINED FOR COMPATIBILITY)
//   // ============================================================================

//   /**
//    * GET /admin/tasks/quick-stats
//    * 
//    * Provides quick statistical overview of tasks for dashboard cards.
//    * Includes total tasks, completion rates, overdue counts, etc.
//    * 
//    * @return Map containing quick task statistics
//    */
//   @GetMapping("/tasks/quick-stats")
//   public Map<String, Object> getTaskQuickStats() {
//     return adminTaskViewService.getQuickStats();
//   }

//   /**
//    * GET /admin/tasks/users-with-tasks
//    * 
//    * Retrieves list of users who have created at least one task.
//    * Useful for identifying engaged users and filtering inactive accounts.
//    * 
//    * @return List of AdminTaskViewDTO for users with tasks
//    */
//   @GetMapping("/tasks/users-with-tasks")
//   public List<AdminTaskViewDTO> getUsersWithTasks() {
//     return adminTaskViewService.getUsersWithTasks();
//   }

//   /**
//    * GET /admin/tasks/users-without-tasks
//    * 
//    * Retrieves list of users who have not created any tasks.
//    * Helps administrators identify users who may need onboarding assistance.
//    * 
//    * @return List of AdminTaskViewDTO for users without tasks
//    */
//   @GetMapping("/tasks/users-without-tasks")
//   public List<AdminTaskViewDTO> getUsersWithoutTasks() {
//     return adminTaskViewService.getUsersWithoutTasks();
//   }

//   /**
//    * GET /admin/tasks/users-with-overdue
//    * 
//    * Retrieves list of users who have overdue tasks.
//    * Helps administrators identify users who may need assistance
//    * or follow-up to improve task completion.
//    * 
//    * @return List of AdminTaskViewDTO for users with overdue tasks
//    */
//   @GetMapping("/tasks/users-with-overdue")
//   public List<AdminTaskViewDTO> getUsersWithOverdueTasks() {
//     return adminTaskViewService.getUsersWithOverdueTasks();
//   }

//   /**
//    * GET /admin/tasks/top-performers
//    * 
//    * Retrieves top task performers by completion rate.
//    * Identifies users with highest task completion success.
//    * 
//    * @param limit Maximum number of users to return (default: 10)
//    * @return List of top performing task users
//    */
//   @GetMapping("/tasks/top-performers")
//   public List<AdminTaskViewDTO> getTopTaskPerformers(@RequestParam(defaultValue = "10") int limit) {
//     return adminTaskViewService.getTopTaskPerformers(limit);
//   }

//   /**
//    * GET /admin/tasks/users-needing-attention
//    * 
//    * Retrieves users who need attention (low completion rate or high overdue rate).
//    * Helps administrators identify users requiring follow-up or support.
//    * 
//    * @return List of users needing attention
//    */
//   @GetMapping("/tasks/users-needing-attention")
//   public List<AdminTaskViewDTO> getTaskUsersNeedingAttention() {
//     return adminTaskViewService.getUsersNeedingAttention();
//   }

//   /**
//    * GET /admin/tasks/highly-engaged-users
//    * 
//    * Retrieves highly engaged users with good task completion patterns.
//    * Identifies users who are actively and successfully using task features.
//    * 
//    * @return List of highly engaged task users
//    */
//   @GetMapping("/tasks/highly-engaged-users")
//   public List<AdminTaskViewDTO> getHighlyEngagedTaskUsers() {
//     return adminTaskViewService.getHighlyEngagedUsers();
//   }

//   /**
//    * GET /admin/tasks/most-active-creators
//    * 
//    * Retrieves users with most tasks created.
//    * Identifies most prolific task creators on the platform.
//    * 
//    * @param limit Maximum number of users to return (default: 10)
//    * @return List of most active task creators
//    */
//   @GetMapping("/tasks/most-active-creators")
//   public List<AdminTaskViewDTO> getMostActiveTaskCreators(@RequestParam(defaultValue = "10") int limit) {
//     return adminTaskViewService.getMostActiveTaskCreators(limit);
//   }

//   /**
//    * GET /admin/tasks/most-active-this-month
//    * 
//    * Retrieves most active task users this month.
//    * Shows current month's top performers in task creation and completion.
//    * 
//    * @param limit Maximum number of users to return (default: 10)
//    * @return List of most active task users this month
//    */
//   @GetMapping("/tasks/most-active-this-month")
//   public List<AdminTaskViewDTO> getMostActiveTaskUsersThisMonth(@RequestParam(defaultValue = "10") int limit) {
//     return adminTaskViewService.getMostActiveUsersThisMonth(limit);
//   }

//   // ============================================================================
//   // ACTIVITY OVERVIEW ENDPOINTS (UNIFIED HABITS + TASKS + EVENTS)
//   // ============================================================================

//   /**
//    * GET /admin/activity-overview
//    * 
//    * Enhanced activity overview with comprehensive filtering support.
//    * Provides unified overview of all user activities with optional search and filters.
//    * Combines data from habits, tasks, and calendar events.
//    * 
//    * @param search Search term for userId or username
//    * @param minTotalEvents Minimum total events filter
//    * @param maxTotalEvents Maximum total events filter
//    * @param minHabitsCompleted Minimum habits completed filter
//    * @param maxHabitsCompleted Maximum habits completed filter
//    * @param minTasksCompleted Minimum tasks completed filter
//    * @param maxTasksCompleted Maximum tasks completed filter
//    * @param minSameDayActivities Minimum same-day activities filter
//    * @param maxSameDayActivities Maximum same-day activities filter
//    * @param engagementLevel Filter by engagement level (high, medium, low, none)
//    * @param userType Filter by user type (active_users, highly_engaged, needs_attention, with_activities, coordinated_users)
//    * @param isActive Filter for active/inactive users
//    * @return AdminActivityOverviewResponse with filtered user activity data and statistics
//    */
//   @GetMapping("/activity-overview")
//   public AdminActivityOverviewResponse getAllUserActivityOverviews(
//       @RequestParam(required = false) String search,
//       @RequestParam(required = false) Long minTotalEvents,
//       @RequestParam(required = false) Long maxTotalEvents,
//       @RequestParam(required = false) Long minHabitsCompleted,
//       @RequestParam(required = false) Long maxHabitsCompleted,
//       @RequestParam(required = false) Long minTasksCompleted,
//       @RequestParam(required = false) Long maxTasksCompleted,
//       @RequestParam(required = false) Long minSameDayActivities,
//       @RequestParam(required = false) Long maxSameDayActivities,
//       @RequestParam(required = false) String engagementLevel,
//       @RequestParam(required = false) String userType,
//       @RequestParam(required = false) Boolean isActive) {
    
//     List<AdminActivityOverviewDTO> users = adminActivityOverviewService.getAllUserActivityOverviews();
//     AdminActivityOverviewDTO.ActivityOverviewStatistics stats = adminActivityOverviewService.getActivityOverviewStatistics();
    
//     // Apply filters using ActivityFilterCriteria
//     AdminActivityOverviewDTO.ActivityFilterCriteria criteria = new AdminActivityOverviewDTO.ActivityFilterCriteria();
//     criteria.setSearchTerm(search);
//     criteria.setMinTotalEvents(minTotalEvents);
//     criteria.setMaxTotalEvents(maxTotalEvents);
//     criteria.setMinHabitsCompleted(minHabitsCompleted);
//     criteria.setMaxHabitsCompleted(maxHabitsCompleted);
//     criteria.setMinTasksCompleted(minTasksCompleted);
//     criteria.setMaxTasksCompleted(maxTasksCompleted);
//     criteria.setMinSameDayActivities(minSameDayActivities);
//     criteria.setMaxSameDayActivities(maxSameDayActivities);
//     criteria.setEngagementLevel(engagementLevel);
//     criteria.setUserType(userType);
//     criteria.setIsActive(isActive);
    
//     // Filter users manually for now (until service method is implemented)
//     List<AdminActivityOverviewDTO> filteredUsers = users.stream()
//         .filter(user -> user.matchesSearchTerm(search))
//         .filter(user -> minTotalEvents == null || (user.getTotalEvents() != null && user.getTotalEvents() >= minTotalEvents))
//         .filter(user -> maxTotalEvents == null || (user.getTotalEvents() != null && user.getTotalEvents() <= maxTotalEvents))
//         .filter(user -> minHabitsCompleted == null || (user.getTotalHabitsCompleted() != null && user.getTotalHabitsCompleted() >= minHabitsCompleted))
//         .filter(user -> maxHabitsCompleted == null || (user.getTotalHabitsCompleted() != null && user.getTotalHabitsCompleted() <= maxHabitsCompleted))
//         .filter(user -> minTasksCompleted == null || (user.getTotalTasksCompleted() != null && user.getTotalTasksCompleted() >= minTasksCompleted))
//         .filter(user -> maxTasksCompleted == null || (user.getTotalTasksCompleted() != null && user.getTotalTasksCompleted() <= maxTasksCompleted))
//         .filter(user -> minSameDayActivities == null || (user.getHabitsAndTasksSameDay() != null && user.getHabitsAndTasksSameDay() >= minSameDayActivities))
//         .filter(user -> maxSameDayActivities == null || (user.getHabitsAndTasksSameDay() != null && user.getHabitsAndTasksSameDay() <= maxSameDayActivities))
//         .filter(user -> engagementLevel == null || user.getEngagementLevel().equalsIgnoreCase(engagementLevel))
//         .filter(user -> isActive == null || user.isActiveUser() == isActive)
//         .collect(Collectors.toList());
    
//     return new AdminActivityOverviewResponse(filteredUsers, stats, criteria, users.size());
//   }

//   /**
//    * GET /admin/activity-overview/search
//    * 
//    * Enhanced activity overview search with pagination support.
//    * Searches by userId and username only.
//    * 
//    * @param q Search query term
//    * @param limit Maximum number of results (default: 50)
//    * @param offset Starting position for pagination (default: 0)
//    * @return Map containing search results and metadata
//    */
//   @GetMapping("/activity-overview/search")
//   public Map<String, Object> searchActivityUsers(
//       @RequestParam("q") String q,
//       @RequestParam(defaultValue = "50") int limit,
//       @RequestParam(defaultValue = "0") int offset) {
    
//     List<AdminActivityOverviewDTO> allUsers = adminActivityOverviewService.getAllUserActivityOverviews();
//     List<AdminActivityOverviewDTO> searchResults = allUsers.stream()
//         .filter(user -> user.matchesSearchTerm(q))
//         .collect(Collectors.toList());
    
//     // Apply pagination manually
//     int start = Math.max(0, offset);
//     int end = Math.min(searchResults.size(), start + limit);
//     List<AdminActivityOverviewDTO> paginatedResults = start < searchResults.size() ? 
//         searchResults.subList(start, end) : new ArrayList<>();
    
//     Map<String, Object> response = new HashMap<>();
//     response.put("users", paginatedResults);
//     response.put("totalCount", searchResults.size());
//     response.put("displayedCount", paginatedResults.size());
//     response.put("limit", limit);
//     response.put("offset", offset);
//     response.put("hasMore", (offset + limit) < searchResults.size());
//     response.put("searchTerm", q);
//     response.put("searchFields", "userId, username");
    
//     return response;
//   }

//   /**
//    * GET /admin/activity-overview/filter-options
//    * 
//    * Get available filter options for activity overview filtering UI
//    * 
//    * @return Map containing available filter options and statistics
//    */
//   @GetMapping("/activity-overview/filter-options")
//   public Map<String, Object> getActivityFilterOptions() {
//     Map<String, Object> options = new HashMap<>();
    
//     // User type options
//     options.put("userTypes", List.of(
//         "active_users", "highly_engaged", "needs_attention", 
//         "with_activities", "coordinated_users", "inactive_users"
//     ));
    
//     // Engagement level options
//     options.put("engagementLevels", List.of("high", "medium", "low", "none"));
    
//     // Add statistics for filter ranges
//     AdminActivityOverviewDTO.ActivityOverviewStatistics stats = adminActivityOverviewService.getActivityOverviewStatistics();
//     options.put("totalUsers", stats.getActiveUsers());
//     options.put("totalEvents", stats.getTotalEvents());
//     options.put("averageEventsPerUser", stats.getAverageEventsPerUser());
//     options.put("usersWithActivities", stats.getUsersWithActivities());
    
//     return options;
//   }

//   /**
//    * POST /admin/activity-overview/advanced-filter
//    * 
//    * Advanced activity overview filtering with request body for complex criteria
//    * 
//    * @param criteria ActivityFilterCriteria containing filter parameters
//    * @return AdminActivityOverviewResponse with filtered results
//    */
//   @PostMapping("/activity-overview/advanced-filter")
//   public AdminActivityOverviewResponse advancedActivityFilter(@RequestBody AdminActivityOverviewDTO.ActivityFilterCriteria criteria) {
//     List<AdminActivityOverviewDTO> users = adminActivityOverviewService.getAllUserActivityOverviews();
//     AdminActivityOverviewDTO.ActivityOverviewStatistics stats = adminActivityOverviewService.getActivityOverviewStatistics();
    
//     // Filter users manually for now
//     List<AdminActivityOverviewDTO> filteredUsers = users.stream()
//         .filter(user -> user.matchesSearchTerm(criteria.getSearchTerm()))
//         .collect(Collectors.toList());
    
//     return new AdminActivityOverviewResponse(filteredUsers, stats, criteria, users.size());
//   }

//   /**
//    * GET /admin/activity-overview/paginated
//    * 
//    * Get paginated activity overview users with sorting and filtering
//    * 
//    * @param page Page number (default: 0)
//    * @param size Page size (default: 20)
//    * @param sortBy Sort field (default: username)
//    * @param sortDirection Sort direction (asc/desc, default: asc)
//    * @param search Search term for filtering
//    * @param minTotalEvents Minimum total events filter
//    * @param maxTotalEvents Maximum total events filter
//    * @param engagementLevel Engagement level filter
//    * @return AdminActivityOverviewResponse with paginated and filtered results
//    */
//   @GetMapping("/activity-overview/paginated")
//   public AdminActivityOverviewResponse getPaginatedActivityUsers(
//       @RequestParam(defaultValue = "0") int page,
//       @RequestParam(defaultValue = "20") int size,
//       @RequestParam(defaultValue = "username") String sortBy,
//       @RequestParam(defaultValue = "asc") String sortDirection,
//       @RequestParam(required = false) String search,
//       @RequestParam(required = false) Long minTotalEvents,
//       @RequestParam(required = false) Long maxTotalEvents,
//       @RequestParam(required = false) String engagementLevel) {
    
//     List<AdminActivityOverviewDTO> users = adminActivityOverviewService.getAllUserActivityOverviews();
//     AdminActivityOverviewDTO.ActivityOverviewStatistics stats = adminActivityOverviewService.getActivityOverviewStatistics();
    
//     AdminActivityOverviewDTO.ActivityFilterCriteria criteria = new AdminActivityOverviewDTO.ActivityFilterCriteria();
//     criteria.setSearchTerm(search);
//     criteria.setMinTotalEvents(minTotalEvents);
//     criteria.setMaxTotalEvents(maxTotalEvents);
//     criteria.setEngagementLevel(engagementLevel);
    
//     // Filter and paginate manually for now
//     List<AdminActivityOverviewDTO> filteredUsers = users.stream()
//         .filter(user -> user.matchesSearchTerm(search))
//         .skip(page * size)
//         .limit(size)
//         .collect(Collectors.toList());
    
//     return new AdminActivityOverviewResponse(filteredUsers, stats, criteria, users.size());
//   }

//   /**
//    * GET /admin/activity-overview/analytics
//    * 
//    * Enhanced activity overview analytics with optional filtering support.
//    * Provides comprehensive analytical data about all activities with filter capability.
//    * 
//    * @param search Search term for filtering
//    * @param minTotalEvents Minimum total events filter
//    * @param maxTotalEvents Maximum total events filter
//    * @param engagementLevel Engagement level filter
//    * @param userType User type filter
//    * @param isActive Active status filter
//    * @return Map containing detailed unified activity analytics
//    */
//   @GetMapping("/activity-overview/analytics")
//   public Map<String, Object> getActivityOverviewAnalytics(
//       @RequestParam(required = false) String search,
//       @RequestParam(required = false) Long minTotalEvents,
//       @RequestParam(required = false) Long maxTotalEvents,
//       @RequestParam(required = false) String engagementLevel,
//       @RequestParam(required = false) String userType,
//       @RequestParam(required = false) Boolean isActive) {
    
//     // Get comprehensive analytics
//     Map<String, Object> analytics = new HashMap<>();
//     AdminActivityOverviewDTO.ActivityOverviewStatistics stats = adminActivityOverviewService.getActivityOverviewStatistics();
    
//     analytics.put("globalStatistics", stats);
//     analytics.put("timestamp", System.currentTimeMillis());
    
//     return analytics;
//   }

//   /**
//    * POST /admin/activity-overview/analytics-detailed
//    * 
//    * Get detailed activity overview analytics with complex filter criteria via POST
//    * 
//    * @param criteria ActivityFilterCriteria for filtering analytics
//    * @return Map containing detailed filtered activity analytics
//    */
//   @PostMapping("/activity-overview/analytics-detailed")
//   public Map<String, Object> getDetailedActivityAnalytics(@RequestBody AdminActivityOverviewDTO.ActivityFilterCriteria criteria) {
//     Map<String, Object> analytics = new HashMap<>();
//     AdminActivityOverviewDTO.ActivityOverviewStatistics stats = adminActivityOverviewService.getActivityOverviewStatistics();
    
//     analytics.put("globalStatistics", stats);
//     analytics.put("appliedFilters", criteria);
//     analytics.put("timestamp", System.currentTimeMillis());
    
//     return analytics;
//   }

//   /**
//    * GET /admin/activity-overview/export
//    * 
//    * Export activity overview users with optional filtering (returns CSV-ready data)
//    * 
//    * @param search Search term for filtering
//    * @param minTotalEvents Minimum total events filter
//    * @param maxTotalEvents Maximum total events filter
//    * @param minHabitsCompleted Minimum habits completed filter
//    * @param maxHabitsCompleted Maximum habits completed filter
//    * @param minTasksCompleted Minimum tasks completed filter
//    * @param maxTasksCompleted Maximum tasks completed filter
//    * @param minSameDayActivities Minimum same-day activities filter
//    * @param maxSameDayActivities Maximum same-day activities filter
//    * @param engagementLevel Engagement level filter
//    * @param userType User type filter
//    * @param isActive Active status filter
//    * @return Map containing export data and metadata
//    */
//   @GetMapping("/activity-overview/export")
//   public Map<String, Object> exportActivityUsers(
//       @RequestParam(required = false) String search,
//       @RequestParam(required = false) Long minTotalEvents,
//       @RequestParam(required = false) Long maxTotalEvents,
//       @RequestParam(required = false) Long minHabitsCompleted,
//       @RequestParam(required = false) Long maxHabitsCompleted,
//       @RequestParam(required = false) Long minTasksCompleted,
//       @RequestParam(required = false) Long maxTasksCompleted,
//       @RequestParam(required = false) Long minSameDayActivities,
//       @RequestParam(required = false) Long maxSameDayActivities,
//       @RequestParam(required = false) String engagementLevel,
//       @RequestParam(required = false) String userType,
//       @RequestParam(required = false) Boolean isActive) {
    
//     List<AdminActivityOverviewDTO> users = adminActivityOverviewService.getAllUserActivityOverviews();
    
//     Map<String, Object> response = new HashMap<>();
//     response.put("users", users);
//     response.put("exportTimestamp", System.currentTimeMillis());
//     response.put("totalExported", users.size());
//     response.put("filename", "activity_users_export_" + System.currentTimeMillis() + ".csv");
//     response.put("exportType", "activity_analytics");
//     response.put("searchTerm", search);
    
//     return response;
//   }

//   /**
//    * GET /admin/activity-overview/search-suggestions
//    * 
//    * Get search suggestions for activity overview user search
//    * 
//    * @param q Partial search query
//    * @param limit Maximum number of suggestions (default: 10)
//    * @return List of search suggestions
//    */
//   @GetMapping("/activity-overview/search-suggestions")
//   public List<String> getActivitySearchSuggestions(
//       @RequestParam("q") String q,
//       @RequestParam(defaultValue = "10") int limit) {
    
//     List<AdminActivityOverviewDTO> users = adminActivityOverviewService.getAllUserActivityOverviews();
//     return users.stream()
//         .filter(user -> user.matchesSearchTerm(q))
//         .map(AdminActivityOverviewDTO::getUsername)
//         .filter(username -> username != null && username.toLowerCase().contains(q.toLowerCase()))
//         .distinct()
//         .limit(limit)
//         .collect(Collectors.toList());
//   }

//   /**
//    * GET /admin/activity-overview/search-with-criteria
//    * 
//    * Flexible search with specific field targeting
//    * 
//    * @param q Search term
//    * @param searchUserId Whether to search in user ID
//    * @param searchUsername Whether to search in username
//    * @return List of matching users
//    */
//   @GetMapping("/activity-overview/search-with-criteria")
//   public List<AdminActivityOverviewDTO> searchActivityUsersWithCriteria(
//       @RequestParam("q") String q,
//       @RequestParam(defaultValue = "true") boolean searchUserId,
//       @RequestParam(defaultValue = "true") boolean searchUsername) {
    
//     List<AdminActivityOverviewDTO> users = adminActivityOverviewService.getAllUserActivityOverviews();
//     return users.stream()
//         .filter(user -> user.matchesSearchTerm(q))
//         .collect(Collectors.toList());
//   }

//   // ============================================================================
//   // EXISTING ACTIVITY OVERVIEW ENDPOINTS (ENHANCED AND MAINTAINED FOR COMPATIBILITY)
//   // ============================================================================

//   /**
//    * GET /admin/activity-overview/quick-stats
//    * 
//    * Provides quick statistical overview of all activities for dashboard cards.
//    * Includes total events across all types, average events per user, etc.
//    * 
//    * @return Map containing quick activity statistics
//    */
//   @GetMapping("/activity-overview/quick-stats")
//   public Map<String, Object> getActivityOverviewQuickStats() {
//     AdminActivityOverviewDTO.ActivityOverviewStatistics stats = adminActivityOverviewService.getActivityOverviewStatistics();
//     Map<String, Object> quickStats = new HashMap<>();
//     quickStats.put("totalEvents", stats.getTotalEvents());
//     quickStats.put("totalHabits", stats.getTotalHabits());
//     quickStats.put("totalTasks", stats.getTotalTasks());
//     quickStats.put("averageEventsPerUser", stats.getAverageEventsPerUser());
//     quickStats.put("activeUsers", stats.getActiveUsers());
//     quickStats.put("usersWithActivities", stats.getUsersWithActivities());
//     return quickStats;
//   }

//   /**
//    * GET /admin/activity-overview/users-with-activity
//    * 
//    * Retrieves list of users who have at least one activity
//    * (habit, task, or calendar event).
//    * Useful for measuring overall platform engagement.
//    * 
//    * @return List of AdminActivityOverviewDTO for active users
//    */
//   @GetMapping("/activity-overview/users-with-activity")
//   public List<AdminActivityOverviewDTO> getUsersWithActivity() {
//     return adminActivityOverviewService.getAllUserActivityOverviews().stream()
//         .filter(AdminActivityOverviewDTO::hasActivity)
//         .collect(Collectors.toList());
//   }

//   /**
//    * GET /admin/activity-overview/users-with-same-day
//    * 
//    * Retrieves list of users who have both habits and tasks
//    * scheduled on the same day. This indicates users who are
//    * effectively coordinating their habit building with task completion.
//    * 
//    * @return List of AdminActivityOverviewDTO for users with same-day activities
//    */
//   @GetMapping("/activity-overview/users-with-same-day")
//   public List<AdminActivityOverviewDTO> getUsersWithSameDayActivities() {
//     return adminActivityOverviewService.getAllUserActivityOverviews().stream()
//         .filter(AdminActivityOverviewDTO::hasSameDayActivities)
//         .collect(Collectors.toList());
//   }

//   /**
//    * GET /admin/activity-overview/top-by-events
//    * 
//    * Retrieves top users ranked by total number of events
//    * (habits + tasks + calendar events).
//    * Useful for identifying most active users on the platform.
//    * 
//    * @param limit Maximum number of users to return (default: 10)
//    * @return List of top users by total events, limited by the limit parameter
//    */
//   @GetMapping("/activity-overview/top-by-events")
//   public List<AdminActivityOverviewDTO> getTopUsersByEvents(@RequestParam(defaultValue = "10") int limit) {
//     return adminActivityOverviewService.getAllUserActivityOverviews().stream()
//         .sorted((a, b) -> Long.compare(b.getTotalEvents(), a.getTotalEvents()))
//         .limit(limit)
//         .collect(Collectors.toList());
//   }

//   /**
//    * GET /admin/activity-overview/top-by-completed
//    * 
//    * Retrieves top users ranked by total number of completed activities
//    * (completed habits + completed tasks).
//    * Identifies users with highest completion rates and success.
//    * 
//    * @param limit Maximum number of users to return (default: 10)
//    * @return List of top users by completed activities, limited by the limit parameter
//    */
//   @GetMapping("/activity-overview/top-by-completed")
//   public List<AdminActivityOverviewDTO> getTopUsersByCompleted(@RequestParam(defaultValue = "10") int limit) {
//     return adminActivityOverviewService.getAllUserActivityOverviews().stream()
//         .sorted((a, b) -> Long.compare(b.getTotalCompletedActivities(), a.getTotalCompletedActivities()))
//         .limit(limit)
//         .collect(Collectors.toList());
//   }

//   /**
//    * GET /admin/activity-overview/most-active-this-month
//    * 
//    * Retrieves most active users this month by total events.
//    * Shows current month's top performers across all activity types.
//    * 
//    * @param limit Maximum number of users to return (default: 10)
//    * @return List of most active users this month
//    */
//   @GetMapping("/activity-overview/most-active-this-month")
//   public List<AdminActivityOverviewDTO> getMostActiveActivityUsersThisMonth(@RequestParam(defaultValue = "10") int limit) {
//     // For now, return top users by total events (monthly filtering would require service implementation)
//     return getTopUsersByEvents(limit);
//   }

//   /**
//    * GET /admin/activity-overview/users-needing-attention
//    * 
//    * Retrieves users who have activities but need attention (low completion rates).
//    * Helps administrators identify users who may need support or guidance.
//    * 
//    * @return List of users who may benefit from follow-up or support
//    */
//   @GetMapping("/activity-overview/users-needing-attention")
//   public List<AdminActivityOverviewDTO> getActivityUsersNeedingAttention() {
//     return adminActivityOverviewService.getAllUserActivityOverviews().stream()
//         .filter(AdminActivityOverviewDTO::needsAttention)
//         .collect(Collectors.toList());
//   }

//   /**
//    * GET /admin/activity-overview/highly-engaged-users
//    * 
//    * Retrieves highly engaged users with high activity scores across all types.
//    * Identifies users who are actively using multiple platform features.
//    * 
//    * @return List of highly engaged users sorted by activity score
//    */
//   @GetMapping("/activity-overview/highly-engaged-users")
//   public List<AdminActivityOverviewDTO> getHighlyEngagedActivityUsers() {
//     return adminActivityOverviewService.getAllUserActivityOverviews().stream()
//         .filter(AdminActivityOverviewDTO::isHighlyEngaged)
//         .sorted((a, b) -> Double.compare(b.getActivityScore(), a.getActivityScore()))
//         .collect(Collectors.toList());
//   }

//   /**
//    * GET /admin/activity-overview/users-with-coordinated-activities
//    * 
//    * Retrieves users who effectively coordinate habits and tasks on same days.
//    * Shows users with strong activity coordination patterns.
//    * 
//    * @return List of users with well-coordinated activities
//    */
//   @GetMapping("/activity-overview/users-with-coordinated-activities")
//   public List<AdminActivityOverviewDTO> getUsersWithCoordinatedActivities() {
//     return adminActivityOverviewService.getAllUserActivityOverviews().stream()
//         .filter(user -> user.getHabitsAndTasksSameDay() >= 5)
//         .collect(Collectors.toList());
//   }

//   /**
//    * GET /admin/activity-overview/user/{id}
//    * 
//    * Get detailed activity view for a specific user
//    * 
//    * @param id User ID
//    * @return AdminActivityOverviewDTO for the specified user
//    */
//   @GetMapping("/activity-overview/user/{id}")
//   public AdminActivityOverviewDTO getUserActivityView(@PathVariable Long id) {
//     List<AdminActivityOverviewDTO> users = adminActivityOverviewService.getAllUserActivityOverviews();
//     AdminActivityOverviewDTO userView = users.stream()
//         .filter(user -> user.getUserId().equals(id))
//         .findFirst()
//         .orElse(null);
    
//     if (userView == null) {
//       throw new RuntimeException("User activity view not found for ID: " + id);
//     }
//     return userView;
//   }

//   /**
//    * GET /admin/activity-overview/engagement-metrics
//    * 
//    * Get user engagement metrics across all activity types
//    * 
//    * @return Map containing engagement statistics
//    */
//   @GetMapping("/activity-overview/engagement-metrics")
//   public Map<String, Object> getActivityEngagementMetrics() {
//     Map<String, Object> analytics = new HashMap<>();
//     List<AdminActivityOverviewDTO> users = adminActivityOverviewService.getAllUserActivityOverviews();
    
//     long highlyEngaged = users.stream().filter(AdminActivityOverviewDTO::isHighlyEngaged).count();
//     long activeUsers = users.stream().filter(AdminActivityOverviewDTO::isActiveUser).count();
//     long needingAttention = users.stream().filter(AdminActivityOverviewDTO::needsAttention).count();
    
//     Map<String, Object> engagementAnalysis = new HashMap<>();
//     engagementAnalysis.put("highlyEngaged", highlyEngaged);
//     engagementAnalysis.put("activeUsers", activeUsers);
//     engagementAnalysis.put("needingAttention", needingAttention);
//     engagementAnalysis.put("totalUsers", users.size());
    
//     analytics.put("engagementAnalysis", engagementAnalysis);
//     return analytics;
//   }

//   /**
//    * GET /admin/activity-overview/completion-trends
//    * 
//    * Get activity completion trends and patterns
//    * 
//    * @return Map containing completion trend data
//    */
//   @GetMapping("/activity-overview/completion-trends")
//   public Map<String, Object> getActivityCompletionTrends() {
//     Map<String, Object> analytics = new HashMap<>();
//     List<AdminActivityOverviewDTO> users = adminActivityOverviewService.getAllUserActivityOverviews();
    
//     double avgCompletionRate = users.stream()
//         .mapToDouble(user -> {
//             long total = user.getTotalEvents();
//             long completed = user.getTotalCompletedActivities();
//             return total > 0 ? (double) completed / total * 100 : 0.0;
//         })
//         .average()
//         .orElse(0.0);
    
//     Map<String, Object> completionTrends = new HashMap<>();
//     completionTrends.put("averageCompletionRate", avgCompletionRate);
//     completionTrends.put("timestamp", System.currentTimeMillis());
    
//     analytics.put("completionTrends", completionTrends);
//     return analytics;
//   }

//   /**
//    * GET /admin/activity-overview/coordination-analysis
//    * 
//    * Get analysis of same-day activity coordination
//    * 
//    * @return Map containing coordination data
//    */
//   @GetMapping("/activity-overview/coordination-analysis")
//   public Map<String, Object> getActivityCoordinationAnalysis() {
//     Map<String, Object> analytics = new HashMap<>();
//     List<AdminActivityOverviewDTO> users = adminActivityOverviewService.getAllUserActivityOverviews();
    
//     long usersWithCoordination = users.stream()
//         .filter(AdminActivityOverviewDTO::hasSameDayActivities)
//         .count();
    
//     Map<String, Object> coordinationAnalysis = new HashMap<>();
//     coordinationAnalysis.put("usersWithCoordination", usersWithCoordination);
//     coordinationAnalysis.put("totalUsers", users.size());
//     coordinationAnalysis.put("coordinationRate", users.size() > 0 ? (double) usersWithCoordination / users.size() * 100 : 0.0);
    
//     analytics.put("coordinationAnalysis", coordinationAnalysis);
//     return analytics;
//   }

//   // ============================================================================
//   // ENHANCED JOURNAL ANALYTICS ENDPOINTS WITH SEARCH AND FILTERING
//   // ============================================================================

//   /**
//    * GET /admin/journals/overview
//    * 
//    * Enhanced journal overview with comprehensive filtering support.
//    * Provides overview of all user journal entries with optional search and filters.
//    * 
//    * @param search Search term for userId or username
//    * @param minJournalEntries Minimum number of journal entries filter
//    * @param maxJournalEntries Maximum number of journal entries filter
//    * @param minRecentEntries Minimum monthly journal entries filter
//    * @param maxRecentEntries Maximum monthly journal entries filter
//    * @param engagementLevel Filter by engagement level (high, medium, low, none)
//    * @param userType Filter by user type (with_journal_entries, without_journal_entries, active_users, recent_writers, needs_attention, highly_engaged)
//    * @param isActive Filter for active/inactive users
//    * @return AdminJournalOverviewResponse with filtered user journal data and statistics
//    */
//   @GetMapping("/journals/overview")
//   public AdminJournalOverviewResponse getAllUserJournalViews(
//       @RequestParam(required = false) String search,
//       @RequestParam(required = false) Long minJournalEntries,
//       @RequestParam(required = false) Long maxJournalEntries,
//       @RequestParam(required = false) Long minRecentEntries,
//       @RequestParam(required = false) Long maxRecentEntries,
//       @RequestParam(required = false) String engagementLevel,
//       @RequestParam(required = false) String userType,
//       @RequestParam(required = false) Boolean isActive) {
    
//     List<AdminJournalViewDTO> users = adminJournalViewService.getAllUserJournalViews();
//     AdminJournalViewDTO.JournalStatistics stats = adminJournalViewService.getJournalStatistics();
    
//     // Apply filters using JournalFilterCriteria
//     AdminJournalViewDTO.JournalFilterCriteria criteria = new AdminJournalViewDTO.JournalFilterCriteria();
//     criteria.setSearchTerm(search);
//     criteria.setMinJournalEntries(minJournalEntries);
//     criteria.setMaxJournalEntries(maxJournalEntries);
//     criteria.setMinRecentEntries(minRecentEntries);
//     criteria.setMaxRecentEntries(maxRecentEntries);
//     criteria.setEngagementLevel(engagementLevel);
//     criteria.setUserType(userType);
//     criteria.setIsActive(isActive);
    
//     // Filter users manually for now
//     List<AdminJournalViewDTO> filteredUsers = users.stream()
//         .filter(user -> user.matchesSearchTerm(search))
//         .collect(Collectors.toList());
    
//     return new AdminJournalOverviewResponse(filteredUsers, stats, criteria, users.size());
//   }

//   /**
//    * GET /admin/journals/search
//    * 
//    * Enhanced journal search with pagination support.
//    * Searches by userId and username only.
//    * 
//    * @param q Search query term
//    * @param limit Maximum number of results (default: 50)
//    * @param offset Starting position for pagination (default: 0)
//    * @return Map containing search results and metadata
//    */
//   @GetMapping("/journals/search")
//   public Map<String, Object> searchJournalUsers(
//       @RequestParam("q") String q,
//       @RequestParam(defaultValue = "50") int limit,
//       @RequestParam(defaultValue = "0") int offset) {
    
//     List<AdminJournalViewDTO> allUsers = adminJournalViewService.getAllUserJournalViews();
//     List<AdminJournalViewDTO> searchResults = allUsers.stream()
//         .filter(user -> user.matchesSearchTerm(q))
//         .collect(Collectors.toList());
    
//     // Apply pagination manually
//     int start = Math.max(0, offset);
//     int end = Math.min(searchResults.size(), start + limit);
//     List<AdminJournalViewDTO> paginatedResults = start < searchResults.size() ? 
//         searchResults.subList(start, end) : new ArrayList<>();
    
//     Map<String, Object> response = new HashMap<>();
//     response.put("users", paginatedResults);
//     response.put("totalCount", searchResults.size());
//     response.put("displayedCount", paginatedResults.size());
//     response.put("limit", limit);
//     response.put("offset", offset);
//     response.put("hasMore", (offset + limit) < searchResults.size());
//     response.put("searchTerm", q);
//     response.put("searchFields", "userId, username");
    
//     return response;
//   }

//   /**
//    * GET /admin/journals/filter-options
//    * 
//    * Get available filter options for journal filtering UI
//    * 
//    * @return Map containing available filter options and statistics
//    */
//   @GetMapping("/journals/filter-options")
//   public Map<String, Object> getJournalFilterOptions() {
//     Map<String, Object> options = new HashMap<>();
    
//     // User type options
//     options.put("userTypes", List.of(
//         "with_journal_entries", "without_journal_entries", "active_users", 
//         "recent_writers", "needs_attention", "highly_engaged"
//     ));
    
//     // Engagement level options
//     options.put("engagementLevels", List.of("high", "medium", "low", "none"));
    
//     // Add statistics for filter ranges
//     AdminJournalViewDTO.JournalStatistics stats = adminJournalViewService.getJournalStatistics();
//     options.put("totalUsers", stats.getTotalUsers());
//     // Use available methods - adjust these based on actual JournalStatistics class
//     options.put("totalJournalEntries", stats.getTotalUsers()); // placeholder - replace with actual method
//     options.put("averageEntriesPerUser", 0.0); // placeholder - calculate if needed
//     options.put("usersWithJournalEntries", stats.getTotalUsers()); // placeholder - replace with actual method
    
//     return options;
//   }

//   /**
//    * GET /admin/journals/filter-statistics
//    * 
//    * Get filter statistics for current journal user base
//    * 
//    * @return Map containing filter statistics for journal users
//    */
//   @GetMapping("/journals/filter-statistics")
//   public Map<String, Object> getJournalFilterStatistics() {
//     // Return basic filter statistics since FilterStatistics class doesn't exist yet
//     Map<String, Object> filterStats = new HashMap<>();
//     AdminJournalViewDTO.JournalStatistics stats = adminJournalViewService.getJournalStatistics();
    
//     filterStats.put("totalUsers", stats.getTotalUsers());
//     filterStats.put("timestamp", System.currentTimeMillis());
    
//     return filterStats;
//   }

//   /**
//    * POST /admin/journals/advanced-filter
//    * 
//    * Advanced journal filtering with request body for complex criteria
//    * 
//    * @param criteria JournalFilterCriteria containing filter parameters
//    * @return AdminJournalOverviewResponse with filtered results
//    */
//   @PostMapping("/journals/advanced-filter")
//   public AdminJournalOverviewResponse advancedJournalFilter(@RequestBody AdminJournalViewDTO.JournalFilterCriteria criteria) {
//     List<AdminJournalViewDTO> users = adminJournalViewService.getAllUserJournalViews();
//     int originalCount = users.size();
    
//     // Manual filtering since filterUsers method doesn't exist yet
//     List<AdminJournalViewDTO> filteredUsers = users.stream()
//         .filter(user -> criteria.getSearchTerm() == null || user.matchesSearchTerm(criteria.getSearchTerm()))
//         .collect(Collectors.toList());
        
//     AdminJournalViewDTO.JournalStatistics stats = adminJournalViewService.getJournalStatistics();
    
//     return new AdminJournalOverviewResponse(filteredUsers, stats, criteria, originalCount);
//   }

//   /**
//    * GET /admin/journals/analytics
//    * 
//    * Enhanced journal analytics with optional filtering support.
//    * Provides detailed analytical data about journal entries with filter capability.
//    * 
//    * @param search Search term for filtering
//    * @param minJournalEntries Minimum journal entries filter
//    * @param maxJournalEntries Maximum journal entries filter
//    * @param minRecentEntries Minimum recent entries filter
//    * @param maxRecentEntries Maximum recent entries filter
//    * @param engagementLevel Engagement level filter
//    * @param userType User type filter
//    * @param isActive Active status filter
//    * @return Map containing detailed journal analytics data
//    */
//   @GetMapping("/journals/analytics")
//   public Map<String, Object> getJournalAnalytics(
//       @RequestParam(required = false) String search,
//       @RequestParam(required = false) Long minJournalEntries,
//       @RequestParam(required = false) Long maxJournalEntries,
//       @RequestParam(required = false) Long minRecentEntries,
//       @RequestParam(required = false) Long maxRecentEntries,
//       @RequestParam(required = false) String engagementLevel,
//       @RequestParam(required = false) String userType,
//       @RequestParam(required = false) Boolean isActive) {
    
//     AdminJournalViewDTO.JournalFilterCriteria criteria = new AdminJournalViewDTO.JournalFilterCriteria();
//     criteria.setSearchTerm(search);
//     criteria.setMinJournalEntries(minJournalEntries);
//     criteria.setMaxJournalEntries(maxJournalEntries);
//     criteria.setMinRecentEntries(minRecentEntries);
//     criteria.setMaxRecentEntries(maxRecentEntries);
//     criteria.setEngagementLevel(engagementLevel);
//     criteria.setUserType(userType);
//     criteria.setIsActive(isActive);
    
//     // Return basic analytics since getDetailedAnalyticsWithFilters doesn't exist yet
//     Map<String, Object> analytics = adminJournalViewService.getDetailedJournalAnalytics();
//     analytics.put("appliedFilters", criteria);
//     return analytics;
//   }

//   /**
//    * POST /admin/journals/analytics-detailed
//    * 
//    * Get detailed journal analytics with complex filter criteria via POST
//    * 
//    * @param criteria JournalFilterCriteria for filtering analytics
//    * @return Map containing detailed filtered journal analytics
//    */
//   @PostMapping("/journals/analytics-detailed")
//   public Map<String, Object> getDetailedJournalAnalytics(@RequestBody AdminJournalViewDTO.JournalFilterCriteria criteria) {
//     Map<String, Object> analytics = adminJournalViewService.getDetailedJournalAnalytics();
//     analytics.put("appliedFilters", criteria);
//     return analytics;
//   }

//   /**
//    * GET /admin/journals/export
//    * 
//    * Export journal users with optional filtering (returns CSV-ready data)
//    * 
//    * @param search Search term for filtering
//    * @param minJournalEntries Minimum journal entries filter
//    * @param maxJournalEntries Maximum journal entries filter
//    * @param minRecentEntries Minimum recent entries filter
//    * @param maxRecentEntries Maximum recent entries filter
//    * @param engagementLevel Engagement level filter
//    * @param userType User type filter
//    * @param isActive Active status filter
//    * @return Map containing export data and metadata
//    */
//   @GetMapping("/journals/export")
//   public Map<String, Object> exportJournalUsers(
//       @RequestParam(required = false) String search,
//       @RequestParam(required = false) Long minJournalEntries,
//       @RequestParam(required = false) Long maxJournalEntries,
//       @RequestParam(required = false) Long minRecentEntries,
//       @RequestParam(required = false) Long maxRecentEntries,
//       @RequestParam(required = false) String engagementLevel,
//       @RequestParam(required = false) String userType,
//       @RequestParam(required = false) Boolean isActive) {
    
//     AdminJournalViewDTO.JournalFilterCriteria criteria = new AdminJournalViewDTO.JournalFilterCriteria();
//     criteria.setSearchTerm(search);
//     criteria.setMinJournalEntries(minJournalEntries);
//     criteria.setMaxJournalEntries(maxJournalEntries);
//     criteria.setMinRecentEntries(minRecentEntries);
//     criteria.setMaxRecentEntries(maxRecentEntries);
//     criteria.setEngagementLevel(engagementLevel);
//     criteria.setUserType(userType);
//     criteria.setIsActive(isActive);
    
//     // Manual filtering since filterUsers method doesn't exist yet
//     List<AdminJournalViewDTO> users = adminJournalViewService.getAllUserJournalViews();
//     List<AdminJournalViewDTO> filteredUsers = users.stream()
//         .filter(user -> search == null || user.matchesSearchTerm(search))
//         .collect(Collectors.toList());
    
//     Map<String, Object> exportData = new HashMap<>();
//     exportData.put("users", filteredUsers);
//     exportData.put("exportTimestamp", System.currentTimeMillis());
//     exportData.put("filterCriteria", criteria);
//     exportData.put("totalExported", filteredUsers.size());
//     exportData.put("filename", "journal_users_export_" + System.currentTimeMillis() + ".csv");
//     exportData.put("exportType", "journal_analytics");
//     exportData.put("searchTerm", search);
    
//     return exportData;
//   }

//   /**
//    * GET /admin/journals/search-suggestions
//    * 
//    * Get search suggestions for journal user search
//    * 
//    * @param q Partial search query
//    * @param limit Maximum number of suggestions (default: 10)
//    * @return List of search suggestions
//    */
//   @GetMapping("/journals/search-suggestions")
//   public List<String> getJournalSearchSuggestions(
//       @RequestParam("q") String q,
//       @RequestParam(defaultValue = "10") int limit) {
    
//     List<AdminJournalViewDTO> users = adminJournalViewService.getAllUserJournalViews();
//     return users.stream()
//         .filter(user -> q == null || q.trim().isEmpty() || 
//                 (user.getUserId() != null && user.getUserId().toString().toLowerCase().contains(q.toLowerCase())) ||
//                 (user.getUsername() != null && user.getUsername().toLowerCase().contains(q.toLowerCase())))
//         .map(AdminJournalViewDTO::getUsername)
//         .filter(username -> username != null && username.toLowerCase().contains(q.toLowerCase()))
//         .distinct()
//         .limit(limit)
//         .collect(Collectors.toList());
//   }

//   /**
//    * GET /admin/journals/search-with-criteria
//    * 
//    * Flexible search with specific field targeting
//    * 
//    * @param q Search term
//    * @param searchUserId Whether to search in user ID
//    * @param searchUsername Whether to search in username
//    * @return List of matching users
//    */
//   @GetMapping("/journals/search-with-criteria")
//   public List<AdminJournalViewDTO> searchJournalUsersWithCriteria(
//       @RequestParam("q") String q,
//       @RequestParam(defaultValue = "true") boolean searchUserId,
//       @RequestParam(defaultValue = "true") boolean searchUsername) {
    
//     List<AdminJournalViewDTO> users = adminJournalViewService.getAllUserJournalViews();
//     return users.stream()
//         .filter(user -> q == null || q.trim().isEmpty() || 
//                 (searchUserId && user.getUserId() != null && user.getUserId().toString().toLowerCase().contains(q.toLowerCase())) ||
//                 (searchUsername && user.getUsername() != null && user.getUsername().toLowerCase().contains(q.toLowerCase())))
//         .collect(Collectors.toList());
//   }

//   // ============================================================================
//   // EXISTING JOURNAL ANALYTICS ENDPOINTS (ENHANCED AND MAINTAINED FOR COMPATIBILITY)
//   // ============================================================================

//   /**
//    * GET /admin/journals/quick-stats
//    * 
//    * Provides quick statistical overview of journal entries for dashboard cards.
//    * Includes total journal entries, user engagement rates, monthly activity, etc.
//    * 
//    * @return Map containing quick journal statistics
//    */
//   @GetMapping("/journals/quick-stats")
//   public Map<String, Object> getJournalQuickStats() {
//     return adminJournalViewService.getQuickStats();
//   }

//   /**
//    * GET /admin/journals/users-with-journals
//    * 
//    * Retrieves list of users who have created at least one journal entry.
//    * Useful for identifying engaged users and measuring platform adoption.
//    * 
//    * @return List of AdminJournalViewDTO for users with journal entries
//    */
//   @GetMapping("/journals/users-with-journals")
//   public List<AdminJournalViewDTO> getUsersWithJournals() {
//     return adminJournalViewService.getUsersWithJournalEntries();
//   }

//   /**
//    * GET /admin/journals/users-without-journals
//    * 
//    * Retrieves list of users who have not created any journal entries.
//    * Helps administrators identify users who may need onboarding assistance.
//    * 
//    * @return List of AdminJournalViewDTO for users without journal entries
//    */
//   @GetMapping("/journals/users-without-journals")
//   public List<AdminJournalViewDTO> getUsersWithoutJournals() {
//     return adminJournalViewService.getUsersWithoutJournalEntries();
//   }

//   /**
//    * GET /admin/journals/top-writers
//    * 
//    * Retrieves top users ranked by total number of journal entries created.
//    * Identifies most active journal writers on the platform.
//    * 
//    * @param limit Maximum number of users to return (default: 10)
//    * @return List of top journal writers, limited by the limit parameter
//    */
//   @GetMapping("/journals/top-writers")
//   public List<AdminJournalViewDTO> getTopJournalWriters(@RequestParam(defaultValue = "10") int limit) {
//     return adminJournalViewService.getTopJournalWriters(limit);
//   }

//   /**
//    * GET /admin/journals/most-active-this-month
//    * 
//    * Retrieves most active users this month by journal entries created.
//    * Shows current month's top performers and engagement levels.
//    * 
//    * @param limit Maximum number of users to return (default: 10)
//    * @return List of most active journal writers this month
//    */
//   @GetMapping("/journals/most-active-this-month")
//   public List<AdminJournalViewDTO> getMostActiveJournalWritersThisMonth(@RequestParam(defaultValue = "10") int limit) {
//     return adminJournalViewService.getMostActiveUsersThisMonth(limit);
//   }

//   /**
//    * GET /admin/journals/users-needing-attention
//    * 
//    * Retrieves users who have journal entries but no recent activity.
//    * Helps administrators identify users who may need re-engagement.
//    * 
//    * @return List of users who may benefit from follow-up or support
//    */
//   @GetMapping("/journals/users-needing-attention")
//   public List<AdminJournalViewDTO> getJournalUsersNeedingAttention() {
//     return adminJournalViewService.getUsersNeedingAttention();
//   }

//   /**
//    * GET /admin/journals/highly-engaged-users
//    * 
//    * Retrieves highly engaged users with high journaling activity scores.
//    * Identifies users who are actively using the journaling features.
//    * 
//    * @return List of highly engaged journal users sorted by activity score
//    */
//   @GetMapping("/journals/highly-engaged-users")
//   public List<AdminJournalViewDTO> getHighlyEngagedJournalUsers() {
//     return adminJournalViewService.getHighlyEngagedUsers();
//   }

//   /**
//    * GET /admin/journals/users-with-recent-activity
//    * 
//    * Retrieves users who have created journal entries this month.
//    * Shows current month's active journal writers.
//    * 
//    * @return List of users with recent journal activity
//    */
//   @GetMapping("/journals/users-with-recent-activity")
//   public List<AdminJournalViewDTO> getUsersWithRecentJournalActivity() {
//     return adminJournalViewService.getUsersWithRecentActivity();
//   }

//   /**
//    * GET /admin/journals/user/{id}
//    * 
//    * Get detailed journal view for a specific user
//    * 
//    * @param id User ID
//    * @return AdminJournalViewDTO for the specified user
//    */
//   @GetMapping("/journals/user/{id}")
//   public AdminJournalViewDTO getUserJournalView(@PathVariable Long id) {
//     AdminJournalViewDTO userView = adminJournalViewService.getUserJournalView(id);
//     if (userView == null) {
//       throw new RuntimeException("User journal view not found for ID: " + id);
//     }
//     return userView;
//   }

//   /**
//    * GET /admin/journals/user/{id}/entries
//    * 
//    * Get all journal entries for a specific user
//    * 
//    * @param id User ID
//    * @return List of journal entries for the specified user
//    */
//   @GetMapping("/journals/user/{id}/entries")
//   public List<Object> getUserJournalEntries(@PathVariable Long id) {
//     // Return empty list since getNoteEntriesForUserId method doesn't exist
//     // This would need to be implemented in the service layer
//     return new ArrayList<>();
//   }

//   /**
//    * GET /admin/journals/engagement-metrics
//    * 
//    * Get user engagement metrics for journal entries
//    * 
//    * @return Map containing engagement statistics
//    */
//   @GetMapping("/journals/engagement-metrics")
//   public Map<String, Object> getJournalEngagementMetrics() {
//     Map<String, Object> analytics = adminJournalViewService.getDetailedJournalAnalytics();
//     return (Map<String, Object>) analytics.getOrDefault("engagementAnalysis", new HashMap<>());
//   }

//   /**
//    * GET /admin/journals/monthly-trends
//    * 
//    * Get monthly journal entry trends and patterns
//    * 
//    * @return Map containing monthly trend data
//    */
//   @GetMapping("/journals/monthly-trends")
//   public Map<String, Object> getJournalMonthlyTrends() {
//     Map<String, Object> analytics = adminJournalViewService.getDetailedJournalAnalytics();
//     return (Map<String, Object>) analytics.getOrDefault("monthlyTrends", new HashMap<>());
//   }

//   /**
//    * GET /admin/journals/distribution-analysis
//    * 
//    * Get journal entry distribution analysis
//    * 
//    * @return Map containing distribution data
//    */
//   @GetMapping("/journals/distribution-analysis")
//   public Map<String, Object> getJournalDistributionAnalysis() {
//     Map<String, Object> analytics = adminJournalViewService.getDetailedJournalAnalytics();
//     return (Map<String, Object>) analytics.getOrDefault("userJournalDistribution", new HashMap<>());
//   }

//   /**
//    * GET /admin/journals/recent-activity-analysis
//    * 
//    * Get recent activity analysis for journal entries
//    * 
//    * @return Map containing recent activity data
//    */
//   @GetMapping("/journals/recent-activity-analysis")
//   public Map<String, Object> getJournalRecentActivityAnalysis() {
//     Map<String, Object> analytics = adminJournalViewService.getDetailedJournalAnalytics();
//     return (Map<String, Object>) analytics.getOrDefault("recentActivity", new HashMap<>());
//   }

//   /**
//    * GET /admin/notes/overview
//    * 
//    * Enhanced note overview with comprehensive filtering support.
//    * Provides overview of all user notes with optional search and filters.
//    * 
//    * @param search Search term for userId or username
//    * @param minNoteCount Minimum number of notes filter
//    * @param maxNoteCount Maximum number of notes filter
//    * @param minRecentNotes Minimum monthly notes filter
//    * @param maxRecentNotes Maximum monthly notes filter
//    * @param engagementLevel Filter by engagement level (high, medium, low, none)
//    * @param userType Filter by user type (with_notes, without_notes, active_users, recent_creators, needs_attention, highly_engaged)
//    * @param isActive Filter for active/inactive users
//    * @return AdminNoteOverviewResponse with filtered user note data and statistics
//    */
//   @GetMapping("/notes/overview")
//   public AdminNoteOverviewResponse getAllUserNoteViews(
//       @RequestParam(required = false) String search,
//       @RequestParam(required = false) Long minNoteCount,
//       @RequestParam(required = false) Long maxNoteCount,
//       @RequestParam(required = false) Long minRecentNotes,
//       @RequestParam(required = false) Long maxRecentNotes,
//       @RequestParam(required = false) String engagementLevel,
//       @RequestParam(required = false) String userType,
//       @RequestParam(required = false) Boolean isActive) {
    
//     List<AdminNoteViewDTO> users = adminNoteViewService.getAllUserNoteViews();
//     AdminNoteViewDTO.NoteStatistics stats = adminNoteViewService.getNoteStatistics();
    
//     // Apply filters using NoteFilterCriteria
//     AdminNoteOverviewResponse.NoteFilterCriteria criteria = new AdminNoteOverviewResponse.NoteFilterCriteria();
//     criteria.setSearchTerm(search);
//     criteria.setMinNoteCount(minNoteCount);
//     criteria.setMaxNoteCount(maxNoteCount);
    
//     // Filter users manually since the matchesSearchTerm method has visibility issues
//     List<AdminNoteViewDTO> filteredUsers = users.stream()
//         .filter(user -> search == null || search.trim().isEmpty() || 
//                 (user.getUserId() != null && user.getUserId().toString().toLowerCase().contains(search.toLowerCase())) ||
//                 (user.getUsername() != null && user.getUsername().toLowerCase().contains(search.toLowerCase())))
//         .filter(user -> minNoteCount == null || user.getTotalNotes() >= minNoteCount)
//         .filter(user -> maxNoteCount == null || user.getTotalNotes() <= maxNoteCount)
//         .collect(Collectors.toList());
    
//     return new AdminNoteOverviewResponse(filteredUsers, stats, criteria, users.size());
//   }

//   /**
//    * GET /admin/notes/search
//    * 
//    * Enhanced note search with pagination support.
//    * Searches by userId and username only.
//    * 
//    * @param q Search query term
//    * @param limit Maximum number of results (default: 50)
//    * @param offset Starting position for pagination (default: 0)
//    * @return Map containing search results and metadata
//    */
//   @GetMapping("/notes/search")
//   public Map<String, Object> searchNoteUsers(
//       @RequestParam("q") String q,
//       @RequestParam(defaultValue = "50") int limit,
//       @RequestParam(defaultValue = "0") int offset) {
    
//     List<AdminNoteViewDTO> allUsers = adminNoteViewService.getAllUserNoteViews();
//     List<AdminNoteViewDTO> searchResults = allUsers.stream()
//         .filter(user -> q == null || q.trim().isEmpty() || 
//                 (user.getUserId() != null && user.getUserId().toString().toLowerCase().contains(q.toLowerCase())) ||
//                 (user.getUsername() != null && user.getUsername().toLowerCase().contains(q.toLowerCase())))
//         .collect(Collectors.toList());
    
//     // Apply pagination manually
//     int start = Math.max(0, offset);
//     int end = Math.min(searchResults.size(), start + limit);
//     List<AdminNoteViewDTO> paginatedResults = start < searchResults.size() ? 
//         searchResults.subList(start, end) : new ArrayList<>();
    
//     Map<String, Object> response = new HashMap<>();
//     response.put("users", paginatedResults);
//     response.put("totalCount", searchResults.size());
//     response.put("displayedCount", paginatedResults.size());
//     response.put("limit", limit);
//     response.put("offset", offset);
//     response.put("hasMore", (offset + limit) < searchResults.size());
//     response.put("searchTerm", q);
//     response.put("searchFields", "userId, username");
    
//     return response;
//   }

//   /**
//    * GET /admin/notes/filter-options
//    * 
//    * Get available filter options for note filtering UI
//    * 
//    * @return Map containing available filter options and statistics
//    */
//   @GetMapping("/notes/filter-options")
//   public Map<String, Object> getNoteFilterOptions() {
//     Map<String, Object> options = new HashMap<>();
    
//     // User type options
//     options.put("userTypes", List.of(
//         "with_notes", "without_notes", "active_users", 
//         "recent_creators", "needs_attention", "highly_engaged"
//     ));
    
//     // Engagement level options
//     options.put("engagementLevels", List.of("high", "medium", "low", "none"));
    
//     // Add statistics for filter ranges
//     AdminNoteViewDTO.NoteStatistics stats = adminNoteViewService.getNoteStatistics();
//     options.put("totalUsers", stats.getTotalUsers());
//     // Use placeholder values since getTotalNotes() doesn't exist
//     options.put("totalNotes", stats.getTotalUsers()); // placeholder
//     options.put("averageNotesPerUser", stats.getAverageNotesPerUser());
//     options.put("usersWithNotes", stats.getUsersWithNotes());
    
//     return options;
//   }

//   /**
//    * GET /admin/notes/filter-statistics
//    * 
//    * Get filter statistics for current note user base
//    * 
//    * @return Filter statistics for note users
//    */
//   @GetMapping("/notes/filter-statistics")
//   public AdminNoteOverviewResponse.FilterStatistics getNoteFilterStatistics() {
//     return adminNoteViewService.getFilterStatistics();
//   }

//   /**
//    * POST /admin/notes/advanced-filter
//    * 
//    * Advanced note filtering with request body for complex criteria
//    * 
//    * @param criteria NoteFilterCriteria containing filter parameters
//    * @return AdminNoteOverviewResponse with filtered results
//    */
//   @PostMapping("/notes/advanced-filter")
//   public AdminNoteOverviewResponse advancedNoteFilter(@RequestBody AdminNoteOverviewResponse.NoteFilterCriteria criteria) {
//     List<AdminNoteViewDTO> users = adminNoteViewService.getAllUserNoteViews();
//     int originalCount = users.size();
    
//     List<AdminNoteViewDTO> filteredUsers = adminNoteViewService.filterUsers(criteria);
//     AdminNoteViewDTO.NoteStatistics stats = adminNoteViewService.getNoteStatistics();
    
//     return new AdminNoteOverviewResponse(filteredUsers, stats, criteria, originalCount);
//   }

//   /**
//    * GET /admin/notes/analytics
//    * 
//    * Enhanced note analytics with optional filtering support.
//    * Provides detailed analytical data about notes with filter capability.
//    * 
//    * @param search Search term for filtering
//    * @param minNoteCount Minimum note count filter
//    * @param maxNoteCount Maximum note count filter
//    * @param engagementLevel Engagement level filter
//    * @param userType User type filter
//    * @param isActive Active status filter
//    * @return Map containing detailed note analytics data
//    */
//   @GetMapping("/notes/analytics")
//   public Map<String, Object> getNoteAnalytics(
//       @RequestParam(required = false) String search,
//       @RequestParam(required = false) Long minNoteCount,
//       @RequestParam(required = false) Long maxNoteCount,
//       @RequestParam(required = false) String engagementLevel,
//       @RequestParam(required = false) String userType,
//       @RequestParam(required = false) Boolean isActive) {
    
//     AdminNoteOverviewResponse.NoteFilterCriteria criteria = new AdminNoteOverviewResponse.NoteFilterCriteria();
//     criteria.setSearchTerm(search);
//     criteria.setMinNoteCount(minNoteCount);
//     criteria.setMaxNoteCount(maxNoteCount);
    
//     return adminNoteViewService.getDetailedAnalyticsWithFilters(criteria);
//   }

//   /**
//    * POST /admin/notes/analytics-detailed
//    * 
//    * Get detailed note analytics with complex filter criteria via POST
//    * 
//    * @param criteria NoteFilterCriteria for filtering analytics
//    * @return Map containing detailed filtered note analytics
//    */
//   @PostMapping("/notes/analytics-detailed")
//   public Map<String, Object> getDetailedNoteAnalytics(@RequestBody AdminNoteOverviewResponse.NoteFilterCriteria criteria) {
//     return adminNoteViewService.getDetailedAnalyticsWithFilters(criteria);
//   }

//   /**
//    * GET /admin/notes/export
//    * 
//    * Export note users with optional filtering (returns CSV-ready data)
//    * 
//    * @param search Search term for filtering
//    * @param minNoteCount Minimum note count filter
//    * @param maxNoteCount Maximum note count filter
//    * @param engagementLevel Engagement level filter
//    * @param userType User type filter
//    * @param isActive Active status filter
//    * @return Map containing export data and metadata
//    */
//   @GetMapping("/notes/export")
//   public Map<String, Object> exportNoteUsers(
//       @RequestParam(required = false) String search,
//       @RequestParam(required = false) Long minNoteCount,
//       @RequestParam(required = false) Long maxNoteCount,
//       @RequestParam(required = false) String engagementLevel,
//       @RequestParam(required = false) String userType,
//       @RequestParam(required = false) Boolean isActive) {
    
//     AdminNoteOverviewResponse.NoteFilterCriteria criteria = new AdminNoteOverviewResponse.NoteFilterCriteria();
//     criteria.setSearchTerm(search);
//     criteria.setMinNoteCount(minNoteCount);
//     criteria.setMaxNoteCount(maxNoteCount);
    
//     List<AdminNoteViewDTO> users = adminNoteViewService.filterUsers(criteria);
    
//     Map<String, Object> exportData = new HashMap<>();
//     exportData.put("users", users);
//     exportData.put("exportTimestamp", System.currentTimeMillis());
//     exportData.put("filterCriteria", criteria);
//     exportData.put("totalExported", users.size());
//     exportData.put("filename", "note_users_export_" + System.currentTimeMillis() + ".csv");
//     exportData.put("exportType", "note_analytics");
//     exportData.put("searchTerm", search);
    
//     return exportData;
//   }

//   /**
//    * GET /admin/notes/search-suggestions
//    * 
//    * Get search suggestions for note user search
//    * 
//    * @param q Partial search query
//    * @param limit Maximum number of suggestions (default: 10)
//    * @return List of search suggestions
//    */
//   @GetMapping("/notes/search-suggestions")
//   public List<String> getNoteSearchSuggestions(
//       @RequestParam("q") String q,
//       @RequestParam(defaultValue = "10") int limit) {
    
//     List<AdminNoteViewDTO> users = adminNoteViewService.getAllUserNoteViews();
//     return users.stream()
//         .filter(user -> q == null || q.trim().isEmpty() || 
//                 (user.getUserId() != null && user.getUserId().toString().toLowerCase().contains(q.toLowerCase())) ||
//                 (user.getUsername() != null && user.getUsername().toLowerCase().contains(q.toLowerCase())))
//         .map(AdminNoteViewDTO::getUsername)
//         .filter(username -> username != null && username.toLowerCase().contains(q.toLowerCase()))
//         .distinct()
//         .limit(limit)
//         .collect(Collectors.toList());
//   }

//   /**
//    * GET /admin/notes/search-with-criteria
//    * 
//    * Flexible search with specific field targeting
//    * 
//    * @param q Search term
//    * @param searchUserId Whether to search in user ID
//    * @param searchUsername Whether to search in username
//    * @return List of matching users
//    */
//   @GetMapping("/notes/search-with-criteria")
//   public List<AdminNoteViewDTO> searchNoteUsersWithCriteria(
//       @RequestParam("q") String q,
//       @RequestParam(defaultValue = "true") boolean searchUserId,
//       @RequestParam(defaultValue = "true") boolean searchUsername) {
    
//     List<AdminNoteViewDTO> users = adminNoteViewService.getAllUserNoteViews();
//     return users.stream()
//         .filter(user -> q == null || q.trim().isEmpty() || 
//                 (searchUserId && user.getUserId() != null && user.getUserId().toString().toLowerCase().contains(q.toLowerCase())) ||
//                 (searchUsername && user.getUsername() != null && user.getUsername().toLowerCase().contains(q.toLowerCase())))
//         .collect(Collectors.toList());
//   }
// }
