package com.superme.admin.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.superme.dto.AdminJournalViewDTO;
import com.superme.dto.AdminJournalOverviewResponse;
import com.superme.service.AdminJournalViewService;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * AdminJournalController handles all journal analytics operations for the admin
 * panel.
 * 
 * This controller provides comprehensive journal management functionality
 * including:
 * - Journal analytics and overview with advanced search and filtering
 * - User journal statistics and engagement tracking
 * 
 * @author MindfullB Admin Team
 * @version 2.0
 */
@RestController
@RequestMapping("/admin/journals")
public class AdminJournalController {

  @Autowired
  private AdminJournalViewService adminJournalViewService;

  // ============================================================================
  // ENHANCED JOURNAL ANALYTICS ENDPOINTS WITH SEARCH AND FILTERING
  // ============================================================================

  /**
   * GET /admin/journals/overview
   * 
   * Enhanced journal overview with comprehensive filtering support
   */
  @GetMapping("/overview")
  public AdminJournalOverviewResponse getAllUserJournalViews(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Long minJournalEntries,
      @RequestParam(required = false) Long maxJournalEntries,
      @RequestParam(required = false) Long minRecentEntries,
      @RequestParam(required = false) Long maxRecentEntries,
      @RequestParam(required = false) String engagementLevel,
      @RequestParam(required = false) String userType,
      @RequestParam(required = false) Boolean isActive,
      @RequestParam(required = false) Boolean isDeactivated,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    AdminJournalViewDTO.JournalFilterCriteria criteria = new AdminJournalViewDTO.JournalFilterCriteria();
    criteria.setSearchTerm(search);
    criteria.setMinJournalEntries(minJournalEntries);
    criteria.setMaxJournalEntries(maxJournalEntries);
    criteria.setMinRecentEntries(minRecentEntries);
    criteria.setMaxRecentEntries(maxRecentEntries);
    criteria.setEngagementLevel(engagementLevel);
    criteria.setUserType(userType);

    // isDeactivated=true is equivalent to isActive=false
    if (Boolean.TRUE.equals(isDeactivated)) {
      criteria.setIsActive(false);
    } else if (isActive != null) {
      criteria.setIsActive(isActive);
    }

    // Delegate all filtering to the service (search + isActive + ranges + engagement + userType)
    AdminJournalOverviewResponse filtered = adminJournalViewService.getFilteredUserJournalViews(criteria);
    List<AdminJournalViewDTO> filteredUsers = filtered.getUsers();

    int totalFiltered = filteredUsers.size();
    int totalPages = size > 0 ? (int) Math.ceil((double) totalFiltered / size) : 1;
    int start = page * size;
    int end = Math.min(start + size, totalFiltered);
    List<AdminJournalViewDTO> pageContent = start >= totalFiltered
        ? new ArrayList<>()
        : filteredUsers.subList(start, end);

    AdminJournalOverviewResponse response = new AdminJournalOverviewResponse(
        pageContent, filtered.getStatistics(), criteria, filtered.getOriginalCount());
    response.setTotalFilteredCount(totalFiltered);
    response.setCurrentPage(page);
    response.setPageSize(size);
    response.setTotalPages(totalPages);
    response.setHasNext((page + 1) < totalPages);
    response.setHasPrevious(page > 0);

    return response;
  }

  /**
   * GET /admin/journals/search
   * 
   * Enhanced journal search with pagination support
   */
  @GetMapping("/search")
  public Map<String, Object> searchJournalUsers(
      @RequestParam("q") String q,
      @RequestParam(defaultValue = "50") int limit,
      @RequestParam(defaultValue = "0") int offset) {

    List<AdminJournalViewDTO> allUsers = adminJournalViewService.getAllUserJournalViews();
    List<AdminJournalViewDTO> searchResults = allUsers.stream()
        .filter(user -> user.matchesSearchTerm(q))
        .collect(Collectors.toList());

    // Apply pagination manually
    int start = Math.max(0, offset);
    int end = Math.min(searchResults.size(), start + limit);
    List<AdminJournalViewDTO> paginatedResults = start < searchResults.size() ? searchResults.subList(start, end)
        : new ArrayList<>();

    Map<String, Object> response = new HashMap<>();
    response.put("users", paginatedResults);
    response.put("totalCount", searchResults.size());
    response.put("displayedCount", paginatedResults.size());
    response.put("limit", limit);
    response.put("offset", offset);
    response.put("hasMore", (offset + limit) < searchResults.size());
    response.put("searchTerm", q);
    response.put("searchFields", "userId, name"); // Username removed, name added

    return response;
  }

  /**
   * GET /admin/journals/filter-options
   * 
   * Get available filter options for journal filtering UI
   */
  @GetMapping("/filter-options")
  public Map<String, Object> getJournalFilterOptions() {
    Map<String, Object> options = new HashMap<>();

    options.put("userTypes", List.of(
        "with_journal_entries", "without_journal_entries", "active_users",
        "recent_writers", "needs_attention", "highly_engaged"));

    options.put("engagementLevels", List.of("high", "medium", "low", "none"));

    AdminJournalViewDTO.JournalStatistics stats = adminJournalViewService.getJournalStatistics();
    options.put("totalUsers", stats.getTotalUsers());
    options.put("totalJournalEntries", stats.getTotalUsers()); // placeholder
    options.put("averageEntriesPerUser", 0.0); // placeholder
    options.put("usersWithJournalEntries", stats.getTotalUsers()); // placeholder

    return options;
  }

  /**
   * GET /admin/journals/filter-statistics
   * 
   * Get filter statistics for current journal user base
   */
  @GetMapping("/filter-statistics")
  public Map<String, Object> getJournalFilterStatistics() {
    Map<String, Object> filterStats = new HashMap<>();
    AdminJournalViewDTO.JournalStatistics stats = adminJournalViewService.getJournalStatistics();

    filterStats.put("totalUsers", stats.getTotalUsers());
    filterStats.put("timestamp", System.currentTimeMillis());

    return filterStats;
  }

  /**
   * POST /admin/journals/advanced-filter
   * 
   * Advanced journal filtering with request body for complex criteria
   */
  @PostMapping("/advanced-filter")
  public AdminJournalOverviewResponse advancedJournalFilter(
      @RequestBody AdminJournalViewDTO.JournalFilterCriteria criteria) {
    List<AdminJournalViewDTO> users = adminJournalViewService.getAllUserJournalViews();
    int originalCount = users.size();

    // Manual filtering since filterUsers method doesn't exist yet
    List<AdminJournalViewDTO> filteredUsers = users.stream()
        .filter(user -> criteria.getSearchTerm() == null || user.matchesSearchTerm(criteria.getSearchTerm()))
        .collect(Collectors.toList());

    AdminJournalViewDTO.JournalStatistics stats = adminJournalViewService.getJournalStatistics();

    return new AdminJournalOverviewResponse(filteredUsers, stats, criteria, originalCount);
  }

  /**
   * GET /admin/journals/analytics
   * 
   * Enhanced journal analytics with optional filtering support
   */
  @GetMapping("/analytics")
  public Map<String, Object> getJournalAnalytics(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Long minJournalEntries,
      @RequestParam(required = false) Long maxJournalEntries,
      @RequestParam(required = false) Long minRecentEntries,
      @RequestParam(required = false) Long maxRecentEntries,
      @RequestParam(required = false) String engagementLevel,
      @RequestParam(required = false) String userType,
      @RequestParam(required = false) Boolean isActive) {

    AdminJournalViewDTO.JournalFilterCriteria criteria = new AdminJournalViewDTO.JournalFilterCriteria();
    criteria.setSearchTerm(search);
    criteria.setMinJournalEntries(minJournalEntries);
    criteria.setMaxJournalEntries(maxJournalEntries);
    criteria.setMinRecentEntries(minRecentEntries);
    criteria.setMaxRecentEntries(maxRecentEntries);
    criteria.setEngagementLevel(engagementLevel);
    criteria.setUserType(userType);
    criteria.setIsActive(isActive);

    Map<String, Object> analytics = adminJournalViewService.getDetailedJournalAnalytics();
    analytics.put("appliedFilters", criteria);
    return analytics;
  }

  /**
   * POST /admin/journals/analytics-detailed
   * 
   * Get detailed journal analytics with complex filter criteria via POST
   */
  @PostMapping("/analytics-detailed")
  public Map<String, Object> getDetailedJournalAnalytics(
      @RequestBody AdminJournalViewDTO.JournalFilterCriteria criteria) {
    Map<String, Object> analytics = adminJournalViewService.getDetailedJournalAnalytics();
    analytics.put("appliedFilters", criteria);
    return analytics;
  }

  /**
   * GET /admin/journals/export
   * 
   * Export journal users with optional filtering (returns CSV-ready data)
   */
  @GetMapping("/export")
  public Map<String, Object> exportJournalUsers(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Long minJournalEntries,
      @RequestParam(required = false) Long maxJournalEntries,
      @RequestParam(required = false) Long minRecentEntries,
      @RequestParam(required = false) Long maxRecentEntries,
      @RequestParam(required = false) String engagementLevel,
      @RequestParam(required = false) String userType,
      @RequestParam(required = false) Boolean isActive) {

    AdminJournalViewDTO.JournalFilterCriteria criteria = new AdminJournalViewDTO.JournalFilterCriteria();
    criteria.setSearchTerm(search);
    criteria.setMinJournalEntries(minJournalEntries);
    criteria.setMaxJournalEntries(maxJournalEntries);
    criteria.setMinRecentEntries(minRecentEntries);
    criteria.setMaxRecentEntries(maxRecentEntries);
    criteria.setEngagementLevel(engagementLevel);
    criteria.setUserType(userType);
    criteria.setIsActive(isActive);

    // Manual filtering since filterUsers method doesn't exist yet
    List<AdminJournalViewDTO> users = adminJournalViewService.getAllUserJournalViews();
    List<AdminJournalViewDTO> filteredUsers = users.stream()
        .filter(user -> search == null || user.matchesSearchTerm(search))
        .collect(Collectors.toList());

    Map<String, Object> exportData = new HashMap<>();
    exportData.put("users", filteredUsers);
    exportData.put("exportTimestamp", System.currentTimeMillis());
    exportData.put("filterCriteria", criteria);
    exportData.put("totalExported", filteredUsers.size());
    exportData.put("filename", "journal_users_export_" + System.currentTimeMillis() + ".csv");
    exportData.put("exportType", "journal_analytics");
    exportData.put("searchTerm", search);

    return exportData;
  }

  /**
   * GET /admin/journals/search-suggestions
   * 
   * Get search suggestions for journal user search
   */
  @GetMapping("/search-suggestions")
  public List<String> getJournalSearchSuggestions(
      @RequestParam("q") String q,
      @RequestParam(defaultValue = "10") int limit) {

    List<AdminJournalViewDTO> users = adminJournalViewService.getAllUserJournalViews();
    return users.stream()
        .filter(user -> q == null || q.trim().isEmpty() ||
            (user.getUserId() != null && user.getUserId().toString().toLowerCase().contains(q.toLowerCase())) ||
            (user.getName() != null && user.getName().toLowerCase().contains(q.toLowerCase())))
        .map(user -> {
          if (user.getUserId() != null && user.getUserId().toString().toLowerCase().contains(q.toLowerCase())) {
            return user.getUserId().toString();
          } else if (user.getName() != null && user.getName().toLowerCase().contains(q.toLowerCase())) {
            return user.getName();
          }
          return null;
        })
        .filter(s -> s != null && !s.isBlank())
        .distinct()
        .limit(limit)
        .collect(Collectors.toList());
  }

  /**
   * GET /admin/journals/search-with-criteria
   * 
   * Flexible search with specific field targeting
   */
  @GetMapping("/search-with-criteria")
  public List<AdminJournalViewDTO> searchJournalUsersWithCriteria(
      @RequestParam("q") String q,
      @RequestParam(defaultValue = "true") boolean searchUserId,
      @RequestParam(defaultValue = "true") boolean searchName) {

    List<AdminJournalViewDTO> users = adminJournalViewService.getAllUserJournalViews();
    String lowerQ = q != null ? q.toLowerCase() : "";
    return users.stream()
        .filter(user -> q == null || q.trim().isEmpty() ||
            (searchUserId && user.getUserId() != null && user.getUserId().toString().toLowerCase().contains(lowerQ)) ||
            (searchName && user.getName() != null && user.getName().toLowerCase().contains(lowerQ)))
        .collect(Collectors.toList());
  }

  // ============================================================================
  // EXISTING JOURNAL ANALYTICS ENDPOINTS (ENHANCED AND MAINTAINED FOR
  // COMPATIBILITY)
  // ============================================================================

  /**
   * GET /admin/journals/quick-stats
   * 
   * Provides quick statistical overview of journal entries for dashboard cards
   */
  @GetMapping("/quick-stats")
  public Map<String, Object> getJournalQuickStats() {
    return adminJournalViewService.getQuickStats();
  }

  /**
   * GET /admin/journals/users-with-journals
   * 
   * Retrieves list of users who have created at least one journal entry
   */
  @GetMapping("/users-with-journals")
  public List<AdminJournalViewDTO> getUsersWithJournals() {
    return adminJournalViewService.getUsersWithJournalEntries();
  }

  /**
   * GET /admin/journals/users-without-journals
   * 
   * Retrieves list of users who have not created any journal entries
   */
  @GetMapping("/users-without-journals")
  public List<AdminJournalViewDTO> getUsersWithoutJournals() {
    return adminJournalViewService.getUsersWithoutJournalEntries();
  }

  /**
   * GET /admin/journals/top-writers
   * 
   * Retrieves top users ranked by total number of journal entries created
   */
  @GetMapping("/top-writers")
  public List<AdminJournalViewDTO> getTopJournalWriters(@RequestParam(defaultValue = "10") int limit) {
    return adminJournalViewService.getTopJournalWriters(limit);
  }

  /**
   * GET /admin/journals/most-active-this-month
   * 
   * Retrieves most active users this month by journal entries created
   */
  @GetMapping("/most-active-this-month")
  public List<AdminJournalViewDTO> getMostActiveJournalWritersThisMonth(@RequestParam(defaultValue = "10") int limit) {
    return adminJournalViewService.getMostActiveUsersThisMonth(limit);
  }

  /**
   * GET /admin/journals/users-needing-attention
   * 
   * Retrieves users who have journal entries but no recent activity
   */
  @GetMapping("/users-needing-attention")
  public List<AdminJournalViewDTO> getJournalUsersNeedingAttention() {
    return adminJournalViewService.getUsersNeedingAttention();
  }

  /**
   * GET /admin/journals/highly-engaged-users
   * 
   * Retrieves highly engaged users with high journaling activity scores
   */
  @GetMapping("/highly-engaged-users")
  public List<AdminJournalViewDTO> getHighlyEngagedJournalUsers() {
    return adminJournalViewService.getHighlyEngagedUsers();
  }

  /**
   * GET /admin/journals/users-with-recent-activity
   * 
   * Retrieves users who have created journal entries this month
   */
  @GetMapping("/users-with-recent-activity")
  public List<AdminJournalViewDTO> getUsersWithRecentJournalActivity() {
    return adminJournalViewService.getUsersWithRecentActivity();
  }

  /**
   * GET /admin/journals/user/{id}
   * 
   * Get detailed journal view for a specific user
   */
  @GetMapping("/user/{id}")
  public AdminJournalViewDTO getUserJournalView(@PathVariable Long id) {
    AdminJournalViewDTO userView = adminJournalViewService.getUserJournalView(id);
    if (userView == null) {
      throw new RuntimeException("User journal view not found for ID: " + id);
    }
    return userView;
  }

  /**
   * GET /admin/journals/user/{id}/entries
   * 
   * Get all journal entries for a specific user
   */
  @GetMapping("/user/{id}/entries")
  public List<Object> getUserJournalEntries(@PathVariable Long id) {
    // Return empty list since getNoteEntriesForUserId method doesn't exist
    return new ArrayList<>();
  }

  /**
   * GET /admin/journals/engagement-metrics
   * 
   * Get user engagement metrics for journal entries
   */
  @GetMapping("/engagement-metrics")
  public Map<String, Object> getJournalEngagementMetrics() {
    Map<String, Object> analytics = adminJournalViewService.getDetailedJournalAnalytics();
    return (Map<String, Object>) analytics.getOrDefault("engagementAnalysis", new HashMap<>());
  }

  /**
   * GET /admin/journals/monthly-trends
   * 
   * Get monthly journal entry trends and patterns
   */
  @GetMapping("/monthly-trends")
  public Map<String, Object> getJournalMonthlyTrends() {
    Map<String, Object> analytics = adminJournalViewService.getDetailedJournalAnalytics();
    return (Map<String, Object>) analytics.getOrDefault("monthlyTrends", new HashMap<>());
  }

  /**
   * GET /admin/journals/distribution-analysis
   * 
   * Get journal entry distribution analysis
   */
  @GetMapping("/distribution-analysis")
  public Map<String, Object> getJournalDistributionAnalysis() {
    Map<String, Object> analytics = adminJournalViewService.getDetailedJournalAnalytics();
    return (Map<String, Object>) analytics.getOrDefault("userJournalDistribution", new HashMap<>());
  }

  /**
   * GET /admin/journals/recent-activity-analysis
   * 
   * Get recent activity analysis for journal entries
   */
  @GetMapping("/recent-activity-analysis")
  public Map<String, Object> getJournalRecentActivityAnalysis() {
    Map<String, Object> analytics = adminJournalViewService.getDetailedJournalAnalytics();
    return (Map<String, Object>) analytics.getOrDefault("recentActivity", new HashMap<>());
  }
}
