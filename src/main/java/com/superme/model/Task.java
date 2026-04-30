package com.superme.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
// import com.mindfull.model.User;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import java.util.List;

@Entity
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    public enum Priority {
        HIGH, MEDIUM, LOW
    }


    private String routine;


    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private LocalTime endTime;

    @ManyToOne
    @JsonBackReference
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "family_id", nullable = true)
    @JsonBackReference
    @JsonIgnore
    private Family family;

    @ManyToOne
    @JsonBackReference
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @ElementCollection(targetClass = DayOfWeek.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "task_days_of_week", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "day_of_week")
    private Set<DayOfWeek> daysOfWeek;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    public enum TaskStatus {
        PENDING,
        ONGOING,
        COMPLETED
    }

    private Integer rewardCoins;

    @Column(nullable = false)
    @Builder.Default
    private Boolean sharedWithParent = true;

    private boolean isEveryday;
    private boolean isEveryWeekend;

    @ElementCollection
    @CollectionTable(name = "task_tags", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "tag")
    private List<String> tags;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        // Set default rewardCoins to 5 if not set
        if (this.rewardCoins == null) {
            this.rewardCoins = 5;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}