package com.superme.admin.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import com.superme.dto.*;
import com.superme.model.User;
import com.superme.service.AdminUserViewService;
import com.superme.service.AdminUserCrudService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AdminUserController handles all user management operations for the admin
 * panel.
 * 
 * This controller provides comprehensive user management functionality
 * including:
 * - User CRUD operations (Create, Read, Update, Delete)
 * - User analytics and filtering
 * - User search and export capabilities
 * 
 * @author MindfullB Admin Team
 * @version 2.0
 */
@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

  private final AdminUserViewService adminUserViewService;
  private final AdminUserCrudService adminUserCrudService;

  public AdminUserController(AdminUserViewService adminUserViewService, AdminUserCrudService adminUserCrudService) {
    this.adminUserViewService = adminUserViewService;
    this.adminUserCrudService = adminUserCrudService;
  }

  // ============================================================================
  // ENHANCED USER MANAGEMENT ENDPOINTS WITH FILTERING
  // ============================================================================

  /**
   * GET /admin/users/overview
   * 
   * Enhanced version with comprehensive filtering support
   */
  @GetMapping("/overview")
  public AdminUserOverviewResponse getAllUserViews(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) List<String> gender,
      @RequestParam(required = false) List<String> relationship,
      @RequestParam(required = false) Boolean activeOnly,
      @RequestParam(required = false) Boolean inactiveOnly) {

    List<AdminUserViewDTO> users = adminUserViewService.getAllUserViews();
    int originalCount = users.size();

    AdminUserViewDTO.FilterCriteria criteria = new AdminUserViewDTO.FilterCriteria();
    criteria.setSearchTerm(search);
    criteria.setGenders(gender);
    criteria.setRelationships(relationship);
    criteria.setActiveOnly(activeOnly);
    criteria.setInactiveOnly(inactiveOnly);

    UserStatisticsDto stats = adminUserViewService.getUserStatistics();

    return new AdminUserOverviewResponse(users, stats, criteria, originalCount);
  }

  /**
   * GET /admin/users/search
   * 
   * Enhanced search with pagination support
   */
  @GetMapping("/search")
  public Map<String, Object> searchUsers(
      @RequestParam("q") String q,
      @RequestParam(defaultValue = "50") int limit,
      @RequestParam(required = false) List<String> relationships,
      @RequestParam(defaultValue = "0") int offset) {

    AdminUserViewDTO.FilterCriteria criteria = new AdminUserViewDTO.FilterCriteria();
    criteria.setSearchTerm(q);
    criteria.setRelationships(relationships);
    List<AdminUserViewDTO> filteredUsers = adminUserViewService.filterUsersWithPagination(criteria, limit, offset);
    long totalCount = adminUserViewService.getFilterResultsCount(criteria);

    Map<String, Object> response = new HashMap<>();
    response.put("users", filteredUsers);
    response.put("totalCount", totalCount);
    response.put("displayedCount", filteredUsers.size());
    response.put("limit", limit);
    response.put("offset", offset);
    response.put("hasMore", (offset + limit) < totalCount);

    return response;
  }

  /**
   * GET /admin/users/filter-options
   * 
   * Get available filter options for the frontend
   */
  @GetMapping("/filter-options")
  public Map<String, Object> getFilterOptions() {
    return adminUserViewService.getAvailableFilterOptions();
  }

  /**
   * GET /admin/users/filter-statistics
   * 
   * Get filter statistics for current user base
   */
  @GetMapping("/filter-statistics")
  public AdminUserViewDTO.FilterStatistics getFilterStatistics() {
    return adminUserViewService.getFilterStatistics();
  }

  /**
   * POST /admin/users/advanced-filter
   * 
   * Advanced filtering with request body for complex criteria
   */
  @PostMapping("/advanced-filter")
  public AdminUserOverviewResponse advancedFilter(@RequestBody AdminUserViewDTO.FilterCriteria criteria) {
    List<AdminUserViewDTO> users = adminUserViewService.getAllUserViews();
    int originalCount = users.size();

    List<AdminUserViewDTO> filteredUsers = adminUserViewService.filterUsers(criteria);
    UserStatisticsDto stats = adminUserViewService.getUserStatistics();

    return new AdminUserOverviewResponse(filteredUsers, stats, criteria, originalCount);
  }

  /**
   * GET /admin/users/analytics
   * 
   * Get comprehensive user analytics with optional filtering
   */
  @GetMapping("/analytics")
  public Map<String, Object> getUserAnalytics(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) List<String> gender,
      @RequestParam(required = false) List<String> relationship,
      @RequestParam(required = false) Boolean activeOnly) {

    AdminUserViewDTO.FilterCriteria criteria = new AdminUserViewDTO.FilterCriteria();
    criteria.setSearchTerm(search);
    criteria.setGenders(gender);
    criteria.setRelationships(relationship);
    criteria.setActiveOnly(activeOnly);

    return adminUserViewService.getDetailedAnalyticsWithFilters(criteria);
  }

  /**
   * POST /admin/users/analytics-detailed
   * 
   * Get detailed analytics with complex filter criteria via POST
   */
  @PostMapping("/analytics-detailed")
  public Map<String, Object> getDetailedAnalytics(@RequestBody AdminUserViewDTO.FilterCriteria criteria) {
    return adminUserViewService.getDetailedAnalyticsWithFilters(criteria);
  }

  /**
   * GET /admin/users/export
   * 
   * Export users with optional filtering (returns CSV-ready data)
   */
  @GetMapping("/export")
  public Map<String, Object> exportUsers(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) List<String> gender,
      @RequestParam(required = false) List<String> relationship,
      @RequestParam(required = false) Boolean activeOnly) {

    AdminUserViewDTO.FilterCriteria criteria = new AdminUserViewDTO.FilterCriteria();
    criteria.setSearchTerm(search);
    criteria.setGenders(gender);
    criteria.setRelationships(relationship);
    criteria.setActiveOnly(activeOnly);

    List<AdminUserViewDTO> users = adminUserViewService.filterUsers(criteria);

    Map<String, Object> exportData = new HashMap<>();
    exportData.put("users", users);
    java.time.LocalDateTime exportTimestamp = java.time.LocalDateTime.now();
    exportData.put("exportTimestamp", exportTimestamp);
    exportData.put("filterCriteria", criteria);
    exportData.put("totalExported", users.size());
    exportData.put("filename",
        "users_export_" + exportTimestamp.toString().replace(":", "-").replace("T", "_") + ".csv");

    return exportData;
  }

  // ============================================================================
  // BASIC USER CRUD ENDPOINTS
  // ============================================================================

  /**
   * POST /admin/users
   * 
   * Creates a new user in the system
   */
  @PostMapping("/create")
  public User createUser(@RequestBody AdminCreateUserDTO userDto) {
    return adminUserCrudService.createUser(userDto);
  }

  /**
   * GET /admin/users/{id}
   * 
   * Retrieves a specific user by their ID
   */
  @GetMapping("/{id}")
  public User getUser(@PathVariable Long id) {
    return adminUserCrudService.getUserById(id)
        .orElseThrow(() -> new RuntimeException("User not found"));
  }

  /**
   * PUT /admin/users/{id}
   * 
   * Updates an existing user's information
   */
  @PutMapping("/{id}")
  public User updateUser(@PathVariable Long id, @RequestBody User user) {
    return adminUserCrudService.updateUser(id, user);
  }

  /**
   * DELETE /admin/users/{id}
   * 
   * Permanently deletes a user from the system
   */
  @DeleteMapping("/{id}")
  public void deleteUser(@PathVariable Long id) {

    adminUserCrudService.deleteUser(id);
  }



  @PostMapping("/search")
  public ResponseEntity<AdminSearchResponseDto> searchUsers(
          @RequestBody UserSearchRequestDto request,
          @RequestParam(defaultValue = "0") int page,
          @RequestParam(defaultValue = "50") int size,
          @RequestParam(defaultValue = "id") String sortBy,
          @RequestParam(defaultValue = "ASC") String direction
  ) {
    AdminSearchResponseDto result = adminUserViewService.searchUsersAdmin(request, page, size, sortBy, direction);
    return ResponseEntity.ok(result);
  }
}


