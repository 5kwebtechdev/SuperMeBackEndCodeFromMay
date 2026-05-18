package com.superme.dto;

import com.superme.enums.AgeGroup;
import com.superme.enums.Status;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * DTO for Course data in admin overview table.
 */
@Data
public class AdminCourseDTO {

    private Long id;
    private String courseName;
    private String description;
    private Integer duration;
    private List<AgeGroup> ageGroups;
    private Integer noOfLessons;
    private String difficulty;
    private String format;
    private Status status;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private String category;
    private String thumbnailUrl;
    private String attachmentUrl;
    private Integer totalCoins;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ============================================================================
    // FORMATTING HELPERS
    // ============================================================================

    public String getFormattedCreatedAt() {
        return Optional.ofNullable(createdAt)
                .map(date -> date.format(DATE_FORMATTER))
                .orElse("");
    }

    public String getFormattedLastUpdated() {
        return Optional.ofNullable(lastUpdated)
                .map(date -> date.format(DATE_FORMATTER))
                .orElse("");
    }

    // ============================================================================
    // FILTER UTILITIES
    // ============================================================================

    /** Matches a search term against key text fields. */
    public boolean matchesSearchTerm(String searchTerm) {
        if (isBlank(searchTerm)) return true;
        String term = searchTerm.trim().toLowerCase();

        return containsIgnoreCase(courseName, term)
                || containsIgnoreCase(description, term)
                || containsIgnoreCase(difficulty, term)
                || containsIgnoreCase(category, term);
    }

    /** Applies filters for difficulty, ageGroup, status, and category. */
    public boolean matchesFilters(String difficulty, AgeGroup ageGroup, Status status, String category) {
        if (!isBlank(difficulty) && !difficulty.equalsIgnoreCase(this.difficulty)) return false;
        if (ageGroup != null && (this.ageGroups == null || !this.ageGroups.contains(ageGroup))) return false;
        if (status != null && status != this.status) return false;
        if (!isBlank(category) && !category.equalsIgnoreCase(this.category)) return false;
        return true;
    }

    // ============================================================================
    // PRIVATE HELPERS
    // ============================================================================

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private boolean containsIgnoreCase(String source, String term) {
        return source != null && source.toLowerCase().contains(term);
    }
}
