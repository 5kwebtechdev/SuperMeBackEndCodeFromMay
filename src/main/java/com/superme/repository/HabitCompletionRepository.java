package com.superme.repository;

import com.superme.model.Habit;
import com.superme.model.HabitCompletion;
import com.superme.model.TaskCompletion;
import com.superme.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HabitCompletionRepository extends JpaRepository<HabitCompletion, Long> {

    Optional<HabitCompletion> findByHabitAndCompletionDate(Habit habit, LocalDate completionDate);

    List<HabitCompletion> findByHabit(Habit habit);

    List<HabitCompletion> findByHabitAndCompletionDateBetween(Habit habit, LocalDate start, LocalDate end);

    long countByHabit(Habit habit);

    long countByHabitAndCompleted(Habit habit, boolean completed);

    void deleteByHabit(Habit habit);

    // For marking missed habits
    List<HabitCompletion> findByCompletionDateAndStatus(LocalDate completionDate, HabitCompletion.CompletionStatus status);

    List<HabitCompletion> findByHabitAndCompleted(Habit habit, Boolean completed);

    @Query("SELECT hc FROM HabitCompletion hc WHERE hc.habit = :habit AND hc.completed = false")
    List<HabitCompletion> findPendingCompletionsByHabit(@Param("habit") Habit habit);

    @Query("SELECT hc FROM HabitCompletion hc WHERE hc.habit = :habit AND hc.status = 'PENDING'")
    List<HabitCompletion> findPendingStatusCompletionsByHabit(@Param("habit") Habit habit);

    Optional<HabitCompletion> findByHabitAndStatusAndCompletionDate(Habit habit, com.superme.model.HabitCompletion.CompletionStatus completionStatus, LocalDate today);

    List<HabitCompletion> findByCompletionDateAndHabit_CreatedBy(LocalDate today, User user);

    List<HabitCompletion> findByHabitIdAndCompletionDateBetween(Long habitId, LocalDate start, LocalDate end);

    @Transactional
    void deleteByHabitIdAndCompletionDateAfter(Long habitId, LocalDate today);

    @Transactional
    void deleteByHabitIdAndCompletionDateGreaterThanEqual(Long habitId, LocalDate today);

    List<HabitCompletion> findByCompletionDateAndHabit_AssignedTo(LocalDate targetDate, User user);

    @Query("""
                SELECT hc
                FROM HabitCompletion hc
                WHERE hc.completionDate = :date
                  AND hc.habit.assignedTo IN :children
                  AND hc.habit.sharedWithParent = true
            """)
    List<HabitCompletion> findForParentView(
            @Param("date") LocalDate date,
            @Param("children") List<User> children);

    // 1️⃣ Single user - DAY/MONTH (replaces findByCompletionDateAndHabit_AssignedTo)
    @Query("SELECT hc FROM HabitCompletion hc " +
            "WHERE hc.completionDate BETWEEN :startDate AND :endDate " +
            "AND hc.habit.assignedTo = :user")
    List<HabitCompletion> findByCompletionDateBetweenAndHabit_AssignedTo(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("user") User user);

    // 2️⃣ Parent view - DAY/MONTH (extends findForParentView)
    @Query("""
                SELECT hc
                FROM HabitCompletion hc
                WHERE hc.completionDate BETWEEN :startDate AND :endDate
                  AND hc.habit.assignedTo IN :children
                  AND hc.habit.sharedWithParent = true
                ORDER BY hc.completionDate, hc.id
            """)
    List<HabitCompletion> findForParentViewRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("children") List<User> children);

    @Query("""
    SELECT hc
    FROM HabitCompletion hc
    JOIN hc.habit h
    WHERE h.assignedTo = :user
      AND (:habitId IS NULL OR h.id <> :habitId)
      AND h.startDate <= :endDate
      AND h.endDate   >= :startDate
      AND h.startTime < :endTime
      AND h.endTime   > :startTime
      AND hc.status <> com.superme.model.HabitCompletion.CompletionStatus.SKIPPED

    ORDER BY hc.completionDate ASC
""")
    List<HabitCompletion> findScheduleConflicts(
            @Param("user") User user,
            @Param("habitId") Long habitId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            Pageable pageable
    );



    List<HabitCompletion> findByHabitId(Long id);
    Optional<HabitCompletion> findByHabitIdAndCompletionDate(Long habitId, LocalDate date);

    int countByHabit_CreatedByAndCompletionDateAndStatus(User targetUser, LocalDate today, HabitCompletion.CompletionStatus completionStatus);

    int countByHabit_CreatedByAndCompletionDate(User targetUser, LocalDate today);

    List<HabitCompletion> findByCompletionDate(LocalDate completionDate);

}
