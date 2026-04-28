package com.superme.controller;

import com.superme.dto.CalendarEventRequestDTO;
import com.superme.dto.CalendarFullResponse;
import com.superme.exception.BusinessException;
import com.superme.model.CalendarEvent;
import com.superme.model.User;
import com.superme.service.CalendarEventService;
import com.superme.repository.UserRepository;
import java.security.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/calendar")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE, RequestMethod.OPTIONS })
public class CalendarEventController {
    // Error response DTO for consistent error output
    public static class ErrorResponse {
        private final int code;
        private final String message;

        public ErrorResponse(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }

    // Helper to extract User from Principal (userId only, no username fallback)
    private User getUserFromPrincipal(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));
    }

    @Autowired
    private CalendarEventService calendarEventService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Add a new calendar event for the authenticated user.
     */
    @PostMapping("/add")
    public ResponseEntity<?> addEvent(
            @RequestBody CalendarEventRequestDTO request,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Unauthorized: Principal is null"));
        }
        try {
            User user = getUserFromPrincipal(principal);
            CalendarEvent event = mapRequestDtoToEntity(request);
            CalendarEvent created = calendarEventService.addEvent(event, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad request: " + e.getMessage()));
        }
    }

    /**
     * Update an existing calendar event for the authenticated user.
     */
    @PutMapping("/update/{eventId}")
    public ResponseEntity<Object> updateEvent(
            @PathVariable Long eventId,
            @RequestBody CalendarEventRequestDTO request,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Unauthorized: Principal is null"));
        }
        try {
            User user = getUserFromPrincipal(principal);
            CalendarEvent updatedEvent = mapRequestDtoToEntity(request);
            Optional<CalendarEvent> updated = calendarEventService.updateEvent(eventId, updatedEvent, user);
            return updated.<ResponseEntity<Object>>map(event -> ResponseEntity.ok((Object) event))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Event not found")));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad request: " + e.getMessage()));
        }
    }

    // Helper to map CalendarEventRequestDTO to CalendarEvent entity
    private CalendarEvent mapRequestDtoToEntity(CalendarEventRequestDTO dto) {
        CalendarEvent event = new CalendarEvent();
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getNotes());
        event.setTags(dto.getTags());
        event.setEveryday(dto.isEveryday());
        event.setEveryWeekend(dto.isEveryWeekend());
        event.setDaysOfWeek(dto.getDaysOfWeek());
        event.setStartTime(dto.getStartTime());
        event.setEndTime(dto.getEndTime());
        event.setStartDate(dto.getStartDate());
        event.setEndDate(dto.getEndDate());
        event.setType(CalendarEvent.TYPE_MANUAL_EVENT); // Set type for manual calendar events
        if (dto.getPriority() != null) {
            // Map DTO's Priority enum to entity's Priority enum by name
            try {
                event.setPriority(CalendarEvent.Priority.valueOf(dto.getPriority().name()));
            } catch (IllegalArgumentException e) {
                event.setPriority(CalendarEvent.Priority.MEDIUM); // fallback
            }
        }
        return event;
    }

    /**
     * Get all events for the authenticated user by priority.
     */
    @GetMapping("/user-events/priority/{priority}")
    public ResponseEntity<?> getEventsForUserByPriority(
            Principal principal,
            @PathVariable String priority) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Unauthorized: Principal is null"));
        }
        User user = getUserFromPrincipal(principal);
        try {
            CalendarEvent.Priority enumPriority = CalendarEvent.Priority.valueOf(priority.toUpperCase());
            List<CalendarEvent> events = calendarEventService.getEventsByUserAndPriority(user.getId(), enumPriority);
            return ResponseEntity.ok(events);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Invalid priority value"));
        }
    }

    /**
     * Delete a calendar event for the authenticated user.
     */
    @DeleteMapping("/delete/{eventId}")
    public ResponseEntity<?> deleteEvent(
            @PathVariable Long eventId,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Unauthorized: Principal is null"));
        }
        try {
            User user = getUserFromPrincipal(principal);
            boolean deleted = calendarEventService.deleteEvent(eventId, user);
            if (deleted) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Event not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad request: " + e.getMessage()));
        }
    }

    /**
     * Get all events for a family by familyId.
     */
    @GetMapping("/family-events")
    public ResponseEntity<List<CalendarEvent>> getEventsForFamily(@RequestParam Long familyId) {
        List<CalendarEvent> events = calendarEventService.getEventsForFamilyByFamilyId(familyId);
        if (events == null || events.isEmpty()) {
            throw new BusinessException("No events found for family");
        }
        return ResponseEntity.ok(events);
    }

    /**
     * Get all events for a family within a time range by familyId.
     */
    @GetMapping("/family-events/range")
    public ResponseEntity<List<CalendarEvent>> getEventsForFamilyInRange(
            @RequestParam Long familyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        // Make sure CalendarEventService uses LocalDate not long/epoch
        List<CalendarEvent> events = calendarEventService.getEventsForFamilyInRangeByFamilyId(familyId, startDate,
                endDate);
        if (events == null || events.isEmpty()) {
            throw new BusinessException("No events found for family in range");
        }
        return ResponseEntity.ok(events);
    }

    /**
     * Get all events created by the authenticated user.
     */
    @GetMapping("/user-events")
    public ResponseEntity<?> getEventsForUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Unauthorized: Principal is null"));
        }
        User user = getUserFromPrincipal(principal);
        List<CalendarEvent> events = calendarEventService.getEventsForUser(user.getId());
        if (events == null || events.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "No events found for user"));
        }
        return ResponseEntity.ok(events);
    }

    /**
     * Get all events created by the authenticated user within a time range.
     */
    @GetMapping("/user-events/range")
    public ResponseEntity<?> getEventsForUserInRange(
            Principal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Unauthorized: Principal is null"));
        }
        User user = getUserFromPrincipal(principal);
        // Make sure CalendarEventService uses LocalDate not long/epoch
        List<CalendarEvent> events = calendarEventService.getEventsForUserInRange(user.getId(), startDate, endDate);
        if (events == null || events.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "No events found for user in range"));
        }
        return ResponseEntity.ok(events);
    }

    /**
     * Get the calendar for the authenticated user by date and view type, including
     * events, tasks, and habits.
     */
    @GetMapping("/user-calendar")
    public ResponseEntity<?> getUserCalendar(
            Principal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long forUserId,
            @RequestParam(defaultValue = "day") String viewType) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Unauthorized: Principal is null"));
        }
            User user = getUserFromPrincipal(principal);
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
            // Make sure CalendarEventService uses LocalDate not long/epoch
            CalendarFullResponse response = calendarEventService.getFullCalendarForUser(user, forUserId,viewType, start, end);
            return ResponseEntity.ok(response);
    }
}