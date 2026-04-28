package com.superme.service;

import com.superme.dto.*;
import com.superme.enums.ActivityType;
import com.superme.enums.Relationship;
import com.superme.exception.BusinessException;
import com.superme.exception.ResourceNotFoundException;
import com.superme.model.*;
import com.superme.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HabitService {
    private final TaskCompletionRepository taskCompletionRepository;

    private final HabitRepository habitRepository;
    private final UserRepository userRepository;
    private final CoinTransactionRepository coinTransactionRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final FamilyMemberService familyMemberService;
    private final ApplicationEventPublisher eventPublisher;
    private final BadgeService badgeService;
    private final HabitCompletionRepository habitCompletionRepository;
    private final TaskRepository taskRepository;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM dd, yyyy");


    // Update habit: allow creator or parent of the child
    @Transactional
    public Optional<Habit> updateHabit(Long habitId, Habit updatedHabit, User user) {
        Optional<Habit> habitOpt = habitRepository.findById(habitId);
        if (habitOpt.isEmpty())
            return Optional.empty();
        Habit habit = habitOpt.get();

        // If user is creator, always allow
        if (habit.getCreatedBy().equals(user)) {
            return updateHabitFields(habit, updatedHabit);
        }

        // If user is parent in the same family and habit belongs to their child
        if (user.getFamily() != null && habit.getCreatedBy() != null && habit.getCreatedBy().getFamily() != null &&
                user.getFamily().getId().equals(habit.getCreatedBy().getFamily().getId())) {

            // Check relationships
            Optional<FamilyMember> parentFm = familyMemberService.getByUser(user);
            Optional<FamilyMember> childFm = familyMemberService.getByUser(habit.getCreatedBy());
            if (parentFm.isPresent() && childFm.isPresent()
                    && "parent".equals(parentFm.get().getRelationship())
                    && "child".equals(childFm.get().getRelationship())) {
                return updateHabitFields(habit, updatedHabit);
            }
        }
        return Optional.empty();
    }

    // Helper to update fields
    private Optional<Habit> updateHabitFields(Habit habit, Habit updatedHabit) {
        if (updatedHabit.getTitle() != null) {
            habit.setTitle(updatedHabit.getTitle());
        }
        if (updatedHabit.getDescription() != null) {
            habit.setDescription(updatedHabit.getDescription());
        }
        if (updatedHabit.getPriority() != null) {
            habit.setPriority(updatedHabit.getPriority());
        }
        if (updatedHabit.getStatus() != null) {
            habit.setStatus(updatedHabit.getStatus());
        }
        if (updatedHabit.getCoinReward() > 0) {
            habit.setCoinReward(updatedHabit.getCoinReward());
        }
        if (updatedHabit.getTags() != null) {
            habit.setTags(updatedHabit.getTags());
        }
        if (updatedHabit.getStartDate() != null) {
            habit.setStartDate(updatedHabit.getStartDate());
        }
        if (updatedHabit.getEndDate() != null) {
            habit.setEndDate(updatedHabit.getEndDate());
        }
        if (updatedHabit.getStartTime() != null) {
            habit.setStartTime(updatedHabit.getStartTime());
        }
        if (updatedHabit.getEndTime() != null) {
            habit.setEndTime(updatedHabit.getEndTime());
        }
        if (updatedHabit.getDaysOfWeek() != null) {
            habit.setDaysOfWeek(updatedHabit.getDaysOfWeek());
        }

        checkForDuplicateHabitOrTask(habit, habit.getAssignedTo());

        habit.setRoutine(updatedHabit.getRoutine());
        // Update everyday/everyWeekend toggles
        habit.setEveryday(updatedHabit.isEveryday());
        habit.setEveryWeekend(updatedHabit.isEveryWeekend());

        // Update timestamp
        habit.setUpdatedAt(LocalDate.now());

        Habit savedHabit = habitRepository.save(habit);

        // 2️⃣ Recalculate scheduled completions
        updateScheduledCompletions(savedHabit);
        return Optional.of(savedHabit);
    }

    // Overload for controller compatibility: getHabitsForUser
    public List<Habit> getHabitsForUser(User user) {
        List<Habit> habits = getAllHabitsForUser(user);

        if (habits == null || habits.isEmpty()) {
            throw new ResourceNotFoundException("No habits found for user.");
        }
        return habits;
    }

    // Overload for controller compatibility: addHabit(Habit, User)
    public Habit addHabit(Habit habit, User user) {
        return addHabit(habit, user, habit.getDaysOfWeek());
    }

    // Delete habit: allow creator or parent of the child
    @Transactional
    public boolean deleteHabit(Long habitId, User user) {
        Optional<Habit> habitOpt = habitRepository.findById(habitId);
        if (habitOpt.isEmpty())
            return false;
        Habit habit = habitOpt.get();

        // If user is creator, always allow
        if (habit.getCreatedBy().equals(user)) {
            // Delete all related records in correct order
            deleteFutureScheduledHabitRecords(habit);
            return true;
        }

        // If user is parent in the same family and habit belongs to their child
        if (user.getFamily() != null && habit.getCreatedBy().getFamily() != null &&
                user.getFamily().getId().equals(habit.getCreatedBy().getFamily().getId())) {

            var parentFm = familyMemberService.getByUser(user);
            var childFm = familyMemberService.getByUser(habit.getCreatedBy());
            if (parentFm.isPresent() && childFm.isPresent()
                    && "parent".equals(parentFm.get().getRelationship())
                    && "child".equals(childFm.get().getRelationship())) {
                // Delete all related records in correct order
                deleteFutureScheduledHabitRecords(habit);
                return true;
            }
        }
        return false;
    }

    @Transactional
    protected void deleteFutureScheduledHabitRecords(Habit habit) {

        if (habit == null || habit.getId() == null) return;

        Long habitId = habit.getId();
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        LocalDate startDate = habit.getStartDate();
        LocalTime startTime = habit.getStartTime();

        // ✅ Habit started ONLY based on today's time
        boolean habitStarted =
                startDate.isEqual(today) && !startTime.isAfter(now);

        // 1️⃣ Delete habit completions
        if (habitStarted) {
            // 🔹 Do NOT delete today, delete only future
            habitCompletionRepository
                    .deleteByHabitIdAndCompletionDateAfter(habitId, today);
        } else {
            // 🔹 Delete today + future
            habitCompletionRepository
                    .deleteByHabitIdAndCompletionDateGreaterThanEqual(habitId, today);
        }

        // 2️⃣ Set endDate
        if (habitStarted) {
            habit.setEndDate(today);
        } else {
            habit.setEndDate(today.minusDays(1));
        }

        habitRepository.save(habit);
    }




    // Get all habits for a parent (by family)
    public List<Habit> getHabitsForParent(User parent) {
        return habitRepository.findByFamilyId(parent.getFamily().getId());
    }

    // Mark a habit as completed by the assigned user (child/self/creator)
    @Transactional
    public Optional<HabitCompletion> completeHabit(Long habitId, User user) {
        try {
            final int DAILY_COIN_THRESHOLD  = 50;

            Optional<Habit> habitOpt = habitRepository.findById(habitId);
            if (habitOpt.isEmpty()) {
                return Optional.empty();
            }

            Habit habit = habitOpt.get();
            LocalDate today = LocalDate.now();

            // Find today's completion record
            Optional<HabitCompletion> completionOpt = habitCompletionRepository
                    .findByHabitAndCompletionDate(habit, today);

            if (completionOpt.isEmpty()) {
                throw new BusinessException("No scheduled habit for today");
            }

            HabitCompletion completion = completionOpt.get();

            // Check if already completed
            if (completion.getCompleted()) {
                throw new BusinessException("Habit already completed for today");
            }

            // Check permissions
            boolean canComplete = habit.getCreatedBy().equals(user)
                    || (habit.getFamily() != null && habit.getFamily().equals(user.getFamily()));

            if (!canComplete) {
                throw new BusinessException("Not authorized to complete this habit");
            }

            // Calculate coins based on timing
            int coinsEarned = calculateCoinsForCompletion(habit, completion);

            // Enforce daily coin cap

            int coinsEarnedToday = getCoinsEarnedToday(user);
            if (coinsEarnedToday >= DAILY_COIN_THRESHOLD ) {
                coinsEarned = 0;
            }

            // Update completion record
            completion.setCompleted(true);
            completion.setCoinsEarned(coinsEarned);
            completion.setStatus(HabitCompletion.CompletionStatus.COMPLETED);
            completion.setCompletionTime(LocalTime.now());

            HabitCompletion savedCompletion = habitCompletionRepository.save(completion);

            // Update user coins
            if (coinsEarned > 0) {
                user.setCoins(user.getCoins() + coinsEarned);
                userRepository.save(user);

                // Record transaction
                CoinTransaction tx = new CoinTransaction();
                tx.setUser(user);
                tx.setAmount(coinsEarned);
                tx.setType("HABIT_COMPLETION");
                tx.setDescription("Earned " + coinsEarned + " coins for completing habit");
                tx.setHabit(habit);
                tx.setTimestamp(LocalDateTime.now());
                coinTransactionRepository.save(tx);
            }

            // Update overall habit status
            updateHabitStatus(habit);

            // Trigger badge updates
//            triggerBadgeUpdates(user, coinsEarned);

            badgeService.triggerBadgeUpdate(
                    user.getId(),
                    ActivityType.HABIT_COMPLETED,
                    coinsEarned,
                    "HABITS"
            );


            return Optional.of(savedCompletion);
        } catch (Exception e) {
            log.error("Error completing habit {}: {}", habitId, e.getMessage());
            throw new BusinessException("Error completing habit: " + e.getMessage());
        }

    }

    // Get all habits for a kid (by family)
    public List<Habit> getHabitsForKid(User kid) {
        return habitRepository.findByFamilyId(kid.getFamily().getId());
    }

    // Get all habits for a specific family
    public List<Habit> getHabitsByFamily(Long familyId) {
        return habitRepository.findByFamilyId(familyId);
    }

    // Get all habits for a user on a specific day
    public List<Habit> getHabitsForUserByDay(User user, LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        List<Habit> habits = habitRepository.findByCreatedByAndDaysOfWeekContaining(user, dayOfWeek);

        if (habits == null || habits.isEmpty()) {
            throw new ResourceNotFoundException("No habits found for today.");
        }

        return habits;
    }

    // Add habit with days of week (for advanced use), and add to CalendarEvent
    @Transactional
    public Habit addHabit(Habit habit, User user, Set<DayOfWeek> daysOfWeek) {
        validateHabitDates(habit);
        checkForDuplicateHabitOrTask(habit, user);
        habit.setFamily(user.getFamily());

        // ✅ ENSURE: For habits, everyWeekend is always false
        habit.setEveryWeekend(false);

        // Handle everyday logic
        if (habit.isEveryday()) {
            Set<DayOfWeek> allDays = EnumSet.allOf(DayOfWeek.class);
            habit.setDaysOfWeek(allDays);
        } else {
            habit.setDaysOfWeek(daysOfWeek != null ? daysOfWeek : new HashSet<>());
        }

        // ✅ FIXED: Always set to PENDING initially, regardless of recurring status
        LocalDate today = LocalDate.now();
        if (habit.getEndDate().isBefore(today)) {
            // If end date is in the past, set to COMPLETED immediately
            habit.setStatus(Habit.HabitStatus.COMPLETED);
        } else {
            // For all active habits, start as PENDING until first completion
            habit.setStatus(Habit.HabitStatus.PENDING);
        }

        habit.setCreatedAt(LocalDate.now());
        habit.setUpdatedAt(LocalDate.now());

        Habit savedHabit = habitRepository.save(habit);

        // ✅ CREATE HABIT COMPLETION ENTRIES for each scheduled day
        updateScheduledCompletions(savedHabit);

        // Create calendar event
        createCalendarEvent(savedHabit);

        return savedHabit;
    }

    // Validate habit dates
    private void validateHabitDates(Habit habit) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        if (habit.getStartDate().isBefore(today)) {
            throw new BusinessException("Start date cannot be in the past");
        }

        if (habit.getStartDate().isEqual(today) && habit.getStartTime().isBefore(now)) {
            throw new BusinessException("Start time cannot be in the past for today's date");
        }

        if (habit.getEndDate().isBefore(habit.getStartDate())) {
            throw new BusinessException("End date cannot be before start date");
        }

        if (habit.getStartDate().equals(habit.getEndDate()) &&
                habit.getEndTime().isBefore(habit.getStartTime())) {
            throw new BusinessException("End time cannot be before start time on the same day");
        }
    }

    // Check for duplicate habits
    private void checkForDuplicateHabitOrTask(Habit habit, User user) {

        // ⛔ Mandatory validation (missing earlier)
        if (!habit.getEndTime().isAfter(habit.getStartTime())) {
            throw new BusinessException("End time must be after start time.");
        }

        // Helper: checks at least one common day (everyday + null safe)
        Predicate<Set<DayOfWeek>> hasCommonDay =
                existingDays -> {
                    if (habit.isEveryday()) return true;
                    if (existingDays == null || habit.getDaysOfWeek() == null) return false;
                    return existingDays.stream().anyMatch(habit.getDaysOfWeek()::contains);
                };

        Long currentHabitId = habit.getId(); // null for create, not null for update

        // 1️⃣ Check HABIT schedules
        Optional<Habit> overlappingHabit = habitRepository.findByAssignedTo(user).stream()
                .filter(h ->
                        (currentHabitId == null || !h.getId().equals(currentHabitId)) &&

                                // Date overlap
                                !h.getEndDate().isBefore(habit.getStartDate()) &&
                                !h.getStartDate().isAfter(habit.getEndDate()) &&

                                // Time overlap (STRICT, boundary-safe)
                                h.getStartTime().isBefore(habit.getEndTime()) &&
                                h.getEndTime().isAfter(habit.getStartTime()) &&

                                // Day overlap
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


        // 2️⃣ Check TASK schedules
        Optional<Task> overlappingTask = taskRepository.findByAssignedTo(user).stream()
                .filter(t ->
                        !t.getEndDate().isBefore(habit.getStartDate()) &&
                                !t.getStartDate().isAfter(habit.getEndDate()) &&
                                t.getStartTime().isBefore(habit.getEndTime()) &&
                                t.getEndTime().isAfter(habit.getStartTime()) &&
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


        // 3️⃣ Check HABIT COMPLETION (status table, schedule-based check)
        Optional<HabitCompletion> overlappingCompletion =
                habitCompletionRepository
                        .findScheduleConflicts(
                                user,
                                habit.getId(),
                                habit.getStartDate(),
                                habit.getEndDate(),
                                habit.getStartTime(),
                                habit.getEndTime(),
                                PageRequest.of(0, 1)
                        )
                        .stream()
                        .findFirst();

        if (overlappingCompletion.isPresent()) {
            Habit h = overlappingCompletion.get().getHabit();

            String startDate = h.getStartDate().format(DATE_FORMATTER);
            String endDate   = h.getEndDate().format(DATE_FORMATTER);

            throw new BusinessException(
                    "This time overlaps with habit '" + h.getTitle() +
                            "' (" + h.getStartTime() + " - " + h.getEndTime() + "). " +
                            "Pick a different slot. [" + startDate + " - " + endDate + "]"
            );
        }

    }



    // Create completion records
    @Transactional
    public void updateScheduledCompletions(Habit habit) {

        List<HabitCompletion> existingCompletions =
                habitCompletionRepository.findByHabitId(habit.getId());

        Map<LocalDate, HabitCompletion> existingMap =
                existingCompletions.stream()
                        .collect(Collectors.toMap(
                                HabitCompletion::getCompletionDate,
                                c -> c
                        ));

        // build valid dates
        Set<LocalDate> validDates = new HashSet<>();
        LocalDate currentDate = habit.getStartDate();
        LocalDate endDate = habit.getEndDate();

        while (!currentDate.isAfter(endDate)) {
            if (isDateScheduledForHabit(habit, currentDate)) {
                validDates.add(currentDate);
            }
            currentDate = currentDate.plusDays(1);
        }

        // delete invalid future PENDING entries
        List<HabitCompletion> toDelete = existingCompletions.stream()
                .filter(c ->
                        c.getCompletionDate().isAfter(LocalDate.now()) &&
                                c.getStatus() == HabitCompletion.CompletionStatus.PENDING &&
                                !validDates.contains(c.getCompletionDate())
                )
                .toList();

        habitCompletionRepository.deleteAll(toDelete);

        // create missing future entries
        List<HabitCompletion> toCreate = new ArrayList<>();

        for (LocalDate date : validDates) {
            if (!existingMap.containsKey(date) && !date.isBefore(LocalDate.now())) {
                toCreate.add(
                        HabitCompletion.builder()
                                .habit(habit)
                                .completionDate(date)
                                .completionTime(null)
                                .coinsEarned(0)
                                .completed(false)
                                .status(HabitCompletion.CompletionStatus.PENDING)
                                .build()
                );
            }
        }

        habitCompletionRepository.saveAll(toCreate);

        log.info(
                "Habit {} updated → deleted {}, created {} completion entries",
                habit.getId(), toDelete.size(), toCreate.size()
        );
    }


    // Create calendar event
    private void createCalendarEvent(Habit habit) {
        CalendarEvent event = CalendarEvent.builder()
                .title(habit.getTitle())
                .description(habit.getDescription())
                .startDate(habit.getStartDate())
                .startTime(habit.getStartTime() != null ? habit.getStartTime().toString() : null)
                .endDate(habit.getEndDate())
                .endTime(habit.getEndTime() != null ? habit.getEndTime().toString() : null)
                .type(CalendarEvent.TYPE_HABIT)
                .completed(habit.getStatus() == Habit.HabitStatus.COMPLETED)
                .createdBy(habit.getCreatedBy())
                .family(habit.getFamily())
                .build();
        calendarEventRepository.save(event);
    }

    // Update habit with days of week (for advanced use)
    public Habit updateHabit(Long habitId, Habit updatedHabit, User user, Set<DayOfWeek> daysOfWeek) {
        Habit habit = habitRepository.findById(habitId)
                .filter(h -> h.getCreatedBy().equals(user))
                .orElseThrow();
        habit.setTitle(updatedHabit.getTitle());
        habit.setDescription(updatedHabit.getDescription());
        habit.setPriority(updatedHabit.getPriority());
        habit.setRoutine(updatedHabit.getRoutine());
        if (updatedHabit.getStartDate() != null) {
            habit.setStartDate(updatedHabit.getStartDate());
        }
        if (updatedHabit.getEndDate() != null) {
            habit.setEndDate(updatedHabit.getEndDate());
        }
        habit.setStartTime(updatedHabit.getStartTime());
        habit.setEndTime(updatedHabit.getEndTime());
        habit.setDaysOfWeek(daysOfWeek);
        return habitRepository.save(habit);
    }

    // Get all habits created by a user
    public List<Habit> getAllHabitsForUser(User user) {
        return habitRepository.findByCreatedBy(user);
    }

    // Return all habits for a user as HabitResponse DTOs with metrics and 4 badges
    public List<HabitResponse> getAllHabitResponsesForUser(User user) {
        List<Habit> habits = getAllHabitsForUser(user);
        return habits.stream()
                .map(this::convertToHabitResponseWithMetrics)
                .collect(Collectors.toList());
    }

    private HabitResponse convertToHabitResponseWithMetrics(Habit habit) {
        // Calculate habit-specific metrics using completion records
        HabitMetrics metrics = calculateHabitMetrics(habit);

        // Get only 4 milestone badges
        List<BadgeResponseDTO> limitedBadges = getLimitedMilestoneBadges(habit.getCreatedBy().getId());

        return HabitResponse.builder()
                .id(habit.getId())
                .title(habit.getTitle())
                .description(habit.getDescription())
                .status(habit.getStatus())
                .priority(habit.getPriority() != null ? habit.getPriority().name() : null)
                .routine(habit.getRoutine())
                .startDate(habit.getStartDate())
                .startTime(habit.getStartTime())
                .endDate(habit.getEndDate())
                .endTime(habit.getEndTime())
                .createdBy(habit.getCreatedBy().getId())
                .familyId(habit.getFamily() != null ? habit.getFamily().getId() : null)
                .daysOfWeek(habit.getDaysOfWeek())
                .createdAt(habit.getCreatedAt())
                .updatedAt(habit.getUpdatedAt())
                .isEveryday(habit.isEveryday())
                .isEveryWeekend(habit.isEveryWeekend())
                .coinReward(habit.getCoinReward())
                .tags(habit.getTags())
                .completed(habit.getStatus() == Habit.HabitStatus.COMPLETED)
                .currentHabitStreak(metrics.currentHabitStreak())
                .highestHabitStreak(metrics.highestHabitStreak())
                .coinsEarned(metrics.coinsEarned())
                .completionPercentage(metrics.completionPercentage())
                .totalScheduledDays(metrics.totalScheduledDays())
                .totalCompletedDays(metrics.totalCompletedDays())
                .milestoneBadges(limitedBadges)
                .build();
    }

    private HabitMetrics calculateHabitMetrics(Habit habit) {
        // Use HabitCompletion records for accurate metrics
        List<HabitCompletion> completions = habitCompletionRepository.findByHabit(habit);

        int totalScheduledDays = completions.size();
        int totalCompletedDays = (int) completions.stream().filter(HabitCompletion::getCompleted).count();
        int coinsEarned = completions.stream().mapToInt(HabitCompletion::getCoinsEarned).sum();
        double completionPercentage = totalScheduledDays > 0 ?
                (double) totalCompletedDays / totalScheduledDays * 100 : 0.0;

        // Calculate streak based on consecutive completed days
        int currentStreak = calculateCurrentStreak(completions);
        int highestStreak = calculateHighestStreak(completions);

        return new HabitMetrics(currentStreak, highestStreak, coinsEarned, completionPercentage,
                totalScheduledDays, totalCompletedDays);
    }

    private int calculateCurrentStreak(List<HabitCompletion> completions) {

        List<LocalDate> completedDates = completions.stream()
                .filter(HabitCompletion::getCompleted)
                .map(HabitCompletion::getCompletionDate)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        if (completedDates.isEmpty()) return 0;

        int streak = 1; // at least 1 day completed
        LocalDate expectedDate = completedDates.get(0); // most recent completed day

        for (int i = 1; i < completedDates.size(); i++) {
            LocalDate nextDate = completedDates.get(i);

            if (nextDate.equals(expectedDate.minusDays(1))) {
                streak++;
                expectedDate = nextDate;
            } else {
                break;
            }
        }

        return streak;
    }


    private int calculateHighestStreak(List<HabitCompletion> completions) {
        List<HabitCompletion> completed = completions.stream()
                .filter(HabitCompletion::getCompleted)
                .sorted(Comparator.comparing(HabitCompletion::getCompletionDate))
                .collect(Collectors.toList());

        if (completed.isEmpty()) return 0;

        int highestStreak = 0;
        int currentStreak = 1;
        LocalDate previousDate = completed.get(0).getCompletionDate();

        for (int i = 1; i < completed.size(); i++) {
            LocalDate currentDate = completed.get(i).getCompletionDate();
            if (previousDate.plusDays(1).equals(currentDate)) {
                currentStreak++;
            } else {
                highestStreak = Math.max(highestStreak, currentStreak);
                currentStreak = 1;
            }
            previousDate = currentDate;
        }

        return Math.max(highestStreak, currentStreak);
    }

    private List<BadgeResponseDTO> getLimitedMilestoneBadges(Long userId) {
        try {
            // Get all badges for the user - the badge service should already return the 4 specific ones
            List<BadgeResponseDTO> allBadges = badgeService.getUserBadges(userId);

            // Since badgeService.getUserBadges() already returns the 4 specific badges,
            // we just need to ensure we don't exceed 4
            return allBadges.stream()
                    .limit(4)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting limited badges for user {}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    // Get habit completion details for a specific month
    public HabitMonthlyResponse getHabitDetailsForMonth(Long habitId, int year, int month, User user) {

        Habit habit = habitRepository.findByIdAndCreatedBy(habitId, user)
                .orElseThrow(() -> new BusinessException("Habit not found for the user"));

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<HabitCompletion> completions =
                habitCompletionRepository.findByHabitIdAndCompletionDateBetween(habitId, start, end);

        List<HabitMonthlyResponse.DayStatus> list =
                completions.stream()
                        .map(c -> new HabitMonthlyResponse.DayStatus(
                                c.getCompletionDate(),
                                c.getStatus()
                        ))
                        .toList();

        return new HabitMonthlyResponse(habit.getId(), habit.getTitle(), list);
    }

    // Helper record for metrics
    private record HabitMetrics(
            int currentHabitStreak,
            int highestHabitStreak,
            int coinsEarned,
            double completionPercentage,
            int totalScheduledDays,
            int totalCompletedDays
    ) {}

    // UPDATED: searchHabits method to return HabitResponse with metrics
    public HabitSearchResponse searchHabits(
            User user,
            String title,
            String description,
            Habit.HabitStatus status,
            Habit.Priority priority,
            Habit.Routine routine,
            Boolean everyday,
            Boolean everyWeekend,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate createdAt,
            LocalDate updatedAt,
            Integer coinReward,
            Set<String> tags,
            Set<DayOfWeek> daysOfWeek,
            String sortBy,
            String sortDir) {

        List<Habit> allUserHabits = habitRepository.findByCreatedBy(user);

        // Apply filters manually
        List<Habit> filteredHabits = allUserHabits.stream()
                .filter(habit -> {
                    // Title filter
                    if (title != null && !title.trim().isEmpty()) {
                        if (!habit.getTitle().toLowerCase().contains(title.toLowerCase())) {
                            return false;
                        }
                    }

                    // Description filter
                    if (description != null && !description.trim().isEmpty()) {
                        if (habit.getDescription() == null ||
                                !habit.getDescription().toLowerCase().contains(description.toLowerCase())) {
                            return false;
                        }
                    }

                    // Status filter
                    if (status != null && !status.equals(habit.getStatus())) {
                        return false;
                    }

                    // Priority filter
                    if (priority != null && !priority.equals(habit.getPriority())) {
                        return false;
                    }

                    // Routine filter
                    if (routine != null && !routine.equals(habit.getRoutine())) {
                        return false;
                    }

                    // Everyday filter
                    if (everyday != null && everyday != habit.isEveryday()) {
                        return false;
                    }

                    // Every weekend filter
                    if (everyWeekend != null && everyWeekend != habit.isEveryWeekend()) {
                        return false;
                    }

                    // Start date filter
                    if (startDate != null && habit.getStartDate() != null &&
                            habit.getStartDate().isBefore(startDate)) {
                        return false;
                    }

                    // End date filter
                    if (endDate != null && habit.getEndDate() != null &&
                            habit.getEndDate().isAfter(endDate)) {
                        return false;
                    }

                    // Created at filter
                    if (createdAt != null && habit.getCreatedAt() != null &&
                            habit.getCreatedAt().isBefore(createdAt)) {
                        return false;
                    }

                    // Updated at filter
                    if (updatedAt != null && habit.getUpdatedAt() != null &&
                            habit.getUpdatedAt().isAfter(updatedAt)) {
                        return false;
                    }

                    // Coin reward filter
                    if (coinReward != null && !coinReward.equals(habit.getCoinReward())) {
                        return false;
                    }

                    // Tags filter
                    if (tags != null && !tags.isEmpty()) {
                        if (habit.getTags() == null || habit.getTags().isEmpty()) {
                            return false;
                        }
                        boolean hasAnyTag = false;
                        for (String tag : tags) {
                            if (habit.getTags().contains(tag)) {
                                hasAnyTag = true;
                                break;
                            }
                        }
                        if (!hasAnyTag) {
                            return false;
                        }
                    }

                    // Days of week filter
                    if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
                        if (habit.getDaysOfWeek() == null || habit.getDaysOfWeek().isEmpty()) {
                            return false;
                        }
                        boolean hasAnyDay = false;
                        for (DayOfWeek day : daysOfWeek) {
                            if (habit.getDaysOfWeek().contains(day)) {
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

        // Apply sorting manually
        if (sortBy != null && !sortBy.isBlank()) {
            boolean ascending = !"desc".equalsIgnoreCase(sortDir);

            switch (sortBy.toLowerCase()) {
                case "title":
                    filteredHabits.sort((h1, h2) -> {
                        int result = h1.getTitle().compareToIgnoreCase(h2.getTitle());
                        return ascending ? result : -result;
                    });
                    break;
                case "priority":
                    filteredHabits.sort((h1, h2) -> {
                        if (h1.getPriority() == null && h2.getPriority() == null)
                            return 0;
                        if (h1.getPriority() == null)
                            return ascending ? 1 : -1;
                        if (h2.getPriority() == null)
                            return ascending ? -1 : 1;
                        int result = h1.getPriority().compareTo(h2.getPriority());
                        return ascending ? result : -result;
                    });
                    break;
                case "createdat":
                    filteredHabits.sort((h1, h2) -> {
                        if (h1.getCreatedAt() == null && h2.getCreatedAt() == null)
                            return 0;
                        if (h1.getCreatedAt() == null)
                            return ascending ? 1 : -1;
                        if (h2.getCreatedAt() == null)
                            return ascending ? -1 : 1;
                        int result = h1.getCreatedAt().compareTo(h2.getCreatedAt());
                        return ascending ? result : -result;
                    });
                    break;
                case "coinreward":
                    filteredHabits.sort((h1, h2) -> {
                        int result = Integer.compare(h1.getCoinReward(), h2.getCoinReward());
                        return ascending ? result : -result;
                    });
                    break;
                default: // "id"
                    filteredHabits.sort((h1, h2) -> {
                        int result = Long.compare(h1.getId(), h2.getId());
                        return ascending ? result : -result;
                    });
                    break;
            }
        }

        // Convert filtered habits to HabitResponse with metrics
        List<HabitResponse> habitResponses = filteredHabits.stream()
                .map(this::convertToHabitResponseWithMetrics)
                .collect(Collectors.toList());

        // Compute totals
        long totalHabits = filteredHabits.size();
        long totalCompleted = filteredHabits.stream()
                .filter(h -> h.getStatus() == Habit.HabitStatus.COMPLETED)
                .count();

        // Return HabitSearchResponse with HabitResponse objects containing streaks and coins
        return new HabitSearchResponse(habitResponses, totalCompleted, totalHabits);
    }

    // Helper methods
    private boolean isDateScheduledForHabit(Habit habit, LocalDate date) {
        if (habit.isEveryday()) {
            return true;
        }

        if (habit.isEveryWeekend()) {
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        }

        if (habit.getDaysOfWeek() != null && !habit.getDaysOfWeek().isEmpty()) {
            return habit.getDaysOfWeek().contains(date.getDayOfWeek());
        }

        // If no specific schedule, check if date is within start/end range
        return !date.isBefore(habit.getStartDate()) &&
                (habit.getEndDate() == null || !date.isAfter(habit.getEndDate()));
    }

    private boolean isConsecutiveScheduledCompletion(Habit habit, LocalDate previousDate, LocalDate currentDate) {
        // Check if currentDate is the next scheduled day after previousDate
        LocalDate nextScheduled = findNextScheduledDate(habit, previousDate);
        return nextScheduled != null && nextScheduled.equals(currentDate);
    }

    private LocalDate findNextScheduledDate(Habit habit, LocalDate fromDate) {
        LocalDate checkDate = fromDate.plusDays(1);
        LocalDate endDate = habit.getEndDate() != null ? habit.getEndDate() : LocalDate.now().plusYears(1);

        while (!checkDate.isAfter(endDate)) {
            if (isDateScheduledForHabit(habit, checkDate)) {
                return checkDate;
            }
            checkDate = checkDate.plusDays(1);
        }

        return null;
    }

    // Existing methods remain the same...
    public List<Habit> getTodayHabitsByStatus(User user, Habit.HabitStatus status) {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        return habitRepository.findByCreatedByAndStatusAndDaysOfWeekContaining(user, status, today);
    }

    public List<Habit> getTodayHabitsByPriority(User user, Habit.Priority priority) {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        return habitRepository.findByCreatedByAndPriorityAndDaysOfWeekContaining(user, priority, today);
    }

    public List<Habit> getTodayHabitsByRoutine(User user, Habit.Routine routine) {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        return habitRepository.findByCreatedByAndRoutineAndDaysOfWeekContaining(user, routine, today);
    }

    // Method for CalendarEventService integration
    public List<HabitResponse> getHabitsForUserInRange(Long userId, LocalDate startDate,
                                                       LocalDate endDate) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return List.of();
        }
        User user = userOpt.get();

        List<Habit> allHabits = habitRepository.findByCreatedBy(user);

        List<Habit> filteredHabits = allHabits.stream()
                .filter(habit -> isHabitValidForDateRange(habit, startDate, endDate))
                .collect(Collectors.toList());

        return filteredHabits.stream()
                .map(this::convertToHabitResponseWithMetrics)
                .collect(Collectors.toList());
    }

    // Helper method to check if habit is valid for date range
    private boolean isHabitValidForDateRange(Habit habit, LocalDate startDate, LocalDate endDate) {
        if (habit.getStartDate().isAfter(endDate) || habit.getEndDate().isBefore(startDate)) {
            return false;
        }

        if (habit.isEveryday()) {
            return true;
        }

        if (habit.isEveryWeekend()) {
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

        if (habit.getDaysOfWeek() != null && !habit.getDaysOfWeek().isEmpty()) {
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                if (habit.getDaysOfWeek().contains(current.getDayOfWeek())) {
                    return true;
                }
                current = current.plusDays(1);
            }
            return false;
        }

        return true;
    }

    // Legacy method for backward compatibility
    private HabitResponse convertToHabitResponse(Habit habit) {
        return convertToHabitResponseWithMetrics(habit);
    }

    public Habit getHabitById(Long id) {
        return habitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Habit not found with ID: " + id));
    }

    public List<HabitResponse> getHabitsForMonth(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Habit> habits = habitRepository.findHabitsInDateRange(startDate, endDate);

        if (habits == null || habits.isEmpty()) {
            throw new BusinessException("No habits found for the selected month.");
        }

        return habits.stream()
                .map(this::convertToHabitResponseWithMetrics)
                .collect(Collectors.toList());
    }


    /**
     * Get HabitResponse by ID with metrics and 4 badges
     */
    public HabitResponseTodayDto getHabitResponseById(Long habitId, LocalDate date) {

        if (date == null) {
            date = LocalDate.now();
        }

        Habit habit = getHabitById(habitId);

        // 🔴 Date-specific validation (IMPORTANT)
        HabitCompletion completion = habitCompletionRepository
                .findByHabitIdAndCompletionDate(habitId, date)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Habit not found for the selected date"
                        )
                );

        HabitResponseTodayDto response = toHabitResponseTodayDto(completion);

        // Ensure sharedWithParent is not null
        if (response.getSharedWithParent() == null) {
            response.setSharedWithParent(false);
        }
        return response;
    }

    // Calculate coins for completion
    private int calculateCoinsForCompletion(Habit habit, HabitCompletion completion) {
        LocalDateTime scheduledEnd = LocalDateTime.of(completion.getCompletionDate(), habit.getEndTime());
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
    private int getCoinsEarnedToday(User user) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        try {
            List<CoinTransaction> todayTransactions = coinTransactionRepository.findByUserAndTimestampBetween(
                    user, startOfDay, endOfDay
            );

            return todayTransactions.stream()
                    .filter(tx -> "HABIT_COMPLETION".equals(tx.getType()) || "TASK_COMPLETION".equals(tx.getType()))
                    .mapToInt(CoinTransaction::getAmount)
                    .sum();

        } catch (Exception e) {
            log.error("Error calculating coins earned today for user {}: {}", user.getId(), e.getMessage());
            return 0;
        }
    }

    // Update overall habit status
    private void updateHabitStatus(Habit habit) {
        Habit.HabitStatus newStatus = calculateHabitStatus(habit);
        if (!habit.getStatus().equals(newStatus)) {
            habit.setStatus(newStatus);
            habit.setUpdatedAt(LocalDate.now());
            habitRepository.save(habit);
        }
    }

    // Calculate habit status based on completions AND end date
    public Habit.HabitStatus calculateHabitStatus(Habit habit) {
        LocalDate today = LocalDate.now();

        // If end date has passed, habit should be COMPLETED
        if (habit.getEndDate().isBefore(today)) {
            return Habit.HabitStatus.COMPLETED;
        }

        // For active habits, calculate based on completions
        long totalScheduled = habitCompletionRepository.countByHabit(habit);
        long totalCompleted = habitCompletionRepository.countByHabitAndCompleted(habit, true);

        if (totalCompleted == 0) {
            return Habit.HabitStatus.PENDING;
        } else if (totalCompleted < totalScheduled) {
            return Habit.HabitStatus.ONGOING;
        } else {
            return Habit.HabitStatus.COMPLETED;
        }
    }

    // Check if habit is recurring
    private boolean isRecurringHabit(Habit habit) {
        return habit.isEveryday() ||
                (habit.getDaysOfWeek() != null && !habit.getDaysOfWeek().isEmpty()) ||
                habit.getStartDate().isBefore(habit.getEndDate());
    }

    // Scheduled method to mark missed habits
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void markMissedHabits() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        List<HabitCompletion> pendingCompletions = habitCompletionRepository
                .findByCompletionDateAndStatus(yesterday, HabitCompletion.CompletionStatus.PENDING);

        for (HabitCompletion completion : pendingCompletions) {
            completion.setStatus(HabitCompletion.CompletionStatus.SKIPPED);
            habitCompletionRepository.save(completion);

            // Update overall habit status
            updateHabitStatus(completion.getHabit());
        }
    }

    // NEW: Scheduled method to update habit status when end date passes
    @Scheduled(cron = "0 0 0 * * ?") // Runs daily at midnight
    @Transactional
    public void updateHabitStatusesBasedOnEndDate() {
        LocalDate today = LocalDate.now();

        // Find all habits that are still PENDING but their end date has passed
        List<Habit> expiredHabits = habitRepository.findByStatusAndEndDateBefore(
                Habit.HabitStatus.PENDING,
                today
        );

        // Also find ONGOING habits that have ended
        List<Habit> completedHabits = habitRepository.findByStatusAndEndDateBefore(
                Habit.HabitStatus.ONGOING,
                today
        );

        List<Habit> allHabitsToUpdate = new ArrayList<>();
        allHabitsToUpdate.addAll(expiredHabits);
        allHabitsToUpdate.addAll(completedHabits);

        for (Habit habit : allHabitsToUpdate) {
            log.info("Auto-updating habit {} from {} to COMPLETED (end date: {})",
                    habit.getId(), habit.getStatus(), habit.getEndDate());

            habit.setStatus(Habit.HabitStatus.COMPLETED);
            habit.setUpdatedAt(today);

            // Also update any pending completions for this habit
            updatePendingCompletionsForCompletedHabit(habit);
        }

        habitRepository.saveAll(allHabitsToUpdate);
        log.info("Updated {} habits to COMPLETED status based on end date", allHabitsToUpdate.size());
    }

    // Helper method to update completion records when habit is auto-completed
    private void updatePendingCompletionsForCompletedHabit(Habit habit) {
        List<HabitCompletion> pendingCompletions = habitCompletionRepository
                .findByHabitAndCompleted(habit, false);

        for (HabitCompletion completion : pendingCompletions) {
            if (completion.getStatus() == HabitCompletion.CompletionStatus.PENDING) {
                completion.setStatus(HabitCompletion.CompletionStatus.SKIPPED);
            }
        }

        habitCompletionRepository.saveAll(pendingCompletions);
    }

    // Add these new methods to HabitService
    public List<HabitResponseTodayDto> getHabitByDate(
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

        List<HabitCompletion> habitList;

        if (user.getRelationship() == Relationship.PARENT && forUserId != null) {
            User child = userRepository.findById(forUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid child userId"));

            if (!child.getFamily().equals(user.getFamily()) ||
                    child.getRelationship() != Relationship.CHILD) {
                throw new BusinessException("forUserId must be a child of the parent");
            }

            habitList = habitCompletionRepository
                    .findByCompletionDateBetweenAndHabit_AssignedTo(startDate, endDate, child);

        } else if (user.getRelationship() == Relationship.PARENT) {
            List<User> children = userRepository
                    .findByFamilyAndRelationship(user.getFamily(), Relationship.CHILD);

            if (children.isEmpty()) {
                return List.of();
            }

            habitList = habitCompletionRepository.findForParentViewRange(startDate, endDate, children);

        } else {
            habitList = habitCompletionRepository
                    .findByCompletionDateBetweenAndHabit_AssignedTo(startDate, endDate, user);
        }

        if ("DAY".equals(finalViewType) && date != null && date.equals(LocalDate.now())) {
            LocalTime now = LocalTime.now();

            habitList.stream()
                    .filter(h -> h.getStatus() == HabitCompletion.CompletionStatus.PENDING)
                    .filter(h -> h.getHabit() != null && h.getHabit().getEndTime() != null)
                    .filter(h -> now.isAfter(h.getHabit().getEndTime()))
                    .forEach(h -> h.setStatus(HabitCompletion.CompletionStatus.valueOf("SKIPPED")));

            habitList.stream()
                    .filter(h -> h.getStatus() == HabitCompletion.CompletionStatus.ONGOING)
                    .filter(h -> h.getHabit() != null && h.getHabit().getEndTime() != null)
                    .filter(h -> !now.isBefore(h.getHabit().getEndTime()))
                    .forEach(h -> {
                        h.setStatus(HabitCompletion.CompletionStatus.COMPLETED);
                        completeHabit(h.getHabit().getId(), user);
                    });

            habitCompletionRepository.saveAll(habitList);
        }

        status = (status == null || status.trim().isEmpty()) ? null : status.trim().toUpperCase();
        routine = (routine == null || routine.trim().isEmpty()) ? null : routine.trim().toUpperCase();
        priority = (priority == null || priority.trim().isEmpty()) ? null : priority.trim().toUpperCase();

        String finalStatus = status;
        String finalRoutine = routine;
        String finalPriority = priority;
        return habitList.stream()
                .filter(h -> finalStatus == null || h.getStatus().name().equalsIgnoreCase(finalStatus))
                .filter(h -> finalRoutine == null || h.getHabit().getRoutine().equalsIgnoreCase(finalRoutine))
                .filter(h -> finalPriority == null || h.getHabit().getPriority().name().equalsIgnoreCase(finalPriority))
                .map(this::toHabitResponseTodayDto)
                .toList();
    }



    private HabitResponseTodayDto toHabitResponseTodayDto(HabitCompletion hc) {

        Habit habit = hc.getHabit();

        // Reuse your existing metric calculator
        HabitMetrics metrics = calculateHabitMetrics(habit);

        List<BadgeResponseDTO> limitedBadges =
                getLimitedMilestoneBadges(habit.getCreatedBy().getId());

        return HabitResponseTodayDto.builder()
                .id(habit.getId())
                .assignedTo(habit.getAssignedTo().getId())
                .createdBy(habit.getCreatedBy().getId())
                .createdAt(habit.getCreatedAt())
                .updatedAt(habit.getUpdatedAt())
                .title(habit.getTitle())
                .description(habit.getDescription())
                .status(hc.getStatus().name())
                .completionDate(hc.getCompletionDate())
                .completionTime(hc.getCompletionTime())
                .priority(habit.getPriority().name())
                .routine(habit.getRoutine())
                .coinReward(habit.getCoinReward())
                .sharedWithParent(habit.getSharedWithParent())
                .isEveryday(habit.isEveryday())
                .isEveryWeekend(habit.isEveryWeekend())
                .tags(habit.getTags())
                .daysOfWeek(habit.getDaysOfWeek())
                .startDate(habit.getStartDate())
                .startTime(habit.getStartTime())
                .endDate(habit.getEndDate())
                .endTime(habit.getEndTime())

                // METRICS
                .currentHabitStreak(metrics.currentHabitStreak())
                .highestHabitStreak(metrics.highestHabitStreak())
                .coinsEarned(metrics.coinsEarned())
                .completionPercentage(metrics.completionPercentage())
                .totalScheduledDays(metrics.totalScheduledDays())
                .totalCompletedDays(metrics.totalCompletedDays())
                .milestoneBadges(limitedBadges)
                .build();
    }



    public ResponseEntity<Map<String, Object>> startHabitForToday(User user, Long habitId) {
        final LocalDate today = LocalDate.now();

        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new BusinessException("Habit not found"));
        System.out.println(today+" --today");
        HabitCompletion habitCompletion = habitCompletionRepository
                .findByHabitAndCompletionDate(habit, today)
                .orElseThrow(() -> new BusinessException("No scheduled habit for today"));

        // ⭐ Check if already ongoing
        if (habitCompletion.getStatus() == HabitCompletion.CompletionStatus.ONGOING) {

            Map<String, Object> response = Map.of(
                    "message", "Habit already ongoing for today",
                    "habitId", habitId,
                    "completionDate", habitCompletion.getCompletionDate(),
                    "status", habitCompletion.getStatus().name()
            );

            return ResponseEntity.ok(response);
        }

        // ⭐ Otherwise set to ongoing
        habitCompletion.setStatus(HabitCompletion.CompletionStatus.ONGOING);
        habitCompletionRepository.save(habitCompletion);

        Map<String, Object> response = Map.of(
                "message", "Habit started successfully for today!",
                "habitId", habitId,
                "completionDate", habitCompletion.getCompletionDate(),
                "status", habitCompletion.getStatus().name(),
                "startedAt", LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<Map<String, Object>> abortHabitForToday(User user, Long habitId) {
        final LocalDate today = LocalDate.now();

        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new BusinessException("Habit not found"));

        HabitCompletion habitCompletion = habitCompletionRepository
                .findByHabitAndCompletionDate(habit, today)
                .orElseThrow(() -> new BusinessException("No scheduled habit for today"));

        // ⭐ Only change status if currently ONGOING
        if (habitCompletion.getStatus() == HabitCompletion.CompletionStatus.ONGOING) {

            habitCompletion.setStatus(HabitCompletion.CompletionStatus.PENDING);
            habitCompletionRepository.save(habitCompletion);

            Map<String, Object> response = Map.of(
                    "message", "Habit aborted successfully. Status set to pending.",
                    "habitId", habitId,
                    "status", HabitCompletion.CompletionStatus.PENDING.name(),
                    "abortedAt", LocalDateTime.now()
            );

            return ResponseEntity.ok(response);
        }

        // ⭐ If not ongoing → no status change
        Map<String, Object> response = Map.of(
                "message", "Habit is not ongoing, so nothing to abort.",
                "habitId", habitId,
                "currentStatus", habitCompletion.getStatus().name()
        );

        return ResponseEntity.ok(response);
    }




    public List<HabitCompletion> getTodayHabit(User user) {
        LocalDate today = LocalDate.now();
        List<Habit> userHabits = getAllHabitsForUser(user);

        return userHabits.stream()
                .map(habit -> habitCompletionRepository.findByHabitAndCompletionDate(habit, today))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public List<HabitCompletion> getHabitCompletionHistory(Long habitId) {
        Optional<Habit> habitOpt = habitRepository.findById(habitId);
        if (habitOpt.isEmpty()) {
            return List.of();
        }
        return habitCompletionRepository.findByHabit(habitOpt.get());
    }
}
