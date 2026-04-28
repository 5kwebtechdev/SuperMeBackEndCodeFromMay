package com.superme.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarEventRequestDTO {
    private String title;
    private String notes; // maps to description in entity
    private Set<String> tags;
    private boolean isEveryday;
    private boolean isEveryWeekend;
    private Set<DayOfWeek> daysOfWeek;
    private String startTime;
    private String endTime;
    private LocalDate startDate;
    private LocalDate endDate;

    @Default
    private Priority priority = Priority.MEDIUM;

    public enum Priority {
        HIGH, MEDIUM, LOW
    }
}