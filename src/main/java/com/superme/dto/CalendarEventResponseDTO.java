package com.superme.dto;

import com.superme.model.CalendarEvent;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarEventResponseDTO {

    private Long id;
    private String title;
    private String description;

    private Set<String> tags;

    private boolean everyday;
    private boolean everyWeekend;
    private Set<DayOfWeek> daysOfWeek;

    private String startTime;
    private String endTime;

    // original event range
    private LocalDate startDate;
    private LocalDate endDate;

    // 🔑 calendar-day projection (NOT persisted)
    private LocalDate completionDate;

    private CalendarEvent.Priority priority;

    // derived, optional
    private String status;

    // for sorting / filtering
    private LocalDateTime createdAt;
}
