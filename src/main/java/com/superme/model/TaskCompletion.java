package com.superme.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minidev.json.annotate.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskCompletion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(nullable = false)
    private LocalDate completionDate;

    @Column(nullable = false)
    private LocalTime completionTime;

    @Column(nullable = false)
    private Integer coinsEarned;

    @Column(nullable = false)
    @Builder.Default
    private Boolean completed = false;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CompletionStatus status = CompletionStatus.PENDING;

    public enum CompletionStatus {
        ONGOING,
        PENDING,
        COMPLETED,
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