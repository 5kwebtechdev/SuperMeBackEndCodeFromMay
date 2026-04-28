package com.superme.model;

import com.superme.enums.BadgeType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "badge_progress")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BadgeProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String badgeName; // Base badge name (e.g., "STREAK_KEEPER")

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BadgeType badgeType;

    @Column(nullable = false)
    @Builder.Default
    private Integer currentLevel = 1; // Current level (1-5)

    @Column(nullable = false)
    @Builder.Default
    private Integer currentProgress = 0; // Current progress count

    @Column(nullable = false)
    private Integer targetProgress; // Target for current level

    @Column(nullable = false)
    @Builder.Default
    private Integer totalProgress = 0; // Total cumulative progress

    @Column(nullable = false)
    @Builder.Default
    private Boolean completed = false; // Whether current level is completed

    private LocalDateTime levelCompletedAt; // When current level was completed

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Level-specific achievement texts (can be generated dynamically)
    @Column(length = 500)
    private String achievementText;

    // Progress percentage for sorting
    @Transient
    public Double getProgressPercentage() {
        if (targetProgress == null || targetProgress == 0) {
            return 0.0;
        }
        return (double) currentProgress / targetProgress;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}