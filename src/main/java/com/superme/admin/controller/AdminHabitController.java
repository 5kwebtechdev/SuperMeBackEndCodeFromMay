package com.superme.admin.controller;

import java.util.Map;
import java.util.List;

import com.superme.dto.AdminHabitViewDTO;
import com.superme.dto.AdminHabitOverviewResponse;
import com.superme.service.AdminHabitViewService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * AdminHabitController handles all habit analytics operations for the admin
 * panel.
 * 
 * This controller provides comprehensive habit management functionality
 * including:
 * - Habit analytics and overview
 * - User habit statistics and trends
 * 
 * @author MindfullB Admin Team
 * @version 2.0
 */
@RestController
@RequestMapping("/admin/habits")
public class AdminHabitController {

  @Autowired
  private AdminHabitViewService adminHabitViewService;

  // ============================================================================
  // HABIT ANALYTICS ENDPOINTS
  // ============================================================================

  /**
   * GET /admin/habits/overview
   * 
   * Provides comprehensive overview of all user habits.
   * Includes per-user habit statistics and global habit metrics
   * such as total habits created, completion rates, and trends.
   * 
   * @return AdminHabitOverviewResponse with user habit data and statistics
   */
  @GetMapping("/overview")
  public AdminHabitOverviewResponse getAllUserHabitViews() {
    List<AdminHabitViewDTO> users = adminHabitViewService.getAllUserHabitViews();
    AdminHabitViewDTO.HabitStatistics stats = adminHabitViewService.getHabitStatistics();
    // Username has been removed from DTOs and responses
    return new AdminHabitOverviewResponse(users, stats);
  }

  /**
   * GET /admin/habits/analytics
   * 
   * Provides detailed analytical data about habits including:
   * - Top performers by completion rate
   * - Habit distribution by priority/category
   * - Completion trends over time
   * - Monthly statistics
   * 
   * @return Map containing detailed habit analytics data
   */
  @GetMapping("/analytics")
  public ResponseEntity<?> getHabitAnalytics() {
      try {
          Map<String, Object> analytics = adminHabitViewService.getDetailedHabitAnalytics();
          return ResponseEntity.ok(analytics);
      } catch (Exception e) {
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(Map.of(
                          "error", "Failed to fetch analytics",
                          "message", e.getMessage(),
                          "timestamp", System.currentTimeMillis()
                  ));
      }
  }
}
