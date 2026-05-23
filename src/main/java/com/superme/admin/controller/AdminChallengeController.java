package com.superme.admin.controller;

import com.superme.admin.dto.ChallengeApiResponse;
import com.superme.admin.dto.ChallengeResponseDTO;
import com.superme.admin.model.Admin;
import com.superme.admin.repository.AdminRepository;
import com.superme.dto.*;
import com.superme.enums.*;
import com.superme.exception.BusinessException;
import com.superme.exception.ResourceNotFoundException;
import com.superme.model.User;
import com.superme.repository.UserRepository;
import com.superme.service.AdminChallengeService;
import com.superme.service.ChallengeFileStorageService;
import com.superme.util.UserJwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Merged & optimized AdminChallengeController
 *
 * - Keeps latest DTOs (MultiQuestionChallengeRequestDTO / ResponseDTO)
 * - Adds analytics, overview, search, filter-options, export, bulk operations
 * - Uses ResponseEntity with consistent response map payloads
 */
@RestController
@RequestMapping("/admin/challenges")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class AdminChallengeController {

    private final AdminChallengeService adminChallengeService;
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final ChallengeFileStorageService challengeFileStorageService;

    // -----------------------
    // FILE DOWNLOADS
    // -----------------------

    /**
     * GET /admin/challenges/download/thumbnail/{filename}
     * Serves challenge thumbnail images stored under src/uploads/challenge/thumbnails/
     */
    @GetMapping("/download/thumbnail/{filename:.+}")
    public ResponseEntity<Resource> downloadThumbnail(@PathVariable String filename) {
        return serveFile(challengeFileStorageService.getThumbnailsDir(), filename);
    }

    /**
     * GET /admin/challenges/download/attachment/{filename}
     * Serves challenge attachment files stored under the configured challenge upload directory.
     */
    @GetMapping("/download/attachment/{filename:.+}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable String filename) {
        return serveFile(challengeFileStorageService.getAttachmentsDir(), filename);
    }

    private ResponseEntity<Resource> serveFile(String directory, String filename) {
        try {
            Path filePath = Paths.get(directory).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = "application/octet-stream";
            try {
                String probed = Files.probeContentType(filePath);
                if (probed != null) contentType = probed;
            } catch (Exception ignored) {}

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not read file: " + filename, e);
        }
    }

    // -----------------------
    // UPDATE (EDIT)
    // -----------------------

    /**
     * PUT /admin/challenges/edit
     *
     * Multipart form — all fields optional except challengeId.
     * - thumbnail absent  → keep existing thumbnail
     * - thumbnail present → delete old, store new
     * - removedAttachmentIds → delete those attachment files + DB rows
     * - attachments        → append new attachment files
     * - questions          → full replacement of all questions + options
     */
    @PutMapping(value = "/edit", consumes = {"multipart/form-data"})
    public ResponseEntity<Map<String, Object>> updateChallenge(
            @ModelAttribute MultiQuestionChallengeRequestDTO request,
            HttpServletRequest httpRequest) {

        Map<String, Object> response = new HashMap<>();
        try {
            String token = httpRequest.getHeader("Authorization");
            Admin admin = getAdminFromToken(token);

            ChallengeResponseDTO updated = adminChallengeService.updateChallenge(request, admin);
            response.put("success", true);
            response.put("message", "Challenge updated successfully");
            response.put("data", updated);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (BusinessException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Failed to update challenge: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // -----------------------
    // CREATE / BULK CREATE
    // -----------------------
    @PostMapping(value = "/add", consumes = {"multipart/form-data"})
    public ResponseEntity<Map<String, Object>> createChallenge(
            @ModelAttribute MultiQuestionChallengeRequestDTO request,
            HttpServletRequest httpRequest) {

        String token = httpRequest.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Admin admin = adminRepository.findById(Long.valueOf(request.getAdminId()))
                .orElseThrow(() -> new BusinessException("Admin not found"));
//        Admin admin = getAdminFromToken(token);

        Map<String, Object> response = new HashMap<>();
        try {
            ChallengeResponseDTO created = adminChallengeService.createChallenge(request, admin);
            response.put("success", true);
            response.put("message", "Challenge created successfully");
            response.put("data", created);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (BusinessException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Failed to create challenge: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }










    // Helper to extract User from Principal (userId only, no username fallback)
    private User getUserFromPrincipal(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));
    }

//    @PostMapping("/bulk-add")
//    public ResponseEntity<Map<String, Object>> bulkAddChallenge(@RequestBody List<MultiQuestionChallengeRequestDTO> challengeDTOs,
//                                                                Principal principal,
//                                                                HttpServletRequest httpRequest) {
//        Map<String, Object> response = new HashMap<>();
////        User user = getUserFromPrincipal(principal);
//
//
//        String token = httpRequest.getHeader("Authorization");
//        Admin user = getAdminFromToken(token);
//        try {
//            List<MultiQuestionChallengeResponseDTO> savedList = adminChallengeService.addChallengeList(challengeDTOs,user);
//            response.put("success", true);
//            response.put("message", "Challenges added successfully");
//            response.put("challenges", savedList);
//            response.put("count", savedList.size());
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            response.put("success", false);
//            response.put("error", e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//        }
//    }

    // -----------------------
    // READ (single / all)
    // -----------------------
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getChallenge(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            ChallengeResponseDTO challenge = adminChallengeService.getChallengeById(id);
            response.put("success", true);
            response.put("data", challenge);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to get challenge: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

//    @GetMapping
//    public ResponseEntity<Map<String, Object>> getAllChallenges() {
//        Map<String, Object> response = new HashMap<>();
//        try {
//            List<MultiQuestionChallengeResponseDTO> challenges = adminChallengeService.getAllChallenges();
//            response.put("success", true);
//            response.put("data", challenges);
//            response.put("count", challenges.size());
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            response.put("success", false);
//            response.put("error", "Failed to get challenges: " + e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//        }
//    }



    @GetMapping("/getAll")
    public ResponseEntity<ChallengeApiResponse> getAllChallenges(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String searchTerm) {

        try {
            // Default values for pagination
            int pageNum = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? size : 50;

            ChallengeApiResponse response = adminChallengeService.getAllChallenges(
                    pageNum, pageSize, id, title, type, status, searchTerm);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ChallengeApiResponse errorResponse = new ChallengeApiResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Failed to get challenges: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }














    // -----------------------
    // UPDATE
    // -----------------------
//    @PutMapping("/{id}")
//    public ResponseEntity<Map<String, Object>> updateChallenge(
//            @PathVariable Long id,
//            @RequestBody MultiQuestionChallengeRequestDTO request) {
//
//        Map<String, Object> response = new HashMap<>();
//        try {
//            MultiQuestionChallengeResponseDTO updated = adminChallengeService.updateChallenge(id, request);
//            response.put("success", true);
//            response.put("message", "Challenge updated successfully");
//            response.put("data", updated);
//            return ResponseEntity.ok(response);
//        } catch (ResourceNotFoundException e) {
//            response.put("success", false);
//            response.put("error", e.getMessage());
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
//        } catch (IllegalArgumentException e) {
//            response.put("success", false);
//            response.put("error", e.getMessage());
//            return ResponseEntity.badRequest().body(response);
//        } catch (BusinessException e) {
//            response.put("success", false);
//            response.put("error", e.getMessage());
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
//        } catch (Exception e) {
//            response.put("success", false);
//            response.put("error", "Failed to update challenge: " + e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//        }
//    }

    // -----------------------
    // SOFT DELETE / BULK DELETE
    // -----------------------
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteChallenge(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            adminChallengeService.softDeleteChallenge(id);
            response.put("success", true);
            response.put("message", "Challenge deleted successfully");
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (BusinessException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to delete challenge: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/delete")
    public ResponseEntity<Map<String, Object>> softDeleteChallenge(@RequestParam Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            adminChallengeService.softDeleteChallenge(id);
            response.put("success", true);
            response.put("message", "Challenge soft deleted successfully");
            response.put("id", id);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (BusinessException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to delete challenge: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/bulk-delete")
    public ResponseEntity<Map<String, Object>> bulkDeleteChallenges(@RequestParam List<Long> challengeIds) {
        Map<String, Object> response = new HashMap<>();
        try {
            adminChallengeService.bulkDeleteChallenges(challengeIds);
            response.put("success", true);
            response.put("message", "Deleted " + challengeIds.size() + " challenges");
            response.put("deletedCount", challengeIds.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // -----------------------
    // STATUS update (single + bulk)
    // -----------------------
//    @PatchMapping("/{id}/status")
//    public ResponseEntity<Map<String, Object>> updateChallengeStatus(
//            @PathVariable Long id,
//            @RequestParam String status) {
//
//        Map<String, Object> response = new HashMap<>();
//        try {
//            // fetch existing to build update DTO (to reuse existing fields)
//            MultiQuestionChallengeResponseDTO existing = adminChallengeService.getChallengeById(id);
//
//            MultiQuestionChallengeRequestDTO updateRequest = MultiQuestionChallengeRequestDTO.builder()
//                    .name(existing.getName())
//                    .description(existing.getDescription())
//                    .descriptionExpanded(existing.getDescriptionExpanded())
//                    .category(existing.getCategory())
//                    .difficulty(existing.getDifficulty())
//                    .ageGroups(existing.getAgeGroups())
//                    .topic(existing.getTopic())
//                    .status(Status.valueOf(status.toUpperCase()))
//                    .coins(existing.getCoins())
//                    .coinsForCorrectAnswer(existing.getCoinsForCorrectAnswer())
//                    .trophies(existing.getTrophies())
//                    .timeDuration(existing.getTimeDuration())
//                    .sectionTitle(existing.getSectionTitle())
//                    .positiveFeedback(existing.getPositiveFeedback())
//                    .negativeFeedback(existing.getNegativeFeedback())
//                    .negativeFeedbackTryAgain(existing.getNegativeFeedbackTryAgain())
//                    .thumbnailImageUrl(existing.getThumbnailImageUrl())
//                    .innerImageUrl(existing.getInnerImageUrl())
//                    .enabled(existing.isEnabled())
//                    .questions(existing.getQuestions().stream()
//                            .map(this::convertToQuestionRequestDTO)
//                            .collect(Collectors.toList()))
//                    .attachments(existing.getAttachments() != null ?
//                            existing.getAttachments().stream()
//                                    .map(att -> ChallengeAttachmentRequestDTO.builder()
//                                            .fileName(att.getFileName())
//                                            .fileUrl(att.getFileUrl())
//                                            .fileType(att.getFileType())
//                                            .fileSize(att.getFileSize())
//                                            .build())
//                                    .collect(Collectors.toList()) : null)
//                    .build();
//
//            MultiQuestionChallengeResponseDTO updated = adminChallengeService.updateChallenge(id, updateRequest);
//
//            response.put("success", true);
//            response.put("message", "Challenge status updated successfully");
//            response.put("data", updated);
//            return ResponseEntity.ok(response);
//
//        } catch (IllegalArgumentException e) {
//            response.put("success", false);
//            response.put("error", "Invalid status. Allowed: " + Arrays.toString(Status.values()));
//            return ResponseEntity.badRequest().body(response);
//        } catch (ResourceNotFoundException e) {
//            response.put("success", false);
//            response.put("error", e.getMessage());
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
//        } catch (BusinessException e) {
//            response.put("success", false);
//            response.put("error", e.getMessage());
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
//        } catch (Exception e) {
//            response.put("success", false);
//            response.put("error", "Failed to update challenge status: " + e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//        }
//    }

    @PutMapping("/bulk-update-status")
    public ResponseEntity<Map<String, Object>> bulkUpdateChallengeStatus(
            @RequestParam List<Long> challengeIds,
            @RequestParam Status status) {

        Map<String, Object> response = new HashMap<>();
        try {
            adminChallengeService.bulkUpdateStatus(challengeIds, status);
            response.put("success", true);
            response.put("message", "Updated status for " + challengeIds.size() + " challenges");
            response.put("updatedCount", challengeIds.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/update-status")
    public ResponseEntity<Map<String, Object>> updateChallengeStatus(
            @RequestBody StatusUpdateRequest request) {

        Map<String, Object> response = new HashMap<>();
        try {
            adminChallengeService.updateChallengeStatus(request);
            response.put("success", true);
            response.put("message", "Challenge status updated successfully");
            response.put("id", request.getId());
            response.put("status", request.getStatus());
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to update challenge status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // -----------------------
    // OVERVIEW (FILTERING + PAGINATION)
    // -----------------------
    @GetMapping("/overview")
    public ResponseEntity<?> getAllChallengeViews(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean multiQuestion,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String ageGroup,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            QuestionMode questionMode = null;
            if (multiQuestion != null) {
                questionMode = multiQuestion ? QuestionMode.MULTI : QuestionMode.INDIVIDUAL;
            }

            List<AdminChallengeDTO> challenges = adminChallengeService.getAllChallengeViews(questionMode);
            ChallengeStatsResponse stats = adminChallengeService.getDashboardStats();

            List<AdminChallengeDTO> filtered = challenges.stream()
                    .filter(c -> search == null || c.matchesSearchTerm(search))
                    .filter(c -> type == null || (c.getTypeValue() != null && c.getTypeValue().equalsIgnoreCase(type)))
                    .filter(c -> status == null || (c.getStatus() != null && c.getStatus().name().equalsIgnoreCase(status)))
                    .filter(c -> difficulty == null || (c.getDifficultyValue() != null && c.getDifficultyValue().equalsIgnoreCase(difficulty)))
                    .filter(c -> ageGroup == null || (c.getAgeGroups() != null &&
                            c.getAgeGroups().stream().anyMatch(ag -> ag.name().equalsIgnoreCase(ageGroup))))
                    .collect(Collectors.toList());

            int totalCount = filtered.size();
            int start = Math.min(page * size, totalCount);
            int end = Math.min(start + size, totalCount);
            List<AdminChallengeDTO> paginated = filtered.subList(start, end);

            Map<String, Object> pagination = new HashMap<>();
            pagination.put("page", page);
            pagination.put("size", size);
            pagination.put("total", totalCount);

            Map<String, Object> statsMap = new HashMap<>();
            statsMap.put("totalChallengesPublished", stats.getTotalChallengesPublished());
            statsMap.put("totalDrafts", stats.getTotalDrafts());
            statsMap.put("totalUnderReview", stats.getTotalUnderReview());
            statsMap.put("averageCompletionRate", stats.getAverageCompletionRate());

            Map<String, Object> payload = new HashMap<>();
            payload.put("success", true);
            payload.put("data", paginated);
            payload.put("pagination", pagination);
            payload.put("stats", statsMap);

            return ResponseEntity.ok(payload);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "Failed to get overview: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    // -----------------------
    // QUICK STATS
    // -----------------------
    @GetMapping("/quick-stats")
    public ResponseEntity<Map<String, Object>> getQuickChallengeStats() {
        Map<String, Object> response = new HashMap<>();
        try {
            ChallengeStatsResponse stats = adminChallengeService.getDashboardStats();
            response.put("success", true);
            response.put("totalChallenges", stats.getTotalChallenges());
            response.put("totalChallengesPublished", stats.getTotalChallengesPublished());
            response.put("totalDrafts", stats.getTotalDrafts());
            response.put("totalUnderReview", stats.getTotalUnderReview());
            response.put("averageCompletionRate", stats.getAverageCompletionRate());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // -----------------------
    // SEARCH (simple)
    // -----------------------
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchChallenges(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        Map<String, Object> response = new HashMap<>();
        try {
            List<AdminChallengeDTO> all = adminChallengeService.getAllChallengeViews();
            List<AdminChallengeDTO> results = all.stream()
                    .filter(c -> c.matchesSearchTerm(query))
                    .collect(Collectors.toList());

            int start = Math.max(0, offset);
            int end = Math.min(results.size(), start + limit);
            List<AdminChallengeDTO> paginated = start < results.size() ? results.subList(start, end) : Collections.emptyList();

            response.put("success", true);
            response.put("challenges", paginated);
            response.put("totalCount", results.size());
            response.put("limit", limit);
            response.put("offset", offset);
            response.put("hasMore", (offset + limit) < results.size());
            response.put("searchTerm", query);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // -----------------------
    // FILTER OPTIONS
    // -----------------------
    @GetMapping("/filter-options")
    public ResponseEntity<Map<String, Object>> getChallengeFilterOptions() {
        Map<String, Object> response = new HashMap<>();
        try {
            response.put("success", true);
            response.put("types", adminChallengeService.getAvailableTypes());
            response.put("difficulties", adminChallengeService.getAvailableDifficulties());
            response.put("ageGroups", adminChallengeService.getAvailableAgeGroups());
            response.put("answerTypes", adminChallengeService.getAvailableAnswerTypes());
            response.put("statuses", adminChallengeService.getAvailableStatuses());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // -----------------------
    // ANALYTICS & BREAKDOWNS
    // -----------------------
    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getChallengeAnalytics() {
        Map<String, Object> response = new HashMap<>();
        try {
            ChallengeStatsResponse stats = adminChallengeService.getDashboardStats();
            Map<String, Object> breakdowns = adminChallengeService.getChallengeBreakdowns();

            response.put("success", true);
            response.put("statistics", stats);
            response.put("categoryBreakdown", breakdowns.get("categoryBreakdown"));
            response.put("difficultyBreakdown", breakdowns.get("difficultyBreakdown"));
            response.put("ageGroupBreakdown", breakdowns.get("ageGroupBreakdown"));
            response.put("answerTypeBreakdown", breakdowns.get("answerTypeBreakdown"));
            response.put("questionsDistribution", breakdowns.get("questionsDistribution"));
            response.put("completionStats", breakdowns.get("completionStats"));
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // -----------------------
    // EXPORT
    // -----------------------
    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> exportChallengeData(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String ageGroup,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String answerType,
            @RequestParam(defaultValue = "csv") String format) {

        Map<String, Object> response = new HashMap<>();
        try {
            AdminChallengeDTO.ChallengeFilterCriteria criteria = new AdminChallengeDTO.ChallengeFilterCriteria();
            criteria.setSearchTerm(search);
            criteria.setType(type);
            criteria.setDifficulty(difficulty);
            criteria.setAgeGroup(ageGroup);
            criteria.setStatus(status);
            criteria.setAnswerType(answerType);

            String exportData = adminChallengeService.exportChallengeData(criteria, format);

            response.put("success", true);
            response.put("data", exportData);
            response.put("format", format);
            response.put("filename", "challenges_export_" + System.currentTimeMillis() + "." + format);
            response.put("criteria", criteria);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // -----------------------
    // HEALTH / STATUS
    // -----------------------
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getChallengeServiceStatus() {
        Map<String, Object> response = new HashMap<>();
        try {
            ChallengeStatsResponse stats = adminChallengeService.getDashboardStats();
            response.put("success", true);
            response.put("status", "healthy");
            response.put("totalChallenges", stats.getTotalChallenges());
            response.put("serviceVersion", "2.0");
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("status", "error");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // -----------------------
    // Helper: convert QuestionResponseDTO -> QuestionRequestDTO
    // -----------------------
    private QuestionRequestDTO convertToQuestionRequestDTO(QuestionResponseDTO responseDTO) {
        if (responseDTO == null) return null;
        List<QuestionOptionRequestDTO> opts = responseDTO.getOptions() == null ? Collections.emptyList()
                : responseDTO.getOptions().stream()
                .map(opt -> QuestionOptionRequestDTO.builder()
                        .optionText(opt.getOptionText())
                        .optionImageUrl(opt.getOptionImageUrl())
                        .optionOrder(opt.getOptionOrder())
                        .isCorrect(opt.getIsCorrect())
                        .explanation(opt.getExplanation())
                        .build())
                .collect(Collectors.toList());

        return QuestionRequestDTO.builder()
                .questionOrder(responseDTO.getQuestionOrder())
                .questionText(responseDTO.getQuestionText())
                .questionImageUrl(responseDTO.getQuestionImageUrl())
                .hint(responseDTO.getHint())
                .answerType(responseDTO.getAnswerType())
                .points(responseDTO.getPoints())
                .timeLimit(responseDTO.getTimeLimit())
                .attachmentUrl(responseDTO.getAttachmentUrl())
                .attachmentType(responseDTO.getAttachmentType())
                .options(opts)
                .build();
    }



    private Admin getAdminFromToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new BusinessException("Invalid token");
        }

        String jwt = token.replace("Bearer ", "");
        Long adminId;

        try {
            adminId = UserJwtUtil.getUserIdFromAdminToken(jwt);
        } catch (Exception e) {
            throw new BusinessException("Invalid admin token");
        }

        return adminRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException("Admin not found"));
    }



}