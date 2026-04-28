package com.superme.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // Name of the badge with level (e.g., "Streak Keeper - Level 1")

    @Column(nullable = false)
    private String description; // Description of the badge

    @Column(nullable = false)
    private String icon; // URL or path to the badge icon

    @Column(nullable = false)
    private LocalDateTime earnedAt; // Timestamp when the badge was earned

    @Column(nullable = false)
    @Builder.Default
    private Integer level = 1; // Level of the badge (1-5)

    @Column(nullable = false)
    private String badgeType; // Base badge type (e.g., "STREAK_KEEPER")

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Reference to the User who earned the badge

    @Column(nullable = false)
    private boolean popupShown = false;

    @PrePersist
    protected void onCreate() {
        if (earnedAt == null) {
            earnedAt = LocalDateTime.now();
        }
    }
}