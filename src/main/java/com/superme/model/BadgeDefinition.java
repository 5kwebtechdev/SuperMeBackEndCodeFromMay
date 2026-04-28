package com.superme.model;

import com.superme.enums.BadgeType;
import com.superme.enums.AgeGroup;
import com.superme.enums.BadgeRarity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "badge_definition")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BadgeDefinition {
    @Id
    private String name; // Unique identifier (e.g., "STREAK_KEEPER")

    @Column(nullable = false)
    private String displayName; // Display name (e.g., "Streak Keeper")

    @Column(length = 1000)
    private String description; // Base description

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BadgeType badgeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BadgeRarity rarity = BadgeRarity.COMMON;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "badge_definition_age_groups", joinColumns = @JoinColumn(name = "badge_definition_name"))
    @Column(name = "age_group")
    private List<AgeGroup> applicableAgeGroups;

    @Column(nullable = false)
    private int targetValue; // Maximum target value for level 5

    @Column(nullable = false)
    @Builder.Default
    private Integer maxLevel = 5; // Maximum level for this badge

    private String triggerEvent;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // Psychological design elements
    private String persona;
    private String behavior;
    private String emotionEvoked;
    private String retentionIntent;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}