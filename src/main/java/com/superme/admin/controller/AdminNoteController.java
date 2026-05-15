package com.superme.admin.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.superme.dto.AdminNoteViewDTO;
import com.superme.dto.AdminNoteOverviewResponse;
import com.superme.service.AdminNoteViewService;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;


@RestController
@RequestMapping("/admin/notes")
public class AdminNoteController {

  @Autowired
  private AdminNoteViewService adminNoteViewService;


  @GetMapping("/overview")
  public AdminNoteOverviewResponse getAllUserNoteViews(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Long minNoteCount,
      @RequestParam(required = false) Long maxNoteCount,
      @RequestParam(required = false) Long minRecentNotes,
      @RequestParam(required = false) Long maxRecentNotes,
      @RequestParam(required = false) String engagementLevel,
      @RequestParam(required = false) String userType,
      @RequestParam(required = false) Boolean isActive,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    List<AdminNoteViewDTO> users = adminNoteViewService.getAllUserNoteViews();
    AdminNoteViewDTO.NoteStatistics stats = adminNoteViewService.getNoteStatistics();

    AdminNoteOverviewResponse.NoteFilterCriteria criteria = new AdminNoteOverviewResponse.NoteFilterCriteria();
    criteria.setSearchTerm(search);
    criteria.setMinNoteCount(minNoteCount);
    criteria.setMaxNoteCount(maxNoteCount);

    // Filter users
    List<AdminNoteViewDTO> filteredUsers = users.stream()
        .filter(user -> search == null || search.trim().isEmpty() ||
            (user.getUserId() != null && user.getUserId().toString().toLowerCase().contains(search.toLowerCase())))
        .filter(user -> minNoteCount == null || user.getTotalNotes() >= minNoteCount)
        .filter(user -> maxNoteCount == null || user.getTotalNotes() <= maxNoteCount)
        .collect(Collectors.toList());

    int totalFiltered = filteredUsers.size();
    int totalPages = size > 0 ? (int) Math.ceil((double) totalFiltered / size) : 1;
    int start = page * size;
    int end = Math.min(start + size, totalFiltered);
    List<AdminNoteViewDTO> pageContent = start >= totalFiltered
        ? new ArrayList<>()
        : filteredUsers.subList(start, end);

    AdminNoteOverviewResponse response = new AdminNoteOverviewResponse(pageContent, stats, criteria, users.size());
    response.setCurrentPage(page);
    response.setPageSize(size);
    response.setTotalPages(totalPages);
    response.setHasNext((page + 1) < totalPages);
    response.setHasPrevious(page > 0);
    response.setTotalResults(totalFiltered);
    response.setDisplayedResults(pageContent.size());

    return response;
  }

  /**
   * GET /admin/notes/search
   *
   * Enhanced note search with pagination support
   */
  @GetMapping("/search")
  public Map<String, Object> searchNoteUsers(
      @RequestParam("q") String q,
      @RequestParam(defaultValue = "50") int limit,
      @RequestParam(defaultValue = "0") int offset) {

    List<AdminNoteViewDTO> allUsers = adminNoteViewService.getAllUserNoteViews();
    List<AdminNoteViewDTO> searchResults = allUsers.stream()
        .filter(user -> q == null || q.trim().isEmpty() ||
            (user.getUserId() != null && user.getUserId().toString().toLowerCase().contains(q.toLowerCase())))
        .collect(Collectors.toList());

    // Apply pagination manually
    int start = Math.max(0, offset);
    int end = Math.min(searchResults.size(), start + limit);
    List<AdminNoteViewDTO> paginatedResults = start < searchResults.size() ? searchResults.subList(start, end)
        : new ArrayList<>();

    Map<String, Object> response = new HashMap<>();
    response.put("users", paginatedResults);
    response.put("totalCount", searchResults.size());
    response.put("displayedCount", paginatedResults.size());
    response.put("limit", limit);
    response.put("offset", offset);
    response.put("hasMore", (offset + limit) < searchResults.size());
    response.put("searchTerm", q);
    response.put("searchFields", "userId");

    return response;
  }

  /**
   * GET /admin/notes/filter-options
   *
   * Get available filter options for note filtering UI
   */
  @GetMapping("/filter-options")
  public Map<String, Object> getNoteFilterOptions() {
    Map<String, Object> options = new HashMap<>();

    options.put("userTypes", List.of(
        "with_notes", "without_notes", "active_users",
        "recent_creators", "needs_attention", "highly_engaged"));

    options.put("engagementLevels", List.of("high", "medium", "low", "none"));

    AdminNoteViewDTO.NoteStatistics stats = adminNoteViewService.getNoteStatistics();
    options.put("totalUsers", stats.getTotalUsers());
    options.put("totalNotes", stats.getTotalUsers()); // placeholder
    options.put("averageNotesPerUser", stats.getAverageNotesPerUser());
    options.put("usersWithNotes", stats.getUsersWithNotes());

    return options;
  }

  /**
   * GET /admin/notes/filter-statistics
   *
   * Get filter statistics for current note user base
   */
  @GetMapping("/filter-statistics")
  public AdminNoteOverviewResponse.FilterStatistics getNoteFilterStatistics() {
    return adminNoteViewService.getFilterStatistics();
  }

  /**
   * POST /admin/notes/advanced-filter
   *
   * Advanced note filtering with request body for complex criteria
   */
  @PostMapping("/advanced-filter")
  public AdminNoteOverviewResponse advancedNoteFilter(
      @RequestBody AdminNoteOverviewResponse.NoteFilterCriteria criteria) {
    List<AdminNoteViewDTO> users = adminNoteViewService.getAllUserNoteViews();
    int originalCount = users.size();

    List<AdminNoteViewDTO> filteredUsers = adminNoteViewService.filterUsers(criteria);
    AdminNoteViewDTO.NoteStatistics stats = adminNoteViewService.getNoteStatistics();

    return new AdminNoteOverviewResponse(filteredUsers, stats, criteria, originalCount);
  }

  /**
   * GET /admin/notes/analytics
   *
   * Enhanced note analytics with optional filtering support
   */
  @GetMapping("/analytics")
  public Map<String, Object> getNoteAnalytics(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Long minNoteCount,
      @RequestParam(required = false) Long maxNoteCount,
      @RequestParam(required = false) String engagementLevel,
      @RequestParam(required = false) String userType,
      @RequestParam(required = false) Boolean isActive) {

    AdminNoteOverviewResponse.NoteFilterCriteria criteria = new AdminNoteOverviewResponse.NoteFilterCriteria();
    criteria.setSearchTerm(search);
    criteria.setMinNoteCount(minNoteCount);
    criteria.setMaxNoteCount(maxNoteCount);

    return adminNoteViewService.getDetailedAnalyticsWithFilters(criteria);
  }

  /**
   * POST /admin/notes/analytics-detailed
   *
   * Get detailed note analytics with complex filter criteria via POST
   */
  @PostMapping("/analytics-detailed")
  public Map<String, Object> getDetailedNoteAnalytics(
      @RequestBody AdminNoteOverviewResponse.NoteFilterCriteria criteria) {
    return adminNoteViewService.getDetailedAnalyticsWithFilters(criteria);
  }

  /**
   * GET /admin/notes/export
   *
   * Export note users with optional filtering (returns CSV-ready data)
   */
  @GetMapping("/export")
  public Map<String, Object> exportNoteUsers(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Long minNoteCount,
      @RequestParam(required = false) Long maxNoteCount,
      @RequestParam(required = false) String engagementLevel,
      @RequestParam(required = false) String userType,
      @RequestParam(required = false) Boolean isActive) {

    AdminNoteOverviewResponse.NoteFilterCriteria criteria = new AdminNoteOverviewResponse.NoteFilterCriteria();
    criteria.setSearchTerm(search);
    criteria.setMinNoteCount(minNoteCount);
    criteria.setMaxNoteCount(maxNoteCount);

    List<AdminNoteViewDTO> users = adminNoteViewService.filterUsers(criteria);

    Map<String, Object> exportData = new HashMap<>();
    exportData.put("users", users);
    java.time.LocalDate exportDate = java.time.LocalDate.now();
    java.time.LocalTime exportTime = java.time.LocalTime.now();
    exportData.put("exportDate", exportDate);
    exportData.put("exportTime", exportTime);
    exportData.put("filename", "note_users_export_" + exportDate + "_" + exportTime.toSecondOfDay() + ".csv");
    exportData.put("filterCriteria", criteria);
    exportData.put("totalExported", users.size());
    exportData.put("exportType", "note_analytics");
    exportData.put("searchTerm", search);

    return exportData;
  }

  /**
   * GET /admin/notes/search-suggestions
   *
   * Get search suggestions for note user search
   */
  @GetMapping("/search-suggestions")
  public List<String> getNoteSearchSuggestions(
      @RequestParam("q") String q,
      @RequestParam(defaultValue = "10") int limit) {

    List<AdminNoteViewDTO> users = adminNoteViewService.getAllUserNoteViews();
    return users.stream()
        .filter(user -> q == null || q.trim().isEmpty() ||
            (user.getUserId() != null && user.getUserId().toString().toLowerCase().contains(q.toLowerCase())))
        .map(user -> user.getUserId() != null ? user.getUserId().toString() : "")
        .filter(userId -> userId.toLowerCase().contains(q.toLowerCase()))
        .distinct()
        .limit(limit)
        .collect(Collectors.toList());
  }

  /**
   * GET /admin/notes/search-with-criteria
   *
   * Flexible search with specific field targeting
   */
  @GetMapping("/search-with-criteria")
  public List<AdminNoteViewDTO> searchNoteUsersWithCriteria(
      @RequestParam("q") String q,
      @RequestParam(defaultValue = "true") boolean searchUserId,
      @RequestParam(defaultValue = "false") boolean searchUsername) {

    List<AdminNoteViewDTO> users = adminNoteViewService.getAllUserNoteViews();
    String lowerQ = q != null ? q.toLowerCase() : "";
    return users.stream()
        .filter(user -> q == null || q.trim().isEmpty() ||
            (searchUserId && user.getUserId() != null && user.getUserId().toString().toLowerCase().contains(lowerQ)))
        .collect(Collectors.toList());
  }

}