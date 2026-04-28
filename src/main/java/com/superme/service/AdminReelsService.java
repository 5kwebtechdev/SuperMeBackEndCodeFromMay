package com.superme.service;

import com.superme.dto.AdminReelsDTO;
import com.superme.dto.CreateReelRequest;
import com.superme.dto.UpdateReelRequest;
import com.superme.model.Reels;
import com.superme.repository.ReelsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin Reels Service for managing reels in the admin panel.
 * 
 * Provides comprehensive business logic for:
 * - Data table operations with filtering
 * - Statistics calculation for dashboard cards
 * - Reels analytics and breakdowns
 * - Approval/rejection workflow
 * 
 * @author MindfullB Admin Team
 * @version 2.0
 */
@Service
@Transactional
public class AdminReelsService {

    @Autowired
    private ReelsRepository reelsRepository;

    // ============================================================================
    // DATA TABLE OPERATIONS
    // ============================================================================

    public List<AdminReelsDTO> getAllReelsViews() {
        try {
            List<Reels> reels = reelsRepository.findAll();
            return reels.stream()
                    .map(this::convertToAdminDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch reels: " + e.getMessage(), e);
        }
    }

    // ============================================================================
    // STATISTICS FOR DASHBOARD CARDS
    // ============================================================================

    public AdminReelsDTO.ReelsStatistics getReelsStatistics() {
        try {
            Long totalReels = reelsRepository.countTotalReels();

            // Count by status
            Map<Reels.ApprovalStatus, Long> statusMap = reelsRepository.countReelsByStatus()
                    .stream()
                    .collect(Collectors.toMap(
                            arr -> (Reels.ApprovalStatus) arr[0],
                            arr -> (Long) arr[1]));

            AdminReelsDTO.ReelsStatistics stats = new AdminReelsDTO.ReelsStatistics();
            stats.setTotalReels(totalReels);
            stats.setReelsApproved(statusMap.getOrDefault(Reels.ApprovalStatus.APPROVED, 0L));
            stats.setPendingReels(statusMap.getOrDefault(Reels.ApprovalStatus.PENDING, 0L));
            stats.setRejectedReels(statusMap.getOrDefault(Reels.ApprovalStatus.REJECTED, 0L));

            return stats;
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate reels statistics: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getReelsBreakdowns() {
        Map<String, Object> breakdowns = new HashMap<>();

        try {
            // Category breakdown
            List<Object[]> categoryCounts = reelsRepository.countReelsByCategory();
            breakdowns.put("categoryBreakdown", categoryCounts);

            // Status breakdown
            List<Object[]> statusCounts = reelsRepository.countReelsByStatus();
            breakdowns.put("statusBreakdown", statusCounts);

            // Age group breakdown removed (no user/age info in Reels)
            // Duration distribution
            Map<String, Long> durationDistribution = getAllReelsViews().stream()
                    .collect(Collectors.groupingBy(
                            reel -> getDurationRange(reel.getDuration()),
                            Collectors.counting()));
            breakdowns.put("durationDistribution", durationDistribution);

        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate reels breakdowns: " + e.getMessage(), e);
        }

        return breakdowns;
    }

    // ============================================================================
    // FILTER OPTIONS
    // ============================================================================

    public List<Map<String, String>> getAvailableCategories() {
        try {
            return reelsRepository.findDistinctCategories().stream()
                    .map(category -> Map.of("value", category, "label", category))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Arrays.asList(
                    Map.of("value", "Entertainment", "label", "Entertainment"),
                    Map.of("value", "Education", "label", "Education"),
                    Map.of("value", "Sports", "label", "Sports"),
                    Map.of("value", "Music", "label", "Music"));
        }
    }

    public List<Map<String, String>> getAvailableStatuses() {
        return Arrays.stream(Reels.ApprovalStatus.values())
                .map(status -> Map.of("value", status.name(), "label", status.name().toLowerCase()))
                .collect(Collectors.toList());
    }

    // ============================================================================
    // REELS DETAILS AND ACTIONS
    // ============================================================================

    public AdminReelsDTO getReelById(Long id) {
        try {
            Reels reel = reelsRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Reel not found with id: " + id));
            return convertToAdminDTO(reel);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get reel: " + e.getMessage(), e);
        }
    }

    // ============================================================================
    // VIDEO UPLOAD AND FILE MANAGEMENT
    // ============================================================================

    public String uploadVideoToS3(MultipartFile file) {
        try {
            // Validate file
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File is empty");
            }

            // Validate file type (MP4 only as per requirement)
            String contentType = file.getContentType();
            if (!"video/mp4".equals(contentType)) {
                throw new IllegalArgumentException("Only MP4 files are accepted");
            }

            // Validate file size (15MB max as per requirement)
            long maxSize = 15 * 1024 * 1024; // 15MB in bytes
            if (file.getSize() > maxSize) {
                throw new IllegalArgumentException("File size exceeds 15MB limit");
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = "reels/" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString()
                    + extension;

            // TODO: Implement actual S3 upload logic here
            // For now, return a mock S3 URL
            String s3BaseUrl = "https://your-bucket.s3.amazonaws.com/";
            return s3BaseUrl + filename;

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload video: " + e.getMessage(), e);
        }
    }

    // ============================================================================
    // ENHANCED APPROVAL METHODS WITH COMMENTS
    // ============================================================================

    public void approveReel(Long id, String comments) {
        try {
            Reels reel = reelsRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Reel not found with id: " + id));

            reel.setStatus(Reels.ApprovalStatus.APPROVED);
            // TODO: Set action taken by and action taken on fields when User entity is
            // available
            // reel.setActionTakenBy(currentAdmin);
            // reel.setActionTakenOn(System.currentTimeMillis());
            // reel.setComments(comments);

            reelsRepository.save(reel);
        } catch (Exception e) {
            throw new RuntimeException("Failed to approve reel: " + e.getMessage(), e);
        }
    }

    public void rejectReel(Long id, String comments) {
        try {
            Reels reel = reelsRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Reel not found with id: " + id));

            reel.setStatus(Reels.ApprovalStatus.REJECTED);
            // TODO: Set action taken by and action taken on fields when User entity is
            // available
            // reel.setActionTakenBy(currentAdmin);
            // reel.setActionTakenOn(System.currentTimeMillis());
            // reel.setComments(comments);

            reelsRepository.save(reel);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reject reel: " + e.getMessage(), e);
        }
    }

    public void markReelPending(Long id, String comments) {
        try {
            Reels reel = reelsRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Reel not found with id: " + id));

            reel.setStatus(Reels.ApprovalStatus.PENDING);
            // TODO: Set action taken by and action taken on fields when User entity is
            // available
            // reel.setActionTakenBy(currentAdmin);
            // reel.setActionTakenOn(System.currentTimeMillis());
            // reel.setComments(comments);

            reelsRepository.save(reel);
        } catch (Exception e) {
            throw new RuntimeException("Failed to mark reel as pending: " + e.getMessage(), e);
        }
    }

    public String exportReelsData(AdminReelsDTO.ReelsFilterCriteria criteria, String format) {
        try {
            List<AdminReelsDTO> reels = getAllReelsViews();

            // Apply filters
            if (criteria != null) {
                reels = reels.stream()
                        .filter(reel -> reel.matchesSearchTerm(criteria.getSearchTerm()))
                        .filter(reel -> reel.matchesFilters(criteria))
                        .collect(Collectors.toList());
            }

            if ("csv".equalsIgnoreCase(format)) {
                return exportToCSV(reels);
            } else if ("json".equalsIgnoreCase(format)) {
                return exportToJSON(reels);
            } else {
                throw new IllegalArgumentException("Unsupported export format: " + format);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to export reels data: " + e.getMessage(), e);
        }
    }

    private String exportToCSV(List<AdminReelsDTO> reels) {
        StringBuilder csv = new StringBuilder();

        // Headers (removed Username)
        csv.append("ID,Age,Title,Category,Duration,Status,Action Taken By,Created At,Action Taken On\n");

        // Data rows
        for (AdminReelsDTO reel : reels) {
            csv.append(reel.getId()).append(",");
            // Username removed
            csv.append(reel.getAge()).append(",");
            csv.append("\"").append(reel.getTitle()).append("\",");
            csv.append("\"").append(reel.getCategory()).append("\",");
            csv.append(reel.getDuration()).append(",");
            csv.append("\"").append(reel.getStatus()).append("\",");
            csv.append("\"").append(reel.getActionTakenBy()).append("\",");
            csv.append(reel.getFormattedCreatedAt()).append(",");
            csv.append(reel.getFormattedActionTakenOn()).append("\n");
        }

        return csv.toString();
    }

    private String exportToJSON(List<AdminReelsDTO> reels) {
        // Simple JSON export - in production, use a proper JSON library like Jackson
        StringBuilder json = new StringBuilder();
        json.append("{\"reels\":[");

        for (int i = 0; i < reels.size(); i++) {
            AdminReelsDTO reel = reels.get(i);
            json.append("{");
            json.append("\"id\":").append(reel.getId()).append(",");
            // Username removed
            json.append("\"age\":").append(reel.getAge()).append(",");
            json.append("\"title\":\"").append(reel.getTitle()).append("\",");
            json.append("\"category\":\"").append(reel.getCategory()).append("\",");
            json.append("\"duration\":").append(reel.getDuration()).append(",");
            json.append("\"status\":\"").append(reel.getStatus()).append("\",");
            json.append("\"actionTakenBy\":\"").append(reel.getActionTakenBy()).append("\",");
            json.append("\"createdAt\":").append(reel.getCreatedAt()).append(",");
            json.append("\"actionTakenOn\":").append(reel.getActionTakenOn());
            json.append("}");

            if (i < reels.size() - 1) {
                json.append(",");
            }
        }

        json.append("],\"totalCount\":").append(reels.size()).append("}");
        return json.toString();
    }

    // ============================================================================
    // UPDATED CRUD METHODS WITH DESCRIPTION SUPPORT
    // ============================================================================

    public AdminReelsDTO createReel(CreateReelRequest request) {
        try {
            Reels reel = new Reels();
            reel.setTitle(request.getTitle());
            reel.setCategory(request.getCategory());
            reel.setDescription(request.getDescription()); // Add description
            reel.setDuration(request.getDuration());
            reel.setVideoUrl(request.getVideoLink());

            // Set status
            if (request.getStatus() != null && !request.getStatus().isEmpty()) {
                reel.setStatus(Reels.ApprovalStatus.valueOf(request.getStatus().toUpperCase()));
            } else {
                reel.setStatus(Reels.ApprovalStatus.PENDING);
            }

            // Set created date and time
            reel.setCreatedDate(java.time.LocalDate.now());
            reel.setCreatedTime(java.time.LocalTime.now());

            Reels savedReel = reelsRepository.save(reel);
            return convertToAdminDTO(savedReel);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create reel: " + e.getMessage(), e);
        }
    }

    public AdminReelsDTO updateReel(Long id, UpdateReelRequest request) {
        try {
            Reels reel = reelsRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Reel not found with id: " + id));

            // Update fields if provided
            if (request.getTitle() != null) {
                reel.setTitle(request.getTitle());
            }
            if (request.getCategory() != null) {
                reel.setCategory(request.getCategory());
            }
            if (request.getDescription() != null) { // Add description update
                reel.setDescription(request.getDescription());
            }
            if (request.getDuration() != null) {
                reel.setDuration(request.getDuration());
            }
            if (request.getVideoLink() != null) {
                reel.setVideoUrl(request.getVideoLink());
            }
            if (request.getStatus() != null && !request.getStatus().isEmpty()) {
                reel.setStatus(Reels.ApprovalStatus.valueOf(request.getStatus().toUpperCase()));
            }

            Reels savedReel = reelsRepository.save(reel);
            return convertToAdminDTO(savedReel);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update reel: " + e.getMessage(), e);
        }
    }

    // ============================================================================
    // UPDATED CONVERSION METHOD WITH DESCRIPTION
    // ============================================================================

    private AdminReelsDTO convertToAdminDTO(Reels reel) {
        AdminReelsDTO dto = new AdminReelsDTO();
        dto.setId(reel.getId());
        dto.setTitle(reel.getTitle());
        dto.setCategory(reel.getCategory());
        dto.setDescription(reel.getDescription()); // Add description mapping
        dto.setDuration(reel.getDuration());
        dto.setStatus(reel.getStatus() != null
                ? com.superme.dto.AdminReelsDTO.ApprovalStatus.valueOf(reel.getStatus().name())
                : com.superme.dto.AdminReelsDTO.ApprovalStatus.PENDING);
        dto.setStatusValue(reel.getStatus() != null ? reel.getStatus().name() : "PENDING");
        // Combine createdDate and createdTime to epoch millis for createdAt
        if (reel.getCreatedDate() != null && reel.getCreatedTime() != null) {
            java.time.LocalDateTime ldt = java.time.LocalDateTime.of(reel.getCreatedDate(), reel.getCreatedTime());
            dto.setCreatedAt(ldt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
            dto.setCreatedLocalDate(reel.getCreatedDate());
            dto.setCreatedLocalTime(reel.getCreatedTime());
        } else {
            dto.setCreatedAt(null);
            dto.setCreatedLocalDate(null);
            dto.setCreatedLocalTime(null);
        }

        // Username removed
        dto.setAge(25); // Replace with actual user age if available
        dto.setActionTakenBy("Admin"); // Replace with actual admin who took action
        dto.setActionTakenOn(System.currentTimeMillis()); // Replace with actual action timestamp

        return dto;
    }

    private String getDurationRange(Integer duration) {
        if (duration == null || duration == 0)
            return "0s";
        if (duration <= 30)
            return "0-30s";
        if (duration <= 60)
            return "31-60s";
        if (duration <= 120)
            return "1-2min";
        if (duration <= 300)
            return "2-5min";
        return "5min+";
    }

    // ============================================================================
    // ADMIN REEL DELETION
    // ============================================================================

    public void deleteReel(Long id) {
        try {
            Reels reel = reelsRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Reel not found with id: " + id));

            reelsRepository.delete(reel);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete reel: " + e.getMessage(), e);
        }
    }

    // ============================================================================
    // AGE GROUP BREAKDOWN (REMOVED - NOT SUPPORTED BY MODEL)
    // ============================================================================

    // Removed: getReelsByAgeGroupBreakdown() - not supported without user/age info
}
