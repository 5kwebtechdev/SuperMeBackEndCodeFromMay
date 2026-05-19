package com.superme.admin.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import com.superme.admin.dto.BaseUserResponseDTO;
import com.superme.admin.dto.IndividualUserRegisterDTO;
import com.superme.admin.dto.IndividualUserResponseDTO;
import com.superme.admin.service.IndividualUserService;
import com.superme.dto.*;
import com.superme.model.User;
import com.superme.service.AdminUserViewService;
import com.superme.service.AdminUserCrudService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.superme.controller.LoginController.log;

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
  public ResponseEntity<BaseUserResponseDTO> getUserBasicInfo(@PathVariable Long id) {
      log.info("REST request to get user basic info with ID: {}", id);
      BaseUserResponseDTO response = adminUserCrudService.getUserBasicInfo(id);
      return ResponseEntity.ok(response);
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



  @PostMapping("/{id}/reset-password")
  public ResponseEntity<Map<String, Object>> resetPassword(
      @PathVariable Long id,
      @RequestBody Map<String, String> body) {
    String password = body.get("password");
    adminUserCrudService.resetUserPassword(id, password);
    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("message", "Password reset successfully");
    response.put("userId", id);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/deactivate")
  public ResponseEntity<Map<String, Object>> deactivateUser(@PathVariable Long id) {
    adminUserCrudService.deactivateUser(id);
    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("message", "User deactivated successfully");
    response.put("userId", id);
    response.put("enabled", false);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/activate")
  public ResponseEntity<Map<String, Object>> activateUser(@PathVariable Long id) {
    adminUserCrudService.activateUser(id);
    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("message", "User activated successfully");
    response.put("userId", id);
    response.put("enabled", true);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/download-excel")
  public ResponseEntity<byte[]> downloadExcel(@RequestParam(required = false) String q) throws IOException {
    byte[] excel = adminUserViewService.generateUsersExcel(q);
    String filename = "users-" + LocalDate.now() + ".xlsx";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(excel);
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










  @Autowired
  private IndividualUserService individualUserService;

  @PostMapping("/register/individualuser")
  public ResponseEntity<?> registerIndividualUser(@Valid @RequestBody IndividualUserRegisterDTO dto) {
    try {
      User registeredUser = individualUserService.registerIndividualUser(dto);

      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("message", "User registered successfully");
      response.put("userId", registeredUser.getId());
      response.put("email", registeredUser.getEmail());
      response.put("name", registeredUser.getName());

      return ResponseEntity.status(HttpStatus.CREATED).body(response);

    } catch (RuntimeException e) {
      Map<String, String> errorResponse = new HashMap<>();
      errorResponse.put("success", "false");
      errorResponse.put("error", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
  }



// Add to IndividualUserRegistrationController.java

  @GetMapping("/individualuser/{id}")
  public ResponseEntity<?> getIndividualUserById(@PathVariable Long id) {
    try {
      IndividualUserResponseDTO user = individualUserService.getIndividualUserById(id);
      return ResponseEntity.ok(user);
    } catch (RuntimeException e) {
      Map<String, String> errorResponse = new HashMap<>();
      errorResponse.put("success", "false");
      errorResponse.put("error", e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
  }

}


