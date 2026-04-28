package com.superme.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoinTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JsonBackReference
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // User associated with the transaction

    @ManyToOne
    @JoinColumn(name = "habit_id")
    private Habit habit; // Reference to the related habit (nullable for non-habit transactions)

    @ManyToOne
    @JoinColumn(name = "task_id")
    private Task task; // Reference to the related task (nullable for non-task transactions)

    @Column(nullable = false)
    private int amount; // Positive for earning, negative for spending

    @Column(nullable = false)
    private String type; // e.g., "TASK_COMPLETION", "REWARD_REDEMPTION", "COINS_PURCHASE"

    @Column(nullable = false)
    private LocalDateTime timestamp; // Time of the transaction

    @Column(nullable = false)
    private String description; // Add this field for transaction description
}