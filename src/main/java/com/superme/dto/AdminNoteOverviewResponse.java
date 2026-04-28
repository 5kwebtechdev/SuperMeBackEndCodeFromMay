package com.superme.dto;

import java.util.List;
import java.util.stream.Collectors;

public class AdminNoteOverviewResponse {
    private List<AdminNoteViewDTO> users;
    private AdminNoteViewDTO.NoteStatistics globalStatistics;
    private java.time.LocalDateTime timestamp;

    // Search and filter metadata
    private String searchTerm;
    private Integer totalResults;
    private Integer displayedResults;
    private boolean isFiltered;

    // Search metadata
    private SearchMetadata searchMetadata;

    // Filter metadata
    private FilterMetadata filterMetadata;

    // Filter statistics
    private FilterStatistics filterStatistics;

    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================

    public AdminNoteOverviewResponse() {
        this.timestamp = java.time.LocalDateTime.now();
        this.isFiltered = false;
    }

    public AdminNoteOverviewResponse(List<AdminNoteViewDTO> users,
            AdminNoteViewDTO.NoteStatistics globalStatistics) {
        this.users = users;
        this.globalStatistics = globalStatistics;
        this.timestamp = java.time.LocalDateTime.now();
        this.totalResults = users != null ? users.size() : 0;
        this.displayedResults = this.totalResults;
        this.isFiltered = false;
    }

    /**
     * Constructor with search functionality
     */
    public AdminNoteOverviewResponse(List<AdminNoteViewDTO> users,
            AdminNoteViewDTO.NoteStatistics globalStatistics,
            String searchTerm,
            int originalCount) {
        this.users = users;
        this.globalStatistics = globalStatistics;
        this.timestamp = java.time.LocalDateTime.now();
        this.searchTerm = searchTerm;
        this.totalResults = originalCount;
        this.displayedResults = users != null ? users.size() : 0;
        this.isFiltered = searchTerm != null && !searchTerm.trim().isEmpty();

        if (this.isFiltered) {
            this.searchMetadata = createSearchMetadata(searchTerm, originalCount, this.displayedResults);
        }
    }

    /**
     * Constructor with filter criteria
     */
    public AdminNoteOverviewResponse(List<AdminNoteViewDTO> users,
            AdminNoteViewDTO.NoteStatistics globalStatistics,
            NoteFilterCriteria filterCriteria,
            int originalCount) {
        this.users = users;
        this.globalStatistics = globalStatistics;
        this.timestamp = java.time.LocalDateTime.now();
        this.totalResults = originalCount;
        this.displayedResults = users != null ? users.size() : 0;
        this.isFiltered = filterCriteria != null && filterCriteria.hasFilters();

        if (this.isFiltered && filterCriteria != null) {
            this.searchTerm = filterCriteria.getSearchTerm();
            this.filterMetadata = createFilterMetadata(filterCriteria, originalCount, this.displayedResults);

            if (filterCriteria.getSearchTerm() != null && !filterCriteria.getSearchTerm().trim().isEmpty()) {
                this.searchMetadata = createSearchMetadata(filterCriteria.getSearchTerm(), originalCount,
                        this.displayedResults);
            }
        }
    }

    // ============================================================================
    // GETTERS AND SETTERS
    // ============================================================================

    public List<AdminNoteViewDTO> getUsers() {
        return users;
    }

    public void setUsers(List<AdminNoteViewDTO> users) {
        this.users = users;
        this.displayedResults = users != null ? users.size() : 0;
    }

    public AdminNoteViewDTO.NoteStatistics getGlobalStatistics() {
        return globalStatistics;
    }

    public void setGlobalStatistics(AdminNoteViewDTO.NoteStatistics globalStatistics) {
        this.globalStatistics = globalStatistics;
    }

    public java.time.LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(java.time.LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
        this.isFiltered = searchTerm != null && !searchTerm.trim().isEmpty();
    }

    public Integer getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(Integer totalResults) {
        this.totalResults = totalResults;
    }

    public Integer getDisplayedResults() {
        return displayedResults;
    }

    public void setDisplayedResults(Integer displayedResults) {
        this.displayedResults = displayedResults;
    }

    public boolean isFiltered() {
        return isFiltered;
    }

    public void setFiltered(boolean filtered) {
        isFiltered = filtered;
    }

    public SearchMetadata getSearchMetadata() {
        return searchMetadata;
    }

    public void setSearchMetadata(SearchMetadata searchMetadata) {
        this.searchMetadata = searchMetadata;
    }

    public FilterMetadata getFilterMetadata() {
        return filterMetadata;
    }

    public void setFilterMetadata(FilterMetadata filterMetadata) {
        this.filterMetadata = filterMetadata;
    }

    public FilterStatistics getFilterStatistics() {
        return filterStatistics;
    }

    public void setFilterStatistics(FilterStatistics filterStatistics) {
        this.filterStatistics = filterStatistics;
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    public int getUserCount() {
        return users != null ? users.size() : 0;
    }

    public boolean hasUsers() {
        return users != null && !users.isEmpty();
    }

    public int getUsersWithNotes() {
        if (users == null)
            return 0;
        return (int) users.stream().filter(AdminNoteViewDTO::hasNotes).count();
    }

    public int getActiveUsers() {
        if (users == null)
            return 0;
        return (int) users.stream().filter(AdminNoteViewDTO::isActiveUser).count();
    }

    public int getUsersWithRecentActivity() {
        if (users == null)
            return 0;
        return (int) users.stream().filter(AdminNoteViewDTO::hasRecentActivity).count();
    }

    public int getUsersWithoutNotes() {
        if (users == null)
            return 0;
        return (int) users.stream().filter(user -> !user.hasNotes()).count();
    }

    public int getUsersNeedingAttention() {
        if (users == null)
            return 0;
        return (int) users.stream().filter(AdminNoteViewDTO::needsAttention).count();
    }

    public int getHighlyEngagedUsers() {
        if (users == null)
            return 0;
        return (int) users.stream().filter(AdminNoteViewDTO::isHighlyEngaged).count();
    }

    public double getAverageNotesPerUser() {
        if (users == null || users.isEmpty())
            return 0.0;
        return users.stream()
                .mapToLong(AdminNoteViewDTO::getTotalNotes)
                .average()
                .orElse(0.0);
    }

    public double getAverageActivityScore() {
        if (users == null || users.isEmpty())
            return 0.0;
        return users.stream()
                .filter(AdminNoteViewDTO::hasNotes)
                .mapToDouble(AdminNoteViewDTO::getActivityScore)
                .average()
                .orElse(0.0);
    }

    public List<AdminNoteViewDTO> getTopPerformers(int limit) {
        if (users == null)
            return List.of();
        return users.stream()
                .filter(AdminNoteViewDTO::hasNotes)
                .sorted((u1, u2) -> Long.compare(u2.getTotalNotes(), u1.getTotalNotes()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<AdminNoteViewDTO> getMostActiveThisMonth(int limit) {
        if (users == null)
            return List.of();
        return users.stream()
                .filter(AdminNoteViewDTO::hasRecentActivity)
                .sorted((u1, u2) -> Long.compare(u2.getNotesCreatedThisMonth(), u1.getNotesCreatedThisMonth()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<AdminNoteViewDTO> getUsersNeedingAttentionList() {
        if (users == null)
            return List.of();
        return users.stream()
                .filter(AdminNoteViewDTO::needsAttention)
                .collect(Collectors.toList());
    }

    public double getFilterEfficiency() {
        if (totalResults == null || totalResults == 0)
            return 0.0;
        return ((double) displayedResults / totalResults) * 100.0;
    }

    public String getResponseSummary() {
        if (!isFiltered) {
            return String.format("Total users: %d, Users with notes: %d",
                    getUserCount(), getUsersWithNotes());
        } else {
            return String.format("Filtered: %d/%d users, Search: '%s'",
                    displayedResults, totalResults, searchTerm);
        }
    }

    public double getEngagementRate() {
        if (getUserCount() == 0)
            return 0.0;
        return ((double) getUsersWithNotes() / getUserCount()) * 100.0;
    }

    public double getRecentActivityRate() {
        if (getUserCount() == 0)
            return 0.0;
        return ((double) getUsersWithRecentActivity() / getUserCount()) * 100.0;
    }

    public long getTotalNotesAllUsers() {
        if (users == null)
            return 0L;
        return users.stream()
                .mapToLong(AdminNoteViewDTO::getTotalNotes)
                .sum();
    }

    public long getTotalMonthlyNotesAllUsers() {
        if (users == null)
            return 0L;
        return users.stream()
                .mapToLong(AdminNoteViewDTO::getNotesCreatedThisMonth)
                .sum();
    }

    // ============================================================================
    // METADATA CREATION METHODS
    // ============================================================================

    private SearchMetadata createSearchMetadata(String searchTerm, int originalCount, int filteredCount) {
        SearchMetadata metadata = new SearchMetadata();
        metadata.setQuery(searchTerm);
        metadata.setOriginalCount(originalCount);
        metadata.setFilteredCount(filteredCount);
        metadata.setSearchTimestamp(
                timestamp != null ? timestamp.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        : null);
        metadata.setMatchPercentage(originalCount > 0 ? ((double) filteredCount / originalCount) * 100 : 0.0);
        // Removed username from searchFields
        metadata.setSearchFields("userId");
        return metadata;
    }

    private FilterMetadata createFilterMetadata(NoteFilterCriteria criteria, int originalCount, int filteredCount) {
        FilterMetadata metadata = new FilterMetadata();
        metadata.setSearchQuery(criteria.getSearchTerm());
        metadata.setMinNoteCount(criteria.getMinNoteCount());
        metadata.setMaxNoteCount(criteria.getMaxNoteCount());
        metadata.setMinMonthlyNotes(criteria.getMinMonthlyNotes());
        metadata.setMaxMonthlyNotes(criteria.getMaxMonthlyNotes());
        metadata.setMinActivityScore(criteria.getMinActivityScore());
        metadata.setMaxActivityScore(criteria.getMaxActivityScore());
        metadata.setHasNotes(criteria.getHasNotes());
        metadata.setHasRecentActivity(criteria.getHasRecentActivity());
        metadata.setOriginalCount(originalCount);
        metadata.setFilteredCount(filteredCount);
        metadata.setFilterTimestamp(
                timestamp != null ? timestamp.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        : null);
        metadata.setMatchPercentage(originalCount > 0 ? ((double) filteredCount / originalCount) * 100 : 0.0);
        metadata.setFilterSummary(criteria.getSummary());
        return metadata;
    }

    // ============================================================================
    // STATIC INNER CLASSES
    // ============================================================================

    public static class SearchMetadata {
        private String query;
        private Integer originalCount;
        private Integer filteredCount;
        private Long searchTimestamp;
        private Double matchPercentage;
        private String searchFields;

        // Getters and setters
        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public Integer getOriginalCount() {
            return originalCount;
        }

        public void setOriginalCount(Integer originalCount) {
            this.originalCount = originalCount;
        }

        public Integer getFilteredCount() {
            return filteredCount;
        }

        public void setFilteredCount(Integer filteredCount) {
            this.filteredCount = filteredCount;
        }

        public Long getSearchTimestamp() {
            return searchTimestamp;
        }

        public void setSearchTimestamp(Long searchTimestamp) {
            this.searchTimestamp = searchTimestamp;
        }

        public Double getMatchPercentage() {
            return matchPercentage;
        }

        public void setMatchPercentage(Double matchPercentage) {
            this.matchPercentage = matchPercentage;
        }

        public String getSearchFields() {
            return searchFields;
        }

        public void setSearchFields(String searchFields) {
            this.searchFields = searchFields;
        }
    }

    public static class FilterMetadata {
        private String searchQuery;
        private Long minNoteCount;
        private Long maxNoteCount;
        private Long minMonthlyNotes;
        private Long maxMonthlyNotes;
        private Double minActivityScore;
        private Double maxActivityScore;
        private Boolean hasNotes;
        private Boolean hasRecentActivity;
        private Integer originalCount;
        private Integer filteredCount;
        private Long filterTimestamp;
        private Double matchPercentage;
        private String filterSummary;

        // Getters and setters
        public String getSearchQuery() {
            return searchQuery;
        }

        public void setSearchQuery(String searchQuery) {
            this.searchQuery = searchQuery;
        }

        public Long getMinNoteCount() {
            return minNoteCount;
        }

        public void setMinNoteCount(Long minNoteCount) {
            this.minNoteCount = minNoteCount;
        }

        public Long getMaxNoteCount() {
            return maxNoteCount;
        }

        public void setMaxNoteCount(Long maxNoteCount) {
            this.maxNoteCount = maxNoteCount;
        }

        public Long getMinMonthlyNotes() {
            return minMonthlyNotes;
        }

        public void setMinMonthlyNotes(Long minMonthlyNotes) {
            this.minMonthlyNotes = minMonthlyNotes;
        }

        public Long getMaxMonthlyNotes() {
            return maxMonthlyNotes;
        }

        public void setMaxMonthlyNotes(Long maxMonthlyNotes) {
            this.maxMonthlyNotes = maxMonthlyNotes;
        }

        public Double getMinActivityScore() {
            return minActivityScore;
        }

        public void setMinActivityScore(Double minActivityScore) {
            this.minActivityScore = minActivityScore;
        }

        public Double getMaxActivityScore() {
            return maxActivityScore;
        }

        public void setMaxActivityScore(Double maxActivityScore) {
            this.maxActivityScore = maxActivityScore;
        }

        public Boolean getHasNotes() {
            return hasNotes;
        }

        public void setHasNotes(Boolean hasNotes) {
            this.hasNotes = hasNotes;
        }

        public Boolean getHasRecentActivity() {
            return hasRecentActivity;
        }

        public void setHasRecentActivity(Boolean hasRecentActivity) {
            this.hasRecentActivity = hasRecentActivity;
        }

        public Integer getOriginalCount() {
            return originalCount;
        }

        public void setOriginalCount(Integer originalCount) {
            this.originalCount = originalCount;
        }

        public Integer getFilteredCount() {
            return filteredCount;
        }

        public void setFilteredCount(Integer filteredCount) {
            this.filteredCount = filteredCount;
        }

        public Long getFilterTimestamp() {
            return filterTimestamp;
        }

        public void setFilterTimestamp(Long filterTimestamp) {
            this.filterTimestamp = filterTimestamp;
        }

        public Double getMatchPercentage() {
            return matchPercentage;
        }

        public void setMatchPercentage(Double matchPercentage) {
            this.matchPercentage = matchPercentage;
        }

        public String getFilterSummary() {
            return filterSummary;
        }

        public void setFilterSummary(String filterSummary) {
            this.filterSummary = filterSummary;
        }
    }

    public static class FilterStatistics {
        private Integer totalUsers;
        private Integer usersWithNotes;
        private Integer usersWithoutNotes;
        private Integer usersWithRecentActivity;
        private Integer usersNeedingAttention;
        private Integer highlyEngagedUsers;
        private Double averageNotesPerUser;
        private Double averageActivityScore;
        private Double engagementRate;
        private Double recentActivityRate;

        public FilterStatistics() {
        }

        public FilterStatistics(List<AdminNoteViewDTO> users) {
            if (users == null) {
                this.totalUsers = 0;
                this.usersWithNotes = 0;
                this.usersWithoutNotes = 0;
                this.usersWithRecentActivity = 0;
                this.usersNeedingAttention = 0;
                this.highlyEngagedUsers = 0;
                this.averageNotesPerUser = 0.0;
                this.averageActivityScore = 0.0;
                this.engagementRate = 0.0;
                this.recentActivityRate = 0.0;
                return;
            }

            this.totalUsers = users.size();
            this.usersWithNotes = (int) users.stream().filter(AdminNoteViewDTO::hasNotes).count();
            this.usersWithoutNotes = totalUsers - usersWithNotes;
            this.usersWithRecentActivity = (int) users.stream().filter(AdminNoteViewDTO::hasRecentActivity).count();
            this.usersNeedingAttention = (int) users.stream().filter(AdminNoteViewDTO::needsAttention).count();
            this.highlyEngagedUsers = (int) users.stream().filter(AdminNoteViewDTO::isHighlyEngaged).count();

            this.averageNotesPerUser = users.stream()
                    .mapToLong(AdminNoteViewDTO::getTotalNotes)
                    .average()
                    .orElse(0.0);

            this.averageActivityScore = users.stream()
                    .filter(AdminNoteViewDTO::hasNotes)
                    .mapToDouble(AdminNoteViewDTO::getActivityScore)
                    .average()
                    .orElse(0.0);

            this.engagementRate = totalUsers > 0 ? ((double) usersWithNotes / totalUsers) * 100 : 0.0;
            this.recentActivityRate = totalUsers > 0 ? ((double) usersWithRecentActivity / totalUsers) * 100 : 0.0;
        }

        // Getters and setters
        public Integer getTotalUsers() {
            return totalUsers;
        }

        public void setTotalUsers(Integer totalUsers) {
            this.totalUsers = totalUsers;
        }

        public Integer getUsersWithNotes() {
            return usersWithNotes;
        }

        public void setUsersWithNotes(Integer usersWithNotes) {
            this.usersWithNotes = usersWithNotes;
        }

        public Integer getUsersWithoutNotes() {
            return usersWithoutNotes;
        }

        public void setUsersWithoutNotes(Integer usersWithoutNotes) {
            this.usersWithoutNotes = usersWithoutNotes;
        }

        public Integer getUsersWithRecentActivity() {
            return usersWithRecentActivity;
        }

        public void setUsersWithRecentActivity(Integer usersWithRecentActivity) {
            this.usersWithRecentActivity = usersWithRecentActivity;
        }

        public Integer getUsersNeedingAttention() {
            return usersNeedingAttention;
        }

        public void setUsersNeedingAttention(Integer usersNeedingAttention) {
            this.usersNeedingAttention = usersNeedingAttention;
        }

        public Integer getHighlyEngagedUsers() {
            return highlyEngagedUsers;
        }

        public void setHighlyEngagedUsers(Integer highlyEngagedUsers) {
            this.highlyEngagedUsers = highlyEngagedUsers;
        }

        public Double getAverageNotesPerUser() {
            return averageNotesPerUser;
        }

        public void setAverageNotesPerUser(Double averageNotesPerUser) {
            this.averageNotesPerUser = averageNotesPerUser;
        }

        public Double getAverageActivityScore() {
            return averageActivityScore;
        }

        public void setAverageActivityScore(Double averageActivityScore) {
            this.averageActivityScore = averageActivityScore;
        }

        public Double getEngagementRate() {
            return engagementRate;
        }

        public void setEngagementRate(Double engagementRate) {
            this.engagementRate = engagementRate;
        }

        public Double getRecentActivityRate() {
            return recentActivityRate;
        }

        public void setRecentActivityRate(Double recentActivityRate) {
            this.recentActivityRate = recentActivityRate;
        }
    }

    public static class NoteFilterCriteria {
        private String searchTerm;
        private Long minNoteCount;
        private Long maxNoteCount;
        private Long minMonthlyNotes;
        private Long maxMonthlyNotes;
        private Double minActivityScore;
        private Double maxActivityScore;
        private Boolean hasNotes;
        private Boolean hasRecentActivity;

        public NoteFilterCriteria() {
        }

        public boolean hasFilters() {
            return (searchTerm != null && !searchTerm.trim().isEmpty()) ||
                    minNoteCount != null ||
                    maxNoteCount != null ||
                    minMonthlyNotes != null ||
                    maxMonthlyNotes != null ||
                    minActivityScore != null ||
                    maxActivityScore != null ||
                    hasNotes != null ||
                    hasRecentActivity != null;
        }

        public String getSummary() {
            List<String> filters = new java.util.ArrayList<>();

            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                filters.add("Search: '" + searchTerm + "'");
            }
            if (minNoteCount != null || maxNoteCount != null) {
                String range = "";
                if (minNoteCount != null)
                    range += minNoteCount;
                if (maxNoteCount != null)
                    range += (minNoteCount != null ? "-" : "≤") + maxNoteCount;
                filters.add("Note Count: " + range);
            }
            if (minMonthlyNotes != null || maxMonthlyNotes != null) {
                String range = "";
                if (minMonthlyNotes != null)
                    range += minMonthlyNotes;
                if (maxMonthlyNotes != null)
                    range += (minMonthlyNotes != null ? "-" : "≤") + maxMonthlyNotes;
                filters.add("Monthly Notes: " + range);
            }
            if (minActivityScore != null || maxActivityScore != null) {
                String range = "";
                if (minActivityScore != null)
                    range += minActivityScore + "%";
                if (maxActivityScore != null)
                    range += (minActivityScore != null ? "-" : "≤") + maxActivityScore + "%";
                filters.add("Activity Score: " + range);
            }
            if (hasNotes != null) {
                filters.add(hasNotes ? "Has notes" : "No notes");
            }
            if (hasRecentActivity != null) {
                filters.add(hasRecentActivity ? "Recent activity" : "No recent activity");
            }

            return filters.isEmpty() ? "No filters applied" : String.join(" | ", filters);
        }

        // Getters and setters
        public String getSearchTerm() {
            return searchTerm;
        }

        public void setSearchTerm(String searchTerm) {
            this.searchTerm = searchTerm;
        }

        public Long getMinNoteCount() {
            return minNoteCount;
        }

        public void setMinNoteCount(Long minNoteCount) {
            this.minNoteCount = minNoteCount;
        }

        public Long getMaxNoteCount() {
            return maxNoteCount;
        }

        public void setMaxNoteCount(Long maxNoteCount) {
            this.maxNoteCount = maxNoteCount;
        }

        public Long getMinMonthlyNotes() {
            return minMonthlyNotes;
        }

        public void setMinMonthlyNotes(Long minMonthlyNotes) {
            this.minMonthlyNotes = minMonthlyNotes;
        }

        public Long getMaxMonthlyNotes() {
            return maxMonthlyNotes;
        }

        public void setMaxMonthlyNotes(Long maxMonthlyNotes) {
            this.maxMonthlyNotes = maxMonthlyNotes;
        }

        public Double getMinActivityScore() {
            return minActivityScore;
        }

        public void setMinActivityScore(Double minActivityScore) {
            this.minActivityScore = minActivityScore;
        }

        public Double getMaxActivityScore() {
            return maxActivityScore;
        }

        public void setMaxActivityScore(Double maxActivityScore) {
            this.maxActivityScore = maxActivityScore;
        }

        public Boolean getHasNotes() {
            return hasNotes;
        }

        public void setHasNotes(Boolean hasNotes) {
            this.hasNotes = hasNotes;
        }

        public Boolean getHasRecentActivity() {
            return hasRecentActivity;
        }

        public void setHasRecentActivity(Boolean hasRecentActivity) {
            this.hasRecentActivity = hasRecentActivity;
        }
    }

    @Override
    public String toString() {
        return "AdminNoteOverviewResponse{" +
                "userCount=" + getUserCount() +
                ", usersWithNotes=" + getUsersWithNotes() +
                ", usersWithRecentActivity=" + getUsersWithRecentActivity() +
                ", isFiltered=" + isFiltered +
                ", searchTerm='" + searchTerm + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
