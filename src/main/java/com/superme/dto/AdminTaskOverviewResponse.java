package com.superme.dto;

import java.util.List;
import java.util.stream.Collectors;

public class AdminTaskOverviewResponse {
    private List<AdminTaskViewDTO> users;
    private AdminTaskViewDTO.TaskStatistics globalStatistics;
    private Long timestamp;
    // New fields for LocalDate/LocalTime
    private java.time.LocalDate timestampLocalDate;
    private java.time.LocalTime timestampLocalTime;

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

    public AdminTaskOverviewResponse() {
        this.timestamp = System.currentTimeMillis();
        this.isFiltered = false;
    }

    public AdminTaskOverviewResponse(List<AdminTaskViewDTO> users,
            AdminTaskViewDTO.TaskStatistics globalStatistics) {
        this.users = users;
        this.globalStatistics = globalStatistics;
        this.timestamp = System.currentTimeMillis();
        this.totalResults = users != null ? users.size() : 0;
        this.displayedResults = this.totalResults;
        this.isFiltered = false;
    }

    /**
     * Constructor with search functionality
     */
    public AdminTaskOverviewResponse(List<AdminTaskViewDTO> users,
            AdminTaskViewDTO.TaskStatistics globalStatistics,
            String searchTerm,
            int originalCount) {
        this.users = users;
        this.globalStatistics = globalStatistics;
        this.timestamp = System.currentTimeMillis();
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
    public AdminTaskOverviewResponse(List<AdminTaskViewDTO> users,
            AdminTaskViewDTO.TaskStatistics globalStatistics,
            TaskFilterCriteria filterCriteria,
            int originalCount) {
        this.users = users;
        this.globalStatistics = globalStatistics;
        this.timestamp = System.currentTimeMillis();
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

    public List<AdminTaskViewDTO> getUsers() {
        return users;
    }

    public void setUsers(List<AdminTaskViewDTO> users) {
        this.users = users;
        this.displayedResults = users != null ? users.size() : 0;
    }

    public AdminTaskViewDTO.TaskStatistics getGlobalStatistics() {
        return globalStatistics;
    }

    public void setGlobalStatistics(AdminTaskViewDTO.TaskStatistics globalStatistics) {
        this.globalStatistics = globalStatistics;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
        if (timestamp != null) {
            java.time.Instant instant = java.time.Instant.ofEpochMilli(timestamp);
            this.timestampLocalDate = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            this.timestampLocalTime = instant.atZone(java.time.ZoneId.systemDefault()).toLocalTime();
        } else {
            this.timestampLocalDate = null;
            this.timestampLocalTime = null;
        }
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

    public int getUsersWithTasks() {
        if (users == null)
            return 0;
        return (int) users.stream().filter(AdminTaskViewDTO::hasTasks).count();
    }

    public int getUsersWithOverdueTasks() {
        if (users == null)
            return 0;
        return (int) users.stream().filter(AdminTaskViewDTO::hasOverdueTasks).count();
    }

    public int getUsersWithoutTasks() {
        if (users == null)
            return 0;
        return (int) users.stream().filter(user -> !user.hasTasks()).count();
    }

    public double getAverageCompletionRate() {
        if (users == null || users.isEmpty())
            return 0.0;
        return users.stream()
                .filter(AdminTaskViewDTO::hasTasks)
                .mapToDouble(AdminTaskViewDTO::getCompletionRate)
                .average()
                .orElse(0.0);
    }

    public double getAverageTasksPerUser() {
        if (users == null || users.isEmpty())
            return 0.0;
        return users.stream()
                .mapToDouble(AdminTaskViewDTO::getTotalTasksCreated)
                .average()
                .orElse(0.0);
    }

    public List<AdminTaskViewDTO> getTopPerformers(int limit) {
        if (users == null)
            return List.of();
        return users.stream()
                .filter(AdminTaskViewDTO::hasTasks)
                .sorted((u1, u2) -> Double.compare(u2.getCompletionRate(), u1.getCompletionRate()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<AdminTaskViewDTO> getUsersNeedingAttention() {
        if (users == null)
            return List.of();
        return users.stream()
                .filter(AdminTaskViewDTO::needsAttention)
                .collect(Collectors.toList());
    }

    public double getFilterEfficiency() {
        if (totalResults == null || totalResults == 0)
            return 0.0;
        return ((double) displayedResults / totalResults) * 100.0;
    }

    public String getResponseSummary() {
        if (!isFiltered) {
            return String.format("Total users: %d, Users with tasks: %d",
                    getUserCount(), getUsersWithTasks());
        } else {
            return String.format("Filtered: %d/%d users, Search: '%s'",
                    displayedResults, totalResults, searchTerm);
        }
    }

    // ============================================================================
    // METADATA CREATION METHODS
    // ============================================================================

    private SearchMetadata createSearchMetadata(String searchTerm, int originalCount, int filteredCount) {
        SearchMetadata metadata = new SearchMetadata();
        metadata.setQuery(searchTerm);
        metadata.setOriginalCount(originalCount);
        metadata.setFilteredCount(filteredCount);
        metadata.setSearchTimestamp(this.timestamp);
        metadata.setMatchPercentage(originalCount > 0 ? ((double) filteredCount / originalCount) * 100 : 0.0);
        metadata.setSearchFields("userId");
        return metadata;
    }

    private FilterMetadata createFilterMetadata(TaskFilterCriteria criteria, int originalCount, int filteredCount) {
        FilterMetadata metadata = new FilterMetadata();
        metadata.setSearchQuery(criteria.getSearchTerm());
        metadata.setMinCompletionRate(criteria.getMinCompletionRate());
        metadata.setMaxCompletionRate(criteria.getMaxCompletionRate());
        metadata.setMinTaskCount(criteria.getMinTaskCount());
        metadata.setMaxTaskCount(criteria.getMaxTaskCount());
        metadata.setHasOverdueTasks(criteria.getHasOverdueTasks());
        metadata.setHasTasks(criteria.getHasTasks());
        metadata.setOriginalCount(originalCount);
        metadata.setFilteredCount(filteredCount);
        metadata.setFilterTimestamp(this.timestamp);
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
        private Double minCompletionRate;
        private Double maxCompletionRate;
        private Long minTaskCount;
        private Long maxTaskCount;
        private Boolean hasOverdueTasks;
        private Boolean hasTasks;
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

        public Double getMinCompletionRate() {
            return minCompletionRate;
        }

        public void setMinCompletionRate(Double minCompletionRate) {
            this.minCompletionRate = minCompletionRate;
        }

        public Double getMaxCompletionRate() {
            return maxCompletionRate;
        }

        public void setMaxCompletionRate(Double maxCompletionRate) {
            this.maxCompletionRate = maxCompletionRate;
        }

        public Long getMinTaskCount() {
            return minTaskCount;
        }

        public void setMinTaskCount(Long minTaskCount) {
            this.minTaskCount = minTaskCount;
        }

        public Long getMaxTaskCount() {
            return maxTaskCount;
        }

        public void setMaxTaskCount(Long maxTaskCount) {
            this.maxTaskCount = maxTaskCount;
        }

        public Boolean getHasOverdueTasks() {
            return hasOverdueTasks;
        }

        public void setHasOverdueTasks(Boolean hasOverdueTasks) {
            this.hasOverdueTasks = hasOverdueTasks;
        }

        public Boolean getHasTasks() {
            return hasTasks;
        }

        public void setHasTasks(Boolean hasTasks) {
            this.hasTasks = hasTasks;
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
        private Integer usersWithTasks;
        private Integer usersWithoutTasks;
        private Integer usersWithOverdueTasks;
        private Double averageCompletionRate;
        private Double averageTasksPerUser;
        private Integer highPerformers; // Users with >80% completion rate
        private Integer lowPerformers; // Users with <40% completion rate

        public FilterStatistics() {
        }

        public FilterStatistics(List<AdminTaskViewDTO> users) {
            if (users == null) {
                this.totalUsers = 0;
                this.usersWithTasks = 0;
                this.usersWithoutTasks = 0;
                this.usersWithOverdueTasks = 0;
                this.averageCompletionRate = 0.0;
                this.averageTasksPerUser = 0.0;
                this.highPerformers = 0;
                this.lowPerformers = 0;
                return;
            }

            this.totalUsers = users.size();
            this.usersWithTasks = (int) users.stream().filter(AdminTaskViewDTO::hasTasks).count();
            this.usersWithoutTasks = totalUsers - usersWithTasks;
            this.usersWithOverdueTasks = (int) users.stream().filter(AdminTaskViewDTO::hasOverdueTasks).count();

            this.averageCompletionRate = users.stream()
                    .filter(AdminTaskViewDTO::hasTasks)
                    .mapToDouble(AdminTaskViewDTO::getCompletionRate)
                    .average()
                    .orElse(0.0);

            this.averageTasksPerUser = users.stream()
                    .mapToDouble(AdminTaskViewDTO::getTotalTasksCreated)
                    .average()
                    .orElse(0.0);

            this.highPerformers = (int) users.stream()
                    .filter(user -> user.hasTasks() && user.getCompletionRate() >= 80.0)
                    .count();

            this.lowPerformers = (int) users.stream()
                    .filter(user -> user.hasTasks() && user.getCompletionRate() < 40.0)
                    .count();
        }

        // Getters and setters
        public Integer getTotalUsers() {
            return totalUsers;
        }

        public void setTotalUsers(Integer totalUsers) {
            this.totalUsers = totalUsers;
        }

        public Integer getUsersWithTasks() {
            return usersWithTasks;
        }

        public void setUsersWithTasks(Integer usersWithTasks) {
            this.usersWithTasks = usersWithTasks;
        }

        public Integer getUsersWithoutTasks() {
            return usersWithoutTasks;
        }

        public void setUsersWithoutTasks(Integer usersWithoutTasks) {
            this.usersWithoutTasks = usersWithoutTasks;
        }

        public Integer getUsersWithOverdueTasks() {
            return usersWithOverdueTasks;
        }

        public void setUsersWithOverdueTasks(Integer usersWithOverdueTasks) {
            this.usersWithOverdueTasks = usersWithOverdueTasks;
        }

        public Double getAverageCompletionRate() {
            return averageCompletionRate;
        }

        public void setAverageCompletionRate(Double averageCompletionRate) {
            this.averageCompletionRate = averageCompletionRate;
        }

        public Double getAverageTasksPerUser() {
            return averageTasksPerUser;
        }

        public void setAverageTasksPerUser(Double averageTasksPerUser) {
            this.averageTasksPerUser = averageTasksPerUser;
        }

        public Integer getHighPerformers() {
            return highPerformers;
        }

        public void setHighPerformers(Integer highPerformers) {
            this.highPerformers = highPerformers;
        }

        public Integer getLowPerformers() {
            return lowPerformers;
        }

        public void setLowPerformers(Integer lowPerformers) {
            this.lowPerformers = lowPerformers;
        }
    }

    public static class TaskFilterCriteria {
        private String searchTerm;
        private Double minCompletionRate;
        private Double maxCompletionRate;
        private Long minTaskCount;
        private Long maxTaskCount;
        private Boolean hasOverdueTasks;
        private Boolean hasTasks;

        public TaskFilterCriteria() {
        }

        public boolean hasFilters() {
            return (searchTerm != null && !searchTerm.trim().isEmpty()) ||
                    minCompletionRate != null ||
                    maxCompletionRate != null ||
                    minTaskCount != null ||
                    maxTaskCount != null ||
                    hasOverdueTasks != null ||
                    hasTasks != null;
        }

        public String getSummary() {
            List<String> filters = new java.util.ArrayList<>();

            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                filters.add("Search: '" + searchTerm + "'");
            }
            if (minCompletionRate != null || maxCompletionRate != null) {
                String range = "";
                if (minCompletionRate != null)
                    range += minCompletionRate + "%";
                if (maxCompletionRate != null)
                    range += (minCompletionRate != null ? "-" : "≤") + maxCompletionRate + "%";
                filters.add("Completion Rate: " + range);
            }
            if (minTaskCount != null || maxTaskCount != null) {
                String range = "";
                if (minTaskCount != null)
                    range += minTaskCount;
                if (maxTaskCount != null)
                    range += (minTaskCount != null ? "-" : "≤") + maxTaskCount;
                filters.add("Task Count: " + range);
            }
            if (hasOverdueTasks != null) {
                filters.add(hasOverdueTasks ? "Has overdue tasks" : "No overdue tasks");
            }
            if (hasTasks != null) {
                filters.add(hasTasks ? "Has tasks" : "No tasks");
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

        public Double getMinCompletionRate() {
            return minCompletionRate;
        }

        public void setMinCompletionRate(Double minCompletionRate) {
            this.minCompletionRate = minCompletionRate;
        }

        public Double getMaxCompletionRate() {
            return maxCompletionRate;
        }

        public void setMaxCompletionRate(Double maxCompletionRate) {
            this.maxCompletionRate = maxCompletionRate;
        }

        public Long getMinTaskCount() {
            return minTaskCount;
        }

        public void setMinTaskCount(Long minTaskCount) {
            this.minTaskCount = minTaskCount;
        }

        public Long getMaxTaskCount() {
            return maxTaskCount;
        }

        public void setMaxTaskCount(Long maxTaskCount) {
            this.maxTaskCount = maxTaskCount;
        }

        public Boolean getHasOverdueTasks() {
            return hasOverdueTasks;
        }

        public void setHasOverdueTasks(Boolean hasOverdueTasks) {
            this.hasOverdueTasks = hasOverdueTasks;
        }

        public Boolean getHasTasks() {
            return hasTasks;
        }

        public void setHasTasks(Boolean hasTasks) {
            this.hasTasks = hasTasks;
        }
    }

    public String getFormattedTimestamp() {
        if (timestampLocalDate == null) {
            return "No timestamp";
        }
        return timestampLocalDate.toString()
                + (timestampLocalTime != null ? (" " + timestampLocalTime.toString()) : "");
    }

    @Override
    public String toString() {
        return "AdminTaskOverviewResponse{" +
                "userCount=" + getUserCount() +
                ", usersWithTasks=" + getUsersWithTasks() +
                ", usersWithOverdueTasks=" + getUsersWithOverdueTasks() +
                ", isFiltered=" + isFiltered +
                ", searchTerm='" + searchTerm + '\'' +
                ", timestamp=" + getFormattedTimestamp() +
                '}';
    }
}
