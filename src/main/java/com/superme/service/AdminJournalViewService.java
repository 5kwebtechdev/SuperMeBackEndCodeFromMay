package com.superme.service;

import com.superme.dto.AdminJournalViewDTO;
import com.superme.dto.AdminJournalViewDTO.JournalStatistics;
import com.superme.dto.AdminJournalViewDTO.JournalFilterCriteria;
import com.superme.dto.AdminJournalOverviewResponse;
import com.superme.model.User;
import com.superme.model.JournalEntry;
import com.superme.repository.UserRepository;



import com.superme.repository.JournalEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;

/**
 * Enhanced Service for managing admin journal entry view operations.
 * 
 * Provides comprehensive journal entry analytics, filtering, search, and
 * overview functionality
 * for administrative dashboard with advanced search and filtering capabilities.
 * 
 * @author MindfullB Admin Team
 * @version 2.0
 */
@Service
public class AdminJournalViewService {

    // ============================================================================
    // DEPENDENCY INJECTION
    // ============================================================================

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    // ============================================================================
    // ENHANCED PUBLIC API METHODS WITH SEARCH AND FILTERING
    // ============================================================================

    /**
     * Retrieves comprehensive journal entry overview for all users.
     * Includes total journal entries and entries created this month for each user.
     * 
     * @return List of AdminJournalViewDTO with user journal entry data
     */
    public List<AdminJournalViewDTO> getAllUserJournalViews() {
        try {
            List<User> users = userRepository.findAll();
            List<AdminJournalViewDTO> result = new ArrayList<>();

            for (User user : users) {
                AdminJournalViewDTO dto = createUserJournalViewDTO(user);
                result.add(dto);
            }

            return result;
        } catch (Exception e) {
            System.err.println("Error fetching user journal views: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Enhanced search functionality for journal users by userId and name
     *
     * @param searchTerm Search term to match against userId or name
     * @return List of matching AdminJournalViewDTO
     */
    public List<AdminJournalViewDTO> searchUsers(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllUserJournalViews();
        }

        return getAllUserJournalViews().stream()
                .filter(user -> user.matchesSearchTerm(searchTerm)) // matches userId and name
                .collect(Collectors.toList());
    }

    /**
     * Get filtered journal users with comprehensive criteria
     * 
     * @param criteria JournalFilterCriteria containing filter parameters
     * @return AdminJournalOverviewResponse with filtered results and metadata
     */
    public AdminJournalOverviewResponse getFilteredUserJournalViews(JournalFilterCriteria criteria) {
        try {
            // Get all users first
            List<AdminJournalViewDTO> allUsers = getAllUserJournalViews();
            int originalCount = allUsers.size();

            // Apply filters
            List<AdminJournalViewDTO> filteredUsers = applyFilters(allUsers, criteria);

            // Get global statistics
            JournalStatistics globalStats = getJournalStatistics();

            // Create response
            return new AdminJournalOverviewResponse(filteredUsers, globalStats, criteria, originalCount);

        } catch (Exception e) {
            System.err.println("Error getting filtered journal views: " + e.getMessage());
            return new AdminJournalOverviewResponse(new ArrayList<>(), new JournalStatistics());
        }
    }

    /**
     * Get paginated journal users with filtering and sorting
     * 
     * @param criteria      Filter criteria
     * @param page          Page number (0-based)
     * @param size          Page size
     * @param sortBy        Sort field
     * @param sortDirection Sort direction (asc/desc)
     * @return AdminJournalOverviewResponse with paginated results
     */
    public AdminJournalOverviewResponse getPaginatedUserJournalViews(
            JournalFilterCriteria criteria, int page, int size, String sortBy, String sortDirection) {
        try {
            // Get filtered results first
            AdminJournalOverviewResponse response = getFilteredUserJournalViews(criteria);
            List<AdminJournalViewDTO> filteredUsers = response.getUsers();

            // Apply sorting
            List<AdminJournalViewDTO> sortedUsers = applySorting(filteredUsers, sortBy, sortDirection);

            // Apply pagination
            List<AdminJournalViewDTO> paginatedUsers = applyPagination(sortedUsers, page, size);

            // Update response with paginated data
            response.setUsers(paginatedUsers);

            return response;

        } catch (Exception e) {
            System.err.println("Error getting paginated journal views: " + e.getMessage());
            return new AdminJournalOverviewResponse(new ArrayList<>(), new JournalStatistics());
        }
    }

    // ============================================================================
    // FILTERING METHODS
    // ============================================================================

    /**
     * Apply comprehensive filters to journal user list
     */
    private List<AdminJournalViewDTO> applyFilters(List<AdminJournalViewDTO> users, JournalFilterCriteria criteria) {
        if (criteria == null || !criteria.hasFilters()) {
            return users;
        }

        return users.stream()
                .filter(user -> matchesSearchTerm(user, criteria.getSearchTerm()))
                .filter(user -> matchesJournalEntriesRange(user, criteria.getMinJournalEntries(),
                        criteria.getMaxJournalEntries()))
                .filter(user -> matchesRecentEntriesRange(user, criteria.getMinRecentEntries(),
                        criteria.getMaxRecentEntries()))
                .filter(user -> matchesEngagementLevelFilter(user, criteria.getEngagementLevel()))
                .filter(user -> matchesUserTypeFilter(user, criteria.getUserType()))
                .filter(user -> matchesActiveStatusFilter(user, criteria.getIsActive()))
                .filter(user -> matchesDeactivatedFilter(user, criteria.getIsDeactivated()))
                .collect(Collectors.toList());
    }

    /**
     * Check if user matches search term (userId and username)
     */
    private boolean matchesSearchTerm(AdminJournalViewDTO user, String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return true;
        }

        return user.matchesSearchTerm(searchTerm);
    }

    /**
     * Check if user's journal entries count is within range
     */
    private boolean matchesJournalEntriesRange(AdminJournalViewDTO user, Long minEntries, Long maxEntries) {
        long journalEntries = user.getTotalJournalEntries() != null ? user.getTotalJournalEntries() : 0L;

        if (minEntries != null && journalEntries < minEntries) {
            return false;
        }
        if (maxEntries != null && journalEntries > maxEntries) {
            return false;
        }
        return true;
    }

    /**
     * Check if user's recent entries count is within range
     */
    private boolean matchesRecentEntriesRange(AdminJournalViewDTO user, Long minRecent, Long maxRecent) {
        long recentEntries = user.getJournalEntriesCreatedThisMonth() != null ? user.getJournalEntriesCreatedThisMonth()
                : 0L;

        if (minRecent != null && recentEntries < minRecent) {
            return false;
        }
        if (maxRecent != null && recentEntries > maxRecent) {
            return false;
        }
        return true;
    }

    /**
     * Check if user matches engagement level filter
     */
    private boolean matchesEngagementLevelFilter(AdminJournalViewDTO user, String engagementLevel) {
        if (engagementLevel == null || engagementLevel.trim().isEmpty()) {
            return true;
        }

        return switch (engagementLevel.toLowerCase()) {
            case "high" -> user.isHighlyEngaged();
            case "medium" -> user.hasJournalEntries() && !user.isHighlyEngaged() && user.getActivityScore() >= 20.0;
            case "low" -> user.hasJournalEntries() && user.getActivityScore() < 20.0;
            case "none" -> !user.hasJournalEntries();
            default -> true;
        };
    }

    /**
     * Check if user matches user type filter
     */
    private boolean matchesUserTypeFilter(AdminJournalViewDTO user, String userType) {
        if (userType == null || userType.trim().isEmpty()) {
            return true;
        }

        return switch (userType.toLowerCase()) {
            case "with_journal_entries" -> user.hasJournalEntries();
            case "without_journal_entries" -> !user.hasJournalEntries();
            case "active_users" -> user.isJournalActive();
            case "recent_writers" -> user.hasRecentActivity();
            case "needs_attention" -> user.needsAttention();
            case "highly_engaged" -> user.isHighlyEngaged();
            default -> true;
        };
    }

    /**
     * isActive=false → users who have NOT written any journal in the last 7 days
     */
    private boolean matchesActiveStatusFilter(AdminJournalViewDTO user, Boolean isActive) {
        if (isActive == null) return true;
        // isActive=false means "inactive" = no journal in last 7 days
        boolean journalActiveInLast7Days = user.isHasJournalInLast7Days();
        return isActive ? journalActiveInLast7Days : !journalActiveInLast7Days;
    }

    /**
     * isDeactivated=true → users where enabled = false (account deactivated)
     */
    private boolean matchesDeactivatedFilter(AdminJournalViewDTO user, Boolean isDeactivated) {
        if (isDeactivated == null) return true;
        // isDeactivated=true means account is disabled (enabled=false)
        return isDeactivated ? !user.isActiveUser() : user.isActiveUser();
    }

    // ============================================================================
    // SORTING METHODS
    // ============================================================================

    /**
     * Apply sorting to journal user list
     */
    public List<AdminJournalViewDTO> applySorting(List<AdminJournalViewDTO> users, String sortBy,
            String sortDirection) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "name"; // Default sort is now by name
        }

        boolean ascending = !"desc".equalsIgnoreCase(sortDirection);

        Comparator<AdminJournalViewDTO> comparator = getComparator(sortBy);
        if (!ascending) {
            comparator = comparator.reversed();
        }

        return users.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    /**
     * Get comparator for sorting field (supports both camelCase API names and legacy snake_case)
     */
    private Comparator<AdminJournalViewDTO> getComparator(String sortBy) {
        return switch (sortBy) {
            case "userId" -> Comparator.comparing(AdminJournalViewDTO::getUserId,
                    Comparator.nullsLast(Long::compareTo));
            case "name" -> Comparator.comparing(
                    user -> user.getName() != null ? user.getName().toLowerCase() : "",
                    Comparator.nullsLast(String::compareTo));
            case "email" -> Comparator.comparing(AdminJournalViewDTO::getEmail,
                    Comparator.nullsLast(String::compareTo));
            case "age" -> Comparator.comparing(AdminJournalViewDTO::getAge,
                    Comparator.nullsLast(Integer::compareTo));
            case "totalJournalEntries", "total_journal_entries" ->
                    Comparator.comparing(AdminJournalViewDTO::getTotalJournalEntries,
                            Comparator.nullsLast(Long::compareTo));
            case "journalEntriesCreatedThisMonth", "recent_journal_entries" ->
                    Comparator.comparing(AdminJournalViewDTO::getJournalEntriesCreatedThisMonth,
                            Comparator.nullsLast(Long::compareTo));
            case "firstJournalEntryDate" -> Comparator.comparing(AdminJournalViewDTO::getFirstJournalEntryDate,
                    Comparator.nullsLast(String::compareTo));
            case "lastJournalEntryDate" -> Comparator.comparing(AdminJournalViewDTO::getLastJournalEntryDate,
                    Comparator.nullsLast(String::compareTo));
            case "lastActive" -> Comparator.comparing(AdminJournalViewDTO::getLastActive,
                    Comparator.nullsLast(String::compareTo));
            case "activity_score" -> Comparator.comparing(AdminJournalViewDTO::getActivityScore);
            case "engagement_level" -> Comparator.comparing(AdminJournalViewDTO::getEngagementLevel,
                    Comparator.nullsLast(String::compareTo));
            default -> Comparator.comparing(
                    user -> user.getName() != null ? user.getName().toLowerCase() : "",
                    Comparator.nullsLast(String::compareTo));
        };
    }

    // ============================================================================
    // PAGINATION METHODS
    // ============================================================================

    /**
     * Apply pagination to user list
     */
    private List<AdminJournalViewDTO> applyPagination(List<AdminJournalViewDTO> users, int page, int size) {
        if (size <= 0) {
            return users; // Return all if size is invalid
        }

        int start = Math.max(0, page * size);
        int end = Math.min(users.size(), start + size);

        if (start >= users.size()) {
            return new ArrayList<>(); // Page beyond available data
        }

        return users.subList(start, end);
    }

    // ============================================================================
    // STATISTICS AND ANALYTICS METHODS
    // ============================================================================

    /**
     * Calculates global journal entry statistics across all users.
     * 
     * @return JournalStatistics with system-wide journal entry metrics
     */
    public JournalStatistics getJournalStatistics() {
        try {
            // Get basic statistics
            Long totalJournalEntriesAllUsers = journalEntryRepository.count();
            Long totalUsers = userRepository.count();

            // Calculate users with journal entries
            Map<Long, Long> journalEntriesPerUser = getJournalEntriesCountByUser();
            Long usersWithJournalEntries = (long) journalEntriesPerUser.size();

            // Calculate average journal entries per user
            Double averageJournalEntriesPerUser = totalUsers > 0 ? (double) totalJournalEntriesAllUsers / totalUsers
                    : 0.0;

            // Create JournalStatistics using the correct constructor
            JournalStatistics stats = new JournalStatistics();
            stats.setTotalJournalEntriesAllUsers(totalJournalEntriesAllUsers);
            stats.setTotalUsers(totalUsers);
            stats.setAverageJournalEntriesPerUser(averageJournalEntriesPerUser);
            stats.setUsersWithJournalEntries(usersWithJournalEntries);

            return stats;

        } catch (Exception e) {
            System.err.println("Error calculating journal entry statistics: " + e.getMessage());
            e.printStackTrace();
            return new JournalStatistics();
        }
    }

    /**
     * Gets quick statistics for dashboard cards.
     * 
     * @return Map containing key journal entry metrics for dashboard display
     */
    public Map<String, Object> getQuickStats() {
        try {
            Map<String, Object> quickStats = new HashMap<>();
            JournalStatistics stats = getJournalStatistics();

            quickStats.put("totalJournalEntries", stats.getTotalJournalEntriesAllUsers());
            quickStats.put("totalUsers", stats.getTotalUsers());
            quickStats.put("averageJournalEntriesPerUser", stats.getFormattedAverageJournalEntriesPerUser());
            quickStats.put("usersWithJournalEntries", stats.getUsersWithJournalEntries());
            quickStats.put("usersWithoutJournalEntries", stats.getUsersWithoutJournalEntries());
            quickStats.put("engagementRate", stats.getFormattedEngagementRate());

            // Additional quick metrics
            quickStats.put("usersWithRecentJournalEntries", getUsersWithRecentActivity().size());
            quickStats.put("journalEntriesCreatedThisMonth", getTotalJournalEntriesCreatedThisMonth());

            return quickStats;
        } catch (Exception e) {
            System.err.println("Error calculating quick stats: " + e.getMessage());
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    /**
     * Gets detailed journal entry analytics including trends and distributions.
     * 
     * @return Map containing comprehensive journal entry analytics data
     */
    public Map<String, Object> getDetailedJournalAnalytics() {
        try {
            Map<String, Object> analytics = new HashMap<>();

            JournalStatistics stats = getJournalStatistics();
            List<AdminJournalViewDTO> userViews = getAllUserJournalViews();

            analytics.put("journalStatistics", stats);
            analytics.put("topJournalWriters", getTopJournalWriters(5));
            analytics.put("activeUsers", getUsersWithRecentActivity());
            analytics.put("inactiveUsers", getUsersWithoutRecentActivity());
            analytics.put("monthlyTrends", getMonthlyJournalTrends());
            analytics.put("userJournalDistribution", getUserJournalDistribution());
            analytics.put("engagementAnalysis", getEngagementAnalysis(userViews));
            analytics.put("recentActivity", getRecentActivityAnalysis());

            return analytics;
        } catch (Exception e) {
            System.err.println("Error calculating detailed journal analytics: " + e.getMessage());
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    // ============================================================================
    // USER CATEGORIZATION METHODS
    // ============================================================================

    /**
     * Gets list of users who have created at least one journal entry.
     * 
     * @return List of users with journal entries
     */
    public List<AdminJournalViewDTO> getUsersWithJournalEntries() {
        return getAllUserJournalViews().stream()
                .filter(AdminJournalViewDTO::hasJournalEntries)
                .collect(Collectors.toList());
    }

    /**
     * Gets list of users who have not created any journal entries.
     * 
     * @return List of users without journal entries
     */
    public List<AdminJournalViewDTO> getUsersWithoutJournalEntries() {
        return getAllUserJournalViews().stream()
                .filter(user -> !user.hasJournalEntries())
                .collect(Collectors.toList());
    }

    /**
     * Gets list of users who have created journal entries this month.
     * 
     * @return List of users with recent journal entry activity
     */
    public List<AdminJournalViewDTO> getUsersWithRecentActivity() {
        return getAllUserJournalViews().stream()
                .filter(AdminJournalViewDTO::hasRecentActivity)
                .collect(Collectors.toList());
    }

    /**
     * Gets list of users who have not created journal entries this month.
     * 
     * @return List of users without recent journal entry activity
     */
    public List<AdminJournalViewDTO> getUsersWithoutRecentActivity() {
        return getAllUserJournalViews().stream()
                .filter(user -> !user.hasRecentActivity())
                .collect(Collectors.toList());
    }

    /**
     * Gets top journal writers by total entries.
     * 
     * @param limit Maximum number of users to return
     * @return List of top journal writers sorted by total entries descending
     */
    public List<AdminJournalViewDTO> getTopJournalWriters(int limit) {
        return getAllUserJournalViews().stream()
                .filter(AdminJournalViewDTO::hasJournalEntries)
                .sorted((u1, u2) -> Long.compare(u2.getTotalJournalEntries(), u1.getTotalJournalEntries()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Gets most active users this month by journal entries created.
     * 
     * @param limit Maximum number of users to return
     * @return List of most active users this month
     */
    public List<AdminJournalViewDTO> getMostActiveUsersThisMonth(int limit) {
        return getAllUserJournalViews().stream()
                .filter(AdminJournalViewDTO::hasRecentActivity)
                .sorted((u1, u2) -> Long.compare(u2.getJournalEntriesCreatedThisMonth(),
                        u1.getJournalEntriesCreatedThisMonth()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Gets users who need attention (have journal entries but no recent activity).
     * 
     * @return List of users who may need re-engagement
     */
    public List<AdminJournalViewDTO> getUsersNeedingAttention() {
        return getAllUserJournalViews().stream()
                .filter(user -> user.hasJournalEntries() && !user.hasRecentActivity())
                .sorted((u1, u2) -> Long.compare(u2.getTotalJournalEntries(), u1.getTotalJournalEntries()))
                .collect(Collectors.toList());
    }

    /**
     * Gets highly engaged users (active with high activity scores).
     * 
     * @return List of highly engaged users
     */
    public List<AdminJournalViewDTO> getHighlyEngagedUsers() {
        return getAllUserJournalViews().stream()
                .filter(user -> user.isJournalActive() && user.getActivityScore() > 50.0)
                .sorted((u1, u2) -> Double.compare(u2.getActivityScore(), u1.getActivityScore()))
                .collect(Collectors.toList());
    }

    // ============================================================================
    // EXPORT AND UTILITY METHODS
    // ============================================================================

    /**
     * Export journal user data for reports
     */
    public List<Map<String, Object>> exportUserJournalData() {
        try {
            List<AdminJournalViewDTO> users = getAllUserJournalViews();
            return users.stream()
                    .map(user -> {
                        Map<String, Object> data = new HashMap<>();
                        data.put("userId", user.getUserId());
                        // data.put("username", user.getUsername()); // Removed username
                        data.put("name", user.getName()); // Add name
                        data.put("totalJournalEntries", user.getTotalJournalEntries());
                        data.put("journalEntriesThisMonth", user.getJournalEntriesCreatedThisMonth());
                        data.put("activityScore", String.format("%.1f", user.getActivityScore()));
                        data.put("engagementLevel", user.getEngagementLevel());
                        data.put("hasJournalEntries", user.hasJournalEntries());
                        data.put("hasRecentActivity", user.hasRecentActivity());
                        data.put("isActiveUser", user.isActiveUser());
                        data.put("isHighlyEngaged", user.isHighlyEngaged());
                        data.put("needsAttention", user.needsAttention());
                        return data;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error exporting user journal data: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get journal view for a specific user
     */
    public AdminJournalViewDTO getUserJournalView(Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                return createUserJournalViewDTO(userOpt.get());
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error fetching user journal view for ID " + userId + ": " + e.getMessage());
            return null;
        }
    }

    // ============================================================================
    // PRIVATE HELPER METHODS
    // ============================================================================

    /**
     * Creates journal view DTO for a single user.
     * 
     * @param user The user to create journal view for
     * @return AdminJournalViewDTO with user's journal entry data
     */
    private AdminJournalViewDTO createUserJournalViewDTO(User user) {
        AdminJournalViewDTO dto = new AdminJournalViewDTO();

        dto.setUserId(user.getId());
        dto.setName(user.getName());
        dto.setActiveUser(user.isEnabled());
        dto.setPhone(user.getPhone());
        dto.setEmail(user.getEmail());
        dto.setGender(user.getGender());

        if (user.getDateOfBirth() != null) {
            dto.setAge(Period.between(user.getDateOfBirth(), LocalDate.now()).getYears());
        }

        if (user.getRelationship() != null) {
            String display = user.getRelationship().getDisplayName();
            dto.setRole("Self".equals(display) ? "Individual" : display);
        }

        if (user.getLastLoginDate() != null) {
            dto.setLastActive(user.getLastLoginDate().toString());
        }

        try {
            Long totalJournalEntries = getTotalJournalEntriesForUser(user.getId());
            Long journalEntriesThisMonth = getJournalEntriesCreatedThisMonthForUser(user.getId());

            dto.setTotalJournalEntries(totalJournalEntries);
            dto.setJournalEntriesCreatedThisMonth(journalEntriesThisMonth);
            dto.setHasJournalInLast7Days(hasJournalEntryInLast7Days(user.getId()));

            journalEntryRepository.findFirstJournalEntryDateByUserId(user.getId())
                    .ifPresent(d -> dto.setFirstJournalEntryDate(d.toString()));
            journalEntryRepository.findLastJournalEntryDateByUserId(user.getId())
                    .ifPresent(d -> dto.setLastJournalEntryDate(d.toString()));

        } catch (Exception e) {
            System.err.println("Error calculating journal entry stats for user " + user.getId() + ": " + e.getMessage());
            e.printStackTrace();
            dto.setTotalJournalEntries(0L);
            dto.setJournalEntriesCreatedThisMonth(0L);
            dto.setHasJournalInLast7Days(false);
        }

        return dto;
    }

    /**
     * Returns true if the user has at least one journal entry in the last 7 days.
     */
    private boolean hasJournalEntryInLast7Days(Long userId) {
        try {
            long sevenDaysAgoSeconds = System.currentTimeMillis() / 1000L - (7L * 24L * 60L * 60L);
            return journalEntryRepository.findByCreatedById(userId).stream()
                    .anyMatch(entry -> {
                        try {
                            if (entry.getCreationDate() == null || entry.getCreationTime() == null) return false;
                            long created = java.time.LocalDateTime
                                    .of(entry.getCreationDate(), entry.getCreationTime())
                                    .atZone(ZoneId.systemDefault()).toEpochSecond();
                            return created >= sevenDaysAgoSeconds;
                        } catch (Exception e) {
                            return false;
                        }
                    });
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets total journal entries for a specific user.
     * 
     * @param userId The user ID
     * @return Total number of journal entries for the user
     */
    private Long getTotalJournalEntriesForUser(Long userId) {
        try {
            // Try repository method first
            try {
                return journalEntryRepository.countByCreatedById(userId);
            } catch (Exception ex) {
                // Fallback: count journal entries by filtering from all entries
                return journalEntryRepository.findAll().stream()
                        .filter(journalEntry -> {
                            try {
                                // Use proper Lombok getter
                                User user = journalEntry.getCreatedBy();
                                return user != null && user.getId().equals(userId);
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .count();
            }
        } catch (Exception e) {
            System.err.println("Error getting total journal entries for user " + userId + ": " + e.getMessage());
            return 0L;
        }
    }

    /**
     * Gets journal entries created this month for a specific user.
     * 
     * @param userId The user ID
     * @return Number of journal entries created this month
     */
    private Long getJournalEntriesCreatedThisMonthForUser(Long userId) {
        try {
            // Calculate start and end of current month
            YearMonth currentMonth = YearMonth.now();
            LocalDate startOfMonth = currentMonth.atDay(1);
            LocalDate endOfMonth = currentMonth.atEndOfMonth();

            // Convert to epoch seconds (JournalEntry uses seconds, not millis)
            long startTime = startOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().getEpochSecond();
            long endTime = endOfMonth.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();

            // Get user's journal entries and filter by month
            try {
                return journalEntryRepository.findByCreatedById(userId).stream()
                        .filter(journalEntry -> {
                            try {
                                // Combine creationDate and creationTime to epoch seconds
                                if (journalEntry.getCreationDate() == null || journalEntry.getCreationTime() == null)
                                    return false;
                                long creationDateTime = java.time.LocalDateTime
                                        .of(journalEntry.getCreationDate(), journalEntry.getCreationTime())
                                        .atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
                                return creationDateTime >= startTime && creationDateTime <= endTime;
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .count();
            } catch (Exception ex) {
                // Fallback: filter from all journal entries
                return journalEntryRepository.findAll().stream()
                        .filter(journalEntry -> {
                            try {
                                // Use proper Lombok getters
                                User user = journalEntry.getCreatedBy();
                                if (user != null && user.getId().equals(userId)) {
                                    if (journalEntry.getCreationDate() == null
                                            || journalEntry.getCreationTime() == null)
                                        return false;
                                    long creationDateTime = java.time.LocalDateTime
                                            .of(journalEntry.getCreationDate(), journalEntry.getCreationTime())
                                            .atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
                                    return creationDateTime >= startTime && creationDateTime <= endTime;
                                }
                            } catch (Exception e) {
                                // Ignore
                            }
                            return false;
                        })
                        .count();
            }

        } catch (Exception e) {
            System.err.println("Error calculating monthly journal entries for user " + userId + ": " + e.getMessage());
            return 0L;
        }
    }

    /**
     * Gets journal entries count by user (Map of userId -> count).
     * 
     * @return Map containing user IDs and their journal entry counts
     */
    private Map<Long, Long> getJournalEntriesCountByUser() {
        try {
            // Try to use existing repository method
            try {
                List<Object[]> results = journalEntryRepository.countJournalEntriesByUser();
                Map<Long, Long> journalEntriesCountMap = new HashMap<>();

                for (Object[] result : results) {
                    Long userId = (Long) result[0];
                    Long count = (Long) result[1];
                    journalEntriesCountMap.put(userId, count);
                }

                return journalEntriesCountMap;
            } catch (Exception ex) {
                // Fallback: manually count from all journal entries
                return journalEntryRepository.findAll().stream()
                        .filter(journalEntry -> {
                            try {
                                // Use proper Lombok getter
                                return journalEntry.getCreatedBy() != null;
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .collect(Collectors.groupingBy(
                                // Use proper Lombok getter
                                journalEntry -> journalEntry.getCreatedBy().getId(),
                                Collectors.counting()));
            }
        } catch (Exception e) {
            System.err.println("Error getting journal entries count by user: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Gets total journal entries created this month across all users.
     * 
     * @return Total journal entries created this month
     */
    private Long getTotalJournalEntriesCreatedThisMonth() {
        try {
            // Calculate start and end of current month
            YearMonth currentMonth = YearMonth.now();
            LocalDate startOfMonth = currentMonth.atDay(1);
            LocalDate endOfMonth = currentMonth.atEndOfMonth();

            // Convert to epoch seconds (JournalEntry uses seconds, not millis)
            long startTime = startOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().getEpochSecond();
            long endTime = endOfMonth.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();

            // Count all journal entries created this month
            return journalEntryRepository.findAll().stream()
                    .filter(journalEntry -> {
                        try {
                            if (journalEntry.getCreationDate() == null || journalEntry.getCreationTime() == null)
                                return false;
                            long creationDateTime = java.time.LocalDateTime
                                    .of(journalEntry.getCreationDate(), journalEntry.getCreationTime())
                                    .atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
                            return creationDateTime >= startTime && creationDateTime <= endTime;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .count();

        } catch (Exception e) {
            System.err.println("Error calculating total monthly journal entries: " + e.getMessage());
            return 0L;
        }
    }

    /**
     * Gets monthly journal entry creation trends.
     * 
     * @return Map containing monthly trend data
     */
    private Map<String, Object> getMonthlyJournalTrends() {
        try {
            Map<String, Object> trends = new HashMap<>();

            // Get current month data
            Long thisMonth = getTotalJournalEntriesCreatedThisMonth();

            // Get previous month data
            YearMonth previousMonth = YearMonth.now().minusMonths(1);
            LocalDate startOfPrevMonth = previousMonth.atDay(1);
            LocalDate endOfPrevMonth = previousMonth.atEndOfMonth();

            // Convert to epoch seconds (JournalEntry uses seconds, not millis)
            long prevStartTime = startOfPrevMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().getEpochSecond();
            long prevEndTime = endOfPrevMonth.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()
                    .getEpochSecond();

            Long lastMonth = journalEntryRepository.findAll().stream()
                    .filter(journalEntry -> {
                        try {
                            if (journalEntry.getCreationDate() == null || journalEntry.getCreationTime() == null)
                                return false;
                            long creationDateTime = java.time.LocalDateTime
                                    .of(journalEntry.getCreationDate(), journalEntry.getCreationTime())
                                    .atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
                            return creationDateTime >= prevStartTime && creationDateTime <= prevEndTime;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .count();

            trends.put("thisMonth", thisMonth);
            trends.put("lastMonth", lastMonth);
            trends.put("monthOverMonth", lastMonth > 0 ? ((double) (thisMonth - lastMonth) / lastMonth) * 100 : 0.0);
            trends.put("trend", thisMonth > lastMonth ? "up" : thisMonth < lastMonth ? "down" : "stable");

            return trends;
        } catch (Exception e) {
            System.err.println("Error calculating monthly trends: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Gets user journal entry distribution analysis.
     * 
     * @return Map containing distribution data
     */
    private Map<String, Object> getUserJournalDistribution() {
        try {
            List<AdminJournalViewDTO> users = getAllUserJournalViews();
            Map<String, Object> distribution = new HashMap<>();

            Map<String, Long> journalCounts = users.stream()
                    .collect(Collectors.groupingBy(
                            user -> {
                                long journalEntries = user.getTotalJournalEntries();
                                if (journalEntries == 0)
                                    return "No Journal Entries";
                                else if (journalEntries <= 5)
                                    return "1-5 Journal Entries";
                                else if (journalEntries <= 15)
                                    return "6-15 Journal Entries";
                                else if (journalEntries <= 30)
                                    return "16-30 Journal Entries";
                                else
                                    return "30+ Journal Entries";
                            },
                            Collectors.counting()));

            distribution.put("journalDistribution", journalCounts);
            distribution.put("totalUsers", (long) users.size());

            return distribution;
        } catch (Exception e) {
            System.err.println("Error calculating user journal distribution: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Gets engagement analysis.
     * 
     * @param userViews List of user views to analyze
     * @return Map containing engagement analysis data
     */
    private Map<String, Object> getEngagementAnalysis(List<AdminJournalViewDTO> userViews) {
        try {
            Map<String, Object> analysis = new HashMap<>();

            long totalUsers = userViews.size();
            long usersWithJournalEntries = userViews.stream().filter(AdminJournalViewDTO::hasJournalEntries).count();
            long activeUsers = userViews.stream().filter(AdminJournalViewDTO::isJournalActive).count();
            long recentUsers = userViews.stream().filter(AdminJournalViewDTO::hasRecentActivity).count();

            analysis.put("totalUsers", totalUsers);
            analysis.put("usersWithJournalEntries", usersWithJournalEntries);
            analysis.put("activeUsers", activeUsers);
            analysis.put("recentUsers", recentUsers);
            analysis.put("overallEngagementRate",
                    totalUsers > 0 ? (double) usersWithJournalEntries / totalUsers * 100 : 0.0);
            analysis.put("monthlyEngagementRate", totalUsers > 0 ? (double) recentUsers / totalUsers * 100 : 0.0);

            // Calculate average activity scores
            double avgActivityScore = userViews.stream()
                    .filter(AdminJournalViewDTO::hasJournalEntries)
                    .mapToDouble(AdminJournalViewDTO::getActivityScore)
                    .average()
                    .orElse(0.0);
            analysis.put("averageActivityScore", avgActivityScore);

            return analysis;
        } catch (Exception e) {
            System.err.println("Error calculating engagement analysis: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Gets recent activity analysis.
     * 
     * @return Map containing recent activity data
     */
    private Map<String, Object> getRecentActivityAnalysis() {
        try {
            Map<String, Object> activity = new HashMap<>();

            // Get journal entries from last 7 days (convert to epoch seconds)
            long sevenDaysAgo = System.currentTimeMillis() / 1000L - (7L * 24L * 60L * 60L);

            Long recentJournalEntries = journalEntryRepository.findAll().stream()
                    .filter(journalEntry -> {
                        try {
                            if (journalEntry.getCreationDate() == null || journalEntry.getCreationTime() == null)
                                return false;
                            long creationDateTime = java.time.LocalDateTime
                                    .of(journalEntry.getCreationDate(), journalEntry.getCreationTime())
                                    .atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
                            return creationDateTime >= sevenDaysAgo;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .count();

            activity.put("journalEntriesLast7Days", recentJournalEntries);
            activity.put("averageJournalEntriesPerDay", recentJournalEntries / 7.0);
            activity.put("journalEntriesThisMonth", getTotalJournalEntriesCreatedThisMonth());

            return activity;
        } catch (Exception e) {
            System.err.println("Error calculating recent activity analysis: " + e.getMessage());
            return new HashMap<>();
        }
    }

    // ============================================================================
    // ADDITIONAL UTILITY METHODS
    // ============================================================================

    /**
     * Gets journal entries for a specific user by userId.
     * 
     * @param userId The user ID
     * @return List of journal entries for the user
     */
    public List<JournalEntry> getJournalEntriesForUserId(Long userId) {
        try {
            // Try repository method first
            try {
                return journalEntryRepository.findByCreatedById(userId);
            } catch (Exception ex) {
                // Fallback: filter from all journal entries
                return journalEntryRepository.findAll().stream()
                        .filter(journalEntry -> {
                            try {
                                // Use proper Lombok getter
                                User user = journalEntry.getCreatedBy();
                                return user != null && user.getId().equals(userId);
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            System.err.println("Error fetching journal entries for user ID " + userId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Gets journal entries within a specific date range.
     * 
     * @param startDate Start date (epoch seconds)
     * @param endDate   End date (epoch seconds)
     * @return List of journal entries within the date range
     */
    public List<JournalEntry> getJournalEntriesInDateRange(Long startDate, Long endDate) {
        try {
            return journalEntryRepository.findAll().stream()
                    .filter(journalEntry -> {
                        try {
                            if (journalEntry.getCreationDate() == null || journalEntry.getCreationTime() == null)
                                return false;
                            long creationDateTime = java.time.LocalDateTime
                                    .of(journalEntry.getCreationDate(), journalEntry.getCreationTime())
                                    .atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
                            return creationDateTime >= startDate && creationDateTime <= endDate;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error fetching journal entries in date range: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
