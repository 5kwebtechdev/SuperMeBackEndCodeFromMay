package com.superme.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HabitCompletion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "habit_id", nullable = false)
    private Habit habit;

    @Column(nullable = false)
    private LocalDate completionDate;

    private LocalTime completionTime;

    @Column(nullable = false)
    @Builder.Default
    private Integer coinsEarned = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean completed = false;

    // ✅ Daily completion status
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CompletionStatus status = CompletionStatus.PENDING;

    public enum CompletionStatus {
        ONGOING,
        PENDING,
        COMPLETED,
        CANCELLED,
        SKIPPED
    }

    @PrePersist
    @PreUpdate
    protected void normalizeTime() {

        if (completionDate == null) {
            completionDate = LocalDate.now();
        }

        if (completionTime == null) {
            completionTime = LocalTime.now().withNano(0);
        } else {
            completionTime = completionTime.withNano(0);
        }
    }

}