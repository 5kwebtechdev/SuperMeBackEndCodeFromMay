package com.superme.repository;

import com.superme.model.Task;
import com.superme.model.TaskCompletion;
import com.superme.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // ✅ ADD: Import
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> { // ✅ ADD:
                                                                                                    // JpaSpecificationExecutor

        // =========================
        // TAGS QUERIES
        // =========================

        /**
         * Find tasks containing a specific tag
         */
        List<Task> findByTagsContaining(String tag);

        /**
         * Find tasks containing any of the given tags
         */
        List<Task> findByTagsIn(List<String> tags);

        /**
         * Find tasks that contain all of the given tags
         * (Requires custom query because JPA does not support 'all elements in
         * collection' by default)
         */
        @Query("SELECT t FROM Task t JOIN t.tags tag WHERE tag IN :tags GROUP BY t HAVING COUNT(DISTINCT tag) = :tagCount")
        List<Task> findByAllTags(@Param("tags") List<String> tags, @Param("tagCount") long tagCount);

        // ============================================================================
        // EXISTING FAMILY AND USER TASK QUERIES
        // ============================================================================

        // Fetch all tasks for a family
        List<Task> findByFamilyId(Long familyId);

        // Fetch tasks for a user that are active on a specific day
        List<Task> findByAssignedToAndDaysOfWeekContaining(User user, DayOfWeek dayOfWeek);

        // Temporary fix: Find tasks by createdBy OR assignedTo for a specific day
        @Query("SELECT t FROM Task t WHERE (t.assignedTo = :user OR t.createdBy = :user) AND :dayOfWeek MEMBER OF t.daysOfWeek")
        List<Task> findTasksForUserByDay(@Param("user") User user, @Param("dayOfWeek") DayOfWeek dayOfWeek);

        // Fetch tasks for a user that are active on any of the given days
        List<Task> findByAssignedToAndDaysOfWeekIn(User user, List<DayOfWeek> daysOfWeek);

        // Fetch all tasks created by a user
        List<Task> findByCreatedBy(User user);

        // Fetch all tasks for a family and a specific day
        List<Task> findByFamilyIdAndDaysOfWeekContaining(Long familyId, DayOfWeek dayOfWeek);

        // Fetch all tasks for a user that are marked as COMPLETED
        List<Task> findByAssignedToAndStatus(User user, Task.TaskStatus status);

        // Fetch tasks by priority
        List<Task> findByAssignedToAndPriority(User user, Task.Priority priority);

        // Fetch tasks by routine
        List<Task> findByAssignedToAndRoutine(User user, String routine);

        // Fetch tasks by start date range (LocalDate)
        List<Task> findByAssignedToAndStartDateBetween(User user, LocalDate start, LocalDate end);

        // Fetch tasks by end date range (LocalDate)
        List<Task> findByAssignedToAndEndDateBetween(User user, LocalDate start, LocalDate end);

        // Fetch all tasks assigned to a user
        List<Task> findByAssignedTo(User user);

        // Fetch tasks by assigned user ID and date range
        List<Task> findByAssignedToIdAndStartDateBetween(Long assignedToId, LocalDate startDate, LocalDate endDate);

        // ============================================================================
        // ADMIN ANALYTICS QUERIES (UPDATED TO MATCH ENTITY STRUCTURE)
        // ============================================================================

        /**
         * Count distinct users who have created at least one task
         */
        @Query("SELECT COUNT(DISTINCT t.createdBy.id) FROM Task t WHERE t.createdBy IS NOT NULL")
        Long countUsersWithTasks();

        /**
         * Count distinct users who have been assigned at least one task
         */
        @Query("SELECT COUNT(DISTINCT t.assignedTo.id) FROM Task t WHERE t.assignedTo IS NOT NULL")
        Long countUsersWithAssignedTasks();

        /**
         * Count total tasks created by a specific user
         */
        @Query("SELECT COUNT(t) FROM Task t WHERE t.createdBy.id = :userId")
        Long countTasksByCreatedByUserId(@Param("userId") Long userId);

        /**
         * Count total tasks assigned to a specific user
         */
        @Query("SELECT COUNT(t) FROM Task t WHERE t.assignedTo.id = :userId")
        Long countTasksByAssignedToUserId(@Param("userId") Long userId);

        /**
         * Count completed tasks assigned to a specific user
         */
        @Query("SELECT COUNT(t) FROM Task t WHERE t.assignedTo.id = :userId AND t.status = 'COMPLETED'")
        Long countCompletedTasksByUserId(@Param("userId") Long userId);

        /**
         * Count incomplete tasks assigned to a specific user
         */
        @Query("SELECT COUNT(t) FROM Task t WHERE t.assignedTo.id = :userId AND t.status <> 'COMPLETED'")
        Long countIncompleteTasksByUserId(@Param("userId") Long userId);

        /**
         * Count tasks by status for a specific user
         */
        @Query("SELECT COUNT(t) FROM Task t WHERE t.assignedTo.id = :userId AND t.status = :status")
        Long countTasksByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Task.TaskStatus status);

        /**
         * Get task counts grouped by user (assigned to)
         */
        @Query("SELECT t.assignedTo.id, COUNT(t) FROM Task t WHERE t.assignedTo IS NOT NULL GROUP BY t.assignedTo.id")
        List<Object[]> countTasksByAssignedUser();

        /**
         * Get task counts grouped by creator
         */
        @Query("SELECT t.createdBy.id, COUNT(t) FROM Task t WHERE t.createdBy IS NOT NULL GROUP BY t.createdBy.id")
        List<Object[]> countTasksByCreator();

        /**
         * Get completed task counts grouped by user (assigned to)
         */
        @Query("SELECT t.assignedTo.id, COUNT(t) FROM Task t WHERE t.assignedTo IS NOT NULL AND t.status = 'COMPLETED' GROUP BY t.assignedTo.id")
        List<Object[]> countCompletedTasksByUser();

        /**
         * Find earliest task creation date for a user (by assigned to)
         */
        @Query("SELECT MIN(t.createdAt) FROM Task t WHERE t.assignedTo.id = :userId AND t.createdAt IS NOT NULL")
        java.time.LocalDateTime findFirstTaskDateByAssignedUserId(@Param("userId") Long userId);

        /**
         * Find latest task creation date for a user (by assigned to)
         */
        @Query("SELECT MAX(t.createdAt) FROM Task t WHERE t.assignedTo.id = :userId AND t.createdAt IS NOT NULL")
        java.time.LocalDateTime findLastTaskDateByAssignedUserId(@Param("userId") Long userId);

        /**
         * Find earliest task creation date for a user (by creator)
         */
        @Query("SELECT MIN(t.createdAt) FROM Task t WHERE t.createdBy.id = :userId AND t.createdAt IS NOT NULL")
        java.time.LocalDateTime findFirstTaskDateByCreatorUserId(@Param("userId") Long userId);

        /**
         * Find latest task creation date for a user (by creator)
         */
        @Query("SELECT MAX(t.createdAt) FROM Task t WHERE t.createdBy.id = :userId AND t.createdAt IS NOT NULL")
        java.time.LocalDateTime findLastTaskDateByCreatorUserId(@Param("userId") Long userId);

        /**
         * Count overdue tasks for a user (tasks with end date in the past and not
         * completed)
         */
        @Query("SELECT COUNT(t) FROM Task t WHERE t.assignedTo.id = :userId AND t.endDate < :currentTime AND t.status <> 'COMPLETED'")
        Long countOverdueTasksByUserId(@Param("userId") Long userId, @Param("currentTime") LocalDate currentTime);

        /**
         * Count tasks due today for a user
         */
        @Query("SELECT COUNT(t) FROM Task t WHERE t.assignedTo.id = :userId AND t.endDate BETWEEN :startOfDay AND :endOfDay")
        Long countTasksDueTodayByUserId(@Param("userId") Long userId, @Param("startOfDay") LocalDate startOfDay,
                        @Param("endOfDay") LocalDate endOfDay);

        /**
         * Count tasks by priority for a user
         */
        @Query("SELECT COUNT(t) FROM Task t WHERE t.assignedTo.id = :userId AND t.priority = :priority")
        Long countTasksByUserIdAndPriority(@Param("userId") Long userId, @Param("priority") Task.Priority priority);

        /**
         * Count tasks by routine for a user
         */
        @Query("SELECT COUNT(t) FROM Task t WHERE t.assignedTo.id = :userId AND t.routine = :routine")
        Long countTasksByUserIdAndRoutine(@Param("userId") Long userId, @Param("routine") String routine);

        /**
         * Get task completion statistics by user
         */
        @Query("SELECT t.assignedTo.id, " +
                        "COUNT(t) as totalTasks, " +
                        "SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) as completedTasks, " +
                        "SUM(CASE WHEN t.status <> 'COMPLETED' THEN 1 ELSE 0 END) as incompleteTasks " +
                        "FROM Task t WHERE t.assignedTo IS NOT NULL GROUP BY t.assignedTo.id")
        List<Object[]> getTaskCompletionStatsByUser();

        /**
         * Find tasks within date range for analytics
         */
        @Query("SELECT t FROM Task t WHERE t.startDate >= :startDate AND t.endDate <= :endDate")
        List<Task> findTasksInDateRange(@Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);

        /**
         * Count tasks created in a specific time period
         */
        @Query("SELECT COUNT(t) FROM Task t WHERE t.createdAt BETWEEN :startDate AND :endDate")
        Long countTasksCreatedInPeriod(@Param("startDate") java.time.LocalDateTime startDate,
                        @Param("endDate") java.time.LocalDateTime endDate);

        /**
         * Get task creation trends by day
         */
        @Query("SELECT DATE(t.createdAt) as date, COUNT(t) as count " +
                        "FROM Task t WHERE t.createdAt BETWEEN :startDate AND :endDate " +
                        "GROUP BY DATE(t.createdAt) ORDER BY DATE(t.createdAt)")
        List<Object[]> getTaskCreationTrendsByDay(@Param("startDate") java.time.LocalDateTime startDate,
                        @Param("endDate") java.time.LocalDateTime endDate);

        // ============================================================================
        // CONVENIENCE METHODS FOR BACKWARD COMPATIBILITY
        // ============================================================================

        /**
         * Find tasks by user (assuming this means assigned to user)
         */
        default List<Task> findByUser(User user) {
                return findByAssignedTo(user);
        }

        /**
         * Find tasks by user ID (assuming this means assigned to user)
         */
        default List<Task> findByUserId(Long userId) {
                return findByAssignedToId(userId);
        }

        /**
         * Find tasks by assigned to user ID
         */
        List<Task> findByAssignedToId(Long assignedToId);

        /**
         * Find tasks by creator user ID
         */
        List<Task> findByCreatedById(Long createdById);

        /**
         * Count tasks by user ID (backward compatibility - using assigned to)
         */
        default Long countTasksByUserId(Long userId) {
                return countTasksByAssignedToUserId(userId);
        }

        List<Task> findByCreatedByAndStatusAndDaysOfWeekContaining(User createdBy, Task.TaskStatus status,
                        DayOfWeek day);

        List<Task> findByCreatedByAndPriorityAndDaysOfWeekContaining(User createdBy, Task.Priority priority,
                        DayOfWeek day);

        List<Task> findByCreatedByAndRoutineAndDaysOfWeekContaining(User createdBy, String routine,
                        DayOfWeek day);

        // ✅ Only this version is correct!
        List<Task> findByCreatedByIdAndStartDateBetween(Long userId, LocalDate startDate, LocalDate endDate);


        @Query("SELECT COUNT(t) FROM Task t WHERE t.createdBy = :user AND t.createdAt >= :start AND t.createdAt < :end")
        int countByCreatedByAndDate(@Param("user") User user,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

        @Query("SELECT COUNT(t) FROM Task t WHERE t.createdBy = :user AND t.createdAt >= :start AND t.createdAt < :end AND t.status = 'COMPLETED'")
        int countCompletedByCreatedByAndDate(@Param("user") User user,
                                             @Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);

        @Query("SELECT COUNT(t) FROM Task t WHERE t.createdBy = :user AND :today BETWEEN t.startDate AND t.endDate")
        int countByCreatedByAndActiveOnDate(@Param("user") User user, @Param("today") LocalDate today);

        @Query("SELECT COUNT(t) FROM Task t WHERE t.createdBy = :user AND :today BETWEEN t.startDate AND t.endDate AND t.status = 'COMPLETED'")
        int countCompletedByCreatedByAndActiveOnDate(@Param("user") User user, @Param("today") LocalDate today);

        @Query("SELECT COUNT(t) FROM Task t WHERE t.assignedTo.id = :userId AND t.status = 'COMPLETED'")
        int countCompletedTasksByUser(Long userId);

        int countByCreatedBy(User targetUser);

        @Query("SELECT COUNT(t) FROM Task t WHERE t.createdBy = :createdBy AND t.status = 'COMPLETED'")
        int countCompletedByCreatedBy(User createdBy);

        @Query("SELECT COUNT(t) FROM Task t WHERE t.createdBy = :createdBy AND t.startDate >= :startDate AND t.endDate <= :endDate")
        int countByCreatedByAndDateRange(User createdBy, LocalDate startDate, LocalDate endDate);

        @Query("SELECT COUNT(t) FROM Task t WHERE t.createdBy = :createdBy AND t.status = 'COMPLETED' AND t.startDate >= :startDate AND t.endDate <= :endDate")
        int countCompletedByCreatedByAndDateRange(User createdBy, LocalDate startDate, LocalDate endDate);

        List<Task> findByStatusAndEndDateBefore(Task.TaskStatus status, LocalDate date);

        List<Task> findByStatusInAndEndDateAfter(List<Task.TaskStatus> statuses, LocalDate date);

        @Query("SELECT t FROM Task t WHERE t.endDate < :today AND t.status IN :statuses")
        List<Task> findExpiredTasks(@Param("today") LocalDate today, @Param("statuses") List<Task.TaskStatus> statuses);

        Optional<Task> findByIdAndCreatedBy(Long taskId, User user);

    List<Task> findByCreatedByIdInAndStartDateBetween(List<Long> targetUserIds, LocalDate startDate, LocalDate endDate);
}