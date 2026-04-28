package com.superme.admin.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.superme.dto.AdminReelsDTO;
import com.superme.dto.AdminReelsOverviewResponse;
import com.superme.dto.CreateReelRequest;
import com.superme.dto.UpdateReelRequest;
import com.superme.service.AdminReelsService;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * AdminReelsController handles all reels analytics operations for the admin
 * panel.
 * 
 * This controller provides comprehensive reels management functionality
 * including:
 * - Reels overview with data tables (id, username, age, title, category,
 * duration, status, action taken by, created at, action taken on)
 * - Statistics cards (total reels, reels approved, pending reels, rejected
 * reels)
 * - Advanced search and filtering (by category, age, status)
 * - Search bar functionality (username, title, age, category)
 * - Admin CRUD operations (create, update, delete reels)
 * - Video upload to S3
 * 
 * @author MindfullB Admin Team
 * @version 2.0
 */
@RestController
@RequestMapping("/admin/reels")
public class AdminReelsController {

  @Autowired
  private AdminReelsService adminReelsService;

  // ============================================================================
  // ENHANCED REELS ANALYTICS ENDPOINTS WITH SEARCH AND FILTERING
  // ============================================================================

  /**
   * GET /admin/reels/overview
   * 
   * Enhanced reels overview with comprehensive filtering support
   * Data Table Fields: id, age, title, category, duration, status, action taken
   * by, created at, action taken on
   * Stats Cards: total reels, reels approved, pending reels, rejected reels
   */
  @GetMapping("/overview")
  public AdminReelsOverviewResponse getAllReelsViews(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) Integer age,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Integer minDuration,
      @RequestParam(required = false) Integer maxDuration) {

    List<AdminReelsDTO> reels = adminReelsService.getAllReelsViews();
    AdminReelsDTO.ReelsStatistics stats = adminReelsService.getReelsStatistics();

    AdminReelsDTO.ReelsFilterCriteria criteria = new AdminReelsDTO.ReelsFilterCriteria();
    criteria.setSearchTerm(search);
    criteria.setCategory(category);
    // criteria.setAge(age);
    criteria.setStatus(status);

    // Filter reels manually for now (username removed)
    List<AdminReelsDTO> filteredReels = reels.stream()
        .filter(reel -> reel.matchesSearchTerm(search))
        .filter(reel -> reel.matchesFilters(criteria))
        .filter(reel -> minDuration == null || reel.getDuration() >= minDuration)
        .filter(reel -> maxDuration == null || reel.getDuration() <= maxDuration)
        .collect(Collectors.toList());

    AdminReelsOverviewResponse response = new AdminReelsOverviewResponse(filteredReels, stats, criteria);
    response.setTotalCount(reels.size());
    response.setFilteredCount(filteredReels.size());
    response.setHasMore(false);

    return response;
  }

  /**
   * GET /admin/reels/quick-stats
   * 
   * Quick statistics for dashboard cards
   * Returns: Total Reels, Reels Approved, Pending Reels, Rejected Reels
   */
  @GetMapping("/quick-stats")
  public Map<String, Object> getQuickReelsStats() {
    Map<String, Object> stats = new HashMap<>();

    try {
      AdminReelsDTO.ReelsStatistics reelsStats = adminReelsService.getReelsStatistics();

      stats.put("totalReels", reelsStats.getTotalReels());
      stats.put("reelsApproved", reelsStats.getReelsApproved());
      stats.put("pendingReels", reelsStats.getPendingReels());
      stats.put("rejectedReels", reelsStats.getRejectedReels());
      stats.put("success", true);
      stats.put("timestamp", System.currentTimeMillis());
    } catch (Exception e) {
      stats.put("success", false);
      stats.put("error", e.getMessage());
    }

    return stats;
  }

  /**
   * GET /admin/reels/search
   * 
   * Search reels with pagination support
   * Search fields: title, age, category
   */
  @GetMapping("/search")
  public Map<String, Object> searchReels(
      @RequestParam("q") String query,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "0") int offset) {

    Map<String, Object> response = new HashMap<>();

    try {
      List<AdminReelsDTO> allReels = adminReelsService.getAllReelsViews();

      List<AdminReelsDTO> searchResults = allReels.stream()
          .filter(reel -> reel.matchesSearchTerm(query))
          .collect(Collectors.toList());

      // Apply pagination manually
      int start = Math.max(0, offset);
      int end = Math.min(searchResults.size(), start + limit);
      List<AdminReelsDTO> paginatedResults = start < searchResults.size() ? searchResults.subList(start, end)
          : new ArrayList<>();

      response.put("reels", paginatedResults);
      response.put("totalCount", searchResults.size());
      response.put("limit", limit);
      response.put("offset", offset);
      response.put("hasMore", (offset + limit) < searchResults.size());
      response.put("searchTerm", query);
      response.put("success", true);
    } catch (Exception e) {
      response.put("success", false);
      response.put("error", e.getMessage());
    }

    return response;
  }

  /**
   * GET /admin/reels/filter-options
   * 
   * Get available filter options for UI dropdowns
   */
  @GetMapping("/filter-options")
  public Map<String, Object> getReelsFilterOptions() {
    Map<String, Object> options = new HashMap<>();

    try {
      options.put("categories", adminReelsService.getAvailableCategories());
      options.put("statuses", adminReelsService.getAvailableStatuses());
      options.put("success", true);
    } catch (Exception e) {
      options.put("success", false);
      options.put("error", e.getMessage());
    }

    return options;
  }

  /**
   * GET /admin/reels/analytics
   * 
   * Detailed reels analytics with breakdowns
   */
  @GetMapping("/analytics")
  public Map<String, Object> getReelsAnalytics() {
    Map<String, Object> analytics = new HashMap<>();

    try {
      AdminReelsDTO.ReelsStatistics stats = adminReelsService.getReelsStatistics();
      Map<String, Object> breakdowns = adminReelsService.getReelsBreakdowns();

      analytics.put("statistics", stats);
      analytics.put("categoryBreakdown", breakdowns.get("categoryBreakdown"));
      analytics.put("statusBreakdown", breakdowns.get("statusBreakdown"));
      analytics.put("durationDistribution", breakdowns.get("durationDistribution"));
      analytics.put("success", true);
      analytics.put("timestamp", System.currentTimeMillis());
    } catch (Exception e) {
      analytics.put("success", false);
      analytics.put("error", e.getMessage());
    }

    return analytics;
  }

  // ============================================================================
  // ADMIN REEL MANAGEMENT (CRUD OPERATIONS)
  // ============================================================================

  /**
   * POST /admin/reels/create
   * 
   * Create/Add a new reel by admin
   */
  @PostMapping("/create")
  public Map<String, Object> createReel(@RequestBody CreateReelRequest request) {
    Map<String, Object> response = new HashMap<>();

    try {
      AdminReelsDTO createdReel = adminReelsService.createReel(request);
      response.put("reel", createdReel);
      response.put("success", true);
      response.put("message", "Reel created successfully");
    } catch (Exception e) {
      response.put("success", false);
      response.put("error", e.getMessage());
    }

    return response;
  }

  /**
   * PUT /admin/reels/{id}/update
   * 
   * Update an existing reel
   */
  @PutMapping("/{id}/update")
  public Map<String, Object> updateReel(@PathVariable Long id, @RequestBody UpdateReelRequest request) {
    Map<String, Object> response = new HashMap<>();

    try {
      AdminReelsDTO updatedReel = adminReelsService.updateReel(id, request);
      response.put("reel", updatedReel);
      response.put("success", true);
      response.put("message", "Reel updated successfully");
    } catch (Exception e) {
      response.put("success", false);
      response.put("error", e.getMessage());
    }

    return response;
  }

  /**
   * DELETE /admin/reels/{id}/delete
   * 
   * Delete a reel
   */
  @DeleteMapping("/{id}/delete")
  public Map<String, Object> deleteReel(@PathVariable Long id) {
    Map<String, Object> response = new HashMap<>();

    try {
      adminReelsService.deleteReel(id);
      response.put("success", true);
      response.put("message", "Reel deleted successfully");
    } catch (Exception e) {
      response.put("success", false);
      response.put("error", e.getMessage());
    }

    return response;
  }

  /**
   * POST /admin/reels/upload-video
   * 
   * Handle video upload to S3 and return video link
   */
  @PostMapping("/upload-video")
  public Map<String, Object> uploadVideo(@RequestParam("file") MultipartFile file) {
    Map<String, Object> response = new HashMap<>();

    try {
      String videoLink = adminReelsService.uploadVideoToS3(file);
      response.put("videoLink", videoLink);
      response.put("fileName", file.getOriginalFilename());
      response.put("fileSize", file.getSize());
      response.put("success", true);
      response.put("message", "Video uploaded successfully");
    } catch (Exception e) {
      response.put("success", false);
      response.put("error", e.getMessage());
    }

    return response;
  }

  // ============================================================================
  // REEL DETAILS AND APPROVAL ACTIONS
  // ============================================================================

  /**
   * GET /admin/reels/{id}
   * 
   * Get detailed reel information by ID
   */
  @GetMapping("/{id}")
  public Map<String, Object> getReelById(@PathVariable Long id) {
    Map<String, Object> response = new HashMap<>();

    try {
      AdminReelsDTO reel = adminReelsService.getReelById(id);
      response.put("reel", reel);
      response.put("success", true);
    } catch (Exception e) {
      response.put("success", false);
      response.put("error", e.getMessage());
    }

    return response;
  }

  /**
   * PUT /admin/reels/{id}/approve
   * 
   * Approve a reel
   */
  @PutMapping("/{id}/approve")
  public Map<String, Object> approveReel(@PathVariable Long id,
      @RequestParam(required = false) String comments) {
    Map<String, Object> response = new HashMap<>();

    try {
      adminReelsService.approveReel(id, comments);
      response.put("success", true);
      response.put("message", "Reel approved successfully");
      response.put("timestamp", System.currentTimeMillis());
    } catch (Exception e) {
      response.put("success", false);
      response.put("error", e.getMessage());
    }

    return response;
  }

  /**
   * PUT /admin/reels/{id}/reject
   * 
   * Reject a reel
   */
  @PutMapping("/{id}/reject")
  public Map<String, Object> rejectReel(@PathVariable Long id,
      @RequestParam(required = false) String comments) {
    Map<String, Object> response = new HashMap<>();

    try {
      adminReelsService.rejectReel(id, comments);
      response.put("success", true);
      response.put("message", "Reel rejected successfully");
      response.put("timestamp", System.currentTimeMillis());
    } catch (Exception e) {
      response.put("success", false);
      response.put("error", e.getMessage());
    }

    return response;
  }

  /**
   * PUT /admin/reels/{id}/pending
   * 
   * Mark reel as pending review
   */
  @PutMapping("/{id}/pending")
  public Map<String, Object> markReelPending(@PathVariable Long id,
      @RequestParam(required = false) String comments) {
    Map<String, Object> response = new HashMap<>();

    try {
      adminReelsService.markReelPending(id, comments);
      response.put("success", true);
      response.put("message", "Reel marked as pending review");
      response.put("timestamp", System.currentTimeMillis());
    } catch (Exception e) {
      response.put("success", false);
      response.put("error", e.getMessage());
    }

    return response;
  }

  // ============================================================================
  // SERVICE STATUS AND HEALTH CHECK
  // ============================================================================

  /**
   * GET /admin/reels/status
   * 
   * Get service status and health check
   */
  @GetMapping("/status")
  public Map<String, Object> getReelsServiceStatus() {
    Map<String, Object> status = new HashMap<>();

    try {
      AdminReelsDTO.ReelsStatistics stats = adminReelsService.getReelsStatistics();
      status.put("status", "healthy");
      status.put("totalReels", stats.getTotalReels());
      status.put("pendingReels", stats.getPendingReels());
      status.put("serviceVersion", "2.0");
      java.time.Instant instant = java.time.Instant.ofEpochMilli(System.currentTimeMillis());
      java.time.ZoneId zone = java.time.ZoneId.systemDefault();
      java.time.LocalDate localDate = instant.atZone(zone).toLocalDate();
      java.time.LocalTime localTime = instant.atZone(zone).toLocalTime();
      status.put("localDate", localDate);
      status.put("localTime", localTime);
      status.put("success", true);
    } catch (Exception e) {
      status.put("status", "error");
      status.put("error", e.getMessage());
      status.put("success", false);
    }

    return status;
  }

  /**
   * GET /admin/reels/export
   * 
   * Export reels data with optional filtering
   */
  @GetMapping("/export")
  public Map<String, Object> exportReelsData(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "csv") String format) {

    Map<String, Object> response = new HashMap<>();

    try {
      AdminReelsDTO.ReelsFilterCriteria criteria = new AdminReelsDTO.ReelsFilterCriteria();
      criteria.setSearchTerm(search);
      criteria.setCategory(category);
      criteria.setStatus(status);

      String exportData = adminReelsService.exportReelsData(criteria, format);

      response.put("data", exportData);
      response.put("format", format);
      response.put("filename", "reels_export_" + System.currentTimeMillis() + "." + format);
      response.put("success", true);
    } catch (Exception e) {
      response.put("success", false);
      response.put("error", e.getMessage());
    }

    return response;
  }
}
