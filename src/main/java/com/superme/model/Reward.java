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
public class Reward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // Name of the reward

    @Column(nullable = false)
    private int cost; // Coins required to claim the reward

    @Column(nullable = false)
    @Builder.Default
    private boolean isClaimed = false; // Whether the reward has been claimed

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    private User user; // User who claimed the reward (nullable for available rewards)

    @Column(nullable = true)
    private LocalDateTime claimedAt; // Time when the reward was claimed
}