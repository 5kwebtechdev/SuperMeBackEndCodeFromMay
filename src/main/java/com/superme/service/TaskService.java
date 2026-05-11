package com.superme.service;

import com.superme.dto.*;
import com.superme.enums.ActivityType;
import com.superme.enums.Relationship;
import com.superme.exception.BusinessException;
import com.superme.exception.ResourceNotFoundException;
import com.superme.exception.UnauthorizedActionException;
import com.superme.model.*;
import com.superme.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {
    private final HabitCompletionRepository habitCompletionRepository;
    private final HabitRepository habitRepository;

    private final TaskRepository taskRepository;
    private final TaskCompletionRepository taskCompletionRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final FamilyMemberService familyMemberService;
    private final CoinTransactionRepository coinTransactionRepository;
    private final UserRepository userRepository;
    private final BadgeService badgeService;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM dd, yyyy");


    // Add task with validation for dates and duplicate prevention
    @Transactional
    public Task addTask(Task task, User user, TaskRequest request) {

        validateTaskDates(task);
        checkForDuplicateTaskOrHabit(task, task.getAssignedTo());

        User assignee = user; // default self

        // 👨‍👩‍👧 Parent creating task for child
        if (user.getRelationship() == Relationship.PARENT && request.getForUserId() != null) {

            User child = userRepository.findById(request.getForUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Child user not found"));

            FamilyMember childMember = familyMemberService
                    .getByUserAndFamilyId(child, user.getFamily().getId())
                    .orElseThrow(() -> new UnauthorizedActionException("Target user is not a family member."));

            if (!"child".equalsIgnoreCase(childMember.getRelationship()) ||
                    !child.getFamily().getId().equals(user.getFamily().getId())) {
                throw new UnauthorizedActionException("Can only add tasks for your own child.");
            }

            // 🎯 Parent reward constraint
            if (request.getRewardCoins() != 5 && request.getRewardCoins() != 10) {
                throw new BusinessException(
                        "Coin reward must be either 5 or 10 for parent-created tasks."
                );
            }

            assignee = child;
        }

        // ✅ OWNERSHIP
        task.setCreatedBy(user);
        task.setAssignedTo(assignee);
        task.setFamily(user.getFamily());

        // ✅ DEFAULT VISIBILITY
        task.setSharedWithParent(request.getSharedWithParent() == null || request.getSharedWithParent());

        // ✅ Tasks never have weekend-only logic
        task.setEveryWeekend(false);

        // ✅ Days logic
        if (task.isEveryday()) {
            task.setDaysOfWeek(EnumSet.allOf(DayOfWeek.class));
        } else {
            task.setDaysOfWeek(
                    request.getDaysOfWeek() != null
                            ? request.getDaysOfWeek()
                            : EnumSet.noneOf(DayOfWeek.class)
            );
        }

        // ✅ Initial status
        LocalDate today = LocalDate.now();
        task.setStatus(
                task.getEndDate().isBefore(today)
                        ? Task.TaskStatus.COMPLETED
                        : Task.TaskStatus.PENDING
        );

        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        // ✅ SAVE TASK FIRST
        Task savedTask = taskRepository.save(task);

        // ✅ NOW SAFE: coin logic
        handleCoinLogicForParent(savedTask, user);

        // ✅ Create completions
        updateScheduledCompletions(savedTask);

        // ✅ Calendar
        createCalendarEvent(savedTask);

        return savedTask;
    }


    // Validate that dates are in the future and start date is before end date
    private void validateTaskDates(Task task) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // Check if start date is in the past
        if (task.getStartDate().isBefore(today)) {
            throw new BusinessException("Start date cannot be in the past");
        }

        // If start date is today, check if start time is in the past
        if (task.getStartDate().isEqual(today) && task.getStartTime().isBefore(now)) {
            throw new BusinessException("Start time cannot be in the past for today's date");
        }

        // Check if end date is before start date
        if (task.getEndDate().isBefore(task.getStartDate())) {
            throw new BusinessException("End date cannot be before start date");
        }

        // Check if end time is before start time when dates are the same
        if (task.getStartDate().equals(task.getEndDate()) &&
                task.getEndTime().isBefore(task.getStartTime())) {
            throw new BusinessException("End time cannot be before start time on the same day");
        }
    }

    // Check for duplicate tasks (same title, same time, same day)
    private void checkForDuplicateTaskOrHabit(Task task, User user) {

        // ⛔ Mandatory validation (missing earlier)
        if (!task.getEndTime().isAfter(task.getStartTime())) {
            throw new BusinessException("End time must be after start time.");
        }

        // Helper: checks at least one common day (everyday + null safe)
        Predicate<Set<DayOfWeek>> hasCommonDay =
                existingDays -> {
                    if (task.isEveryday()) return true;
                    if (existingDays == null || task.getDaysOfWeek() == null) return false;
                    return existingDays.stream().anyMatch(task.getDaysOfWeek()::contains);
                };

        Long currentTaskId = task.getId(); // NULL for create, NOT NULL for update

        // 1️⃣ Check existing TASKS (ignore self during update)
        Optional<Task> overlappingTask = taskRepository.findByAssignedTo(user).stream()
                .filter(t ->
                        (currentTaskId == null || !t.getId().equals(currentTaskId)) &&

                                // Date overlap
                                !t.getEndDate().isBefore(task.getStartDate()) &&
                                !t.getStartDate().isAfter(task.getEndDate()) &&

                                // Time overlap (STRICT, boundary-safe)
                                t.getStartTime().isBefore(task.getEndTime()) &&
                                t.getEndTime().isAfter(task.getStartTime()) &&

                                // Day overlap
                                (t.isEveryday() || hasCommonDay.test(t.getDaysOfWeek()))
                )
                .findFirst();

        if (overlappingTask.isPresent()) {
            Task t = overlappingTask.get();

            String startDate = t.getStartDate().format(DATE_FORMATTER);
            String endDate   = t.getEndDate().format(DATE_FORMATTER);

            throw new BusinessException(
                    "This time overlaps with task '" + t.getTitle() +
                            "' (" + t.getStartTime() + " - " + t.getEndTime() + "). " +
                            "Pick a different slot. [" + startDate + " - " + endDate + "]"
            );
        }


        // 2️⃣ Check existing HABITS
        Optional<Habit> overlappingHabit = habitRepository.findByAssignedTo(user).stream()
                .filter(h ->
                        !h.getEndDate().isBefore(task.getStartDate()) &&
                                !h.getStartDate().isAfter(task.getEndDate()) &&
                                h.getStartTime().isBefore(task.getEndTime()) &&
                                h.getEndTime().isAfter(task.getStartTime()) &&
                                (h.isEveryday() || hasCommonDay.test(h.getDaysOfWeek()))
                )
                .findFirst();

        if (overlappingHabit.isPresent()) {
            Habit h = overlappingHabit.get();

            String startDate = h.getStartDate().format(DATE_FORMATTER);
            String endDate   = h.getEndDate().format(DATE_FORMATTER);

            throw new BusinessException(
                    "This time overlaps with habit '" + h.getTitle() +
                            "' (" + h.getStartTime() + " - " + h.getEndTime() + "). " +
                            "Pick a different slot. [" + startDate + " - " + endDate + "]"
            );
        }


        // 3️⃣ Check TASK COMPLETION (schedule-based, ignore self)
        Optional<TaskCompletion> completionConflict =
                taskCompletionRepository
                        .findScheduleConflicts(
                                user,
                                task.getId(),
                                task.getStartDate(),
                                task.getEndDate(),
                                task.getStartTime(),
                                task.getEndTime(),
                                PageRequest.of(0, 1)
                        )
                        .stream()
                        .findFirst();

        if (completionConflict.isPresent()) {

            Task overlapTask = completionConflict.get().getTask();

            String startDate = overlapTask.getStartDate().format(DATE_FORMATTER);
            String endDate   = overlapTask.getEndDate().format(DATE_FORMATTER);

            throw new BusinessException(
                    "This time overlaps with task '" + overlapTask.getTitle() +
                            "' (" + overlapTask.getStartTime() + " - " + overlapTask.getEndTime() + "). " +
                            "Pick a different slot. [" + startDate + " - " + endDate + "]"
            );
        }

    }







    // Handle coin logic for parent-created tasks
    private void handleCoinLogicForParent(Task task, User user) {
        var familyMemberOpt = familyMemberService.getByUser(user);
        if (familyMemberOpt.isPresent()) {
            String relationship = familyMemberOpt.get().getRelationship();
            if ("parent".equalsIgnoreCase(relationship)) {
                if (user.getFamily() == null) {
                    throw new BusinessException("Parent must belong to a family to add a task.");
                }
                task.setFamily(user.getFamily());

                // Parent must specify rewardCoins as 5 or 10
                int coinsToSpend = task.getRewardCoins();
                if (coinsToSpend != 5 && coinsToSpend != 10) {
                    throw new BusinessException("Reward coins must be either 5 or 10 for parent-created tasks.");
                }

                // Deduct coins from parent
                user.setCoins(user.getCoins() - coinsToSpend);
                userRepository.save(user);

                // Record transaction
                CoinTransaction tx = CoinTransaction.builder()
                        .user(user)
                        .amount(-coinsToSpend)
                        .type("TASK_CREATION")
                        .description("Spent " + coinsToSpend + " coins to create task for child")
                        .task(task)
                        .timestamp(LocalDateTime.now())
                        .build();
                coinTransactionRepository.save(tx);

            } else if ("child".equalsIgnoreCase(relationship) || "self".equalsIgnoreCase(relationship)) {
                task.setFamily(user.getFamily());
                task.setRewardCoins(5); // Default reward for self/child
            } else {
                task.setFamily(null);
                task.setRewardCoins(5); // Default reward for others
            }
        } else {
            task.setFamily(null);
            task.setRewardCoins(5); // Default reward for others
        }
    }

    // Create individual completion records for each scheduled day
    // Create scheduled completions with PENDING status
    @Transactional
    public void updateScheduledCompletions(Task task) {

        List<TaskCompletion> existingCompletions =
                taskCompletionRepository.findByTaskId(task.getId());

        Map<LocalDate, TaskCompletion> existingMap =
                existingCompletions.stream()
                        .collect(Collectors.toMap(
                                TaskCompletion::getCompletionDate,
                                c -> c
                        ));

        // 1️⃣ Build valid schedule dates
        Set<LocalDate> validDates = new HashSet<>();
        LocalDate currentDate = task.getStartDate();
        LocalDate endDate = task.getEndDate();

        while (!currentDate.isAfter(endDate)) {
            if (isDateScheduledForTask(task, currentDate)) {
                validDates.add(currentDate);
            }
            currentDate = currentDate.plusDays(1);
        }

        // 2️⃣ Delete invalid future PENDING completions
        List<TaskCompletion> toDelete = existingCompletions.stream()
                .filter(c ->
                        c.getCompletionDate().isAfter(LocalDate.now()) &&
                                c.getStatus() == TaskCompletion.CompletionStatus.PENDING &&
                                !validDates.contains(c.getCompletionDate())
                )
                .toList();

        taskCompletionRepository.deleteAll(toDelete);

        // 3️⃣ Create missing future completions
        List<TaskCompletion> toCreate = new ArrayList<>();

        for (LocalDate date : validDates) {

            // skip if already exists
            if (existingMap.containsKey(date)) {
                continue;
            }

            // create only today or future
            if (!date.isBefore(LocalDate.now())) {
                TaskCompletion completion = TaskCompletion.builder()
                        .task(task)
                        .completionDate(date)
                        .completionTime(null)
                        .coinsEarned(0)
                        .completed(false)
                        .status(TaskCompletion.CompletionStatus.PENDING)
                        .build();

                toCreate.add(completion);
            }
        }

        taskCompletionRepository.saveAll(toCreate);

        log.info(
                "Task {} updated → deleted {}, created {} completion entries",
                task.getId(), toDelete.size(), toCreate.size()
        );
    }


    // Create calendar event
    private void createCalendarEvent(Task task) {
        CalendarEvent event = CalendarEvent.builder()
                .title(task.getTitle())
                .description(task.getDescription())
                .startDate(task.getStartDate())
                .startTime(task.getStartTime() != null ? task.getStartTime().toString() : null)
                .endDate(task.getEndDate())
                .endTime(task.getEndTime() != null ? task.getEndTime().toString() : null)
                .type(CalendarEvent.TYPE_TASK)
                .completed(false)
                .createdBy(task.getCreatedBy())
                .family(task.getFamily())
                .build();
        calendarEventRepository.save(event);
    }

    // Check if task is recurring
    private boolean isRecurringTask(Task task) {
        return task.isEveryday() ||
                (task.getDaysOfWeek() != null && !task.getDaysOfWeek().isEmpty()) ||
                task.getStartDate().isBefore(task.getEndDate());
    }

    // Mark a specific day's task as completed
    @Transactional
    public Optional<TaskCompletion> completeTask(Long taskId, User user) {
        final int DAILY_COIN_THRESHOLD = 50;

        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            return Optional.empty();
        }

        Task task = taskOpt.get();
        LocalDate today = LocalDate.now();

        // Find today's completion record
        Optional<TaskCompletion> completionOpt = taskCompletionRepository
                .findByTaskAndCompletionDate(task, today);

        if (completionOpt.isEmpty()) {
            throw new BusinessException("No scheduled task for today");
        }

        TaskCompletion completion = completionOpt.get();

        // Check if already completed
        if (completion.getCompleted()) {
            throw new BusinessException("Task already completed for today");
        }

        // Check permissions
        boolean canComplete = task.getCreatedBy().equals(user)
                || (task.getFamily() != null && task.getFamily().equals(user.getFamily()));

        if (!canComplete) {
            throw new BusinessException("Not authorized to complete this task");
        }

        // Calculate coins based on timing
        int coinsEarned = calculateCoinsForCompletion(task, completion);

        // Enforce daily coin cap

        int coinsEarnedToday = getCoinsEarnedToday(user);
        if (coinsEarnedToday >= DAILY_COIN_THRESHOLD ) {
            coinsEarned = 0;
        }

        // Update completion record with COMPLETED status
        completion.setCompleted(true);
        completion.setCoinsEarned(coinsEarned);
        completion.setStatus(TaskCompletion.CompletionStatus.COMPLETED); // ✅ Update daily status
        completion.setCompletionTime(LocalTime.now());

        TaskCompletion savedCompletion = taskCompletionRepository.save(completion);

        // Update overall task status
        updateTaskStatus(task);

        // Update user coins
        if (coinsEarned > 0) {
            user.setCoins(user.getCoins() + coinsEarned);
            userRepository.save(user);

            // Record transaction
            CoinTransaction tx = CoinTransaction.builder()
                    .user(user)
                    .amount(coinsEarned)
                    .type("TASK_COMPLETION")
                    .description("Earned " + coinsEarned + " coins for completing task")
                    .task(task)
                    .timestamp(LocalDateTime.now())
                    .build();
            coinTransactionRepository.save(tx);
        }

        // Update overall task status
        updateTaskStatus(task);

        // Trigger badge updates
        badgeService.triggerBadgeUpdate(
                user.getId(),
                ActivityType.TASK_COMPLETED,
                coinsEarned,     // delta for this task
                "TASKS"
        );

        return Optional.of(savedCompletion);
    }

    // Calculate coins based on completion timing
    private int calculateCoinsForCompletion(Task task, TaskCompletion completion) {
        LocalDateTime scheduledEnd = LocalDateTime.of(completion.getCompletionDate(), task.getEndTime());
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(scheduledEnd)) {
            return 5; // Completed before end time
        } else if (now.isBefore(scheduledEnd.plusHours(1))) {
            return 3; // Within 1 hour after scheduled end time
        } else if (now.toLocalDate().isEqual(scheduledEnd.toLocalDate())) {
            return 1; // After 1 hour but same day
        } else {
            return 1; // After the day is over
        }
    }

    // Get coins earned today
    // Fixed method to get coins earned today
    private int getCoinsEarnedToday(User user) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        try {
            // Use the repository method we just added
            List<CoinTransaction> todayTransactions = coinTransactionRepository.findByUserAndTimestampBetween(
                    user, startOfDay, endOfDay
            );

            return todayTransactions.stream()
                    .filter(tx -> "TASK_COMPLETION".equals(tx.getType()))
                    .mapToInt(CoinTransaction::getAmount)
                    .sum();

        } catch (Exception e) {
            log.error("Error calculating coins earned today for user {}: {}", user.getId(), e.getMessage());
            return 0;
        }
    }

    // Update overall task status based on daily completions
    private void updateTaskStatus(Task task) {
        List<TaskCompletion> completions = taskCompletionRepository.findByTask(task);
        long totalScheduled = completions.size();
        long totalCompleted = completions.stream()
                .filter(c -> c.getStatus() == TaskCompletion.CompletionStatus.COMPLETED)
                .count();

        Task.TaskStatus newStatus;
        if (totalCompleted == 0) {
            newStatus = Task.TaskStatus.PENDING;
        } else if (totalCompleted < totalScheduled) {
            newStatus = Task.TaskStatus.ONGOING;
        } else {
            newStatus = Task.TaskStatus.COMPLETED;
        }

        if (!task.getStatus().equals(newStatus)) {
            task.setStatus(newStatus);
            task.setUpdatedAt(LocalDateTime.now());
            taskRepository.save(task);
        }
    }

    // ✅ NEW: Calculate task status based on completions AND end date
    public Task.TaskStatus calculateTaskStatus(Task task) {
        LocalDate today = LocalDate.now();

        // If end date has passed, task should be COMPLETED
        if (task.getEndDate().isBefore(today)) {
            return Task.TaskStatus.COMPLETED;
        }

        // For active tasks, calculate based on completions
        long totalScheduled = taskCompletionRepository.countByTask(task);
        long totalCompleted = taskCompletionRepository.countByTaskAndCompleted(task, true);

        if (totalCompleted == 0) {
            return Task.TaskStatus.PENDING;
        } else if (totalCompleted < totalScheduled) {
            return Task.TaskStatus.ONGOING;
        } else {
            return Task.TaskStatus.COMPLETED;
        }
    }

    // ✅ NEW: Scheduled method to update task status when end date passes
    @Scheduled(cron = "0 0 0 * * ?") // Runs daily at midnight
    @Transactional
    public void updateTaskStatusesBasedOnEndDate() {
        LocalDate today = LocalDate.now();

        // Find all tasks that are still PENDING but their end date has passed
        List<Task> expiredTasks = taskRepository.findByStatusAndEndDateBefore(
                Task.TaskStatus.PENDING,
                today
        );

        // Also find ONGOING tasks that have ended
        List<Task> completedTasks = taskRepository.findByStatusAndEndDateBefore(
                Task.TaskStatus.ONGOING,
                today
        );

        List<Task> allTasksToUpdate = new ArrayList<>();
        allTasksToUpdate.addAll(expiredTasks);
        allTasksToUpdate.addAll(completedTasks);

        for (Task task : allTasksToUpdate) {
            log.info("Auto-updating task {} from {} to COMPLETED (end date: {})",
                    task.getId(), task.getStatus(), task.getEndDate());

            task.setStatus(Task.TaskStatus.COMPLETED);
            task.setUpdatedAt(LocalDateTime.now());

            // Also update any pending completions for this task
            updatePendingCompletionsForCompletedTask(task);
        }

        taskRepository.saveAll(allTasksToUpdate);
        log.info("Updated {} tasks to COMPLETED status based on end date", allTasksToUpdate.size());
    }

    // ✅ NEW: Helper method to update completion records when task is auto-completed
    private void updatePendingCompletionsForCompletedTask(Task task) {
        List<TaskCompletion> pendingCompletions = taskCompletionRepository.findByTaskAndCompleted(task, false);

        for (TaskCompletion completion : pendingCompletions) {
            if (completion.getStatus() == TaskCompletion.CompletionStatus.PENDING) {
                completion.setStatus(TaskCompletion.CompletionStatus.SKIPPED);
            }
        }

        taskCompletionRepository.saveAll(pendingCompletions);
    }

    // Scheduled task to update statuses daily
    @Scheduled(cron = "0 0 0 * * ?") // Run daily at midnight
    @Transactional
    public void updateAllTaskStatuses() {
        List<Task> allTasks = taskRepository.findAll();
        for (Task task : allTasks) {
            Task.TaskStatus newStatus = calculateTaskStatus(task);
            if (!task.getStatus().equals(newStatus)) {
                task.setStatus(newStatus);
                task.setUpdatedAt(LocalDateTime.now());
                taskRepository.save(task);
            }
        }
        log.info("Updated statuses for {} tasks", allTasks.size());
    }

    // Overload for controller compatibility: getTasksForUser
    public List<Task> getTasksForUser(User user) {
        return getAllTasksForUser(user);
    }

    // Update task: allow creator or parent of the assigned child
    @Transactional
    public Optional<Task> updateTask(Long taskId, Task updatedTask, User user) {
        checkForDuplicateTaskOrHabit(updatedTask, updatedTask.getAssignedTo());
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty())
            return Optional.empty();
        Task task = taskOpt.get();

        // If user is creator, always allow
        if (task.getCreatedBy().equals(user)) {
            return Optional.of(updateTaskFields(task, updatedTask));
        }

        // If user is parent in the same family and task is assigned to their child
        if (user.getFamily() != null && task.getAssignedTo() != null &&
                user.getFamily().getId().equals(task.getAssignedTo().getFamily().getId())) {

            var parentFm = familyMemberService.getByUser(user);
            var childFm = familyMemberService.getByUser(task.getAssignedTo());
            if (parentFm.isPresent() && childFm.isPresent()
                    && "parent".equals(parentFm.get().getRelationship())
                    && "child".equals(childFm.get().getRelationship())) {
                return Optional.of(updateTaskFields(task, updatedTask));
            }
        }
        return Optional.empty();
    }

    // Helper to update fields
    private Task updateTaskFields(Task task, Task updatedTask) {
        if (updatedTask.getTitle() != null) {
            task.setTitle(updatedTask.getTitle());
        }
        if (updatedTask.getDescription() != null) {
            task.setDescription(updatedTask.getDescription());
        }
        if (updatedTask.getRewardCoins() > 0) {
            task.setRewardCoins(updatedTask.getRewardCoins());
        }
        task.setSharedWithParent(updatedTask.getSharedWithParent());

        if (updatedTask.getAssignedTo() != null) {
            task.setAssignedTo(updatedTask.getAssignedTo());
        }

        // Use LocalDate directly for all date fields
        if (updatedTask.getStartDate() != null) {
            task.setStartDate(updatedTask.getStartDate());
        }
        if (updatedTask.getEndDate() != null) {
            task.setEndDate(updatedTask.getEndDate());
        }

        if (updatedTask.getStartTime() != null) {
            task.setStartTime(updatedTask.getStartTime());
        }
        if (updatedTask.getEndTime() != null) {
            task.setEndTime(updatedTask.getEndTime());
        }
        if (updatedTask.getDaysOfWeek() != null) {
            task.setDaysOfWeek(updatedTask.getDaysOfWeek());
        }

        // Update timestamp
        task.setUpdatedAt(LocalDateTime.now());

        Task savedTask = taskRepository.save(task);

        // 2️⃣ Recalculate scheduled completions
        updateScheduledCompletions(savedTask);

        return savedTask;
    }

    // Return all tasks for a user as TaskResponse DTOs with metrics and 4 badges
    public List<TaskResponse> getAllTaskResponsesForUser(User user) {
        List<Task> tasks = getAllTasksForUser(user);
        return tasks.stream()
                .map(this::convertToTaskResponseWithMetrics)
                .collect(Collectors.toList());
    }

    private TaskResponse convertToTaskResponseWithMetrics(Task task) {
        // Calculate task-specific metrics
        TaskMetrics metrics = calculateTaskMetrics(task);

        // Get only 4 milestone badges - FIXED: Use assigned user instead of creator
        Long userId = task.getAssignedTo() != null ? task.getAssignedTo().getId() : task.getCreatedBy().getId();
        List<BadgeResponseDTO> limitedBadges = getLimitedMilestoneBadges(userId);

        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority() != null ? task.getPriority().name() : null)
                .routine(task.getRoutine())
                .startDate(task.getStartDate())
                .startTime(task.getStartTime())
                .endDate(task.getEndDate())
                .endTime(task.getEndTime())
                .createdBy(task.getCreatedBy().getId())
                .assignedTo(task.getAssignedTo().getId())
                .familyId(task.getFamily() != null ? task.getFamily().getId() : null)
                .daysOfWeek(task.getDaysOfWeek())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .rewardCoins(task.getRewardCoins())
                .sharedWithParent(task.getSharedWithParent())
                .completed(task.getStatus() == Task.TaskStatus.COMPLETED)
                .isEveryday(task.isEveryday())
                .isEveryWeekend(task.isEveryWeekend())
                .tags(task.getTags())
                .currentTaskStreak(metrics.currentTaskStreak())
                .highestTaskStreak(metrics.highestTaskStreak())
                .coinsEarned(metrics.coinsEarned())
                .completionPercentage(metrics.completionPercentage())
                .totalScheduledDays(metrics.totalScheduledDays())
                .totalCompletedDays(metrics.totalCompletedDays())
                .milestoneBadges(limitedBadges)
                .build();
    }

    private TaskMetrics calculateTaskMetrics(Task task) {
        // Use TaskCompletion records for accurate metrics
        List<TaskCompletion> completions = taskCompletionRepository.findByTask(task);

        int totalScheduledDays = completions.size();
        int totalCompletedDays = (int) completions.stream().filter(TaskCompletion::getCompleted).count();
        int coinsEarned = completions.stream().mapToInt(TaskCompletion::getCoinsEarned).sum();
        double completionPercentage = totalScheduledDays > 0 ?
                (double) totalCompletedDays / totalScheduledDays * 100 : 0.0;

        // Calculate streak based on consecutive completed days
        int streak = calculateCurrentStreak(task, completions);
        int highestStreak = calculateHighestStreak(task,completions);

        return new TaskMetrics(streak, highestStreak, coinsEarned, completionPercentage,
                totalScheduledDays, totalCompletedDays);
    }

//    private int calculateCurrentStreak(List<TaskCompletion> completions) {
//
//        List<LocalDate> completedDates = completions.stream()
//                .filter(TaskCompletion::getCompleted)
//                .map(TaskCompletion::getCompletionDate)
//                .distinct()
//                .sorted(Comparator.reverseOrder())
//                .collect(Collectors.toList());
//
//        if (completedDates.isEmpty()) return 0;
//
//        int streak = 1;
//        LocalDate expectedDate = completedDates.get(0); // last completed day
//
//        for (int i = 1; i < completedDates.size(); i++) {
//            LocalDate date = completedDates.get(i);
//
//            if (date.equals(expectedDate.minusDays(1))) {
//                streak++;
//                expectedDate = date;
//            } else {
//                break;
//            }
//        }
//
//        return streak;
//    }






    private int calculateCurrentStreak(Task task, List<TaskCompletion> completions) {
        // Get all scheduled dates for this task
        List<LocalDate> scheduledDates = getScheduledDates(task);
        if (scheduledDates.isEmpty()) {
            return 0;
        }

        // Sort scheduled dates in descending order (most recent first)
        List<LocalDate> sortedScheduledDates = scheduledDates.stream()
                .sorted(Collections.reverseOrder())
                .toList();

        // Create a set of completed dates for quick lookup
        Set<LocalDate> completedDates = completions.stream()
                .filter(TaskCompletion::getCompleted)
                .map(TaskCompletion::getCompletionDate)
                .collect(Collectors.toSet());

        int streak = 0;
        LocalDate today = LocalDate.now();

        // Start from today and go backwards through scheduled dates
        for (LocalDate scheduledDate : sortedScheduledDates) {
            // Only consider dates up to today
            if (scheduledDate.isAfter(today)) {
                continue;
            }

            if (completedDates.contains(scheduledDate)) {
                streak++;
            } else {
                // Break on first incomplete scheduled date
                break;
            }
        }

        return streak;
    }

    private int calculateHighestStreak(Task task, List<TaskCompletion> completions) {
        // Get all scheduled dates for this task
        List<LocalDate> scheduledDates = getScheduledDates(task);
        if (scheduledDates.isEmpty()) {
            return 0;
        }

        // Sort scheduled dates in ascending order
        List<LocalDate> sortedScheduledDates = scheduledDates.stream()
                .sorted()
                .toList();

        // Create a set of completed dates for quick lookup
        Set<LocalDate> completedDates = completions.stream()
                .filter(TaskCompletion::getCompleted)
                .map(TaskCompletion::getCompletionDate)
                .collect(Collectors.toSet());

        int highestStreak = 0;
        int currentStreak = 0;
        LocalDate previousDate = null;

        for (LocalDate scheduledDate : sortedScheduledDates) {
            if (completedDates.contains(scheduledDate)) {
                // Check if this date is consecutive with the previous completed date
                if (previousDate != null && isConsecutiveScheduledDay(previousDate, scheduledDate, sortedScheduledDates)) {
                    currentStreak++;
                } else {
                    currentStreak = 1;
                }
                highestStreak = Math.max(highestStreak, currentStreak);
                previousDate = scheduledDate;
            } else {
                currentStreak = 0;
                previousDate = null;
            }
        }

        return highestStreak;
    }

    private boolean isConsecutiveScheduledDay(LocalDate date1, LocalDate date2, List<LocalDate> allScheduledDates) {
        // Check if date2 is the next scheduled date after date1
        int index1 = allScheduledDates.indexOf(date1);
        int index2 = allScheduledDates.indexOf(date2);
        return index2 == index1 + 1;
    }

    private List<LocalDate> getScheduledDates(Task task) {
        List<LocalDate> scheduledDates = new ArrayList<>();

        LocalDate startDate = task.getStartDate();
        LocalDate endDate = task.getEndDate();

        if (startDate == null || endDate == null) {
            return scheduledDates;
        }

        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            if (isTaskScheduledForDate(task, currentDate)) {
                scheduledDates.add(currentDate);
            }
            currentDate = currentDate.plusDays(1);
        }

        return scheduledDates;
    }

    private boolean isTaskScheduledForDate(Task task, LocalDate date) {
        // Check if task is everyday task
        if (task.isEveryday()) {
            return true;
        }

        // Check if task is every weekend
        if (task.isEveryWeekend()) {
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        }

        // Check specific days of week
        if (task.getDaysOfWeek() != null && !task.getDaysOfWeek().isEmpty()) {
            String dayOfWeek = date.getDayOfWeek().toString();
            return task.getDaysOfWeek().contains(dayOfWeek);
        }

        return false;
    }

//    private int calculateCurrentStreak(List<TaskCompletion> completions) {
//
//        Set<LocalDate> completedDates = completions.stream()
//                .filter(TaskCompletion::getCompleted)
//                .map(TaskCompletion::getCompletionDate)
//                .collect(Collectors.toSet());
//
//        if (completedDates.isEmpty()) {
//            return 0;
//        }
//
//        LocalDate today = LocalDate.now();
//
//        // If today is not completed,
//        // then yesterday must be completed to continue streak
//        LocalDate checkDate;
//
//        if (completedDates.contains(today)) {
//            checkDate = today;
//        } else if (completedDates.contains(today.minusDays(1))) {
//            checkDate = today.minusDays(1);
//        } else {
//            return 0; // streak broken
//        }
//
//        int streak = 0;
//
//        while (completedDates.contains(checkDate)) {
//            streak++;
//            checkDate = checkDate.minusDays(1);
//        }
//
//        return streak;
//    }
//
//
//    private int calculateHighestStreak(List<TaskCompletion> completions) {
//        List<TaskCompletion> completed = completions.stream()
//                .filter(TaskCompletion::getCompleted)
//                .sorted(Comparator.comparing(TaskCompletion::getCompletionDate))
//                .collect(Collectors.toList());
//
//        if (completed.isEmpty()) return 0;
//
//        int highestStreak = 0;
//        int currentStreak = 1;
//        LocalDate previousDate = completed.get(0).getCompletionDate();
//
//        for (int i = 1; i < completed.size(); i++) {
//            LocalDate currentDate = completed.get(i).getCompletionDate();
//            if (previousDate.plusDays(1).equals(currentDate)) {
//                currentStreak++;
//            } else {
//                highestStreak = Math.max(highestStreak, currentStreak);
//                currentStreak = 1;
//            }
//            previousDate = currentDate;
//        }
//
//        return Math.max(highestStreak, currentStreak);
//    }

    private List<BadgeResponseDTO> getLimitedMilestoneBadges(Long userId) {
        try {
            List<BadgeResponseDTO> allBadges = badgeService.getUserBadges(userId);
            if (allBadges == null) {
                log.warn("Badge service returned null badges for user {}", userId);
                return List.of();
            }
            return allBadges.stream().limit(4).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting limited badges for user {}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    public ResponseEntity<Map<String, Object>> startTaskForToday(User user, Long taskId) {
        final LocalDate today = LocalDate.now();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("Task not found"));

        TaskCompletion taskCompletion = taskCompletionRepository
                .findByTaskAndCompletionDate(task, today)
                .orElseThrow(() -> new BusinessException("No scheduled task for today"));

        // ⭐ Check if already ongoing
        if (taskCompletion.getStatus() == TaskCompletion.CompletionStatus.ONGOING) {

            Map<String, Object> response = Map.of(
                    "message", "Task already ongoing for today",
                    "taskId", taskId,
                    "completionDate", taskCompletion.getCompletionDate(),
                    "status", taskCompletion.getStatus().name()
            );

            return ResponseEntity.ok(response);
        }

        // ⭐ Start the task
        taskCompletion.setStatus(TaskCompletion.CompletionStatus.ONGOING);
        taskCompletionRepository.save(taskCompletion);

        Map<String, Object> response = Map.of(
                "message", "Task started successfully for today!",
                "taskId", taskId,
                "completionDate", taskCompletion.getCompletionDate(),
                "status", taskCompletion.getStatus().name(),
                "startedAt", LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<Map<String, Object>> abortTaskForToday(User user, Long taskId) {

        LocalDate today = LocalDate.now();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("Task not found"));

        TaskCompletion taskCompletion = taskCompletionRepository
                .findByTaskAndCompletionDate(task, today)
                .orElseThrow(() -> new BusinessException("No scheduled task for today"));

        // ⭐ Only abort if ONGOING
        if (taskCompletion.getStatus() == TaskCompletion.CompletionStatus.ONGOING) {

            taskCompletion.setStatus(TaskCompletion.CompletionStatus.PENDING);
            taskCompletionRepository.save(taskCompletion);

            Map<String, Object> response = Map.of(
                    "message", "Task aborted successfully. Status set to pending.",
                    "taskId", taskId,
                    "status", TaskCompletion.CompletionStatus.PENDING.name(),
                    "abortedAt", LocalDateTime.now()
            );

            return ResponseEntity.ok(response);
        }

        // ⭐ Not ongoing → no state change
        Map<String, Object> response = Map.of(
                "message", "Task is not ongoing, so nothing to abort.",
                "taskId", taskId,
                "currentStatus", taskCompletion.getStatus().name()
        );

        return ResponseEntity.ok(response);
    }





    // Helper record for metrics
    private record TaskMetrics(
            int currentTaskStreak,
            int highestTaskStreak,
            int coinsEarned,
            double completionPercentage,
            int totalScheduledDays,
            int totalCompletedDays
    ) {
    }

    // Helper methods for Task scheduling
    private boolean isDateScheduledForTask(Task task, LocalDate date) {
        if (task.isEveryday()) {
            return true;
        }

        if (task.isEveryWeekend()) {
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        }

        if (task.getDaysOfWeek() != null && !task.getDaysOfWeek().isEmpty()) {
            return task.getDaysOfWeek().contains(date.getDayOfWeek());
        }

        return !date.isBefore(task.getStartDate()) &&
                (task.getEndDate() == null || !date.isAfter(task.getEndDate()));
    }

    // Delete task: allow creator or parent of the assigned child
    public boolean deleteTask(Long taskId, User user) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty())
            return false;
        Task task = taskOpt.get();

        // If user is creator, always allow
        if (task.getCreatedBy().equals(user)) {
            // Delete all completion records first
            deleteFutureScheduledTaskRecords(task);
            return true;
        }

        // If user is parent in the same family and task is assigned to their child
        if (user.getFamily() != null && task.getAssignedTo() != null &&
                user.getFamily().getId().equals(task.getAssignedTo().getFamily().getId())) {

            var parentFm = familyMemberService.getByUser(user);
            var childFm = familyMemberService.getByUser(task.getAssignedTo());
            if (parentFm.isPresent() && childFm.isPresent()
                    && "parent".equals(parentFm.get().getRelationship())
                    && "child".equals(childFm.get().getRelationship())) {
                deleteFutureScheduledTaskRecords(task);
                return true;
            }
        }
        return false;
    }

    @Transactional
    public void deleteFutureScheduledTaskRecords(Task task) {

        if (task == null || task.getId() == null) return;

        Long taskId = task.getId();
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        LocalDate startDate = task.getStartDate();
        LocalTime startTime = task.getStartTime();

        boolean taskStarted =
                startDate.isEqual(today) && !startTime.isAfter(now);

        if (taskStarted) {
            taskCompletionRepository
                    .deleteByTaskIdAndCompletionDateAfter(taskId, today);
        } else {
            taskCompletionRepository
                    .deleteByTaskIdAndCompletionDateGreaterThanEqual(taskId, today);
        }

        task.setEndDate(taskStarted ? today : today.minusDays(1));
        taskRepository.save(task);
    }


    // Get all tasks for a parent (by family)
    public List<Task> getTasksForParent(User parent) {
        return taskRepository.findByFamilyId(parent.getFamily().getId());
    }

    // Get all tasks for a kid (by assignedTo)
    public List<Task> getTasksForKid(User kid) {
        return taskRepository.findByAssignedTo(kid);
    }

    // Get all tasks for a specific family
    public List<Task> getTasksByFamily(Long familyId) {
        return taskRepository.findByFamilyId(familyId);
    }

    // Get all tasks for a user on a specific day
    public List<Task> getTasksForUserByDay(User user, LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return taskRepository.findTasksForUserByDay(user, dayOfWeek);
    }

    // Get task completions for a specific date range
    public List<TaskCompletion> getTaskCompletionsForDateRange(Task task, LocalDate startDate, LocalDate endDate) {
        return taskCompletionRepository.findByTaskAndCompletionDateBetween(task, startDate, endDate);
    }

    // Get all tasks created by a user
    public List<Task> getAllTasksForUser(User user) {
        return taskRepository.findByCreatedBy(user);
    }

    public List<Task> getTodayTasksByStatus(User user, Task.TaskStatus status) {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        return taskRepository.findByCreatedByAndStatusAndDaysOfWeekContaining(user, status, today);
    }

    public List<Task> getTodayTasksByPriority(User user, Task.Priority priority) {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        return taskRepository.findByCreatedByAndPriorityAndDaysOfWeekContaining(user, priority, today);
    }

    public List<Task> getTodayTasksByRoutine(User user, String routine) {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        return taskRepository.findByCreatedByAndRoutineAndDaysOfWeekContaining(user, routine, today);
    }

    // ================ DTO CONVERSION METHODS ================

    /**
     * Convert TaskRequest DTO to Task Entity
     */
//    public Task fromTaskRequest(TaskRequest request) {
//
//        // 🔑 CREATE vs UPDATE decision
//        Task task = (request.getTaskId() != null)
//                ? taskRepository.findById(request.getTaskId())
//                .orElseThrow(() -> new ResourceNotFoundException("Task not found"))
//                : new Task();
//
//        // -------------------------
//        // Map fields (same for both)
//        // -------------------------
//        if (request.getTitle() != null) {
//            task.setTitle(request.getTitle());
//        }
//
//        if (request.getDescription() != null) {
//            task.setDescription(request.getDescription());
//        }
//
//        task.setStatus(request.getStatus());
//        task.setPriority(request.getPriority());
//        task.setRoutine(request.getRoutine());
//        task.setTags(request.getTags());
//        task.setStartDate(request.getStartDate());
//        task.setStartTime(request.getStartTime());
//        task.setEndDate(request.getEndDate());
//        task.setEndTime(request.getEndTime());
//        task.setRewardCoins(request.getRewardCoins());
//        task.setEveryday(request.isEveryday());
//        task.setEveryWeekend(request.isEveryWeekend());
//
//        // ✅ Normalize daysOfWeek
//        if (request.isEveryday()) {
//            task.setDaysOfWeek(EnumSet.allOf(DayOfWeek.class));
//        } else {
//            task.setDaysOfWeek(request.getDaysOfWeek());
//        }
//
//        task.setSharedWithParent(request.getSharedWithParent());
//
//        // ✅ Set assigned user ONLY for create
//        if (task.getId() == null) {
//            task.setAssignedTo(
//                    userRepository.findById(request.getForUserId())
//                            .orElseThrow(() -> new ResourceNotFoundException("User not found"))
//            );
//        }
//
//        return task;
//    }
    public Task fromTaskRequest(TaskRequest request) {

        Task task;

        // ✅ SAFE CREATE vs UPDATE
        if (request.getTaskId() != null && request.getTaskId() > 0) {
            task = taskRepository.findById(request.getTaskId())
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        } else {
            task = new Task();
        }

        // -------------------------
        // Map fields
        // -------------------------
        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }

        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setRoutine(request.getRoutine());
        task.setTags(request.getTags());
        task.setStartDate(request.getStartDate());
        task.setStartTime(request.getStartTime());
        task.setEndDate(request.getEndDate());
        task.setEndTime(request.getEndTime());
        task.setRewardCoins(request.getRewardCoins());
        task.setEveryday(request.isEveryday());
        task.setEveryWeekend(request.isEveryWeekend());

        // ✅ Normalize daysOfWeek
        if (request.isEveryday()) {
            task.setDaysOfWeek(EnumSet.allOf(DayOfWeek.class));
        } else {
            task.setDaysOfWeek(request.getDaysOfWeek());
        }

        // ✅ Safe boolean
        task.setSharedWithParent(
                request.getSharedWithParent() != null ? request.getSharedWithParent() : true
        );

        // ✅ Assign user ONLY on create
        if (task.getId() == null) {
            if (request.getForUserId() == null) {
                throw new BusinessException("forUserId is required while creating task");
            }

            task.setAssignedTo(
                    userRepository.findById(request.getForUserId())
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"))
            );
        }

        return task;
    }

    /**
     * Convert Task Entity to TaskResponse DTO
     */
    public TaskResponse toTaskResponse(Task task) {
        if (task == null)
            return null;

        // Calculate metrics for the response
        TaskMetrics metrics = calculateTaskMetrics(task);

        // Get badges for the assigned user or creator
        Long userId = task.getAssignedTo() != null ? task.getAssignedTo().getId() : task.getCreatedBy().getId();
        List<BadgeResponseDTO> limitedBadges = getLimitedMilestoneBadges(userId);

        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority() != null ? task.getPriority().toString() : null)
                .routine(task.getRoutine() != null ? task.getRoutine().toString() : null)
                .startDate(task.getStartDate())
                .startTime(task.getStartTime())
                .endDate(task.getEndDate())
                .endTime(task.getEndTime())
                .createdBy(task.getCreatedBy().getId())
                .assignedTo(task.getAssignedTo().getId())
                .familyId(task.getFamily() != null ? task.getFamily().getId() : null)
                .daysOfWeek(task.getDaysOfWeek())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .rewardCoins(task.getRewardCoins())
                .sharedWithParent(task.getSharedWithParent())
                .completed(task.getStatus() == Task.TaskStatus.COMPLETED)
                .isEveryday(task.isEveryday())
                .isEveryWeekend(task.isEveryWeekend())
                .tags(task.getTags())
                .currentTaskStreak(metrics.currentTaskStreak())
                .highestTaskStreak(metrics.highestTaskStreak())
                .coinsEarned(metrics.coinsEarned())
                .completionPercentage(metrics.completionPercentage())
                .totalScheduledDays(metrics.totalScheduledDays())
                .totalCompletedDays(metrics.totalCompletedDays())
                .milestoneBadges(limitedBadges)
                .build();
    }

    /**
     * Convert Task Entity to TaskRequest DTO
     */
    public TaskRequest toTaskRequest(Task task) {
        if (task == null)
            return null;

        TaskRequest request = new TaskRequest();
        request.setTitle(task.getTitle());
        request.setDescription(task.getDescription());
        request.setStatus(task.getStatus());
        request.setPriority(task.getPriority());
        request.setRoutine(task.getRoutine());
        request.setTags(task.getTags());
        request.setRewardCoins(task.getRewardCoins());
        request.setDaysOfWeek(task.getDaysOfWeek());
        request.setStartDate(task.getStartDate());
        request.setStartTime(task.getStartTime());
        request.setEndDate(task.getEndDate());
        request.setEndTime(task.getEndTime());

        // Set toggles based on daysOfWeek
        if (task.getDaysOfWeek() != null) {
            if (task.getDaysOfWeek().size() == 7) {
                request.setEveryday(true);
                request.setEveryWeekend(false);
            } else if (task.getDaysOfWeek().size() == 2
                    && task.getDaysOfWeek().contains(DayOfWeek.SATURDAY)
                    && task.getDaysOfWeek().contains(DayOfWeek.SUNDAY)) {
                request.setEveryday(false);
                request.setEveryWeekend(true);
            } else {
                request.setEveryday(false);
                request.setEveryWeekend(false);
            }
        } else {
            request.setEveryday(false);
            request.setEveryWeekend(false);
        }

        return request;
    }

    // Search tasks method
    public List<Task> searchTasks(
            User user,
            String title,
            String description,
            Task.TaskStatus status,
            Task.Priority priority,
            String routine,
            Boolean everyday,
            Boolean everyWeekend,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate createdAt,
            LocalDate updatedAt,
            Integer rewardCoins,
            Set<String> tags,
            Set<DayOfWeek> daysOfWeek,
            String sortBy,
            String sortDir) {

        List<Task> allUserTasks = taskRepository.findByCreatedById(user.getId());

        List<Task> filteredTasks = allUserTasks.stream()
                .filter(task -> {
                    if (title != null && !title.trim().isEmpty()) {
                        if (!task.getTitle().toLowerCase().contains(title.toLowerCase())) {
                            return false;
                        }
                    }

                    if (description != null && !description.trim().isEmpty()) {
                        if (task.getDescription() == null ||
                                !task.getDescription().toLowerCase().contains(description.toLowerCase())) {
                            return false;
                        }
                    }

                    if (status != null && !status.equals(task.getStatus())) {
                        return false;
                    }

                    if (priority != null && !priority.equals(task.getPriority())) {
                        return false;
                    }

                    if (routine != null && !routine.equals(task.getRoutine())) {
                        return false;
                    }

                    if (everyday != null && everyday != task.isEveryday()) {
                        return false;
                    }

                    if (everyWeekend != null && everyWeekend != task.isEveryWeekend()) {
                        return false;
                    }

                    if (startDate != null && task.getStartDate() != null &&
                            task.getStartDate().isBefore(startDate)) {
                        return false;
                    }

                    if (endDate != null && task.getEndDate() != null &&
                            task.getEndDate().isAfter(endDate)) {
                        return false;
                    }

                    if (createdAt != null && task.getCreatedAt() != null &&
                            task.getCreatedAt().toLocalDate().isBefore(createdAt)) {
                        return false;
                    }

                    if (updatedAt != null && task.getUpdatedAt() != null &&
                            task.getUpdatedAt().toLocalDate().isAfter(updatedAt)) {
                        return false;
                    }

                    if (rewardCoins != null && !rewardCoins.equals(task.getRewardCoins())) {
                        return false;
                    }

                    if (tags != null && !tags.isEmpty()) {
                        if (task.getTags() == null || task.getTags().isEmpty()) {
                            return false;
                        }
                        boolean hasAnyTag = false;
                        for (String tag : tags) {
                            if (task.getTags().contains(tag)) {
                                hasAnyTag = true;
                                break;
                            }
                        }
                        if (!hasAnyTag) {
                            return false;
                        }
                    }

                    if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
                        if (task.getDaysOfWeek() == null || task.getDaysOfWeek().isEmpty()) {
                            return false;
                        }
                        boolean hasAnyDay = false;
                        for (DayOfWeek day : daysOfWeek) {
                            if (task.getDaysOfWeek().contains(day)) {
                                hasAnyDay = true;
                                break;
                            }
                        }
                        if (!hasAnyDay) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());

        // Apply sorting
        if (sortBy != null && !sortBy.isBlank()) {
            boolean ascending = !"desc".equalsIgnoreCase(sortDir);

            switch (sortBy.toLowerCase()) {
                case "title":
                    filteredTasks.sort((t1, t2) -> {
                        int result = t1.getTitle().compareToIgnoreCase(t2.getTitle());
                        return ascending ? result : -result;
                    });
                    break;
                case "priority":
                    filteredTasks.sort((t1, t2) -> {
                        if (t1.getPriority() == null && t2.getPriority() == null)
                            return 0;
                        if (t1.getPriority() == null)
                            return ascending ? 1 : -1;
                        if (t2.getPriority() == null)
                            return ascending ? -1 : 1;
                        int result = t1.getPriority().compareTo(t2.getPriority());
                        return ascending ? result : -result;
                    });
                    break;
                case "status":
                    filteredTasks.sort((t1, t2) -> {
                        if (t1.getStatus() == null && t2.getStatus() == null)
                            return 0;
                        if (t1.getStatus() == null)
                            return ascending ? 1 : -1;
                        if (t2.getStatus() == null)
                            return ascending ? -1 : 1;
                        int result = t1.getStatus().compareTo(t2.getStatus());
                        return ascending ? result : -result;
                    });
                    break;
                case "routine":
                    filteredTasks.sort((t1, t2) -> {
                        if (t1.getRoutine() == null && t2.getRoutine() == null)
                            return 0;
                        if (t1.getRoutine() == null)
                            return ascending ? 1 : -1;
                        if (t2.getRoutine() == null)
                            return ascending ? -1 : 1;
                        int result = t1.getRoutine().compareTo(t2.getRoutine());
                        return ascending ? result : -result;
                    });
                    break;
                case "createdat":
                    filteredTasks.sort((t1, t2) -> {
                        if (t1.getCreatedAt() == null && t2.getCreatedAt() == null)
                            return 0;
                        if (t1.getCreatedAt() == null)
                            return ascending ? 1 : -1;
                        if (t2.getCreatedAt() == null)
                            return ascending ? -1 : 1;
                        int result = t1.getCreatedAt().compareTo(t2.getCreatedAt());
                        return ascending ? result : -result;
                    });
                    break;
                case "rewardcoins":
                    filteredTasks.sort((t1, t2) -> {
                        int result = Integer.compare(t1.getRewardCoins(), t2.getRewardCoins());
                        return ascending ? result : -result;
                    });
                    break;
                default: // "id"
                    filteredTasks.sort((t1, t2) -> {
                        int result = Long.compare(t1.getId(), t2.getId());
                        return ascending ? result : -result;
                    });
                    break;
            }
        }

        return filteredTasks;
    }

    // Method for CalendarEventService integration
    public List<TaskResponse> getTasksForUserInRange(Long userId, LocalDate startDate,
                                                     LocalDate endDate) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return List.of();
        }
        User user = userOpt.get();

        List<Task> allTasks = taskRepository.findByCreatedById(userId);

        List<Task> filteredTasks = allTasks.stream()
                .filter(task -> isTaskValidForDateRange(task, startDate, endDate))
                .collect(Collectors.toList());

        return filteredTasks.stream().map(this::convertToTaskResponseWithMetrics).collect(Collectors.toList());
    }

    // Helper method to check if task is valid for date range
    private boolean isTaskValidForDateRange(Task task, LocalDate startDate, LocalDate endDate) {
        if (task.getStartDate().isAfter(endDate) || task.getEndDate().isBefore(startDate)) {
            return false;
        }

        if (task.isEveryday()) {
            return true;
        }

        if (task.isEveryWeekend()) {
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                if (current.getDayOfWeek() == DayOfWeek.SATURDAY ||
                        current.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    return true;
                }
                current = current.plusDays(1);
            }
            return false;
        }

        if (task.getDaysOfWeek() != null && !task.getDaysOfWeek().isEmpty()) {
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                if (task.getDaysOfWeek().contains(current.getDayOfWeek())) {
                    return true;
                }
                current = current.plusDays(1);
            }
            return false;
        }

        return true;
    }



    public TaskResponseTodayDto getTaskResponseByIdAndDate(Long taskId, LocalDate date) {

        if(date == null) {
            date = LocalDate.now();
        }
        // 1️⃣ Fetch task
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Task not found with ID: " + taskId)
                );

        // 2️⃣ Fetch completion for that date
        TaskCompletion completion = taskCompletionRepository
                .findByTaskIdAndCompletionDate(taskId, date)
                .orElse(null);

        // 3️⃣ Convert to response
        return toTaskResponseTodayDto2(completion);
    }


    public TaskResponse toTaskResponseDto(Task task) {
        return convertToTaskResponseWithMetrics(task);
    }

    public List<TaskResponse> getTasksForMonth(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Task> taskList = taskRepository.findTasksInDateRange(startDate, endDate);

        return taskList.stream()
                .map(this::convertToTaskResponseWithMetrics)
                .collect(Collectors.toList());
    }

    public TaskMonthlyResponse getTaskDetailsForMonth(Long taskId, int year, int month, User user) {

        Task task = taskRepository.findByIdAndCreatedBy(taskId, user)
                .orElseThrow(() -> new BusinessException("Task not found for the user"));

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<TaskCompletion> completions =
                taskCompletionRepository.findByTaskIdAndCompletionDateBetween(taskId, start, end);

        List<TaskMonthlyResponse.DayStatus> list =
                completions.stream()
                        .map(c -> new TaskMonthlyResponse.DayStatus(
                                c.getCompletionDate(),
                                c.getStatus()   // status enum
                        ))
                        .toList();

        return new TaskMonthlyResponse(task.getId(), task.getTitle(), list);
    }

    // Get today's task completions for a user
    public List<TaskResponseTodayDto> getTaskByDate(
            User user,
            Long forUserId,
            LocalDate date,
            String viewType,
            String status,
            String routine,
            String priority) {

        String finalViewType = (viewType == null || viewType.trim().isEmpty())
                ? "DAY" : viewType.trim().toUpperCase();

        if (!"DAY".equalsIgnoreCase(finalViewType) && !"MONTH".equalsIgnoreCase(finalViewType)) {
            throw new BusinessException("viewType must be DAY or MONTH");
        }

        LocalDate startDate, endDate;
        if ("DAY".equals(finalViewType)) {
            LocalDate targetDate = (date == null) ? LocalDate.now() : date;
            startDate = endDate = targetDate;
        } else {
            YearMonth ym = YearMonth.from(date != null ? date : LocalDate.now());
            startDate = ym.atDay(1);
            endDate = ym.atEndOfMonth();
        }

        List<TaskCompletion> taskList;

        if (user.getRelationship() == Relationship.PARENT && forUserId != null) {
            User child = userRepository.findById(forUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid child userId"));

            if (!child.getFamily().equals(user.getFamily()) ||
                    child.getRelationship() != Relationship.CHILD) {
                throw new BusinessException("forUserId must be a child of the parent");
            }

            taskList = taskCompletionRepository
                    .findByCompletionDateBetweenAndTask_AssignedTo(startDate, endDate, child);

        } else if (user.getRelationship() == Relationship.PARENT) {
            List<User> children = userRepository
                    .findByFamilyAndRelationship(user.getFamily(), Relationship.CHILD);

            if (children.isEmpty()) {
                System.out.print("Parent has no children in the family.");
                return List.of();
            }

            taskList = taskCompletionRepository.findForParentViewRange(startDate, endDate, children);

        } else {
            taskList = taskCompletionRepository
                    .findByCompletionDateBetweenAndTask_AssignedTo(startDate, endDate, user);
        }

        if ("DAY".equals(finalViewType) && date != null && date.equals(LocalDate.now())) {
            LocalTime now = LocalTime.now();

            taskList.stream()
                    .filter(t -> t.getStatus() == TaskCompletion.CompletionStatus.PENDING)
                    .filter(t -> t.getTask() != null && t.getTask().getEndTime() != null)
                    .filter(t -> now.isAfter(t.getTask().getEndTime()))
                    .forEach(t -> t.setStatus(TaskCompletion.CompletionStatus.SKIPPED));

            taskList.stream()
                    .filter(t -> t.getStatus() == TaskCompletion.CompletionStatus.ONGOING)
                    .filter(t -> t.getTask() != null && t.getTask().getEndTime() != null)
                    .filter(t -> !now.isBefore(t.getTask().getEndTime()))
                    .forEach(t -> {
                        t.setStatus(TaskCompletion.CompletionStatus.COMPLETED);
                        completeTask(t.getTask().getId(), user);
                    });

            taskCompletionRepository.saveAll(taskList);
        }

        status = (status == null || status.trim().isEmpty()) ? null : status.trim().toUpperCase();
        routine = (routine == null || routine.trim().isEmpty()) ? null : routine.trim().toUpperCase();
        priority = (priority == null || priority.trim().isEmpty()) ? null : priority.trim().toUpperCase();

        String finalStatus = status;
        String finalRoutine = routine;
        String finalPriority = priority;

        return taskList.stream()
                .filter(t -> finalStatus == null || t.getStatus().name().equalsIgnoreCase(finalStatus))
                .filter(t -> finalRoutine == null || t.getTask().getRoutine().equalsIgnoreCase(finalRoutine))
                .filter(t -> finalPriority == null || t.getTask().getPriority().name().equalsIgnoreCase(finalPriority))
                .map(this::toTaskResponseTodayDto)
                .toList();
    }




    private TaskResponseTodayDto toTaskResponseTodayDto(TaskCompletion tc) {

        if (tc == null) {
            throw new ResourceNotFoundException(
                    "Task not found for the selected date"
            );
        }

        Task task = tc.getTask();

        // Reuse your existing metric calculator
        TaskService.TaskMetrics metrics = calculateTaskMetrics(task);

        List<BadgeResponseDTO> limitedBadges =
                getLimitedMilestoneBadges(task.getCreatedBy().getId());

        return TaskResponseTodayDto.builder()
                .id(task.getId())
                .assignedTo(task.getAssignedTo().getId())
                .createdBy(task.getCreatedBy().getId())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(tc.getStatus().name())
                .completionDate(tc.getCompletionDate())
                .completionTime(tc.getCompletionTime())
                .priority(task.getPriority().name())
                .routine(task.getRoutine())
                .coinReward(task.getRewardCoins())
                .sharedWithParent(task.getSharedWithParent())
                .isEveryday(task.isEveryday())
                .isEveryWeekend(task.isEveryWeekend())
                .tags(task.getTags())
                .daysOfWeek(task.getDaysOfWeek())
                .startDate(task.getStartDate())
                .startTime(task.getStartTime())
                .endDate(task.getEndDate())
                .endTime(task.getEndTime())

                // METRICS
                .currentTaskStreak(metrics.currentTaskStreak())
                .highestTaskStreak(metrics.highestTaskStreak())
                .coinsEarned(metrics.coinsEarned())
                .completionPercentage(metrics.completionPercentage())
                .totalScheduledDays(metrics.totalScheduledDays())
                .totalCompletedDays(metrics.totalCompletedDays())
                .milestoneBadges(limitedBadges)
                .build();
    }





    private TaskResponseTodayDto toTaskResponseTodayDto2(TaskCompletion tc) {
        if (tc == null) {
            throw new ResourceNotFoundException("Task not found for the selected date");
        }

        Task task = tc.getTask();

        System.out.println("\n========== BUILDING TASK RESPONSE FOR: " + task.getTitle() + " ==========");
        System.out.println("Completion date: " + tc.getCompletionDate());
        System.out.println("Completion status: " + tc.getStatus());

        // Reuse your existing metric calculator
        TaskService.TaskMetrics metrics = calculateTaskMetrics2(task);

        System.out.println("Metrics calculated - Current Streak: " + metrics.currentTaskStreak());
        System.out.println("Metrics calculated - Highest Streak: " + metrics.highestTaskStreak());
        System.out.println("Metrics calculated - Completion %: " + metrics.completionPercentage());

        List<BadgeResponseDTO> limitedBadges = getLimitedMilestoneBadges(task.getCreatedBy().getId());

        TaskResponseTodayDto response = TaskResponseTodayDto.builder()
                .id(task.getId())
                .assignedTo(task.getAssignedTo().getId())
                .createdBy(task.getCreatedBy().getId())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(tc.getStatus().name())
                .completionDate(tc.getCompletionDate())
                .completionTime(tc.getCompletionTime())
                .priority(task.getPriority().name())
                .routine(task.getRoutine())
                .coinReward(task.getRewardCoins())
                .sharedWithParent(task.getSharedWithParent())
                .isEveryday(task.isEveryday())
                .isEveryWeekend(task.isEveryWeekend())
                .tags(task.getTags())
                .daysOfWeek(task.getDaysOfWeek())
                .startDate(task.getStartDate())
                .startTime(task.getStartTime())
                .endDate(task.getEndDate())
                .endTime(task.getEndTime())
                // METRICS
                .currentTaskStreak(metrics.currentTaskStreak())
                .highestTaskStreak(metrics.highestTaskStreak())
                .coinsEarned(metrics.coinsEarned())
                .completionPercentage(metrics.completionPercentage())
                .totalScheduledDays(metrics.totalScheduledDays())
                .totalCompletedDays(metrics.totalCompletedDays())
                .milestoneBadges(limitedBadges)
                .build();

        System.out.println("Response built successfully!");
        System.out.println("==========================================\n");

        return response;
    }









    // Get completion history for a task
    public List<TaskCompletion> getTaskCompletionHistory(Long taskId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            return List.of();
        }
        return taskCompletionRepository.findByTask(taskOpt.get());
    }

    // Scheduled method to mark missed tasks
    @Scheduled(cron = "0 0 0 * * ?") // Run daily at midnight
    @Transactional
    public void markMissedTasks() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        List<TaskCompletion> pendingCompletions = taskCompletionRepository
                .findByCompletionDateAndStatus(yesterday, TaskCompletion.CompletionStatus.PENDING);

        for (TaskCompletion completion : pendingCompletions) {
            completion.setStatus(TaskCompletion.CompletionStatus.SKIPPED);
            taskCompletionRepository.save(completion);

            // Update overall task status
            updateTaskStatus(completion.getTask());
        }
    }





































































    private TaskMetrics calculateTaskMetrics2(Task task) {
        System.out.println("\n========== CALCULATING METRICS FOR TASK: " + task.getTitle() + " ==========");

        // Use TaskCompletion records for accurate metrics
        List<TaskCompletion> completions = taskCompletionRepository.findByTask(task);

        System.out.println("Total completions found: " + completions.size());

        int totalScheduledDays = completions.size();
        int totalCompletedDays = (int) completions.stream().filter(TaskCompletion::getCompleted).count();
        int coinsEarned = completions.stream().mapToInt(TaskCompletion::getCoinsEarned).sum();
        double completionPercentage = totalScheduledDays > 0 ?
                (double) totalCompletedDays / totalScheduledDays * 100 : 0.0;

        System.out.println("Total scheduled days: " + totalScheduledDays);
        System.out.println("Total completed days: " + totalCompletedDays);
        System.out.println("Coins earned: " + coinsEarned);
        System.out.println("Completion percentage: " + completionPercentage);

        // Calculate streak based on consecutive scheduled completed days
        int currentStreak = calculateCurrentStreak2(task, completions);
        int highestStreak = calculateHighestStreak2(task, completions);

        System.out.println("Final Current Streak: " + currentStreak);
        System.out.println("Final Highest Streak: " + highestStreak);
        System.out.println("==========================================\n");

        return new TaskMetrics(currentStreak, highestStreak, coinsEarned, completionPercentage,
                totalScheduledDays, totalCompletedDays);
    }

    private int calculateCurrentStreak2(Task task, List<TaskCompletion> completions) {
        System.out.println("\n--- Calculating CURRENT STREAK ---");

        // Get all scheduled dates for this task
        List<LocalDate> scheduledDates = getScheduledDates2(task);
        if (scheduledDates.isEmpty()) {
            System.out.println("No scheduled dates found!");
            return 0;
        }

        System.out.println("All scheduled dates: " + scheduledDates);

        // Sort scheduled dates in descending order (most recent first)
        List<LocalDate> sortedScheduledDates = scheduledDates.stream()
                .sorted(Collections.reverseOrder())
                .toList();

        System.out.println("Scheduled dates (descending): " + sortedScheduledDates);

        // Create a set of completed dates for quick lookup
        Set<LocalDate> completedDates = completions.stream()
                .filter(TaskCompletion::getCompleted)
                .map(TaskCompletion::getCompletionDate)
                .collect(Collectors.toSet());

        System.out.println("Completed dates: " + completedDates);

        int streak = 0;
        LocalDate today = LocalDate.now();
        System.out.println("Today's date: " + today);

        // Start from the most recent scheduled date and go backwards
        for (LocalDate scheduledDate : sortedScheduledDates) {
            System.out.println("Checking scheduled date: " + scheduledDate);

            // Skip future dates
            if (scheduledDate.isAfter(today)) {
                System.out.println("  - Skipping (future date)");
                continue;
            }

            // Skip today if not completed (don't break, just skip)
            if (scheduledDate.equals(today) && !completedDates.contains(scheduledDate)) {
                System.out.println("  - Today NOT COMPLETED - skipping today, will check previous days");
                continue;
            }

            if (completedDates.contains(scheduledDate)) {
                streak++;
                System.out.println("  - COMPLETED! Streak increased to: " + streak);
            } else {
                System.out.println("  - NOT COMPLETED! Breaking streak at: " + scheduledDate);
                break;
            }
        }

        System.out.println("Current streak result: " + streak);
        return streak;
    }

    private int calculateHighestStreak2(Task task, List<TaskCompletion> completions) {
        System.out.println("\n--- Calculating HIGHEST STREAK ---");

        // Get all scheduled dates for this task
        List<LocalDate> scheduledDates = getScheduledDates2(task);
        if (scheduledDates.isEmpty()) {
            System.out.println("No scheduled dates found!");
            return 0;
        }

        // Sort scheduled dates in ascending order
        List<LocalDate> sortedScheduledDates = scheduledDates.stream()
                .sorted()
                .toList();

        System.out.println("Scheduled dates (ascending): " + sortedScheduledDates);

        // Create a set of completed dates for quick lookup
        Set<LocalDate> completedDates = completions.stream()
                .filter(TaskCompletion::getCompleted)
                .map(TaskCompletion::getCompletionDate)
                .collect(Collectors.toSet());

        System.out.println("Completed dates: " + completedDates);

        int highestStreak = 0;
        int currentStreak = 0;

        System.out.println("\nChecking each scheduled date in order:");
        for (int i = 0; i < sortedScheduledDates.size(); i++) {
            LocalDate scheduledDate = sortedScheduledDates.get(i);
            boolean isCompleted = completedDates.contains(scheduledDate);

            System.out.println("  Date " + scheduledDate + " - Completed: " + isCompleted);

            if (isCompleted) {
                currentStreak++;
                System.out.println("    Current streak: " + currentStreak);
                highestStreak = Math.max(highestStreak, currentStreak);
                System.out.println("    Highest streak so far: " + highestStreak);
            } else {
                System.out.println("    Breaking streak at " + scheduledDate + " (not completed)");
                currentStreak = 0;
            }
        }

        System.out.println("Final highest streak: " + highestStreak);
        return highestStreak;
    }

//    private List<LocalDate> getScheduledDates2(Task task) {
//        System.out.println("\n--- Getting scheduled dates for task ---");
//        List<LocalDate> scheduledDates = new ArrayList<>();
//
//        LocalDate startDate = task.getStartDate();
//        LocalDate endDate = task.getEndDate();
//
//        System.out.println("Task start date: " + startDate);
//        System.out.println("Task end date: " + endDate);
//        System.out.println("Days of week: " + task.getDaysOfWeek());
//        System.out.println("Is everyday: " + task.isEveryday());
//        System.out.println("Is every weekend: " + task.isEveryWeekend());
//
//        if (startDate == null || endDate == null) {
//            System.out.println("Start or end date is null!");
//            return scheduledDates;
//        }
//
//        LocalDate currentDate = startDate;
//
//        while (!currentDate.isAfter(endDate)) {
//            if (isTaskScheduledForDate(task, currentDate)) {
//                scheduledDates.add(currentDate);
//                System.out.println("  Scheduled: " + currentDate + " (" + currentDate.getDayOfWeek() + ")");
//            }
//            currentDate = currentDate.plusDays(1);
//        }
//
//        System.out.println("Total scheduled dates: " + scheduledDates.size());
//        return scheduledDates;
//    }

//    private boolean isTaskScheduledForDate2(Task task, LocalDate date) {
//        // Check if task is everyday task
//        if (task.isEveryday()) {
//            return true;
//        }
//
//        // Check if task is every weekend
//        if (task.isEveryWeekend()) {
//            DayOfWeek dayOfWeek = date.getDayOfWeek();
//            return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
//        }
//
//        // Check specific days of week
//        if (task.getDaysOfWeek() != null && !task.getDaysOfWeek().isEmpty()) {
//            return task.getDaysOfWeek().contains(date.getDayOfWeek());
//        }
//
//        return false;
//    }























    private List<LocalDate> getScheduledDates2(Task task) {
        System.out.println("\n--- Getting scheduled dates for task ---");
        List<LocalDate> scheduledDates = new ArrayList<>();

        LocalDate startDate = task.getStartDate();
        LocalDate endDate = task.getEndDate();

        System.out.println("Task start date: " + startDate);
        System.out.println("Task end date: " + endDate);
        System.out.println("Days of week: " + task.getDaysOfWeek());
        System.out.println("Is everyday: " + task.isEveryday());
        System.out.println("Is every weekend: " + task.isEveryWeekend());

        if (startDate == null || endDate == null) {
            System.out.println("Start or end date is null!");
            return scheduledDates;
        }

        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            if (isTaskScheduledForDate2(task, currentDate)) {
                scheduledDates.add(currentDate);
                System.out.println("  Scheduled: " + currentDate + " (" + currentDate.getDayOfWeek() + ")");
            }
            currentDate = currentDate.plusDays(1);
        }

        System.out.println("Total scheduled dates: " + scheduledDates.size());
        return scheduledDates;
    }

















    private boolean isTaskScheduledForDate2(Task task, LocalDate date) {
        // Check if task is everyday task
        if (task.isEveryday()) {
            return true;
        }

        // Check if task is every weekend
        if (task.isEveryWeekend()) {
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        }

        // Check specific days of week
        if (task.getDaysOfWeek() != null && !task.getDaysOfWeek().isEmpty()) {
            // FIX: Compare DayOfWeek objects directly, not strings
            return task.getDaysOfWeek().contains(date.getDayOfWeek());
        }

        return false;
    }






}