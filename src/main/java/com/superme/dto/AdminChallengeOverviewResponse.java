package com.superme.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.List;

/**
 * Response wrapper for challenge overview data including challenges list,
 * statistics, and filter information.
 * Follows the same pattern as AdminJournalOverviewResponse for consistency.
 *
 * @author MindfullB Admin Team
 * @version 2.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminChallengeOverviewResponse {
    private List<AdminChallengeDTO> challenges;
    private ChallengeStatsResponse statistics;
    private AdminChallengeDTO.ChallengeFilterCriteria filterCriteria;
    private int totalCount;
    private int filteredCount;
    @Builder.Default
    private boolean hasMore = false;
    private String message;
    @Builder.Default
    private boolean success = true;
    private PaginationInfo pagination;

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    public boolean hasFiltersApplied() {
        return filterCriteria != null && filterCriteria.hasFilters();
    }

    public int getFilteredOutCount() {
        return totalCount - filteredCount;
    }

    public boolean hasResults() {
        return challenges != null && !challenges.isEmpty();
    }

    public boolean isFiltered() {
        return filteredCount != totalCount;
    }

    public boolean isEmpty() {
        return challenges == null || challenges.isEmpty();
    }

    public int getCurrentPageSize() {
        return challenges != null ? challenges.size() : 0;
    }

    // ============================================================================
    // STATIC FACTORY METHODS
    // ============================================================================

    public static AdminChallengeOverviewResponse success(List<AdminChallengeDTO> challenges,
                                                         ChallengeStatsResponse statistics) {
        return AdminChallengeOverviewResponse.builder()
                .challenges(challenges)
                .statistics(statistics)
                .filterCriteria(null)
                .totalCount(challenges != null ? challenges.size() : 0)
                .filteredCount(challenges != null ? challenges.size() : 0)
                .hasMore(false)
                .message("Challenges retrieved successfully")
                .success(true)
                .build();
    }

    public static AdminChallengeOverviewResponse success(List<AdminChallengeDTO> challenges,
                                                         ChallengeStatsResponse statistics,
                                                         AdminChallengeDTO.ChallengeFilterCriteria filterCriteria,
                                                         int totalCount,
                                                         int filteredCount) {
        boolean hasMoreData = filteredCount > (challenges != null ? challenges.size() : 0);
        return AdminChallengeOverviewResponse.builder()
                .challenges(challenges)
                .statistics(statistics)
                .filterCriteria(filterCriteria)
                .totalCount(totalCount)
                .filteredCount(filteredCount)
                .hasMore(hasMoreData)
                .message("Challenges retrieved successfully with filters")
                .success(true)
                .build();
    }

    public static AdminChallengeOverviewResponse success(List<AdminChallengeDTO> challenges,
                                                         ChallengeStatsResponse statistics,
                                                         AdminChallengeDTO.ChallengeFilterCriteria filterCriteria,
                                                         int totalCount,
                                                         int filteredCount,
                                                         PaginationInfo pagination) {
        boolean hasMoreData = pagination != null && pagination.getHasNext();
        return AdminChallengeOverviewResponse.builder()
                .challenges(challenges)
                .statistics(statistics)
                .filterCriteria(filterCriteria)
                .totalCount(totalCount)
                .filteredCount(filteredCount)
                .hasMore(hasMoreData)
                .message("Challenges retrieved successfully with pagination")
                .success(true)
                .pagination(pagination)
                .build();
    }

    public static AdminChallengeOverviewResponse empty(AdminChallengeDTO.ChallengeFilterCriteria filterCriteria) {
        String message = filterCriteria != null && filterCriteria.hasFilters()
                ? "No challenges found matching the specified filters"
                : "No challenges available";

        return AdminChallengeOverviewResponse.builder()
                .challenges(List.of())
                .statistics(new ChallengeStatsResponse()) // Create empty stats
                .filterCriteria(filterCriteria)
                .totalCount(0)
                .filteredCount(0)
                .hasMore(false)
                .message(message)
                .success(true)
                .build();
    }

    public static AdminChallengeOverviewResponse error(String message) {
        return AdminChallengeOverviewResponse.builder()
                .challenges(null)
                .statistics(null)
                .filterCriteria(null)
                .totalCount(0)
                .filteredCount(0)
                .hasMore(false)
                .message(message)
                .success(false)
                .build();
    }

    public static AdminChallengeOverviewResponse error(String message, AdminChallengeDTO.ChallengeFilterCriteria filterCriteria) {
        return AdminChallengeOverviewResponse.builder()
                .challenges(null)
                .statistics(null)
                .filterCriteria(filterCriteria)
                .totalCount(0)
                .filteredCount(0)
                .hasMore(false)
                .message(message)
                .success(false)
                .build();
    }

    // ============================================================================
    // NESTED CLASSES
    // ============================================================================

    /**
     * Pagination information for challenge overview
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationInfo {
        private int currentPage;
        private int pageSize;
        private int totalPages;
        private long totalElements;
        @Builder.Default
        private boolean hasNext = false;
        @Builder.Default
        private boolean hasPrevious = false;

        public static PaginationInfo of(int currentPage, int pageSize, long totalElements) {
            int totalPages = (int) Math.ceil((double) totalElements / pageSize);
            boolean hasNext = currentPage < totalPages;
            boolean hasPrevious = currentPage > 1;

            return PaginationInfo.builder()
                    .currentPage(currentPage)
                    .pageSize(pageSize)
                    .totalPages(totalPages)
                    .totalElements(totalElements)
                    .hasNext(hasNext)
                    .hasPrevious(hasPrevious)
                    .build();
        }

        public int getOffset() {
            return (currentPage - 1) * pageSize;
        }

        // Getter methods for boolean fields
        public boolean getHasNext() {
            return hasNext;
        }

        public boolean getHasPrevious() {
            return hasPrevious;
        }
    }

    @Override
    public String toString() {
        return "AdminChallengeOverviewResponse{" +
                "challengeCount=" + getCurrentPageSize() +
                ", totalCount=" + totalCount +
                ", filteredCount=" + filteredCount +
                ", hasFilters=" + hasFiltersApplied() +
                ", hasMore=" + hasMore +
                ", success=" + success +
                ", pagination=" + (pagination != null ? pagination.getCurrentPage() + "/" + pagination.getTotalPages() : "N/A") +
                '}';
    }
}