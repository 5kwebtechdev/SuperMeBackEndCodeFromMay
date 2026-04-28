package com.superme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserOverviewResponse {
    private List<AdminUserViewDTO> users;
    private UserStatisticsDto globalStatistics;
    private LocalDateTime timestamp;
    private String searchTerm;
    private Integer totalResults;
    private Integer displayedResults;
    private Boolean isFiltered;
    private SearchMetadata searchMetadata;
    private FilterMetadata filterMetadata;
    private AdminUserViewDTO.FilterStatistics filterStatistics;

    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================

    public AdminUserOverviewResponse(List<AdminUserViewDTO> users,
                                     UserStatisticsDto stats,
                                     AdminUserViewDTO.FilterCriteria criteria,
                                     int originalCount) {
        this.timestamp = LocalDateTime.now();
        this.users = users != null ? users : new ArrayList<>();
        this.globalStatistics = stats != null ? stats : UserStatisticsDto.builder().build();
        this.totalResults = originalCount;
        this.displayedResults = this.users.size();

        // Determine if filtering was applied
        this.isFiltered = isFilteringApplied(criteria);
        this.searchTerm = criteria != null ? criteria.getSearchTerm() : null;

        // Initialize metadata
        this.searchMetadata = SearchMetadata.builder()
                .query(criteria != null ? criteria.getSearchTerm() : null)
                .originalCount(originalCount)
                .filteredCount(this.displayedResults)
                .build();

        this.filterMetadata = FilterMetadata.builder()
                .originalCount(originalCount)
                .filteredCount(this.displayedResults)
                .build();

        this.filterStatistics = AdminUserViewDTO.getFilterStatistics(this.users);
    }

    private boolean isFilteringApplied(AdminUserViewDTO.FilterCriteria criteria) {
        if (criteria == null) return false;

        return (criteria.getSearchTerm() != null && !criteria.getSearchTerm().isEmpty()) ||
                (criteria.getGenders() != null && !criteria.getGenders().isEmpty()) ||
                (criteria.getRelationships() != null && !criteria.getRelationships().isEmpty()) ||
                Boolean.TRUE.equals(criteria.getActiveOnly()) ||
                Boolean.TRUE.equals(criteria.getInactiveOnly());
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    public String getFilterSummary() {
        if (!Boolean.TRUE.equals(isFiltered)) {
            return String.format("Showing all %d users", getUserCount());
        }

        if (filterMetadata != null && filterMetadata.hasActiveFilters()) {
            return filterMetadata.getSummary();
        }

        return String.format("Found %d of %d users matching '%s'",
                getDisplayedResults(), getTotalResults(),
                searchTerm != null ? searchTerm : "");
    }

    public int getUserCount() {
        return users != null ? users.size() : 0;
    }

    public double getSearchMatchPercentage() {
        if (!Boolean.TRUE.equals(isFiltered) || getTotalResults() == 0) {
            return 100.0;
        }
        return ((double) getDisplayedResults() / getTotalResults()) * 100.0;
    }

    // ============================================================================
    // INNER CLASSES
    // ============================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilterMetadata {
        private String searchQuery;
        private List<String> appliedGenderFilters;
        private List<String> appliedRelationshipFilters;
        private Boolean activeOnlyFilter;
        private Boolean inactiveOnlyFilter;
        private Integer originalCount;
        private Integer filteredCount;
        private Long filterTimestamp;
        private Double matchPercentage;
        private String filterSummary;

        public FilterMetadata(AdminUserViewDTO.FilterCriteria criteria,
                              Integer originalCount,
                              Integer filteredCount) {
            this.searchQuery = criteria != null ? criteria.getSearchTerm() : null;
            this.appliedGenderFilters = criteria != null ? criteria.getGenders() : new ArrayList<>();
            this.appliedRelationshipFilters = criteria != null ? criteria.getRelationships() : new ArrayList<>();
            this.activeOnlyFilter = criteria != null ? criteria.getActiveOnly() : false;
            this.inactiveOnlyFilter = criteria != null ? criteria.getInactiveOnly() : false;
            this.originalCount = originalCount != null ? originalCount : 0;
            this.filteredCount = filteredCount != null ? filteredCount : 0;
            this.filterTimestamp = System.currentTimeMillis();
            this.matchPercentage = calculateMatchPercentage();
            this.filterSummary = criteria != null ? criteria.getSummary() : "";
        }

        private Double calculateMatchPercentage() {
            if (originalCount == null || originalCount == 0) return 100.0;
            return ((double) (filteredCount != null ? filteredCount : 0) / originalCount) * 100.0;
        }

        public boolean hasActiveFilters() {
            return (searchQuery != null && !searchQuery.trim().isEmpty()) ||
                    (appliedGenderFilters != null && !appliedGenderFilters.isEmpty()) ||
                    (appliedRelationshipFilters != null && !appliedRelationshipFilters.isEmpty()) ||
                    Boolean.TRUE.equals(activeOnlyFilter) ||
                    Boolean.TRUE.equals(inactiveOnlyFilter);
        }

        public String getSummary() {
            if (!hasActiveFilters()) {
                return String.format("Showing all %d users", originalCount != null ? originalCount : 0);
            }

            StringBuilder summary = new StringBuilder();
            summary.append(String.format("Found %d of %d users",
                    filteredCount != null ? filteredCount : 0,
                    originalCount != null ? originalCount : 0));

            if (filterSummary != null && !filterSummary.trim().isEmpty()) {
                summary.append(" with filters: ").append(filterSummary);
            }

            summary.append(String.format(" (%.1f%% match)", matchPercentage != null ? matchPercentage : 0.0));
            return summary.toString();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchMetadata {
        private String query;
        private Integer originalCount;
        private Integer filteredCount;
        private Long searchTimestamp;
        private Double matchPercentage;
        @Builder.Default
        private String searchFields = "email, phone, userId, familyName, name";

        public SearchMetadata(String query, Integer originalCount, Integer filteredCount) {
            this.query = query;
            this.originalCount = originalCount != null ? originalCount : 0;
            this.filteredCount = filteredCount != null ? filteredCount : 0;
            this.searchTimestamp = System.currentTimeMillis();
            this.matchPercentage = calculateMatchPercentage();
            this.searchFields = "email, phone, userId, familyName, name";
        }

        private Double calculateMatchPercentage() {
            if (originalCount == null || originalCount == 0) return 100.0;
            return ((double) (filteredCount != null ? filteredCount : 0) / originalCount) * 100.0;
        }

        public String getSummary() {
            if (query == null || query.trim().isEmpty()) {
                return "No search applied";
            }
            return String.format("Search for '%s' found %d of %d users (%s)",
                    query, filteredCount, originalCount,
                    String.format("%.1f%%", matchPercentage != null ? matchPercentage : 0.0));
        }
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    public List<AdminUserViewDTO> getActiveUsers() {
        if (users == null) return List.of();
        return users.stream()
                .filter(AdminUserViewDTO::isActiveUser)
                .collect(Collectors.toList());
    }

    public List<AdminUserViewDTO> getInactiveUsers() {
        if (users == null) return List.of();
        return users.stream()
                .filter(user -> !user.isActiveUser())
                .collect(Collectors.toList());
    }

    public int getActiveUserCount() {
        return getActiveUsers().size();
    }

    public int getInactiveUserCount() {
        return getInactiveUsers().size();
    }
}