package com.superme.repository;

import com.superme.model.Habit;
import com.superme.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HabitRepository extends JpaRepository<Habit, Long>, JpaSpecificationExecutor<Habit> {
        // Fetch all habits for a family
        List<Habit> findByFamilyId(Long familyId);

        // Fetch habits by tag (single tag)
        List<Habit> findByTagsContaining(String tag);

        // Fetch habits by multiple tags (any match)
        List<Habit> findByTagsIn(List<String> tags);

        // Fetch habits for a user with a specific tag
        List<Habit> findByCreatedByAndTagsContaining(User user, String tag);

        // Fetch habits for a user with any of the given tags
        List<Habit> findByCreatedByAndTagsIn(User user, List<String> tags);

        // Fetch habits for a user that are active on a specific day
        List<Habit> findByCreatedByAndDaysOfWeekContaining(User user, DayOfWeek dayOfWeek);

        // Fetch habits for a user that are active on any of the given days
        List<Habit> findByCreatedByAndDaysOfWeekIn(User user, List<DayOfWeek> daysOfWeek);

        // Fetch all habits created by a user
        List<Habit> findByCreatedBy(User user);

        // Fetch all habits for a family and a specific day
        List<Habit> findByFamilyIdAndDaysOfWeekContaining(Long familyId, DayOfWeek dayOfWeek);

        // Fetch all habits for a user by status
        List<Habit> findByCreatedByAndStatus(User user, Habit.HabitStatus status);

        // Fetch habits by priority
        List<Habit> findByCreatedByAndPriority(User user, Habit.Priority priority);

        // Fetch habits by routine
        List<Habit> findByCreatedByAndRoutine(User user, Habit.Routine routine);

        // Fetch habits by start date range (LocalDate)
        List<Habit> findByCreatedByAndStartDateBetween(User user, java.time.LocalDate start, java.time.LocalDate end);

        // Fetch habits by end date range (LocalDate)
        List<Habit> findByCreatedByAndEndDateBetween(User user, java.time.LocalDate start, java.time.LocalDate end);

        List<Habit> findByCreatedById(Long createdById);

        // Method to fetch by id and start date and end date
        List<Habit> findByCreatedByIdAndStartDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

        // New methods for admin statistics
        @Query("SELECT COUNT(h) FROM Habit h WHERE h.createdBy.id = :userId")
        Long countHabitsByUserId(@Param("userId") Long userId);

        @Query("SELECT COUNT(h) FROM Habit h WHERE h.createdBy.id = :userId AND h.status = 'COMPLETED'")
        Long countCompletedHabitsByUserId(@Param("userId") Long userId);

        @Query("SELECT h.createdBy.id, COUNT(h) FROM Habit h GROUP BY h.createdBy.id")
        List<Object[]> countHabitsByUser();

        @Query("SELECT COUNT(DISTINCT h.createdBy.id) FROM Habit h")
        Long countUsersWithHabits();

        @Query("SELECT MIN(h.startDate) FROM Habit h WHERE h.createdBy.id = :userId AND h.startDate IS NOT NULL")
        java.time.LocalDate findFirstHabitDateByUserId(@Param("userId") Long userId);

        @Query("SELECT MAX(h.startDate) FROM Habit h WHERE h.createdBy.id = :userId AND h.startDate IS NOT NULL")
        java.time.LocalDate findLastHabitDateByUserId(@Param("userId") Long userId);

        List<Habit> findByCreatedByAndStatusAndDaysOfWeekContaining(User createdBy, Habit.HabitStatus status,
                        DayOfWeek day);

        List<Habit> findByCreatedByAndPriorityAndDaysOfWeekContaining(User createdBy, Habit.Priority priority,
                        DayOfWeek day);

        List<Habit> findByCreatedByAndRoutineAndDaysOfWeekContaining(User createdBy, Habit.Routine routine,
                        DayOfWeek day);

        @Query("SELECT h FROM Habit h WHERE h.startDate >= :startDate AND h.endDate <= :endDate")
        List<Habit> findHabitsInDateRange(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);


        int countByCreatedByAndCreatedAt(User createdBy, LocalDate createdAt);
        int countCompletedByCreatedByAndCreatedAt(User createdBy, LocalDate createdAt);

        @Query("SELECT COUNT(h) FROM Habit h WHERE h.createdBy = :user AND :today BETWEEN h.startDate AND h.endDate")
        int countByCreatedByAndActiveOnDate(@Param("user") User user, @Param("today") LocalDate today);

        @Query("SELECT COUNT(h) FROM Habit h WHERE h.createdBy = :user AND :today BETWEEN h.startDate AND h.endDate AND h.status = 'COMPLETED'")
        int countCompletedByCreatedByAndActiveOnDate(@Param("user") User user, @Param("today") LocalDate today);


        @Query("SELECT COUNT(h) FROM Habit h WHERE h.createdBy.id = :userId AND h.status = 'COMPLETED'")
        int countCompletedHabitsByUser(Long userId);

        @Query("SELECT COUNT(DISTINCT DATE(h.createdAt)) FROM Habit h WHERE h.createdBy.id = :userId AND h.status = 'COMPLETED' AND h.createdAt >= :sinceDate")
        int countUniqueDaysWithHabits(Long userId, LocalDate sinceDate);

        int countByCreatedBy(User targetUser);

        @Query("SELECT COUNT(h) FROM Habit h WHERE h.createdBy = :createdBy AND h.status = 'COMPLETED'")
        int countCompletedByCreatedBy(User createdBy);

        @Query("SELECT COUNT(h) FROM Habit h WHERE h.createdBy = :createdBy AND h.startDate >= :startDate AND h.endDate <= :endDate")
        int countByCreatedByAndDateRange(User createdBy, LocalDate startDate, LocalDate endDate);

        @Query("SELECT COUNT(h) FROM Habit h WHERE h.createdBy = :createdBy AND h.status = 'COMPLETED' AND h.startDate >= :startDate AND h.endDate <= :endDate")
        int countCompletedByCreatedByAndDateRange(User createdBy, LocalDate startDate, LocalDate endDate);

        List<Habit> findByStatusAndEndDateBefore(Habit.HabitStatus status, LocalDate date);

        List<Habit> findByStatusInAndEndDateAfter(List<Habit.HabitStatus> statuses, LocalDate date);


        Optional<Habit> findByIdAndCreatedBy(Long habitId, User user);

    List<Habit> findByCreatedByIdInAndStartDateBetween(List<Long> targetUserIds, LocalDate startDate, LocalDate endDate);

        List<Habit> findByAssignedTo(User user);
}

