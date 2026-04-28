package com.superme.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Habit {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "habit_tags", joinColumns = @JoinColumn(name = "habit_id"))
    @Column(name = "tag")
    private Set<String> tags;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private HabitStatus status = HabitStatus.PENDING;

    public enum HabitStatus {
        PENDING,
        ONGOING,
        COMPLETED
    }

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Priority priority;

    public enum Priority {
        HIGH, MEDIUM, LOW
    }


    private String routine;

    private boolean isEveryday;
    private boolean isEveryWeekend;

    @Column(nullable = false)
    @Builder.Default
    private Boolean sharedWithParent = true;

    public enum Routine {
        MORNING, EVENING, NIGHT
    }

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalTime startTime; // time as is

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private LocalTime endTime; // time as is

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    @JsonIgnore
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "assigned_to", nullable = false)
    @JsonIgnore
    private User assignedTo;

    @ManyToOne
    @JoinColumn(name = "family_id", nullable = true) // Allow family_id to be null
    @JsonBackReference
    @JsonIgnore
    private Family family;

    @ElementCollection(targetClass = DayOfWeek.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "habit_days_of_week", joinColumns = @JoinColumn(name = "habit_id"))
    @Column(name = "day_of_week")
    private Set<DayOfWeek> daysOfWeek; // Days when this habit is active

    @Column(nullable = false, updatable = false)
    private LocalDate createdAt;

    @Column(nullable = true)
    private LocalDate updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private int coinReward = 5; // Default to 5 coins per habit/task





    // Lombok will generate constructors, getters, setters, builder

    // No epoch conversion needed; use LocalDate directly
}
