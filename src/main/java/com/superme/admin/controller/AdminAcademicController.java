package com.superme.admin.controller;

import com.superme.dto.AddCourseMultipartRequest;
import com.superme.dto.AdminAcademicOverviewResponse;
import com.superme.dto.AdminCourseDetailResponse;
import com.superme.dto.CreateCourseRequest;
import com.superme.dto.StatusUpdateRequest;
import com.superme.enums.AgeGroup;
import com.superme.enums.Status;
import com.superme.model.Course;
import com.superme.service.AdminAcademicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AdminAcademicController handles all academic course operations for the admin
 * panel.
 */
@RestController
@RequestMapping("/admin/academic")
public class AdminAcademicController {

    @Autowired
    private AdminAcademicService adminAcademicService;

    public AdminAcademicController(AdminAcademicService adminAcademicService) {
        this.adminAcademicService = adminAcademicService;
    }

    // ============================================================================
    // ADMIN OVERVIEW WITH DATA TABLES AND STATS
    // ============================================================================

    /**
     * GET /admin/academic/overview
     * 
     * Enhanced academic overview with comprehensive filtering support
     * Data Table Fields: id, course name, description, duration, age group, no of
     * lessons,
     * difficulty, format, status, created at, last updated
     * Stats Cards: total courses, total lessons, total categories, average
     * duration, courses in draft
     */
    @GetMapping("/overview")
    public AdminAcademicOverviewResponse getAcademicOverview(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) AgeGroup ageGroup,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false, defaultValue = "true") boolean enabled) {

        return adminAcademicService.getAcademicOverview(search, difficulty, ageGroup, status, category, limit, offset,enabled);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchCourses(
            @RequestParam(required = false) String courseName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String ageGroup,
            @RequestParam(required = false) String format,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer duration,
            @RequestParam(required = false) Integer totalCoins,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Map<String, Object> result = adminAcademicService.searchCourses(
                courseName, description, category, difficulty, ageGroup,
                format, status, duration, totalCoins, page, size
        );
        return ResponseEntity.ok(result);
    }

    /**
     * GET /admin/academic/quick-stats
     * 
     * Quick statistics for dashboard cards
     * Returns: Total Courses, Total Lessons, Total Categories, Average Duration,
     * Courses in Draft
     */
    @GetMapping("/quick-stats")
    public Map<String, Object> getQuickAcademicStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            Map<String, Object> academicStats = adminAcademicService.getAcademicStatistics();

            stats.put("totalCourses", academicStats.get("totalCourses"));
            stats.put("totalLessons", academicStats.get("totalLessons"));
            stats.put("totalCategories", academicStats.get("totalCategories"));
            stats.put("averageDuration", academicStats.get("averageDuration"));
            stats.put("coursesInDraft", academicStats.get("coursesInDraft"));
            stats.put("success", true);
            stats.put("timestamp", System.currentTimeMillis());
        } catch (Exception e) {
            stats.put("success", false);
            stats.put("error", e.getMessage());
        }

        return stats;
    }
    /**
     * GET /admin/academic/filter-options
     * 
     * Get available filter options for UI dropdowns
     */
    @GetMapping("/filter-options")
    public Map<String, Object> getAcademicFilterOptions() {
        Map<String, Object> options = new HashMap<>();

        try {
            options.put("difficulties", adminAcademicService.getDifficultyOptions());
            options.put("ageGroups", adminAcademicService.getAgeGroupOptions());
            options.put("statuses", adminAcademicService.getStatusOptions());
            options.put("categories", adminAcademicService.getCategoryOptions());
            options.put("success", true);
        } catch (Exception e) {
            options.put("success", false);
            options.put("error", e.getMessage());
        }

        return options;
    }

    // ============================================================================
    // EXISTING COURSE MANAGEMENT METHODS
    // ============================================================================

    /**
     * POST /admin/academic/add-course
     *
     * Create a new course with lessons via multipart form data.
     * Supports thumbnail, attachment, per-lesson thumbnails (lessonThumbnail_N),
     * and per-lesson content images (lessonContentImage_N_M).
     */
    @PostMapping(value = "/add-course", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addNewCourse(
            @ModelAttribute AddCourseMultipartRequest request,
            MultipartHttpServletRequest multipartRequest) {
        try {
            Long courseId = adminAcademicService.createCourseFromMultipart(request, multipartRequest);
            return ResponseEntity.ok(Map.of(
                    "courseId", courseId,
                    "success", true,
                    "message", "Course created successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/course/{id}")
    public ResponseEntity<?> getCourseById(@PathVariable Long id) {
        try {
            AdminCourseDetailResponse response = adminAcademicService.getCourseById(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/edit-course")
    public ResponseEntity<?> updateCourse(@RequestBody CreateCourseRequest request) {
        try {
            Long courseId = adminAcademicService.createOrUpdateCourse(request);
            return ResponseEntity.ok(Map.of(
                    "courseId", courseId,
                    "success", true,
                    "message", "Course edited successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/bulk-save")
    public ResponseEntity<?> bulkSaveCourses(@RequestBody List<CreateCourseRequest> requests) {
        try {
            List<Long> savedIds = adminAcademicService.createOrUpdateCourses(requests);
            return ResponseEntity.ok(Map.of(
                    "courseIds", savedIds,
                    "success", true,
                    "message", "Courses processed successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }


    /**
     * POST /admin/academic/courses/{id}/send-for-verification
     * 
     * Send course for verification
     */
    @PostMapping("/courses/{id}/send-for-verification")
    public Map<String, Object> sendForVerification(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            adminAcademicService.sendCourseForVerification(id);
            response.put("success", true);
            response.put("message", "Course sent for verification successfully");
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    /**
     * POST /admin/academic/courses/{id}/save-as-draft
     * 
     * Save course as draft
     */
    @PostMapping("/courses/{id}/save-as-draft")
    public Map<String, Object> saveAsDraft(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            adminAcademicService.saveCourseAsDraft(id);
            response.put("success", true);
            response.put("message", "Course saved as draft successfully");
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }


    @GetMapping("/courses/options")
    public Map<String, Object> getCourseOptions() {
        Map<String, Object> options = new HashMap<>();

        try {
            options.put("categories", adminAcademicService.getCategoryOptions());
            options.put("difficulties", adminAcademicService.getDifficultyOptions());
            options.put("formats", adminAcademicService.getFormatOptions());
            options.put("ageGroups", adminAcademicService.getAgeGroupOptions());
            options.put("success", true);
        } catch (Exception e) {
            options.put("success", false);
            options.put("error", e.getMessage());
        }

        return options;
    }

    /**
     * GET /admin/academic/courses
     * 
     * Get all courses for admin overview
     */
    @GetMapping("/courses")
    public Map<String, Object> getAllCourses() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<Map<String, Object>> courses = adminAcademicService.getAllCourses();
            response.put("courses", courses);
            response.put("totalCount", courses.size());
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    /**
     * GET /admin/academic/courses/statistics
     * 
     * Get course statistics for dashboard
     */
    @GetMapping("/courses/statistics")
    public Map<String, Object> getCourseStatistics() {
        return adminAcademicService.getCourseStatistics();
    }


    @PutMapping("/update-status")
    public ResponseEntity<?> updateStatus(@RequestBody StatusUpdateRequest request) {
        Course updated = adminAcademicService.updateCourseStatus(request);
        return ResponseEntity.ok()
                .body(Map.of("success", true,
                        "message", "Course status updated successfully",
                        "courseId", updated.getId(),
                        "status", updated.getStatus()));
    }

    @PutMapping("/delete-course")
    public ResponseEntity<Map<String, String>> softDeleteCourse(@RequestParam Long id) {
        adminAcademicService.softDeleteCourse(id);
        return ResponseEntity.ok(Map.of("message", "Course deleted successfully"));
    }
    @Value("${file.upload-dir}")
    private String uploadDir;
    @GetMapping("/download/image/{filePath:.+}")
    public ResponseEntity<byte[]> downloadImage(@PathVariable String filePath) {
        try {
            Path path = Paths.get(uploadDir, filePath.replace("/", "\\"));

            if (!Files.exists(path)) {
                throw new RuntimeException("File not found: " + filePath);
            }

            byte[] fileBytes = Files.readAllBytes(path);

            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .header("Content-Disposition", "inline; filename=\"" + path.getFileName() + "\"")
                    .body(fileBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error while downloading file", e);
        }
    }



}
