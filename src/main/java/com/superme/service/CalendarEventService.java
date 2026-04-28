package com.superme.service;

import com.superme.dto.*;
import com.superme.enums.Relationship;
import com.superme.exception.BusinessException;
import com.superme.exception.ResourceNotFoundException;
import com.superme.model.CalendarEvent;
import com.superme.model.Family;
import com.superme.model.User;
import com.superme.model.Task;
import com.superme.model.Habit;
import com.superme.repository.CalendarEventRepository;
import com.superme.repository.TaskRepository;
import com.superme.repository.HabitRepository;
import com.superme.repository.UserRepository;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Updated to support storing and retrieving user's daily mood/vibe as a
 * calendar event.
 */
@Service
public class CalendarEventService {

    @Autowired
    private CalendarEventRepository calendarEventRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private HabitRepository habitRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HabitService habitService;

    @Autowired
    private TaskService taskService;

    /**
     * Add a new calendar event.
     */
    public CalendarEvent addEvent(CalendarEvent event, User user) {
        event.setCreatedBy(user);
        event.setFamily(user.getFamily());
        validateEventDates(event.getStartDate(), event.getEndDate());
        checkForDuplicateOrOverlappingEvent(event, user); // ✅ NEW
        // Support tags, daysOfWeek, isEveryday, isEveryWeekend, priority (already set
        // on event if present)
        // createdAt is set by @PrePersist
        return calendarEventRepository.save(event);
    }

    private void checkForDuplicateOrOverlappingEvent(
            CalendarEvent event, User user) {

        Long currentEventId = event.getId(); // null for create

        Optional<CalendarEvent> overlappingEvent =
                calendarEventRepository.findByFamily(user.getFamily()).stream()
                        .filter(e ->
                                // 1️⃣ Ignore self (update case)
                                (currentEventId == null || !e.getId().equals(currentEventId)) &&

                                        // 2️⃣ Date overlap
                                        !e.getEndDate().isBefore(event.getStartDate()) &&
                                        !e.getStartDate().isAfter(event.getEndDate()) &&

                                        // 3️⃣ Time overlap
                                        e.getStartTime().compareTo(event.getEndTime()) < 0 &&
                                        e.getEndTime().compareTo(event.getStartTime()) > 0 &&

                                        // 4️⃣ Day-of-week overlap
                                        hasCommonDay(e.getDaysOfWeek(), event.getDaysOfWeek())
                        )
                        .findFirst();

        if (overlappingEvent.isPresent()) {
            throw new BusinessException(
                    "Time slot overlaps with '" +
                            overlappingEvent.get().getTitle() +
                            "'. Please choose a different time."
            );
        }
    }

    private boolean hasCommonDay(Set<DayOfWeek> d1, Set<DayOfWeek> d2) {

        return d1 != null && d2 != null &&
                d1.stream().anyMatch(d2::contains);
    }



    /**
     * Update an existing calendar event.
     */
    public Optional<CalendarEvent> updateEvent(Long eventId, CalendarEvent updatedEvent, User user) {
        Optional<CalendarEvent> existingEvent = calendarEventRepository.findById(eventId);
        if (existingEvent.isPresent() && existingEvent.get().getFamily().equals(user.getFamily())) {
            CalendarEvent event = existingEvent.get();
            event.setTitle(updatedEvent.getTitle());
            event.setDescription(updatedEvent.getDescription());
            validateEventDates(updatedEvent.getStartDate(), updatedEvent.getEndDate());
            event.setStartDate(updatedEvent.getStartDate());
            event.setStartTime(updatedEvent.getStartTime());
            event.setEndDate(updatedEvent.getEndDate());
            event.setEndTime(updatedEvent.getEndTime());
            event.setTags(updatedEvent.getTags());
            event.setDaysOfWeek(updatedEvent.getDaysOfWeek());
            event.setEveryday(updatedEvent.isEveryday());
            event.setEveryWeekend(updatedEvent.isEveryWeekend());
            event.setPriority(updatedEvent.getPriority());
            // updatedAt is set by @PreUpdate
            return Optional.of(calendarEventRepository.save(event));
        }
        return Optional.empty();
    }

    /**
     * Get all events by priority.
     */
    public List<CalendarEvent> getEventsByPriority(CalendarEvent.Priority priority) {
        return calendarEventRepository.findByPriority(priority);
    }

    /**
     * Get all events for a user by priority.
     */
    public List<CalendarEvent> getEventsByUserAndPriority(Long userId, CalendarEvent.Priority priority) {
        // ✅ FIXED: Only get manual events by priority
        List<CalendarEvent> manualEvents = calendarEventRepository.findByCreatedByIdAndType(userId,
                CalendarEvent.TYPE_MANUAL_EVENT);
        return manualEvents.stream()
                .filter(event -> event.getPriority() == priority)
                .collect(Collectors.toList());
    }

    /**
     * Get all events that repeat on a specific day of week.
     */
    public List<CalendarEvent> getEventsByDayOfWeek(java.time.DayOfWeek dayOfWeek) {
        return calendarEventRepository.findByDaysOfWeekContaining(dayOfWeek);
    }

    /**
     * Get all events that repeat every day.
     */
    public List<CalendarEvent> getEverydayEvents() {
        return calendarEventRepository.findByIsEverydayTrue();
    }

    /**
     * Get all events that repeat every weekend.
     */
    public List<CalendarEvent> getWeekendEvents() {
        return calendarEventRepository.findByIsEveryWeekendTrue();
    }

    /**
     * Get all events with a specific tag.
     */
    public List<CalendarEvent> getEventsByTag(String tag) {
        return calendarEventRepository.findByTagsContaining(tag);
    }

    /**
     * Delete a calendar event.
     */
    public boolean deleteEvent(Long eventId, User user) {
        Optional<CalendarEvent> event = calendarEventRepository.findById(eventId);
        if (event.isPresent() && event.get().getFamily().equals(user.getFamily())) {
            calendarEventRepository.delete(event.get());
            return true;
        }
        return false;
    }

    /**
     * Get all events for a family.
     */
    public List<CalendarEvent> getEventsForFamily(Family family) {
        return calendarEventRepository.findByFamily(family);
    }

    /**
     * Get all events for a family within a time range.
     */
    public List<CalendarEvent> getEventsForFamilyInRange(Family family, LocalDate startDate, LocalDate endDate) {
        return calendarEventRepository.findByFamilyAndStartDateBetween(family, startDate, endDate);
    }

    /**
     * Get all events created by a specific userId.
     */
    public List<CalendarEvent> getEventsForUser(Long userId) {
        // ✅ FIXED: Only return manual events
        return calendarEventRepository.findByCreatedByIdAndType(userId, CalendarEvent.TYPE_MANUAL_EVENT);
    }

    /**
     * Get all events created by a specific userId within a time range.
     */
    public List<CalendarEvent> getEventsForUserInRange(Long userId, LocalDate startDate, LocalDate endDate) {
        // ✅ FIXED: Only return manual events in range
        return calendarEventRepository.findByCreatedByIdAndTypeInDateRange(userId, CalendarEvent.TYPE_MANUAL_EVENT,
                startDate, endDate);
    }

    /**
     * Get all tasks and habits scheduled for today's date for a specific userId.
     */
    public List<CalendarEvent> getEventsForToday(Long userId) {
        LocalDate today = LocalDate.now();
        List<CalendarEvent> eventsForToday = calendarEventRepository.findByCreatedByIdAndStartDateBetween(userId, today,
                today);
        return eventsForToday.stream()
                .filter(this::isTaskOrHabit)
                .toList();
    }

    /**
     * Get all completed tasks and habits for today for a specific userId.
     */
    public List<CalendarEvent> getCompletedTasksAndHabitsForToday(Long userId) {
        LocalDate today = LocalDate.now();
        List<CalendarEvent> events = calendarEventRepository
                .findByCreatedByIdAndTypeInAndStartDateBetweenOrderByStartDateAsc(
                        userId, List.of("TASK", "HABIT"), today, today);
        return events.stream()
                .filter(CalendarEvent::isCompleted)
                .toList();
    }

    /**
     * Get all incomplete tasks and habits for today for a specific userId.
     */
    public List<CalendarEvent> getIncompleteTasksAndHabitsForToday(Long userId) {
        LocalDate today = LocalDate.now();
        List<CalendarEvent> events = calendarEventRepository
                .findByCreatedByIdAndTypeInAndStartDateBetweenOrderByStartDateAsc(
                        userId, List.of("TASK", "HABIT"), today, today);
        return events.stream()
                .filter(event -> !event.isCompleted())
                .toList();
    }

    /**
     * Get events for a user by view type (day, week, month) using userId.
     */
    public List<CalendarEvent> getEventsForUserByView(Long userId, LocalDate date, String viewType) {
        LocalDate start, end;
        if ("month".equalsIgnoreCase(viewType)) {
            start = date.withDayOfMonth(1);
            end = date.withDayOfMonth(date.lengthOfMonth());
        } else if ("week".equalsIgnoreCase(viewType)) {
            start = date.with(java.time.DayOfWeek.MONDAY);
            end = start.plusDays(6);
        } else { // default to "day"
            start = date;
            end = date;
        }
        return calendarEventRepository.findByCreatedByIdAndStartDateBetween(userId, start, end);
    }

    /**
     * Get full calendar data for a user within a time range using userId.
     */

    public CalendarFullResponse getFullCalendarForUser(
            User user,
            Long forUserId,
            String viewType,
            LocalDate startDate,
            LocalDate endDate) {

        List<Long> targetUserIds;

        if (user.getRelationship() == Relationship.PARENT && forUserId != null) {
            // ✅ parent asked for a specific child

            User child = userRepository.findById(forUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid child userId"));

            if (!child.getFamily().equals(user.getFamily())
                    || child.getRelationship() != Relationship.CHILD) {
                throw new BusinessException("forUserId must be a child of the parent");
            }

            // only that child (and optionally parent if you want)
            targetUserIds = List.of(child.getId(), user.getId());

        } else if (user.getRelationship() == Relationship.PARENT) {
            // ✅ parent with no forUserId → all children + parent

            List<User> children = userRepository
                    .findByFamilyAndRelationship(user.getFamily(), Relationship.CHILD);

            if (children.isEmpty()) {
                targetUserIds = List.of(user.getId());
            } else {
                targetUserIds = children.stream()
                        .map(User::getId)
                        .collect(Collectors.toList());
                targetUserIds.add(user.getId());
            }

        } else {
            // ✅ child/self → only own data
            targetUserIds = List.of(user.getId());
        }

        // manual events for all target users
        List<CalendarEvent> manualEvents = calendarEventRepository
                .findByCreatedByIdInAndTypeInDateRange(
                        targetUserIds, CalendarEvent.TYPE_MANUAL_EVENT, startDate, endDate);

        // habits/tasks: pass same forUserId logic down
        List<HabitResponseTodayDto> habitResponses =
                habitService.getHabitByDate(user, forUserId, startDate,viewType, null, null, null);

        List<TaskResponseTodayDto> taskResponses =
                taskService.getTaskByDate(user, forUserId, startDate, viewType, null, null, null);

        List<TaskDTO> taskDTOs = taskResponses.stream()
                .map(this::convertToTaskDTO)
                .collect(Collectors.toList());

        List<HabitDTO> habitDTOs = habitResponses.stream()
                .map(this::convertToHabitDTO)
                .collect(Collectors.toList());

        List<CalendarEventResponseDTO> eventResponses =
                manualEvents.stream()
                        .flatMap(event -> expandEvent(event,viewType,startDate).stream())
                        .collect(Collectors.toList());

        return new CalendarFullResponse(eventResponses, taskDTOs, habitDTOs);
    }

    private CalendarEventResponseDTO toResponse(
            CalendarEvent event,
            LocalDate calendarDate
    ) {

        return CalendarEventResponseDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .tags(event.getTags())
                .everyday(event.isEveryday())
                .everyWeekend(event.isEveryWeekend())
                .daysOfWeek(event.getDaysOfWeek())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .completionDate(calendarDate) // ✅ derived
                .priority(event.getPriority())
                .status(resolveStatus(event, calendarDate))
                .createdAt(
                        event.getCreatedAt() != null
                                ? event.getCreatedAt().atStartOfDay()
                                : null
                )
                .build();
    }

    private String resolveStatus(CalendarEvent event, LocalDate date) {

        LocalDate today = LocalDate.now();

        if (date.isBefore(today)) return "COMPLETED";
        if (date.isEqual(today)) return "TODAY";
        return "UPCOMING";
    }


    private List<CalendarEventResponseDTO> expandEvent(
            CalendarEvent event,
            String viewType,
            LocalDate targetDate) {

        List<CalendarEventResponseDTO> list = new ArrayList<>();

        // ✅ DAY / DATE view → only ONE occurrence
        if ("DAY".equalsIgnoreCase(viewType) || "DATE".equalsIgnoreCase(viewType)) {

            if (!targetDate.isBefore(event.getStartDate())
                    && !targetDate.isAfter(event.getEndDate())
                    && isValidForDay(event, targetDate)) {

                list.add(toResponse(event, targetDate));
            }

            return list; // 🚨 critical: no expansion
        }

        // ✅ WEEK / MONTH view
        LocalDate date = event.getStartDate();

        while (!date.isAfter(event.getEndDate())) {
            if (isValidForDay(event, date)) {
                list.add(toResponse(event, date));
            }
            date = date.plusDays(1);
        }

        return list;
    }




    private boolean isValidForDay(
            CalendarEvent event,
            LocalDate date
    ) {
        if (event.isEveryday()) return true;

        if (event.isEveryWeekend()) {
            DayOfWeek d = date.getDayOfWeek();
            return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
        }

        if (event.getDaysOfWeek() != null && !event.getDaysOfWeek().isEmpty()) {
            return event.getDaysOfWeek().contains(date.getDayOfWeek());
        }

        return true; // default allow
    }




    private HabitDTO convertToHabitDTO(HabitResponseTodayDto habit) {
        if (habit == null) return null;

        return HabitDTO.builder()
                .id(habit.getId())
                .title(habit.getTitle())
                .description(habit.getDescription())
                .completionDate(habit.getCompletionDate())
                .priority(Habit.Priority.valueOf(habit.getPriority()))  // This is Task.Priority enum
                .routine(habit.getRoutine())    // This is Task.Routine enum
                .startDate(habit.getStartDate())
                .startTime(habit.getStartTime())
                .endDate(habit.getEndDate())
                .endTime(habit.getEndTime())
                .daysOfWeek(habit.getDaysOfWeek())
                .createdAt(habit.getCreatedAt())
                .updatedAt(habit.getUpdatedAt())
                .status(habit.getStatus())      // This is Task.TaskStatus enum
                .rewardCoins(habit.getCoinReward())
                .sharedWithParents(habit.getSharedWithParent())
                .isEveryday(habit.isEveryday())
                .isEveryWeekend(habit.isEveryWeekend())
                .tags(habit.getTags())
                .createdBy(habit.getCreatedBy())
                .assignedTo(habit.getAssignedTo())
                .currentHabitStreak(habit.getCurrentHabitStreak())
                .highestHabitStreak(habit.getHighestHabitStreak())
                .build();
    }


    private TaskDTO convertToTaskDTO(TaskResponseTodayDto task) {
        if (task == null) return null;

        return TaskDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .completionDate(task.getCompletionDate())
                .priority(Task.Priority.valueOf(task.getPriority()))  // This is Task.Priority enum
                .routine(task.getRoutine())    // This is Task.Routine enum
                .startDate(task.getStartDate())
                .startTime(task.getStartTime())
                .endDate(task.getEndDate())
                .endTime(task.getEndTime())
                .daysOfWeek(task.getDaysOfWeek())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .status(task.getStatus())      // This is Task.TaskStatus enum
                .rewardCoins(task.getCoinReward())
                .sharedWithParent(task.getSharedWithParent())
                .isEveryday(task.isEveryday())
                .isEveryWeekend(task.isEveryWeekend())
                .tags(task.getTags())
                .createdBy(task.getCreatedBy())
                .assignedTo(task.getAssignedTo())
                .currentTaskStreak(task.getCurrentTaskStreak())
                .highestTaskStreak(task.getHighestTaskStreak())
                .build();
    }
    // ✅ ADD: Helper method to check if task is valid for date range
    private boolean isTaskValidForDateRange(Task task, LocalDate startDate, LocalDate endDate) {
        // Check if task's date range overlaps with query range
        if (task.getStartDate().isAfter(endDate) || task.getEndDate().isBefore(startDate)) {
            return false;
        }

        // If task is everyday, it's valid
        if (task.isEveryday()) {
            return true;
        }

        // If task is every weekend, check if range includes weekend
        if (task.isEveryWeekend()) {
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                if (current.getDayOfWeek() == java.time.DayOfWeek.SATURDAY ||
                        current.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
                    return true;
                }
                current = current.plusDays(1);
            }
            return false;
        }

        // If task has specific days of week
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

        // Default: task is valid if it's in the date range
        return true;
    }

    // ✅ ADD: Helper method to check if habit is valid for date range
    private boolean isHabitValidForDateRange(Habit habit, LocalDate startDate, LocalDate endDate) {
        // Check if habit's date range overlaps with query range
        if (habit.getStartDate().isAfter(endDate) || habit.getEndDate().isBefore(startDate)) {
            return false;
        }

        // If habit is everyday, it's valid
        if (habit.isEveryday()) {
            return true;
        }

        // If habit is every weekend, check if range includes weekend
        if (habit.isEveryWeekend()) {
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                if (current.getDayOfWeek() == java.time.DayOfWeek.SATURDAY ||
                        current.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
                    return true;
                }
                current = current.plusDays(1);
            }
            return false;
        }

        // If habit has specific days of week
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

        // Default: habit is valid if it's in the date range
        return true;
    }

    /**
     * Helper method to check if an event is a task or habit.
     */
    private boolean isTaskOrHabit(CalendarEvent event) {
        return event.getType() != null &&
                (event.getType().equalsIgnoreCase("TASK") || event.getType().equalsIgnoreCase("HABIT"));
    }

    /**
     * Validate that the start date is before the end date (epoch millis).
     */
    private void validateEventDates(long startDate, long endDate) {
        // Deprecated: Use LocalDate version
    }

    private void validateEventDates(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
    }

    /**
     * Get all events for a family by family ID.
     */
    public List<CalendarEvent> getEventsForFamilyByFamilyId(Long familyId) {
        return calendarEventRepository.findByFamilyId(familyId);
    }

    /**
     * Get all events for a family within a time range by family ID.
     */
    public List<CalendarEvent> getEventsForFamilyInRangeByFamilyId(Long familyId, LocalDate startDate,
            LocalDate endDate) {
        return calendarEventRepository.findByFamilyIdAndStartDateBetween(familyId, startDate, endDate);
    }

    // --- MOOD/VIBE SUPPORT ---

    /**
     * Set the user's vibe for a specific date as a calendar event.
     * If a mood event already exists for that day, update it.
     * Otherwise, create a new event of type "MOOD".
     */
    public void setUserVibeForDate(User user, LocalDate date, String vibe) {
        // Try to find an existing MOOD event for this user and date
        List<CalendarEvent> moodEvents = calendarEventRepository.findByCreatedByIdAndTypeAndStartDateBetween(
                user.getId(), "MOOD", date, date);

        CalendarEvent moodEvent;
        if (!moodEvents.isEmpty()) {
            // Update existing event
            moodEvent = moodEvents.get(0);
            moodEvent.setTitle("Mood");
            moodEvent.setDescription(vibe);
            moodEvent.setStartDate(date);
            moodEvent.setEndDate(date);
            moodEvent.setType("MOOD");
            // updatedAt is set by @PreUpdate
        } else {
            // Create new event
            moodEvent = new CalendarEvent();
            moodEvent.setCreatedBy(user);
            moodEvent.setFamily(user.getFamily());
            moodEvent.setTitle("Mood");
            moodEvent.setDescription(vibe);
            moodEvent.setStartDate(date);
            moodEvent.setEndDate(date);
            moodEvent.setType("MOOD");
            // createdAt is set by @PrePersist
        }
        calendarEventRepository.save(moodEvent);
    }

    /**
     * Get the user's vibe for a specific date from calendar events.
     */
    public String getUserVibeForDate(User user, LocalDate date) {
        List<CalendarEvent> moodEvents = calendarEventRepository.findByCreatedByIdAndTypeAndStartDateBetween(
                user.getId(), "MOOD", date, date);
        if (!moodEvents.isEmpty()) {
            return moodEvents.get(0).getDescription();
        }
        return null;
    }
}