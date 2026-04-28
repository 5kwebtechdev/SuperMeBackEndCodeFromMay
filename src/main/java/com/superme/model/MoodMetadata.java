package com.superme.model;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mood_metadata")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MoodMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "mood", nullable = false, length = 50)
    private String mood; // e.g., VERY_HAPPY, HAPPY, SAD

    @Column(name = "audience", nullable = false, length = 20)
    private String audience; // SELF or PARENT

    @Column(name = "title", nullable = false, length = 255)
    private String title; // e.g., "You're glowing with happiness!"

    @Column(name = "description_template", columnDefinition = "TEXT", nullable = false)
    private String descriptionTemplate; // e.g., "<count> habits and <count> tasks awaits"

    @Column(name = "empty_state_description", columnDefinition = "TEXT", nullable = false)
    private String emptyStateDescription; // e.g., "Add tasks & habits to view your progress!"

    @Column(name = "completed_state_description", columnDefinition = "TEXT", nullable = false)
    private String completedStateDescription; // e.g., "All done, day well played!"
}

