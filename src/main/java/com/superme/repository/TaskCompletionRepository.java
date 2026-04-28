package com.superme.repository;

import com.superme.model.Task;
import com.superme.model.TaskCompletion;
import com.superme.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface TaskCompletionRepository extends JpaRepository<TaskCompletion, Long> {
    Optional<TaskCompletion> findByTaskAndCompletionDate(Task task, LocalDate completionDate);
    List<TaskCompletion> findByTask(Task task);
    List<TaskCompletion> findByTaskAndCompletionDateBetween(Task task, LocalDate start, LocalDate end);
    long countByTask(Task task);
    long countByTaskAndCompleted(Task task, boolean completed);
    void deleteByTask(Task task);


    // In TaskCompletionRepository
    List<TaskCompletion> findByCompletionDateAndStatus(LocalDate completionDate, TaskCompletion.CompletionStatus status);

    List<TaskCompletion> findByTaskAndCompleted(Task task, Boolean completed);

    @Query("SELECT tc FROM TaskCompletion tc WHERE tc.task = :task AND tc.completed = false")
    List<TaskCompletion> findPendingCompletionsByTask(@Param("task") Task task);

    @Query("SELECT tc FROM TaskCompletion tc WHERE tc.task = :task AND tc.status = 'PENDING'")
    List<TaskCompletion> findPendingStatusCompletionsByTask(@Param("task") Task task);

    @Query("SELECT tc FROM TaskCompletion tc WHERE tc.task = :task AND tc.completed = false AND tc.completionDate < :today")
    List<TaskCompletion> findOverdueCompletions(@Param("task") Task task, @Param("today") LocalDate today);

    Optional<TaskCompletion> findByTaskAndStatusAndCompletionDate(Task task, TaskCompletion.CompletionStatus completionStatus, LocalDate today);

    List<TaskCompletion> findByCompletionDate(LocalDate today);

    List<TaskCompletion> findByCompletionDateAndTask_CreatedBy(LocalDate completionDate, User createdBy);

    List<TaskCompletion> findByTaskIdAndCompletionDateBetween(Long taskId, LocalDate start, LocalDate end);

    @Transactional
    void deleteByTaskIdAndCompletionDateAfter(Long taskId, LocalDate today);

    @Transactional
    void deleteByTaskIdAndCompletionDateGreaterThanEqual(Long taskId, LocalDate today);

    @Query("""
    SELECT tc
    FROM TaskCompletion tc
    WHERE tc.completionDate = :date
      AND tc.task.assignedTo IN :children
      AND tc.task.sharedWithParent = true
""")
    List<TaskCompletion> findForParentView(@Param("date") LocalDate date,
                                           @Param("children") List<User> children);

    List<TaskCompletion> findByCompletionDateAndTask_AssignedTo(LocalDate targetDate, User user);



    // Single user - DAY/MONTH
    @Query("SELECT tc FROM TaskCompletion tc " +
            "WHERE tc.completionDate BETWEEN :startDate AND :endDate " +
            "AND tc.task.assignedTo = :user")
    List<TaskCompletion> findByCompletionDateBetweenAndTask_AssignedTo(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("user") User user);

    // Parent view - DAY/MONTH
    @Query("""
    SELECT tc
    FROM TaskCompletion tc
    WHERE tc.completionDate BETWEEN :startDate AND :endDate
      AND tc.task.assignedTo IN :children
      AND tc.task.sharedWithParent = true
    ORDER BY tc.completionDate, tc.id
""")
    List<TaskCompletion> findForParentViewRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("children") List<User> children);

    @Query("""
    SELECT tc
    FROM TaskCompletion tc
    JOIN tc.task t
    WHERE t.assignedTo = :user
      AND (:taskId IS NULL OR t.id <> :taskId)
      AND t.startDate <= :endDate
      AND t.endDate   >= :startDate
      AND tc.status <> com.superme.model.TaskCompletion.CompletionStatus.SKIPPED
      AND t.startTime < :endTime
      AND t.endTime   > :startTime

    ORDER BY tc.completionDate ASC
""")
    List<TaskCompletion> findScheduleConflicts(
            @Param("user") User user,
            @Param("taskId") Long taskId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            Pageable pageable
    );



    List<TaskCompletion> findByTaskId(Long id);

    Optional<TaskCompletion> findByTaskIdAndCompletionDate(
            Long taskId,
            LocalDate completionDate
    );

    int countByTask_CreatedByAndCompletionDate(User targetUser, LocalDate today);

    int countByTask_CreatedByAndCompletionDateAndStatus(User targetUser, LocalDate today, TaskCompletion.CompletionStatus completionStatus);
}
