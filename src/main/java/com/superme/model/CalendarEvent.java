
package com.superme.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Data
public class CalendarEvent {

    // Type constants for different calendar event types
    public static final String TYPE_MANUAL_EVENT = "EVENT";
    public static final String TYPE_TASK = "TASK";
    public static final String TYPE_HABIT = "HABIT";

    // Manual builder for CalendarEvent (since Lombok is not working)
    public static CalendarEventBuilder builder() {
        return new CalendarEventBuilder();
    }

    public static class CalendarEventBuilder {
        private String title;
        private String description;
        private java.time.LocalDate startDate;
        private String startTime;
        private java.time.LocalDate endDate;
        private String endTime;
        private String type;
        private boolean completed = false;
        private User createdBy;
        private Family family;
        private java.time.LocalDate createdAt;
        private java.time.LocalDate updatedAt;
        private java.util.Set<java.time.DayOfWeek> daysOfWeek;
        private java.util.Set<String> tags;
        private boolean everyday;
        private boolean everyWeekend;
        private Priority priority = Priority.MEDIUM;

        public CalendarEventBuilder title(String title) {
            this.title = title;
            return this;
        }

        public CalendarEventBuilder description(String description) {
            this.description = description;
            return this;
        }

        public CalendarEventBuilder startDate(java.time.LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public CalendarEventBuilder startTime(String startTime) {
            this.startTime = startTime;
            return this;
        }

        public CalendarEventBuilder endDate(java.time.LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public CalendarEventBuilder endTime(String endTime) {
            this.endTime = endTime;
            return this;
        }

        public CalendarEventBuilder type(String type) {
            this.type = type;
            return this;
        }

        public CalendarEventBuilder completed(boolean completed) {
            this.completed = completed;
            return this;
        }

        public CalendarEventBuilder createdBy(User createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public CalendarEventBuilder family(Family family) {
            this.family = family;
            return this;
        }

        public CalendarEventBuilder createdAt(java.time.LocalDate createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public CalendarEventBuilder updatedAt(java.time.LocalDate updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public CalendarEventBuilder daysOfWeek(java.util.Set<java.time.DayOfWeek> daysOfWeek) {
            this.daysOfWeek = daysOfWeek;
            return this;
        }

        public CalendarEventBuilder tags(java.util.Set<String> tags) {
            this.tags = tags;
            return this;
        }

        public CalendarEventBuilder everyday(boolean everyday) {
            this.everyday = everyday;
            return this;
        }

        public CalendarEventBuilder everyWeekend(boolean everyWeekend) {
            this.everyWeekend = everyWeekend;
            return this;
        }

        public CalendarEventBuilder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        public CalendarEvent build() {
            CalendarEvent event = new CalendarEvent();
            event.title = this.title;
            event.description = this.description;
            event.startDate = this.startDate;
            event.startTime = this.startTime;
            event.endDate = this.endDate;
            event.endTime = this.endTime;
            event.type = this.type;
            event.completed = this.completed;
            event.createdBy = this.createdBy;
            event.family = this.family;
            event.createdAt = this.createdAt;
            event.updatedAt = this.updatedAt;
            event.daysOfWeek = this.daysOfWeek;
            event.tags = this.tags;
            event.isEveryday = this.everyday;
            event.isEveryWeekend = this.everyWeekend;
            event.priority = this.priority;
            return event;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private String startTime;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private String endTime;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    @Builder.Default
    private boolean completed = false;

    @ManyToOne
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User createdBy;

    @ManyToOne
    @JsonIgnore
    private Family family;

    @Column(nullable = false, updatable = false)
    private LocalDate createdAt;

    @Column(nullable = true)
    private LocalDate updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDate.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDate.now();
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "calendar_event_tags", joinColumns = @JoinColumn(name = "calendar_event_id"))
    @Column(name = "tag")
    private Set<String> tags;

    @ElementCollection(targetClass = DayOfWeek.class, fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "calendar_event_days_of_week", joinColumns = @JoinColumn(name = "calendar_event_id"))
    @Column(name = "day_of_week")
    private Set<DayOfWeek> daysOfWeek;

    private boolean isEveryday;
    private boolean isEveryWeekend;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    public enum Priority {
        HIGH, MEDIUM, LOW
    }

    // Getters and setters

    public Set<DayOfWeek> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(Set<DayOfWeek> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public boolean isEveryday() {
        return isEveryday;
    }

    public void setEveryday(boolean everyday) {
        isEveryday = everyday;
    }

    public boolean isEveryWeekend() {
        return isEveryWeekend;
    }

    public void setEveryWeekend(boolean everyWeekend) {
        isEveryWeekend = everyWeekend;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public Family getFamily() {
        return family;
    }

    public void setFamily(Family family) {
        this.family = family;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDate getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDate updatedAt) {
        this.updatedAt = updatedAt;
    }
}