package com.superme.admin.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import com.superme.dto.AdminTaskViewDTO;
import com.superme.dto.AdminTaskOverviewResponse;
import com.superme.dto.AdminTaskOverviewResponse.TaskFilterCriteria;
import com.superme.service.AdminTaskViewService;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * AdminTaskController handles all task analytics operations for the admin panel.
 * 
 * This controller provides comprehensive task management functionality including:
 * - Task analytics and overview with advanced search and filtering
 * - Task completion rate analysis
 * - User task performance tracking
 * 
 * @author MindfullB Admin Team
 * @version 2.0
 */
@RestController
@RequestMapping("/admin/tasks")
public class AdminTaskController {

  @Autowired
  private AdminTaskViewService adminTaskViewService;

  // ============================================================================
  // ENHANCED TASK ANALYTICS ENDPOINTS WITH SEARCH AND FILTERING
  // ============================================================================

  /**
   * GET /admin/tasks/overview
   * 
   * Enhanced task overview with comprehensive filtering support
   */
  @GetMapping("/overview")
  public AdminTaskOverviewResponse getAllUserTaskViews(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Double minCompletionRate,
      @RequestParam(required = false) Double maxCompletionRate,
      @RequestParam(required = false) Long minTaskCount,
      @RequestParam(required = false) Long maxTaskCount,
      @RequestParam(required = false) Boolean hasOverdueTasks,
      @RequestParam(required = false) Boolean hasTasks) {
    
    List<AdminTaskViewDTO> users = adminTaskViewService.getAllUserTaskViews();
    int originalCount = users.size();
    
    TaskFilterCriteria criteria = new TaskFilterCriteria();
    criteria.setSearchTerm(search);
    criteria.setMinCompletionRate(minCompletionRate);
    criteria.setMaxCompletionRate(maxCompletionRate);
    criteria.setMinTaskCount(minTaskCount);
    criteria.setMaxTaskCount(maxTaskCount);
    criteria.setHasOverdueTasks(hasOverdueTasks);
    criteria.setHasTasks(hasTasks);
    
    List<AdminTaskViewDTO> filteredUsers = adminTaskViewService.filterUsers(criteria);
    AdminTaskViewDTO.TaskStatistics stats = adminTaskViewService.getTaskStatistics();
    
    return new AdminTaskOverviewResponse(filteredUsers, stats, criteria, originalCount);
  }

  /**
   * GET /admin/tasks/search
   * 
   * Enhanced task search with pagination support
   */
  @GetMapping("/search")
  public Map<String, Object> searchTaskUsers(
      @RequestParam("q") String q,
      @RequestParam(defaultValue = "50") int limit,
      @RequestParam(defaultValue = "0") int offset) {
    
    TaskFilterCriteria criteria = new TaskFilterCriteria();
    criteria.setSearchTerm(q);
    
    List<AdminTaskViewDTO> filteredUsers = adminTaskViewService.filterUsersWithPagination(criteria, limit, offset);
    long totalCount = adminTaskViewService.getFilterResultsCount(criteria);
    
    Map<String, Object> response = new HashMap<>();
    response.put("users", filteredUsers);
    response.put("totalCount", totalCount);
    response.put("displayedCount", filteredUsers.size());
    response.put("limit", limit);
    response.put("offset", offset);
    response.put("hasMore", (offset + limit) < totalCount);
    response.put("searchTerm", q);
    response.put("searchFields", "userId"); // Username removed
    
    return response;
  }

  /**
   * GET /admin/tasks/filter-options
   * 
   * Get available filter options for task filtering UI
   */
  @GetMapping("/filter-options")
  public Map<String, Object> getTaskFilterOptions() {
    return adminTaskViewService.getAvailableFilterOptions();
  }

  /**
   * GET /admin/tasks/filter-statistics
   * 
   * Get filter statistics for current task user base
   */
  @GetMapping("/filter-statistics")
  public AdminTaskOverviewResponse.FilterStatistics getTaskFilterStatistics() {
    return adminTaskViewService.getFilterStatistics();
  }

  /**
   * POST /admin/tasks/advanced-filter
   * 
   * Advanced task filtering with request body for complex criteria
   */
  @PostMapping("/advanced-filter")
  public AdminTaskOverviewResponse advancedTaskFilter(@RequestBody TaskFilterCriteria criteria) {
    List<AdminTaskViewDTO> users = adminTaskViewService.getAllUserTaskViews();
    int originalCount = users.size();
    
    List<AdminTaskViewDTO> filteredUsers = adminTaskViewService.filterUsers(criteria);
    AdminTaskViewDTO.TaskStatistics stats = adminTaskViewService.getTaskStatistics();
    
    return new AdminTaskOverviewResponse(filteredUsers, stats, criteria, originalCount);
  }

  /**
   * GET /admin/tasks/analytics
   * 
   * Enhanced task analytics with optional filtering support
   */
  @GetMapping("/analytics")
  public Map<String, Object> getTaskAnalytics(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Double minCompletionRate,
      @RequestParam(required = false) Double maxCompletionRate,
      @RequestParam(required = false) Boolean hasOverdueTasks,
      @RequestParam(required = false) Boolean hasTasks) {
    
    TaskFilterCriteria criteria = new TaskFilterCriteria();
    criteria.setSearchTerm(search);
    criteria.setMinCompletionRate(minCompletionRate);
    criteria.setMaxCompletionRate(maxCompletionRate);
    criteria.setHasOverdueTasks(hasOverdueTasks);
    criteria.setHasTasks(hasTasks);
    
    return adminTaskViewService.getDetailedAnalyticsWithFilters(criteria);
  }

  /**
   * POST /admin/tasks/analytics-detailed
   * 
   * Get detailed task analytics with complex filter criteria via POST
   */
  @PostMapping("/analytics-detailed")
  public Map<String, Object> getDetailedTaskAnalytics(@RequestBody TaskFilterCriteria criteria) {
    return adminTaskViewService.getDetailedAnalyticsWithFilters(criteria);
  }

  /**
   * GET /admin/tasks/export
   * 
   * Export task users with optional filtering (returns CSV-ready data)
   */
  @GetMapping("/export")
  public Map<String, Object> exportTaskUsers(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Double minCompletionRate,
      @RequestParam(required = false) Double maxCompletionRate,
      @RequestParam(required = false) Long minTaskCount,
      @RequestParam(required = false) Long maxTaskCount,
      @RequestParam(required = false) Boolean hasOverdueTasks,
      @RequestParam(required = false) Boolean hasTasks) {
    
    TaskFilterCriteria criteria = new TaskFilterCriteria();
    criteria.setSearchTerm(search);
    criteria.setMinCompletionRate(minCompletionRate);
    criteria.setMaxCompletionRate(maxCompletionRate);
    criteria.setMinTaskCount(minTaskCount);
    criteria.setMaxTaskCount(maxTaskCount);
    criteria.setHasOverdueTasks(hasOverdueTasks);
    criteria.setHasTasks(hasTasks);
    
    List<AdminTaskViewDTO> users = adminTaskViewService.filterUsers(criteria);
    
    Map<String, Object> exportData = new HashMap<>();
    exportData.put("users", users);
    exportData.put("exportTimestamp", System.currentTimeMillis());
    exportData.put("filterCriteria", criteria);
    exportData.put("totalExported", users.size());
    exportData.put("filename", "task_users_export_" + System.currentTimeMillis() + ".csv");
    exportData.put("exportType", "task_analytics");
    
    return exportData;
  }

  /**
   * GET /admin/tasks/search-suggestions
   * 
   * Get search suggestions for task user search
   */
  @GetMapping("/search-suggestions")
  public List<String> getTaskSearchSuggestions(
      @RequestParam("q") String q,
      @RequestParam(defaultValue = "10") int limit) {
    return adminTaskViewService.getSearchSuggestions(q, limit);
  }

  /**
   * GET /admin/tasks/search-with-criteria
   * 
   * Flexible search with specific field targeting
   */
  @GetMapping("/search-with-criteria")
  public List<AdminTaskViewDTO> searchTaskUsersWithCriteria(
      @RequestParam("q") String q,
      @RequestParam(defaultValue = "true") boolean searchUserId,
      @RequestParam(defaultValue = "false") boolean searchUsername) { // Username search removed
    return adminTaskViewService.searchUsersWithCriteria(q, searchUserId, false);
  }

  // ============================================================================
  // EXISTING TASK ANALYTICS ENDPOINTS (MAINTAINED FOR COMPATIBILITY)
  // ============================================================================

  /**
   * GET /admin/tasks/quick-stats
   * 
   * Provides quick statistical overview of tasks for dashboard cards
   */
  @GetMapping("/quick-stats")
  public Map<String, Object> getTaskQuickStats() {
    return adminTaskViewService.getQuickStats();
  }

  /**
   * GET /admin/tasks/users-with-tasks
   * 
   * Retrieves list of users who have created at least one task
   */
  @GetMapping("/users-with-tasks")
  public List<AdminTaskViewDTO> getUsersWithTasks() {
    return adminTaskViewService.getUsersWithTasks();
  }

  /**
   * GET /admin/tasks/users-without-tasks
   * 
   * Retrieves list of users who have not created any tasks
   */
  @GetMapping("/users-without-tasks")
  public List<AdminTaskViewDTO> getUsersWithoutTasks() {
    return adminTaskViewService.getUsersWithoutTasks();
  }

  /**
   * GET /admin/tasks/users-with-overdue
   * 
   * Retrieves list of users who have overdue tasks
   */
  @GetMapping("/users-with-overdue")
  public List<AdminTaskViewDTO> getUsersWithOverdueTasks() {
    return adminTaskViewService.getUsersWithOverdueTasks();
  }

  /**
   * GET /admin/tasks/top-performers
   * 
   * Retrieves top task performers by completion rate
   */
  @GetMapping("/top-performers")
  public List<AdminTaskViewDTO> getTopTaskPerformers(@RequestParam(defaultValue = "10") int limit) {
    return adminTaskViewService.getTopTaskPerformers(limit);
  }

  /**
   * GET /admin/tasks/users-needing-attention
   * 
   * Retrieves users who need attention (low completion rate or high overdue rate)
   */
  @GetMapping("/users-needing-attention")
  public List<AdminTaskViewDTO> getTaskUsersNeedingAttention() {
    return adminTaskViewService.getUsersNeedingAttention();
  }

  /**
   * GET /admin/tasks/highly-engaged-users
   * 
   * Retrieves highly engaged users with good task completion patterns
   */
  @GetMapping("/highly-engaged-users")
  public List<AdminTaskViewDTO> getHighlyEngagedTaskUsers() {
    return adminTaskViewService.getHighlyEngagedUsers();
  }

  /**
   * GET /admin/tasks/most-active-creators
   * 
   * Retrieves users with most tasks created
   */
  @GetMapping("/most-active-creators")
  public List<AdminTaskViewDTO> getMostActiveTaskCreators(@RequestParam(defaultValue = "10") int limit) {
    return adminTaskViewService.getMostActiveTaskCreators(limit);
  }

  /**
   * GET /admin/tasks/most-active-this-month
   * 
   * Retrieves most active task users this month
   */
  @GetMapping("/most-active-this-month")
  public List<AdminTaskViewDTO> getMostActiveTaskUsersThisMonth(@RequestParam(defaultValue = "10") int limit) {
    return adminTaskViewService.getMostActiveUsersThisMonth(limit);
  }
}
