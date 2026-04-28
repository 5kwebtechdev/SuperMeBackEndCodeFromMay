package com.superme.repository;

import java.time.LocalDate;

import com.superme.model.CalendarEvent;
import com.superme.model.Family;
import com.superme.model.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for CalendarEvent entity operations.
 * 
 * Updated to fix SQL syntax errors and align with actual CalendarEvent entity
 * structure.
 * 
 * @author MindfullB Team
 * @version 2.1
 */
@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

        // ============================================================================
        // FAMILY-BASED QUERIES
        // ============================================================================

        /**
         * Find all events for a specific family.
         *
         * @param family The family entity
         * @return List of calendar events for the family
         */
        List<CalendarEvent> findByFamily(Family family);

        /**
         * Find all events for a specific family by familyId.
         *
         * @param familyId The family ID
         * @return List of calendar events for the family
         */
        List<CalendarEvent> findByFamilyId(Long familyId);

        /**
         * Find all events for a specific family within a time range.
         *
         * @param family    The family entity
         * @param startDate Start of time range (epoch millis)
         * @param endDate   End of time range (epoch millis)
         * @return List of calendar events within the time range
         */
        List<CalendarEvent> findByFamilyAndStartDateBetween(Family family, LocalDate startDate, LocalDate endDate);

        /**
         * Find all events for a specific family by familyId within a time range.
         *
         * @param familyId  The family ID
         * @param startDate Start of time range (epoch millis)
         * @param endDate   End of time range (epoch millis)
         * @return List of calendar events within the time range
         */
        List<CalendarEvent> findByFamilyIdAndStartDateBetween(Long familyId, LocalDate startDate, LocalDate endDate);

        /**
         * Find all events for a specific family on a specific date, ordered by start
         * time.
         *
         * @param family     The family entity
         * @param startOfDay Start of day (epoch millis)
         * @param endOfDay   End of day (epoch millis)
         * @return List of calendar events for the day, ordered by start time
         */
        List<CalendarEvent> findByFamilyAndStartDateBetweenOrderByStartDateAsc(Family family, LocalDate startOfDay,
                                                                               LocalDate endOfDay);

        /**
         * Find all events for a specific family with title containing search term
         * (case-insensitive).
         *
         * @param family The family entity
         * @param title  Search term for title
         * @return List of calendar events with matching titles
         */
        List<CalendarEvent> findByFamilyAndTitleContainingIgnoreCase(Family family, String title);

        /**
         * Find all events for a specific family by type(s) on a specific date.
         *
         * @param family     The family entity
         * @param types      List of event types to filter by
         * @param startOfDay Start of day (epoch millis)
         * @param endOfDay   End of day (epoch millis)
         * @return List of calendar events matching types for the day
         */
        List<CalendarEvent> findByFamilyAndTypeInAndStartDateBetweenOrderByStartDateAsc(
                Family family, List<String> types, LocalDate startOfDay, LocalDate endOfDay);

        /**
         * Find all completed events for a specific family.
         *
         * @param family The family entity
         * @return List of completed calendar events
         */
        List<CalendarEvent> findByFamilyAndCompletedTrue(Family family);

        /**
         * Find all incomplete events for a specific family.
         *
         * @param family The family entity
         * @return List of incomplete calendar events
         */
        List<CalendarEvent> findByFamilyAndCompletedFalse(Family family);

        /**
         * Find all events for a specific family by type.
         *
         * @param family The family entity
         * @param type   Event type to filter by
         * @return List of calendar events of the specified type
         */
        List<CalendarEvent> findByFamilyAndType(Family family, String type);

        /**
         * Find all events for a specific family by type and completion status.
         *
         * @param family    The family entity
         * @param type      Event type to filter by
         * @param completed Completion status to filter by
         * @return List of calendar events matching type and completion status
         */
        List<CalendarEvent> findByFamilyAndTypeAndCompleted(Family family, String type, boolean completed);

        // ============================================================================
        // USER-BASED QUERIES (by User object)
        // ============================================================================

        /**
         * Find all events created by a specific user.
         *
         * @param user The user entity
         * @return List of calendar events created by the user
         */
        List<CalendarEvent> findByCreatedBy(User user);

        /**
         * Find all events for a specific user within a time range.
         *
         * @param user      The user entity
         * @param startDate Start of time range (epoch millis)
         * @param endDate   End of time range (epoch millis)
         * @return List of calendar events within the time range
         */
        List<CalendarEvent> findByCreatedByAndStartDateBetween(User user, LocalDate startDate, LocalDate endDate);

        /**
         * Find all events for a specific user on a specific date, ordered by start
         * time.
         *
         * @param user       The user entity
         * @param startOfDay Start of day (epoch millis)
         * @param endOfDay   End of day (epoch millis)
         * @return List of calendar events for the day, ordered by start time
         */
        List<CalendarEvent> findByCreatedByAndStartDateBetweenOrderByStartDateAsc(User user, LocalDate startOfDay,
                                                                                  LocalDate endOfDay);

        /**
         * Find all events for a specific user with title containing search term
         * (case-insensitive).
         *
         * @param user  The user entity
         * @param title Search term for title
         * @return List of calendar events with matching titles
         */
        List<CalendarEvent> findByCreatedByAndTitleContainingIgnoreCase(User user, String title);

        /**
         * Find all events for a specific user by type(s) on a specific date.
         *
         * @param user       The user entity
         * @param types      List of event types to filter by
         * @param startOfDay Start of day (epoch millis)
         * @param endOfDay   End of day (epoch millis)
         * @return List of calendar events matching types for the day
         */
        List<CalendarEvent> findByCreatedByAndTypeInAndStartDateBetweenOrderByStartDateAsc(
                User user, List<String> types, LocalDate startOfDay, LocalDate endOfDay);

        /**
         * Find all completed events for a specific user.
         *
         * @param user The user entity
         * @return List of completed calendar events
         */
        List<CalendarEvent> findByCreatedByAndCompletedTrue(User user);

        /**
         * Find all incomplete events for a specific user.
         *
         * @param user The user entity
         * @return List of incomplete calendar events
         */
        List<CalendarEvent> findByCreatedByAndCompletedFalse(User user);

        /**
         * Find all events for a specific user by type.
         *
         * @param user The user entity
         * @param type Event type to filter by
         * @return List of calendar events of the specified type
         */
        List<CalendarEvent> findByCreatedByAndType(User user, String type);

        /**
         * Find all events for a specific user by type and completion status.
         *
         * @param user      The user entity
         * @param type      Event type to filter by
         * @param completed Completion status to filter by
         * @return List of calendar events matching type and completion status
         */
        List<CalendarEvent> findByCreatedByAndTypeAndCompleted(User user, String type, boolean completed);

        // ============================================================================
        // USER-BASED QUERIES (by userId) - PRIMARY INTERFACE
        // ============================================================================

        /**
         * Find all events created by a specific userId.
         *
         * @param userId The user ID
         * @return List of calendar events created by the user
         */
        List<CalendarEvent> findByCreatedById(Long userId);

        /**
         * Find all events for a specific userId within a time range.
         *
         * @param userId    The user ID
         * @param startDate Start of time range (epoch millis)
         * @param endDate   End of time range (epoch millis)
         * @return List of calendar events within the time range
         */
        List<CalendarEvent> findByCreatedByIdAndStartDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

        /**
         * Find all events for a specific userId on a specific date, ordered by start
         * time.
         *
         * @param userId     The user ID
         * @param startOfDay Start of day (epoch millis)
         * @param endOfDay   End of day (epoch millis)
         * @return List of calendar events for the day, ordered by start time
         */
        List<CalendarEvent> findByCreatedByIdAndStartDateBetweenOrderByStartDateAsc(Long userId, LocalDate startOfDay,
                                                                                    LocalDate endOfDay);

        /**
         * Find all events for a specific userId by type(s) on a specific date.
         *
         * @param userId     The user ID
         * @param types      List of event types to filter by
         * @param startOfDay Start of day (epoch millis)
         * @param endOfDay   End of day (epoch millis)
         * @return List of calendar events matching types for the day
         */
        List<CalendarEvent> findByCreatedByIdAndTypeInAndStartDateBetweenOrderByStartDateAsc(
                Long userId, List<String> types, LocalDate startOfDay, LocalDate endOfDay);

        /**
         * Find all completed events for a specific userId.
         *
         * @param userId The user ID
         * @return List of completed calendar events
         */
        List<CalendarEvent> findByCreatedByIdAndCompletedTrue(Long userId);

        /**
         * Find all incomplete events for a specific userId.
         *
         * @param userId The user ID
         * @return List of incomplete calendar events
         */
        List<CalendarEvent> findByCreatedByIdAndCompletedFalse(Long userId);

        /**
         * Find all events for a specific userId by type.
         *
         * @param userId The user ID
         * @param type   Event type to filter by
         * @return List of calendar events of the specified type
         */
        List<CalendarEvent> findByCreatedByIdAndType(Long userId, String type);

        /**
         * Find all events for a specific userId by type and completion status.
         *
         * @param userId    The user ID
         * @param type      Event type to filter by
         * @param completed Completion status to filter by
         * @return List of calendar events matching type and completion status
         */
        List<CalendarEvent> findByCreatedByIdAndTypeAndCompleted(Long userId, String type, boolean completed);

        /**
         * Find all events for a specific userId with title containing search term
         * (case-insensitive).
         *
         * @param userId The user ID
         * @param title  Search term for title
         * @return List of calendar events with matching titles
         */
        List<CalendarEvent> findByCreatedByIdAndTitleContainingIgnoreCase(Long userId, String title);

        /**
         * Find all events for a specific userId by type within a time range.
         * Used for mood tracking and other type-specific date queries.
         *
         * @param userId     The user ID
         * @param type       Event type to filter by
         * @param startOfDay Start of day (epoch millis)
         * @param endOfDay   End of day (epoch millis)
         * @return List of calendar events matching type within the time range
         */
        List<CalendarEvent> findByCreatedByIdAndTypeAndStartDateBetween(Long userId, String type, LocalDate startOfDay,
                                                                        LocalDate endOfDay);

        // ============================================================================
        // ADMIN ANALYTICS QUERIES (SIMPLIFIED AND FIXED)
        // ============================================================================

        /**
         * Count total number of events for a specific user.
         */
        @Query("SELECT COUNT(e) FROM CalendarEvent e WHERE e.createdBy.id = :userId")
        Long countEventsByUserId(@Param("userId") Long userId);

        /**
         * Count events by user across all users.
         */
        @Query("SELECT e.createdBy.id, COUNT(e) FROM CalendarEvent e WHERE e.createdBy IS NOT NULL GROUP BY e.createdBy.id")
        List<Object[]> countEventsByUser();

        /**
         * Count total number of users who have created at least one event.
         */
        @Query("SELECT COUNT(DISTINCT e.createdBy.id) FROM CalendarEvent e WHERE e.createdBy IS NOT NULL")
        Long countUsersWithEvents();

        /**
         * Count total number of completed events for a specific user.
         */
        @Query("SELECT COUNT(e) FROM CalendarEvent e WHERE e.createdBy.id = :userId AND e.completed = true")
        Long countCompletedEventsByUserId(@Param("userId") Long userId);

        /**
         * Count total number of incomplete events for a specific user.
         */
        @Query("SELECT COUNT(e) FROM CalendarEvent e WHERE e.createdBy.id = :userId AND e.completed = false")
        Long countIncompleteEventsByUserId(@Param("userId") Long userId);

        /**
         * Count events by type across all users.
         */
        @Query("SELECT e.type, COUNT(e) FROM CalendarEvent e WHERE e.type IS NOT NULL GROUP BY e.type")
        List<Object[]> countEventsByType();

        /**
         * Count events by type for a specific user.
         */
        @Query("SELECT e.type, COUNT(e) FROM CalendarEvent e WHERE e.createdBy.id = :userId AND e.type IS NOT NULL GROUP BY e.type")
        List<Object[]> countEventsByTypeForUser(@Param("userId") Long userId);

        /**
         * Count events created within a specific time range.
         */
        @Query("SELECT COUNT(e) FROM CalendarEvent e WHERE e.createdAt BETWEEN :startDate AND :endDate")
        Long countEventsInDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

        /**
         * Count events by month for analytics (simplified).
         */
        @Query("SELECT COUNT(e) FROM CalendarEvent e WHERE e.createdAt IS NOT NULL")
        Long countEventsByMonth(@Param("month") int month);

        /**
         * Find all events with user information for admin overview.
         */
        @Query("SELECT e FROM CalendarEvent e JOIN FETCH e.createdBy WHERE e.createdBy IS NOT NULL")
        List<CalendarEvent> findAllWithUser();

        /**
         * Count events created in the last N days.
         */
        @Query("SELECT COUNT(e) FROM CalendarEvent e WHERE e.createdAt >= :cutoffTime")
        Long countRecentEvents(@Param("cutoffTime") LocalDate cutoffTime);

        /**
         * Find recent events for a user (last N days).
         */
        @Query("SELECT e FROM CalendarEvent e WHERE e.createdBy.id = :userId AND e.createdAt >= :cutoffTime ORDER BY e.createdAt DESC")
        List<CalendarEvent> findRecentEventsForUser(@Param("userId") Long userId,
                                                    @Param("cutoffTime") LocalDate cutoffTime);

        /**
         * Find earliest event creation date for a user.
         */
        @Query("SELECT MIN(e.createdAt) FROM CalendarEvent e WHERE e.createdBy.id = :userId")
        LocalDate findFirstEventDateByUserId(@Param("userId") Long userId);

        /**
         * Find latest event creation date for a user.
         */
        @Query("SELECT MAX(e.createdAt) FROM CalendarEvent e WHERE e.createdBy.id = :userId")
        LocalDate findLastEventDateByUserId(@Param("userId") Long userId);

        /**
         * Count today's events for a user.
         */
        @Query("SELECT COUNT(e) FROM CalendarEvent e WHERE e.createdBy.id = :userId AND e.startDate BETWEEN :startOfDay AND :endOfDay")
        Long countTodaysEventsByUserId(@Param("userId") Long userId, @Param("startOfDay") LocalDate startOfDay,
                                       @Param("endOfDay") LocalDate endOfDay);

        /**
         * Get comprehensive event statistics by user for analytics.
         */
        @Query("SELECT e.createdBy.id, " +
                "COUNT(e) as totalEvents, " +
                "SUM(CASE WHEN e.completed = true THEN 1 ELSE 0 END) as completedEvents, " +
                "SUM(CASE WHEN e.completed = false THEN 1 ELSE 0 END) as incompleteEvents " +
                "FROM CalendarEvent e WHERE e.createdBy IS NOT NULL " +
                "GROUP BY e.createdBy.id")
        List<Object[]> getEventStatisticsByUser();

        // ============================================================================
        // SIMPLE ANALYTICS METHODS (WITHOUT COMPLEX NATIVE QUERIES)
        // ============================================================================

        /**
         * Find top users by event count (using simple approach)
         */
        default List<Object[]> findTopUsersWithMostEvents(int limit) {
                List<Object[]> allCounts = countEventsByUser();
                return allCounts.stream()
                        .sorted((a, b) -> Long.compare((Long) b[1], (Long) a[1]))
                        .limit(limit)
                        .collect(java.util.stream.Collectors.toList());
        }

        /**
         * Get monthly event counts (simplified without native SQL)
         */
        default List<Object[]> getEventCountsByMonth(int year) {
                // Return empty list for now - can be implemented later with proper date
                // handling
                return new java.util.ArrayList<>();
        }

        /**
         * Find top users by completion rate (simplified)
         */
        default List<Object[]> findTopUsersByCompletionRate(int limit) {
                // Return empty list for now - can be implemented later
                return new java.util.ArrayList<>();
        }

        // ============================================================================
        // CONVENIENCE METHODS FOR BACKWARD COMPATIBILITY
        // ============================================================================

        /**
         * Alias for findByCreatedById for backward compatibility.
         *
         * @param userId The user ID
         * @return List of calendar events for the user
         */
        default List<CalendarEvent> findByUserId(Long userId) {
                return findByCreatedById(userId);
        }

        /**
         * Alias for findByCreatedBy for backward compatibility.
         *
         * @param user The user entity
         * @return List of calendar events for the user
         */
        default List<CalendarEvent> findByUser(User user) {
                return findByCreatedBy(user);
        }

        /**
         * Find all events that repeat on a specific day of week.
         */
        List<CalendarEvent> findByDaysOfWeekContaining(java.time.DayOfWeek dayOfWeek);

        /**
         * Find all events that repeat every day.
         */
        List<CalendarEvent> findByIsEverydayTrue();

        /**
         * Find all events that repeat every weekend.
         */
        List<CalendarEvent> findByIsEveryWeekendTrue();

        /**
         * Find all events with a specific tag.
         */
        List<CalendarEvent> findByTagsContaining(String tag);

        /**
         * Find all events by priority.
         */
        List<CalendarEvent> findByPriority(CalendarEvent.Priority priority);

        /**
         * Find all events for a user by priority.
         */
        List<CalendarEvent> findByCreatedByIdAndPriority(Long userId, CalendarEvent.Priority priority);

        // ✅ ADD: Only the missing method that we actually need
        @Query("SELECT ce FROM CalendarEvent ce WHERE ce.createdBy.id = :userId AND ce.type = :type AND ce.startDate <= :endDate AND (ce.endDate IS NULL OR ce.endDate >= :startDate)")
        List<CalendarEvent> findByCreatedByIdAndTypeInDateRange(@Param("userId") Long userId,
                                                                @Param("type") String type, @Param("startDate") LocalDate startDate,
                                                                @Param("endDate") LocalDate endDate);

        @Query("SELECT ce FROM CalendarEvent ce WHERE ce.createdBy.id IN :userIds AND ce.type = :type AND ce.startDate <= :endDate AND (ce.endDate IS NULL OR ce.endDate >= :startDate)")
        List<CalendarEvent> findByCreatedByIdInAndTypeInDateRange(
                @Param("userIds") List<Long> userIds,
                @Param("type") String type,
                @Param("startDate") LocalDate startDate,
                @Param("endDate") LocalDate endDate);



}