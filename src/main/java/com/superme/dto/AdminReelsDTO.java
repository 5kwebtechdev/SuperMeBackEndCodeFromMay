
package com.superme.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * DTO for admin reels overview operations.
 * Contains reels information optimized for data table display and filtering.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminReelsDTO {

    // --- Fields ---
    private Long id;
    private Long createdBy;
    private String title;
    private String category;
    private String hashtags;
    private String description;
    private Integer duration;
    private String videoUrl;
    private Boolean aiLabel;
    private Integer likedByCount;
    private Integer savedByCount;
    private Integer shareCount;
    private Long createdAt;
    private LocalDate createdLocalDate;
    private LocalTime createdLocalTime;
    private ApprovalStatus status;
    private String statusValue;
    private String actionTakenBy;
    private Long actionTakenOn;
    private LocalDate actionTakenLocalDate;
    private LocalTime actionTakenLocalTime;
    private ReelsStatistics statistics;

    // --- Custom methods required by AdminReelsService ---
    public boolean matchesSearchTerm(String term) {
        if (term == null || term.isEmpty())
            return true;
        return (title != null && title.toLowerCase().contains(term.toLowerCase()))
                || (description != null && description.toLowerCase().contains(term.toLowerCase()))
                || (actionTakenBy != null && actionTakenBy.toLowerCase().contains(term.toLowerCase()));
    }

    public boolean matchesFilters(ReelsFilterCriteria criteria) {
        if (criteria == null)
            return true;
        boolean statusMatch = criteria.getStatus() == null || criteria.getStatus().equalsIgnoreCase(this.statusValue);
        boolean categoryMatch = criteria.getCategory() == null
                || criteria.getCategory().equalsIgnoreCase(this.category);
        boolean searchTermMatch = criteria.getSearchTerm() == null || matchesSearchTerm(criteria.getSearchTerm());
        return statusMatch && categoryMatch && searchTermMatch;
    }

    public int getAge() {
        if (createdAt == null)
            return 0;
        long now = System.currentTimeMillis();
        return (int) ((now - createdAt) / (1000 * 60 * 60 * 24));
    }

    public String getFormattedCreatedAt() {
        if (createdLocalDate == null || createdLocalTime == null)
            return "";
        return createdLocalDate.toString() + " " + createdLocalTime.toString();
    }

    public String getFormattedActionTakenOn() {
        if (actionTakenLocalDate == null || actionTakenLocalTime == null)
            return "";
        return actionTakenLocalDate.toString() + " " + actionTakenLocalTime.toString();
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
        if (createdAt != null) {
            Instant instant = Instant.ofEpochMilli(createdAt);
            this.createdLocalDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
            this.createdLocalTime = instant.atZone(ZoneId.systemDefault()).toLocalTime();
        } else {
            this.createdLocalDate = null;
            this.createdLocalTime = null;
        }
    }

    public void setActionTakenOn(Long actionTakenOn) {
        this.actionTakenOn = actionTakenOn;
        if (actionTakenOn != null) {
            Instant instant = Instant.ofEpochMilli(actionTakenOn);
            this.actionTakenLocalDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
            this.actionTakenLocalTime = instant.atZone(ZoneId.systemDefault()).toLocalTime();
        } else {
            this.actionTakenLocalDate = null;
            this.actionTakenLocalTime = null;
        }
    }

    public void setAge(int age) {
        // No-op: Age is derived from createdAt
    }

    /**
     * Approval status enum.
     */
    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    /**
     * Filtering criteria for reels.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReelsFilterCriteria {
        private String searchTerm;
        private String category;
        private String status;

        public boolean hasFilters() {
            return (searchTerm != null && !searchTerm.trim().isEmpty())
                    || (category != null && !category.trim().isEmpty())
                    || (status != null && !status.trim().isEmpty());
        }
    }

    /**
     * Statistics for reels.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReelsStatistics {
        public void setTotalReels(Long totalReels) {
            this.totalReels = totalReels != null ? totalReels : 0L;
        }

        public void setReelsApproved(Long reelsApproved) {
            this.reelsApproved = reelsApproved != null ? reelsApproved : 0L;
        }

        private Long totalReels;
        private Long reelsApproved;
        private Long pendingReels;
        private Long rejectedReels;

        public void setPendingReels(Long pendingReels) {
            this.pendingReels = pendingReels != null ? pendingReels : 0L;
        }

        public Long getRejectedReels() {
            return rejectedReels;
        }

        public void setRejectedReels(Long rejectedReels) {
            this.rejectedReels = rejectedReels != null ? rejectedReels : 0L;
        }
    }
}