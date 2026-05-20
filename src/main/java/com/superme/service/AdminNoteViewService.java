package com.superme.service;

import com.superme.dto.AdminNoteViewDTO;
import com.superme.dto.AdminNoteViewDTO.NoteStatistics;
import com.superme.dto.AdminNoteOverviewResponse;
import com.superme.dto.AdminNoteOverviewResponse.NoteFilterCriteria;
import com.superme.model.User;
import com.superme.model.Note;
import com.superme.repository.UserRepository;
import com.superme.repository.NoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing admin note view operations.
 *
 * Provides comprehensive note analytics and overview functionality
 * for administrative dashboard with advanced search and filtering capabilities.
 *
 * @author MindfullB Admin Team
 * @version 2.0
 */
@Service
public class AdminNoteViewService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NoteRepository noteRepository;

    // ============================================================================
    // PUBLIC API METHODS
    // ============================================================================

    public List<AdminNoteViewDTO> getAllUserNoteViews() {
        try {
            List<User> users = userRepository.findAll();
            List<AdminNoteViewDTO> result = new ArrayList<>();

            for (User user : users) {
                AdminNoteViewDTO dto = createUserNoteViewDTO(user);
                result.add(dto);
            }

            return result;
        } catch (Exception e) {
            System.err.println("Error fetching user note views: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public NoteStatistics getNoteStatistics() {
        try {
            Long totalNotesAllUsers = noteRepository.count();
            Long totalUsers = userRepository.count();
            Map<Long, Long> notesPerUser = getNotesCountByUser();
            Long usersWithNotes = (long) notesPerUser.size();
            Double averageNotesPerUser = totalUsers > 0 ? (double) totalNotesAllUsers / totalUsers : 0.0;
            Long totalMonthlyNotes = getTotalNotesCreatedThisMonth();

            return new NoteStatistics(
                    totalNotesAllUsers,
                    totalUsers,
                    averageNotesPerUser,
                    usersWithNotes,
                    totalMonthlyNotes);

        } catch (Exception e) {
            System.err.println("Error calculating note statistics: " + e.getMessage());
            e.printStackTrace();
            return new NoteStatistics();
        }
    }

    // ============================================================================
    // SEARCH AND FILTER FUNCTIONALITY
    // ============================================================================

    public List<AdminNoteViewDTO> filterUsersBySearchTerm(String searchTerm) {
        List<AdminNoteViewDTO> users = getAllUserNoteViews();
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return users;
        }
        // Only search by userId now (username removed)
        return AdminNoteViewDTO.filterBySearchTerm(users, searchTerm).stream()
                .map(user -> user.getHighlightedVersion(searchTerm))
                .collect(Collectors.toList());
    }

    public List<AdminNoteViewDTO> filterUsers(NoteFilterCriteria criteria) {
        List<AdminNoteViewDTO> users = getAllUserNoteViews();
        if (criteria == null || !criteria.hasFilters()) {
            return users;
        }
        if (criteria.getSearchTerm() != null && !criteria.getSearchTerm().trim().isEmpty()) {
            users = AdminNoteViewDTO.filterBySearchTerm(users, criteria.getSearchTerm());
        }
        if (criteria.getMinNoteCount() != null || criteria.getMaxNoteCount() != null) {
            users = AdminNoteViewDTO.filterByNoteCount(users,
                    criteria.getMinNoteCount(), criteria.getMaxNoteCount());
        }
        if (criteria.getMinMonthlyNotes() != null || criteria.getMaxMonthlyNotes() != null) {
            users = AdminNoteViewDTO.filterByMonthlyActivity(users,
                    criteria.getMinMonthlyNotes(), criteria.getMaxMonthlyNotes());
        }
        if (criteria.getMinActivityScore() != null || criteria.getMaxActivityScore() != null) {
            users = AdminNoteViewDTO.filterByActivityScore(users,
                    criteria.getMinActivityScore(), criteria.getMaxActivityScore());
        }
        if (criteria.getHasNotes() != null) {
            users = AdminNoteViewDTO.filterByNoteExistence(users, criteria.getHasNotes());
        }
        if (criteria.getHasRecentActivity() != null) {
            users = AdminNoteViewDTO.filterByRecentActivity(users, criteria.getHasRecentActivity());
        }
        if (criteria.getSearchTerm() != null && !criteria.getSearchTerm().trim().isEmpty()) {
            users = users.stream()
                    .map(user -> user.getHighlightedVersion(criteria.getSearchTerm()))
                    .collect(Collectors.toList());
        }
        return users;
    }

    public List<AdminNoteViewDTO> filterUsersWithPagination(NoteFilterCriteria criteria, int limit, int offset) {
        List<AdminNoteViewDTO> filteredUsers = filterUsers(criteria);
        return filteredUsers.stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    public long getFilterResultsCount(NoteFilterCriteria criteria) {
        return filterUsers(criteria).size();
    }

    public Map<String, Object> getAvailableFilterOptions() {
        Map<String, Object> options = new HashMap<>();
        try {
            List<AdminNoteViewDTO> users = getAllUserNoteViews();
            List<String> noteCountRanges = Arrays.asList(
                    "1-5", "6-20", "21-50", "51+");
            List<String> monthlyActivityRanges = Arrays.asList(
                    "1-3", "4-10", "11-20", "21+");
            List<String> activityScoreRanges = Arrays.asList(
                    "0-25%", "25-50%", "50-75%", "75-100%");
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalUsers", users.size());
            statistics.put("usersWithNotes", users.stream().filter(AdminNoteViewDTO::hasNotes).count());
            statistics.put("usersWithoutNotes", users.stream().filter(user -> !user.hasNotes()).count());
            statistics.put("usersWithRecentActivity",
                    users.stream().filter(AdminNoteViewDTO::hasRecentActivity).count());
            statistics.put("usersNeedingAttention", users.stream().filter(AdminNoteViewDTO::needsAttention).count());
            options.put("noteCountRanges", noteCountRanges);
            options.put("monthlyActivityRanges", monthlyActivityRanges);
            options.put("activityScoreRanges", activityScoreRanges);
            options.put("filterStatistics", statistics);
            // Only userId is searchable now (username removed)
            options.put("searchFields", Arrays.asList("userId"));
        } catch (Exception e) {
            System.err.println("Error getting filter options: " + e.getMessage());
        }
        return options;
    }

    public AdminNoteOverviewResponse.FilterStatistics getFilterStatistics() {
        List<AdminNoteViewDTO> users = getAllUserNoteViews();
        return new AdminNoteOverviewResponse.FilterStatistics(users);
    }

    public Map<String, Object> getDetailedAnalyticsWithFilters(NoteFilterCriteria criteria) {
        List<AdminNoteViewDTO> users = filterUsers(criteria);
        Map<String, Object> analytics = new HashMap<>();
        try {
            NoteStatistics globalStats = getNoteStatistics();
            NoteStatistics filteredStats = AdminNoteViewDTO.getNoteStatistics(users);
            analytics.put("globalStatistics", globalStats);
            analytics.put("filteredStatistics", filteredStats);
            analytics.put("filterCriteria", criteria);
            analytics.put("totalUsers", getAllUserNoteViews().size());
            analytics.put("filteredUsers", users.size());
            analytics.put("filterEfficiency",
                    users.size() > 0 ? (double) users.size() / getAllUserNoteViews().size() * 100 : 0.0);
            Map<String, Object> engagementBreakdown = new HashMap<>();
            long highEngagement = users.stream()
                    .filter(user -> user.hasNotes() && user.getActivityScore() >= 70.0)
                    .count();
            long mediumEngagement = users.stream()
                    .filter(user -> user.hasNotes() && user.getActivityScore() >= 30.0
                            && user.getActivityScore() < 70.0)
                    .count();
            long lowEngagement = users.stream()
                    .filter(user -> user.hasNotes() && user.getActivityScore() < 30.0)
                    .count();
            engagementBreakdown.put("highEngagement", highEngagement);
            engagementBreakdown.put("mediumEngagement", mediumEngagement);
            engagementBreakdown.put("lowEngagement", lowEngagement);
            engagementBreakdown.put("highEngagementPercentage",
                    users.size() > 0 ? (double) highEngagement / users.size() * 100 : 0.0);
            analytics.put("engagementBreakdown", engagementBreakdown);
            Map<String, Object> activityBreakdown = new HashMap<>();
            long usersWithNotes = users.stream().filter(AdminNoteViewDTO::hasNotes).count();
            long usersWithoutNotes = users.size() - usersWithNotes;
            long usersWithRecentActivity = users.stream().filter(AdminNoteViewDTO::hasRecentActivity).count();
            activityBreakdown.put("usersWithNotes", usersWithNotes);
            activityBreakdown.put("usersWithoutNotes", usersWithoutNotes);
            activityBreakdown.put("usersWithRecentActivity", usersWithRecentActivity);
            activityBreakdown.put("noteAdoptionRate",
                    users.size() > 0 ? (double) usersWithNotes / users.size() * 100 : 0.0);
            analytics.put("activityBreakdown", activityBreakdown);
            analytics.put("topPerformers", AdminNoteViewDTO.sortByNoteCount(users)
                    .stream().limit(10).collect(Collectors.toList()));
            analytics.put("usersNeedingAttention", users.stream()
                    .filter(AdminNoteViewDTO::needsAttention)
                    .collect(Collectors.toList()));
            analytics.put("monthlyTrends", getMonthlyTrendsForUsers(users));
        } catch (Exception e) {
            System.err.println("Error calculating detailed analytics: " + e.getMessage());
            analytics.put("error", "Error calculating analytics: " + e.getMessage());
        }
        return analytics;
    }

    // ============================================================================
    // SEARCH HELPER METHODS
    // ============================================================================

    /**
     * Search users by userId only (username removed)
     */
    public List<AdminNoteViewDTO> searchUsersWithCriteria(String searchTerm, boolean searchUserId,
            boolean searchUsername) {
        List<AdminNoteViewDTO> users = getAllUserNoteViews();
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return users;
        }
        // Only search by userId now
        return users.stream()
                .filter(user -> user.matchesSearchCriteria(searchTerm, searchUserId))
                .map(user -> user.getHighlightedVersion(searchTerm))
                .collect(Collectors.toList());
    }

    /**
     * Get search suggestions based on partial input (userId only)
     */
    public List<String> getSearchSuggestions(String partialInput, int limit) {
        if (partialInput == null || partialInput.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<AdminNoteViewDTO> users = getAllUserNoteViews();
        Set<String> suggestions = new HashSet<>();
        String lowerInput = partialInput.toLowerCase();
        // Only userId suggestions
        users.stream()
                .map(user -> user.getUserId().toString())
                .filter(userId -> userId.toLowerCase().startsWith(lowerInput))
                .forEach(suggestions::add);
        return suggestions.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ============================================================================
    // EXISTING ANALYTICS METHODS (MAINTAINED FOR COMPATIBILITY)
    // ============================================================================

    public Map<String, Object> getQuickStats() {
        try {
            Map<String, Object> quickStats = new HashMap<>();
            NoteStatistics stats = getNoteStatistics();
            quickStats.put("totalNotes", stats.getTotalNotesAllUsers());
            quickStats.put("totalUsers", stats.getTotalUsers());
            quickStats.put("averageNotesPerUser", stats.getFormattedAverageNotesPerUser());
            quickStats.put("usersWithNotes", stats.getUsersWithNotes());
            quickStats.put("usersWithoutNotes", stats.getUsersWithoutNotes());
            quickStats.put("engagementRate", stats.getFormattedEngagementRate());
            quickStats.put("monthlyGrowthRate", stats.getFormattedMonthlyGrowthRate());
            quickStats.put("usersWithRecentNotes", getUsersWithRecentActivity().size());
            quickStats.put("notesCreatedThisMonth", getTotalNotesCreatedThisMonth());
            quickStats.put("highlyEngagedUsers", getHighlyEngagedUsers().size());
            return quickStats;
        } catch (Exception e) {
            System.err.println("Error calculating quick stats: " + e.getMessage());
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public Map<String, Object> getDetailedNoteAnalytics() {
        try {
            Map<String, Object> analytics = new HashMap<>();
            NoteStatistics stats = getNoteStatistics();
            List<AdminNoteViewDTO> userViews = getAllUserNoteViews();
            analytics.put("noteStatistics", stats);
            analytics.put("topNoteCreators", getTopNoteCreators(5));
            analytics.put("activeUsers", getUsersWithRecentActivity());
            analytics.put("inactiveUsers", getUsersWithoutRecentActivity());
            analytics.put("monthlyTrends", getMonthlyNoteTrends());
            analytics.put("userNoteDistribution", getUserNoteDistribution());
            analytics.put("engagementAnalysis", getEngagementAnalysis(userViews));
            analytics.put("recentActivity", getRecentActivityAnalysis());
            analytics.put("mostActiveThisMonth", getMostActiveUsersThisMonth(10));
            analytics.put("usersNeedingAttention", getUsersNeedingAttention());
            analytics.put("highlyEngagedUsers", getHighlyEngagedUsers());
            return analytics;
        } catch (Exception e) {
            System.err.println("Error calculating detailed note analytics: " + e.getMessage());
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    // ============================================================================
    // FILTERING AND ANALYSIS METHODS
    // ============================================================================

    public List<AdminNoteViewDTO> getUsersWithNotes() {
        return getAllUserNoteViews().stream()
                .filter(AdminNoteViewDTO::hasNotes)
                .collect(Collectors.toList());
    }

    public List<AdminNoteViewDTO> getUsersWithoutNotes() {
        return getAllUserNoteViews().stream()
                .filter(user -> !user.hasNotes())
                .collect(Collectors.toList());
    }

    public List<AdminNoteViewDTO> getUsersWithRecentActivity() {
        return getAllUserNoteViews().stream()
                .filter(AdminNoteViewDTO::hasRecentActivity)
                .collect(Collectors.toList());
    }

    public List<AdminNoteViewDTO> getUsersWithoutRecentActivity() {
        return getAllUserNoteViews().stream()
                .filter(user -> !user.hasRecentActivity())
                .collect(Collectors.toList());
    }

    public List<AdminNoteViewDTO> getTopNoteCreators(int limit) {
        return getAllUserNoteViews().stream()
                .filter(AdminNoteViewDTO::hasNotes)
                .sorted((u1, u2) -> Long.compare(u2.getTotalNotes(), u1.getTotalNotes()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<AdminNoteViewDTO> getMostActiveUsersThisMonth(int limit) {
        return getAllUserNoteViews().stream()
                .filter(AdminNoteViewDTO::hasRecentActivity)
                .sorted((u1, u2) -> Long.compare(u2.getNotesCreatedThisMonth(), u1.getNotesCreatedThisMonth()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<AdminNoteViewDTO> getUsersNeedingAttention() {
        return getAllUserNoteViews().stream()
                .filter(AdminNoteViewDTO::needsAttention)
                .sorted((u1, u2) -> Long.compare(u2.getTotalNotes(), u1.getTotalNotes()))
                .collect(Collectors.toList());
    }

    public List<AdminNoteViewDTO> getHighlyEngagedUsers() {
        return getAllUserNoteViews().stream()
                .filter(AdminNoteViewDTO::isHighlyEngaged)
                .sorted((u1, u2) -> Double.compare(u2.getActivityScore(), u1.getActivityScore()))
                .collect(Collectors.toList());
    }

    // ============================================================================
    // PRIVATE HELPER METHODS
    // ============================================================================

    private AdminNoteViewDTO createUserNoteViewDTO(User user) {
        AdminNoteViewDTO dto = new AdminNoteViewDTO();
        dto.setUserId(user.getId());
        dto.setName(user.getName());
        dto.setPhone(user.getPhone());
        dto.setEmail(user.getEmail());
        dto.setAge(user.getAge());
        dto.setGender(user.getGender());
        dto.setRole(user.getRole() != null ? user.getRole().name() : null);
        dto.setAccountStatus(user.isEnabled() ? "active" : "inactive");
        dto.setLastActive(user.getLastLoginDate());
        try {
            Long totalNotes = getTotalNotesForUser(user.getId());
            Long notesThisMonth = getNotesCreatedThisMonthForUser(user.getId());
            List<Note> userNotes = getNotesForUserId(user.getId());
            java.time.LocalDate firstNoteDate = userNotes.stream()
                    .map(Note::getCreatedDate)
                    .filter(java.util.Objects::nonNull)
                    .min(java.time.LocalDate::compareTo)
                    .orElse(null);
            java.time.LocalDate lastNoteDate = userNotes.stream()
                    .map(Note::getCreatedDate)
                    .filter(java.util.Objects::nonNull)
                    .max(java.time.LocalDate::compareTo)
                    .orElse(null);
            dto.setTotalNotes(totalNotes);
            dto.setNotesCreatedThisMonth(notesThisMonth);
            dto.setFirstNoteDate(firstNoteDate);
            dto.setLastNoteDate(lastNoteDate);
        } catch (Exception e) {
            System.err.println("Error calculating note stats for user " + user.getId() + ": " + e.getMessage());
            e.printStackTrace();
            dto.setTotalNotes(0L);
            dto.setNotesCreatedThisMonth(0L);
            dto.setFirstNoteDate(null);
            dto.setLastNoteDate(null);
        }
        return dto;
    }

    private Long getTotalNotesForUser(Long userId) {
        try {
            try {
                return noteRepository.countByUserId(userId);
            } catch (Exception ex) {
                return noteRepository.findAll().stream()
                        .filter(note -> {
                            try {
                                User user = note.getUser();
                                return user != null && user.getId().equals(userId);
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .count();
            }
        } catch (Exception e) {
            System.err.println("Error getting total notes for user " + userId + ": " + e.getMessage());
            return 0L;
        }
    }

    private Long getNotesCreatedThisMonthForUser(Long userId) {
        try {
            YearMonth currentMonth = YearMonth.now();
            LocalDate startOfMonth = currentMonth.atDay(1);
            LocalDate endOfMonth = currentMonth.atEndOfMonth();
            long startTime = startOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endTime = endOfMonth.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            try {
                return noteRepository.findByUserId(userId).stream()
                        .filter(note -> {
                            try {
                                java.time.LocalDate createdDate = note.getCreatedDate();
                                if (createdDate == null)
                                    return false;
                                long createdEpoch = createdDate.atStartOfDay(java.time.ZoneId.systemDefault())
                                        .toInstant().toEpochMilli();
                                return createdEpoch >= startTime && createdEpoch <= endTime;
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .count();
            } catch (Exception ex) {
                return noteRepository.findAll().stream()
                        .filter(note -> {
                            try {
                                User user = note.getUser();
                                if (user != null && user.getId().equals(userId)) {
                                    java.time.LocalDate createdDate = note.getCreatedDate();
                                    if (createdDate == null)
                                        return false;
                                    long createdEpoch = createdDate.atStartOfDay(java.time.ZoneId.systemDefault())
                                            .toInstant().toEpochMilli();
                                    return createdEpoch >= startTime && createdEpoch <= endTime;
                                }
                            } catch (Exception e) {
                            }
                            return false;
                        })
                        .count();
            }
        } catch (Exception e) {
            System.err.println("Error calculating monthly notes for user " + userId + ": " + e.getMessage());
            return 0L;
        }
    }

    private Map<Long, Long> getNotesCountByUser() {
        try {
            try {
                List<Object[]> results = noteRepository.countNotesByUser();
                Map<Long, Long> notesCountMap = new HashMap<>();
                for (Object[] result : results) {
                    Long userId = (Long) result[0];
                    Long count = (Long) result[1];
                    notesCountMap.put(userId, count);
                }
                return notesCountMap;
            } catch (Exception ex) {
                return noteRepository.findAll().stream()
                        .filter(note -> {
                            try {
                                return note.getUser() != null;
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .collect(Collectors.groupingBy(
                                note -> note.getUser().getId(),
                                Collectors.counting()));
            }
        } catch (Exception e) {
            System.err.println("Error getting notes count by user: " + e.getMessage());
            return new HashMap<>();
        }
    }

    private Long getTotalNotesCreatedThisMonth() {
        try {
            YearMonth currentMonth = YearMonth.now();
            LocalDate startOfMonth = currentMonth.atDay(1);
            LocalDate endOfMonth = currentMonth.atEndOfMonth();
            long startTime = startOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endTime = endOfMonth.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            return noteRepository.findAll().stream()
                    .filter(note -> {
                        try {
                            java.time.LocalDate createdDate = note.getCreatedDate();
                            if (createdDate == null)
                                return false;
                            long createdEpoch = createdDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
                                    .toEpochMilli();
                            return createdEpoch >= startTime && createdEpoch <= endTime;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .count();
        } catch (Exception e) {
            System.err.println("Error calculating total monthly notes: " + e.getMessage());
            return 0L;
        }
    }

    private Map<String, Object> getMonthlyNoteTrends() {
        try {
            Map<String, Object> trends = new HashMap<>();
            Long thisMonth = getTotalNotesCreatedThisMonth();
            YearMonth previousMonth = YearMonth.now().minusMonths(1);
            LocalDate startOfPrevMonth = previousMonth.atDay(1);
            LocalDate endOfPrevMonth = previousMonth.atEndOfMonth();
            long prevStartTime = startOfPrevMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long prevEndTime = endOfPrevMonth.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()
                    .toEpochMilli();
            Long lastMonth = noteRepository.findAll().stream()
                    .filter(note -> {
                        try {
                            java.time.LocalDate createdDate = note.getCreatedDate();
                            if (createdDate == null)
                                return false;
                            long createdEpoch = createdDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
                                    .toEpochMilli();
                            return createdEpoch >= prevStartTime && createdEpoch <= prevEndTime;
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

    private Map<String, Object> getMonthlyTrendsForUsers(List<AdminNoteViewDTO> users) {
        try {
            Map<String, Object> trends = new HashMap<>();
            long thisMonthTotal = users.stream()
                    .mapToLong(AdminNoteViewDTO::getNotesCreatedThisMonth)
                    .sum();
            long totalNotes = users.stream()
                    .mapToLong(AdminNoteViewDTO::getTotalNotes)
                    .sum();
            trends.put("thisMonth", thisMonthTotal);
            trends.put("totalNotes", totalNotes);
            trends.put("monthlyPercentage", totalNotes > 0 ? (double) thisMonthTotal / totalNotes * 100 : 0.0);
            return trends;
        } catch (Exception e) {
            System.err.println("Error calculating monthly trends for users: " + e.getMessage());
            return new HashMap<>();
        }
    }

    private Map<String, Object> getUserNoteDistribution() {
        try {
            List<AdminNoteViewDTO> users = getAllUserNoteViews();
            Map<String, Long> noteCounts = users.stream()
                    .collect(Collectors.groupingBy(
                            user -> {
                                long notes = user.getTotalNotes();
                                if (notes == 0)
                                    return "No Notes";
                                else if (notes <= 5)
                                    return "1-5 Notes";
                                else if (notes <= 20)
                                    return "6-20 Notes";
                                else if (notes <= 50)
                                    return "21-50 Notes";
                                else
                                    return "50+ Notes";
                            },
                            Collectors.counting()));
            Map<String, Object> distribution = new HashMap<>();
            distribution.put("noteDistribution", noteCounts);
            distribution.put("totalUsers", (long) users.size());
            return distribution;
        } catch (Exception e) {
            System.err.println("Error calculating user note distribution: " + e.getMessage());
            return new HashMap<>();
        }
    }

    private Map<String, Object> getEngagementAnalysis(List<AdminNoteViewDTO> userViews) {
        try {
            Map<String, Object> analysis = new HashMap<>();
            long totalUsers = userViews.size();
            long usersWithNotes = userViews.stream().filter(AdminNoteViewDTO::hasNotes).count();
            long activeUsers = userViews.stream().filter(AdminNoteViewDTO::isActiveUser).count();
            long recentUsers = userViews.stream().filter(AdminNoteViewDTO::hasRecentActivity).count();
            long highlyEngaged = userViews.stream().filter(AdminNoteViewDTO::isHighlyEngaged).count();
            analysis.put("totalUsers", totalUsers);
            analysis.put("usersWithNotes", usersWithNotes);
            analysis.put("activeUsers", activeUsers);
            analysis.put("recentUsers", recentUsers);
            analysis.put("highlyEngagedUsers", highlyEngaged);
            analysis.put("overallEngagementRate", totalUsers > 0 ? (double) usersWithNotes / totalUsers * 100 : 0.0);
            analysis.put("monthlyEngagementRate", totalUsers > 0 ? (double) recentUsers / totalUsers * 100 : 0.0);
            analysis.put("highEngagementRate", totalUsers > 0 ? (double) highlyEngaged / totalUsers * 100 : 0.0);
            double avgActivityScore = userViews.stream()
                    .filter(AdminNoteViewDTO::hasNotes)
                    .mapToDouble(AdminNoteViewDTO::getActivityScore)
                    .average()
                    .orElse(0.0);
            analysis.put("averageActivityScore", avgActivityScore);
            return analysis;
        } catch (Exception e) {
            System.err.println("Error calculating engagement analysis: " + e.getMessage());
            return new HashMap<>();
        }
    }

    private Map<String, Object> getRecentActivityAnalysis() {
        try {
            Map<String, Object> activity = new HashMap<>();
            long sevenDaysAgo = System.currentTimeMillis() - (7L * 24L * 60L * 60L * 1000L);
            Long recentNotes = noteRepository.findAll().stream()
                    .filter(note -> {
                        try {
                            java.time.LocalDate createdDate = note.getCreatedDate();
                            if (createdDate == null)
                                return false;
                            long createdEpoch = createdDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
                                    .toEpochMilli();
                            return createdEpoch >= sevenDaysAgo;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .count();
            activity.put("notesLast7Days", recentNotes);
            activity.put("averageNotesPerDay", recentNotes / 7.0);
            activity.put("notesThisMonth", getTotalNotesCreatedThisMonth());
            return activity;
        } catch (Exception e) {
            System.err.println("Error calculating recent activity analysis: " + e.getMessage());
            return new HashMap<>();
        }
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    public List<Note> getNotesForUserId(Long userId) {
        try {
            try {
                return noteRepository.findByUserId(userId);
            } catch (Exception ex) {
                return noteRepository.findAll().stream()
                        .filter(note -> {
                            try {
                                User user = note.getUser();
                                return user != null && user.getId().equals(userId);
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            System.err.println("Error fetching notes for user ID " + userId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Note> getNotesInDateRange(Long startDate, Long endDate) {
        try {
            return noteRepository.findAll().stream()
                    .filter(note -> {
                        try {
                            java.time.LocalDate createdDate = note.getCreatedDate();
                            if (createdDate == null)
                                return false;
                            long createdEpoch = createdDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
                                    .toEpochMilli();
                            return createdEpoch >= startDate && createdEpoch <= endDate;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error fetching notes in date range: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get top users with most notes (for admin dashboard cards).
     * Uses repository method with Pageable for efficiency.
     */
    public List<AdminNoteViewDTO> getTopUsersWithMostNotes(int limit) {
        List<Object[]> topUsers = noteRepository
                .findTopUsersWithMostNotes(org.springframework.data.domain.PageRequest.of(0, limit));
        List<Long> userIds = topUsers.stream()
                .map(obj -> (Long) obj[0])
                .collect(Collectors.toList());
        Map<Long, Long> noteCounts = topUsers.stream()
                .collect(Collectors.toMap(
                        obj -> (Long) obj[0],
                        obj -> (Long) obj[1]));
        List<User> users = userRepository.findAllById(userIds);
        // Maintain order as in topUsers
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
        List<AdminNoteViewDTO> result = new ArrayList<>();
        for (Long userId : userIds) {
            User user = userMap.get(userId);
            if (user != null) {
                AdminNoteViewDTO dto = createUserNoteViewDTO(user);
                dto.setTotalNotes(noteCounts.getOrDefault(userId, 0L));
                result.add(dto);
            }
        }
        return result;
    }
}
