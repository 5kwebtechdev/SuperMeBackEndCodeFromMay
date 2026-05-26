package com.superme.admin.controller;

import com.superme.admin.dto.AdminDashboardKpiResponse;
import com.superme.admin.dto.RecentActivityDTO;
import com.superme.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/dashboard")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    /**
     * GET /v1/admin/dashboard?page=0&size=10
     *
     * Returns KPI cards + paginated recent activities in a single response.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getDashboard(
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(name = "limit", defaultValue = "10") int size) {
        Map<String, Object> response = new HashMap<>();
        try {
            AdminDashboardKpiResponse kpi        = adminDashboardService.getKpiCards();
            Map<String, Object>       activities = adminDashboardService.getRecentActivities(page, size);

            Map<String, Object> data = new HashMap<>();
            data.put("kpi", kpi);
            data.put("recentActivities", activities);

            response.put("success", true);
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to load dashboard: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}