package com.superme.controller;

import com.superme.dto.*;
// import removed: HabitResponse
import com.superme.enums.Relationship;
import com.superme.exception.BusinessException;
import com.superme.exception.ResourceNotFoundException;
import com.superme.exception.UnauthorizedActionException;
import com.superme.model.*;

import java.time.DayOfWeek;

import com.superme.service.HabitService;
import com.superme.service.UserService;
import com.superme.service.FamilyMemberService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/habits")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE, RequestMethod.OPTIONS })
public class HabitController {

    @Autowired
    private HabitService habitService;

    @Autowired
    private UserService userService;

    @Autowired
    private FamilyMemberService familyMemberService;

    /**
     * GET /habits/sort/status
     * Returns today's habits grouped and sorted by HabitStatus (Pending, Ongoing,
     * Completed).
     */
    @GetMapping("/sort/status")
    public ResponseEntity<List<HabitRequest>> getHabitsSortedByStatus(Principal principal) {
        try {
            User user = getUserFromPrincipal(principal);
            LocalDate today = LocalDate.now(ZoneId.systemDefault());
            List<Habit> habits = habitService.getHabitsForUserByDay(user, today);
            habits = habits.stream()
                    .sorted((h1, h2) -> h1.getStatus().compareTo(h2.getStatus()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(habits.stream().map(this::mapToHabitRequest).collect(Collectors.toList()));
        } catch (BusinessException e) {
            throw new BusinessException(e.getMessage());
        }
       }

    /**
     * GET /habits/sort/priority
     * Returns today's habits grouped and sorted by Priority (High, Medium, Low).
     */
    @GetMapping("/sort/priority")
    public ResponseEntity<List<HabitRequest>> getHabitsSortedByPriority(Principal principal) {
        try {
            User user = getUserFromPrincipal(principal);
            LocalDate today = LocalDate.now(ZoneId.systemDefault());
            List<Habit> habits = habitService.getHabitsForUserByDay(user, today);
            habits = habits.stream()
                    .sorted((h1, h2) -> h1.getPriority().compareTo(h2.getPriority()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(habits.stream().map(this::mapToHabitRequest).collect(Collectors.toList()));
        } catch (BusinessException e) {
            throw new BusinessException(e.getMessage());
        }
     }

    /**
     * GET /habits/sort/routine
     * Returns today's habits grouped and sorted by Routine (Morning, Evening,
     * Night).
     */
    @GetMapping("/sort/routine")
    public ResponseEntity<List<HabitRequest>> getHabitsSortedByRoutine(Principal principal) {
        try {
            User user = getUserFromPrincipal(principal);
            LocalDate today = LocalDate.now(ZoneId.systemDefault());
            List<Habit> habits = habitService.getHabitsForUserByDay(user, today);
            habits = habits.stream()
                    .sorted((h1, h2) -> h1.getRoutine().compareTo(h2.getRoutine()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(habits.stream().map(this::mapToHabitRequest).collect(Collectors.toList()));
        } catch (BusinessException e) {
            throw new BusinessException(e.getMessage());
        }
      }

    /**
     * GET /habits/sorted?filterBy=status|priority|routine&direction=asc|desc
     * Returns all habits for the user sorted by the specified filter.
     */
    @GetMapping("/sorted")
    public ResponseEntity<List<HabitRequest>> getSortedHabitsForUser(
            Principal principal,
            @RequestParam(name = "filterBy", required = false, defaultValue = "priority") String filterBy,
            @RequestParam(name = "direction", required = false, defaultValue = "asc") String direction) {
        try {
            User user = getUserFromPrincipal(principal);
            List<Habit> habits = habitService.getHabitsForUser(user);
            // Sort habits based on filterBy and direction
            habits = habits.stream()
                    .sorted((h1, h2) -> {
                        int cmp = 0;
                        switch (filterBy.toLowerCase()) {
                            case "status":
                                cmp = h1.getStatus().compareTo(h2.getStatus());
                                break;
                            case "priority":
                                cmp = h1.getPriority().compareTo(h2.getPriority());
                                break;
                            case "routine":
                                cmp = h1.getRoutine().compareTo(h2.getRoutine());
                                break;
                            default:
                                cmp = h1.getPriority().compareTo(h2.getPriority());
                        }
                        return "desc".equalsIgnoreCase(direction) ? -cmp : cmp;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(habits.stream().map(this::mapToHabitRequest).collect(Collectors.toList()));
        } catch (BusinessException e) {
            throw new BusinessException(e.getMessage());
        }
      }

    @PostMapping("/add")
    public ResponseEntity<HabitResponse> addHabit(Principal principal,
                                                  @RequestBody HabitRequest request) {
        User user = getUserFromPrincipal(principal);

        // Create habit from request using simplified structure
        Habit habit = new Habit();

        // IMPORTANT: Do NOT set ID from request for new habits - let database auto-generate
        // Only the database should assign IDs to prevent constraint violations
        habit.setId(null);

        // Set basic habit properties from request (root level)
        habit.setTitle(request.getTitle());
        habit.setDescription(request.getDescription() != null ? request.getDescription() : "No description provided");

        // Set toggles and coinReward from request
        habit.setEveryday(request.isEveryday());
        habit.setEveryWeekend(request.isEveryWeekend());
        habit.setCoinReward(request.getCoinReward());
        habit.setSharedWithParent(request.getSharedWithParent());



        // Set priority and routine (with defaults)
        if (request.getPriority() != null) {
            habit.setPriority(request.getPriority());
        } else {
            habit.setPriority(Habit.Priority.MEDIUM);
        }


        habit.setRoutine(request.getRoutine());

        // Set tags
        habit.setTags(request.getTags());

        // Set start/end date and time directly from request
        habit.setStartDate(request.getStartDate());
        habit.setStartTime(request.getStartTime());

        // Ensure endDate is never null
        if (request.getEndDate() != null) {
            habit.setEndDate(request.getEndDate());
        } else {
            // Set default endDate to 30 days from startDate or today if startDate is null
            LocalDate defaultEndDate = habit.getStartDate() != null
                    ? habit.getStartDate().plusDays(30)
                    : LocalDate.now().plusDays(30);
            habit.setEndDate(defaultEndDate);
        }
        habit.setEndTime(request.getEndTime());

        // Set daysOfWeek based on toggles or from request
        if (request.isEveryday()) {
            habit.setDaysOfWeek(java.util.EnumSet.allOf(DayOfWeek.class));
        } else if (request.isEveryWeekend()) {
            habit.setDaysOfWeek(java.util.EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));
        } else if (request.getDaysOfWeek() != null && !request.getDaysOfWeek().isEmpty()) {
            habit.setDaysOfWeek(request.getDaysOfWeek());
        } else {
            // If no specific days selected and no toggles, default to all days
            habit.setDaysOfWeek(java.util.EnumSet.allOf(DayOfWeek.class));
        }

        // Validate toggles: only one can be true at a time
        if (request.isEveryday() && request.isEveryWeekend()) {
            throw new BusinessException("Cannot select both 'everyday' and 'every weekend' toggles.");
        }

        // Validate that if specific days are selected, toggles should be false
        if (request.getDaysOfWeek() != null && !request.getDaysOfWeek().isEmpty()
                && (request.isEveryday() || request.isEveryWeekend())) {
            throw new BusinessException("Cannot select specific days along with 'everyday' or 'every weekend' toggles.");
        }

        if (user.getFamily() == null) {
            habit.setCreatedBy(user);
            habit.setAssignedTo(user);
            habit.setCoinReward(5); // Default for non-family/self
            Habit savedHabit = habitService.addHabit(habit, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(mapToHabitResponse(savedHabit));
        }

        if (user.getRelationship().equals(Relationship.PARENT) && request.getForUserId() != null) {
            User child = userService.getUserById(request.getForUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Child user not found"));
            FamilyMember childMember = familyMemberService.getByUserAndFamilyId(child, user.getFamily().getId())
                    .orElseThrow(() -> new UnauthorizedActionException("Target user is not a family member."));
            if (!"child".equals(childMember.getRelationship()) ||
                    !child.getFamily().getId().equals(user.getFamily().getId())) {
                throw new UnauthorizedActionException("Can only add habits for your own child.");
            }
            habit.setCreatedBy(user);
            habit.setAssignedTo(child);
            habit.setFamily(user.getFamily());
            // coinReward must be 5 or 10 for parent-created habits
            if (request.getCoinReward() != 5 && request.getCoinReward() != 10) {
                throw new BusinessException("Coin reward must be either 5 or 10 for parent-created habits.");
            }
            habit.setCoinReward(request.getCoinReward());
            Habit savedHabit = habitService.addHabit(habit, child);
            return ResponseEntity.status(HttpStatus.CREATED).body(mapToHabitResponse(savedHabit));
        } else if (user.getRelationship().equals(Relationship.CHILD) || user.getRelationship().equals(Relationship.SELF)) {
            habit.setCreatedBy(user);
            habit.setAssignedTo(user);
            habit.setFamily(user.getFamily());
            habit.setCoinReward(5); // Default for self/child
            Habit savedHabit = habitService.addHabit(habit, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(mapToHabitResponse(savedHabit));
        } else {
            throw new UnauthorizedActionException("You do not have permission to add habits.");
        }
    }


    @PutMapping("/update/{habitId}")
    public ResponseEntity<HabitResponse> updateHabit(Principal principal,
                                                     @PathVariable Long habitId,
                                                     @RequestBody HabitRequest request) {
        User user = getUserFromPrincipal(principal);

        // Create habit object from request fields using simplified structure
        Habit habitUpdates = new Habit();

        // Set basic habit properties from request (root level)
        habitUpdates.setTitle(request.getTitle());
        habitUpdates.setDescription(request.getDescription());
        habitUpdates.setPriority(request.getPriority());
        habitUpdates.setRoutine(request.getRoutine());
        habitUpdates.setStatus(request.getStatus());
        habitUpdates.setTags(request.getTags());

        // Set toggles and coinReward from request
        habitUpdates.setEveryday(request.isEveryday());
        habitUpdates.setEveryWeekend(request.isEveryWeekend());
        if (request.getCoinReward() > 0) {
            habitUpdates.setCoinReward(request.getCoinReward());
        }

        // Set start/end date and time directly from request
        habitUpdates.setStartDate(request.getStartDate());
        habitUpdates.setStartTime(request.getStartTime());
        habitUpdates.setEndDate(request.getEndDate());
        habitUpdates.setEndTime(request.getEndTime());

        // Set daysOfWeek based on toggles or from request
        if (request.isEveryday()) {
            habitUpdates.setDaysOfWeek(java.util.EnumSet.allOf(DayOfWeek.class));
        } else if (request.isEveryWeekend()) {
            habitUpdates.setDaysOfWeek(java.util.EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));
        } else if (request.getDaysOfWeek() != null && !request.getDaysOfWeek().isEmpty()) {
            habitUpdates.setDaysOfWeek(request.getDaysOfWeek());
        }

        if (user.getFamily() == null) {
            Optional<Habit> updated = habitService.updateHabit(habitId, habitUpdates, user);
            return updated.map(this::mapToHabitResponse)
                    .map(ResponseEntity::ok)
                    .orElseThrow(() -> new BusinessException("Habit not found or you do not have permission to update this habit."));
        }

        FamilyMember member = familyMemberService.getByUser(user)
                .orElseThrow(() -> new UnauthorizedActionException("User is not a family member."));

        Optional<Habit> updated;
        if ("parent".equals(member.getRelationship()) && request.getForUserId() != null) {
            User child = userService.getUserById(request.getForUserId())
                    .orElseThrow(() -> new BusinessException("Child user not found"));
            FamilyMember childMember = familyMemberService.getByUser(child)
                    .orElseThrow(() -> new UnauthorizedActionException("Target user is not a family member."));
            if (!"child".equals(childMember.getRelationship()) ||
                    !child.getFamily().getId().equals(user.getFamily().getId())) {
                throw new UnauthorizedActionException("Can only update habits for your own child.");
            }
            updated = habitService.updateHabit(habitId, habitUpdates, child);
        } else {
            updated = habitService.updateHabit(habitId, habitUpdates, user);
        }

        return updated.map(this::mapToHabitResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new BusinessException("Habit not found or you do not have permission to update this habit."));
    }

    private HabitResponse mapToHabitResponse(Habit habit) {
        return new HabitResponse(habit);
    }

    @DeleteMapping("/delete/{habitId}")
    public ResponseEntity<String> deleteHabit(Principal principal,
                                              @PathVariable Long habitId,
                                              @RequestParam(value = "forUserId", required = false) Long forUserId) {
        User user = getUserFromPrincipal(principal);

        // Allow users without a family to delete their own habits
        if (user.getFamily() == null) {
            boolean isDeleted = habitService.deleteHabit(habitId, user);
            if (isDeleted) {
                return ResponseEntity.ok("Habit with ID " + habitId + " has been deleted.");
            } else {
                throw new UnauthorizedActionException("You do not have permission to delete this habit.");
            }
        }

        // If user is in a family, check relationship and permissions
        FamilyMember member = familyMemberService.getByUser(user)
                .orElseThrow(() -> new UnauthorizedActionException("User is not a family member."));

        boolean isDeleted;
        if ("parent".equals(member.getRelationship()) && forUserId != null) {
            // Parent deleting habit for a child
            User child = userService.getUserById(forUserId)
                    .orElseThrow(() -> new BusinessException("Child user not found"));
            FamilyMember childMember = familyMemberService.getByUser(child)
                    .orElseThrow(() -> new UnauthorizedActionException("Target user is not a family member."));
            if (!"child".equals(childMember.getRelationship()) ||
                    !child.getFamily().getId().equals(user.getFamily().getId())) {
                throw new UnauthorizedActionException("Can only delete habits for your own child.");
            }
            isDeleted = habitService.deleteHabit(habitId, child);
        } else {
            // Child or self deleting their own habit
            isDeleted = habitService.deleteHabit(habitId, user);
        }

        if (isDeleted) {
            return ResponseEntity.ok("Habit with ID " + habitId + " has been deleted.");
        } else {
            throw new UnauthorizedActionException("You do not have permission to delete this habit.");
        }
    }

    @GetMapping("/family/{familyId}")
    public ResponseEntity<List<HabitRequest>> getHabitsByFamily(@PathVariable Long familyId) {
        List<Habit> habits = habitService.getHabitsByFamily(familyId);
        if (habits == null || habits.isEmpty()) {
            throw new BusinessException("No habits found for family");
        }
        return ResponseEntity.ok(habits.stream().map(this::mapToHabitRequest).collect(Collectors.toList()));
    }

    @GetMapping("/all")
    public ResponseEntity<List<HabitRequest>> getAllHabitsForUser(Principal principal) {
        User user = getUserFromPrincipal(principal);
        List<Habit> habits = habitService.getHabitsForUser(user);
        if (habits == null || habits.isEmpty()) {
            throw new BusinessException("No habits found for user");
        }
        return ResponseEntity.ok(habits.stream().map(this::mapToHabitRequest).collect(Collectors.toList()));
    }

    @GetMapping("/day")
    public ResponseEntity<List<HabitRequest>> getHabitsForUserByDay(
            Principal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        User user = getUserFromPrincipal(principal);
        List<Habit> habits = habitService.getHabitsForUserByDay(user, date);
        if (habits == null || habits.isEmpty()) {
            throw new BusinessException("No habits found for user on this day");
        }
        return ResponseEntity.ok(habits.stream().map(this::mapToHabitRequest).collect(Collectors.toList()));
    }

    @GetMapping("/debug")
    public String debug() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return "Principal: " + auth.getName() + ", Authorities: " + auth.getAuthorities();
    }

    // Converts Habit entity to HabitRequest for frontend (epoch millis ->
    // LocalDate/LocalTime)
    private HabitRequest mapToHabitRequest(Habit habit) {
        HabitRequest req = new HabitRequest();

        req.setId(habit.getId());
        // Set all fields at root level
        req.setTitle(habit.getTitle());
        req.setDescription(habit.getDescription());
        req.setStatus(habit.getStatus());
        req.setPriority(habit.getPriority());
        req.setRoutine(habit.getRoutine());
        req.setTags(habit.getTags());
        req.setCoinReward(habit.getCoinReward());
        req.setDaysOfWeek(habit.getDaysOfWeek());
        req.setStartDate(habit.getStartDate());
        req.setStartTime(habit.getStartTime());
        req.setEndDate(habit.getEndDate());
        req.setEndTime(habit.getEndTime());

        // Set toggles based on daysOfWeek
        if (habit.getDaysOfWeek() != null) {
            // Check if it's everyday (all 7 days)
            if (habit.getDaysOfWeek().size() == 7) {
                req.setEveryday(true);
                req.setEveryWeekend(false);
            }
            // Check if it's every weekend (only Saturday and Sunday)
            else if (habit.getDaysOfWeek().size() == 2
                    && habit.getDaysOfWeek().contains(DayOfWeek.SATURDAY)
                    && habit.getDaysOfWeek().contains(DayOfWeek.SUNDAY)) {
                req.setEveryday(false);
                req.setEveryWeekend(true);
            }
            // Custom days selected
            else {
                req.setEveryday(false);
                req.setEveryWeekend(false);
            }
        } else {
            req.setEveryday(false);
            req.setEveryWeekend(false);
        }

        return req;
    }

    // Removed unused toEpochMillisAtMidnight method

    private User getUserFromPrincipal(Principal principal) {
        String principalName = principal.getName();
        try {
            Long userId = Long.parseLong(principalName);
            return userService.getUserById(userId)
                    .orElseThrow(() -> new BusinessException("User not found"));
        } catch (NumberFormatException e) {
            // Not a user ID, treat as username
            return userService.getUserByUsername(principalName)
                    .orElseThrow(() -> new BusinessException("User not found"));
        }
    }

    /**
     * GET /habits/today/status/{STATUS}
     * Returns today's habits filtered by HabitStatus.
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<HabitRequest>> getTodayHabitsByStatus(
            Principal principal,
            @PathVariable Habit.HabitStatus status) {
        User user = getUserFromPrincipal(principal);
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        List<Habit> habits = habitService.getHabitsForUserByDay(user, today).stream()
                .filter(h -> h.getStatus() == status)
                .collect(Collectors.toList());

        if (habits.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No habits found for today with status: " + status);
        }

        return ResponseEntity.ok(habits.stream().map(this::mapToHabitRequest).collect(Collectors.toList()));
    }

    /**
     * GET /habits/today/priority/{PRIORITY}
     * Returns today's habits filtered by Priority.
     */
    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<HabitRequest>> getTodayHabitsByPriority(
            Principal principal,
            @PathVariable Habit.Priority priority) {
        User user = getUserFromPrincipal(principal);
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        List<Habit> habits = habitService.getHabitsForUserByDay(user, today).stream()
                .filter(h -> h.getPriority() == priority)
                .collect(Collectors.toList());

        if (habits.isEmpty()) {
            throw new BusinessException("No habits found for today with priority: " + priority);
        }

        return ResponseEntity.ok(habits.stream().map(this::mapToHabitRequest).collect(Collectors.toList()));
    }

    /**
     * GET /habits/today/routine/{ROUTINE}
     * Returns today's habits filtered by Routine.
     */
    @GetMapping("/routine/{routine}")
    public ResponseEntity<List<HabitRequest>> getTodayHabitsByRoutine(
            Principal principal,
            @PathVariable Habit.Routine routine) {
        User user = getUserFromPrincipal(principal);
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        List<Habit> habits = habitService.getHabitsForUserByDay(user, today).stream()
                .filter(h -> h.getRoutine().equalsIgnoreCase(routine.name()))
                .collect(Collectors.toList());

        if (habits.isEmpty()) {
            throw new BusinessException("No habits found for today with routine: " + routine);
        }

        return ResponseEntity.ok(habits.stream().map(this::mapToHabitRequest).collect(Collectors.toList()));
    }

    /**
     * GET /habits/search
     * Returns habits based on search criteria.
     */
    @GetMapping("/search")
    public ResponseEntity<HabitSearchResponse> searchHabits(
            Principal principal, // ✅ ADD: Missing Principal parameter
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Habit.HabitStatus status,
            @RequestParam(required = false) Habit.Priority priority,
            @RequestParam(required = false) Habit.Routine routine,
            @RequestParam(required = false) Boolean everyday,
            @RequestParam(required = false) Boolean everyWeekend,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, // ✅
            // FIXED:
            // Use
            // LocalDate
            // instead
            // of
            // Long
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, // ✅
            // FIXED:
            // Use
            // LocalDate
            // instead
            // of Long
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDate createdAt, // ✅
            // FIXED:
            // Use
            // LocalDate
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDate updatedAt, // ✅
            // FIXED:
            // Use
            // LocalDate
            @RequestParam(required = false) Integer coinReward,
            @RequestParam(required = false) Set<String> tags,
            @RequestParam(required = false) Set<DayOfWeek> daysOfWeek,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir) {
        // ✅ ADD: Get user from principal for security
        User user = getUserFromPrincipal(principal);

        HabitSearchResponse habits = habitService.searchHabits(
                user, // ✅ ADD: Pass user to filter by user's habits
                title, description, status, priority, routine,
                everyday, everyWeekend, startDate, endDate,
                createdAt, updatedAt, coinReward, tags, daysOfWeek,
                sortBy, sortDir);

        // ✅ ADD: Return proper ResponseEntity and convert to HabitRequest
        if (habits == null) {
            throw new BusinessException("No habits found matching the search criteria");
        }

        return ResponseEntity.ok(habits);
    }
    /**
     * GET /habits/{id}
     * Returns a habit by its ID.
     */
    @GetMapping("/habit-by-id")
    public ResponseEntity<HabitResponseTodayDto> getHabitById(@RequestParam Long habitId,
                                                      @RequestParam LocalDate date) {
            HabitResponseTodayDto response = habitService.getHabitResponseById(habitId,date);
            return ResponseEntity.ok(response);
    }

    @GetMapping("/month")
    public List<HabitResponse> getHabitsForMonth(
            @RequestParam int year,
            @RequestParam int month) {
        return habitService.getHabitsForMonth(year, month);
    }

    @GetMapping("/{habitId}/month")
    public ResponseEntity<?> getHabitDetailsForMonth(
            Principal principal,
            @PathVariable Long habitId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        User user = getUserFromPrincipal(principal); // Replace null with Principal if needed
        return ResponseEntity.ok(
                habitService.getHabitDetailsForMonth(habitId, year, month,user)
        );
    }

    // ✅ ADD: Get today's completions
    @GetMapping("/habits-by-date")
    public ResponseEntity<List<HabitResponseTodayDto>> getTodayCompletions(Principal principal,
                                                                           @RequestParam(required = false) LocalDate date,
                                                                           @RequestParam(required = false, defaultValue = "DAY") String viewType,
                                                                           @RequestParam(required = false) Long forUserId,
                                                                           @RequestParam(required = false) String status,
                                                                           @RequestParam(required = false) String routine,
                                                                           @RequestParam(required = false) String priority) {
        User user = getUserFromPrincipal(principal);
        List<HabitResponseTodayDto> completions = habitService.getHabitByDate(user,forUserId,date,viewType,status,routine,priority);
        return ResponseEntity.ok(completions);
    }

    @PostMapping("/lets-go/{habitId}")
    public ResponseEntity<?> startHabit(Principal principal, @PathVariable Long habitId) {
        User user = getUserFromPrincipal(principal);
        return habitService.startHabitForToday(user, habitId);
    }

    @PostMapping("/abort/{habitId}")
    public ResponseEntity<?> abortHabit(Principal principal, @PathVariable Long habitId) {
        User user = getUserFromPrincipal(principal);
        return habitService.abortHabitForToday(user, habitId);
    }

    @PostMapping("/complete/{habitId}")
    public ResponseEntity<?> completeHabit(Principal principal,
                                           @PathVariable Long habitId) {
        User user = getUserFromPrincipal(principal);

        try {
            // ✅ FIXED: Use HabitCompletion instead of Habit
            Optional<HabitCompletion> completionOpt = habitService.completeHabit(habitId, user);
            if (completionOpt.isPresent()) {
                HabitCompletion completion = completionOpt.get();
                return ResponseEntity.ok(java.util.Map.of(
                        "message", "Habit completed successfully for today!",
                        "coinsEarned", completion.getCoinsEarned(),
                        "habitId", habitId,
                        "completionDate", completion.getCompletionDate(),
                        "completedAt", java.time.LocalDateTime.now()));
            } else {
                throw new BusinessException("Habit not found or not scheduled for today");
            }
        } catch (Exception e) {
            throw new BusinessException("Error completing habit: " + e.getMessage());
        }
    }

    // ✅ ADD: Get completion history for a task
    @GetMapping("/{habitId}/completions")
    public ResponseEntity<List<HabitCompletion>> getTaskCompletionHistory(
            Principal principal,
            @PathVariable Long taskId) {
        User user = getUserFromPrincipal(principal);
        List<HabitCompletion> completions = habitService.getHabitCompletionHistory(taskId);
        return ResponseEntity.ok(completions);
    }


}
