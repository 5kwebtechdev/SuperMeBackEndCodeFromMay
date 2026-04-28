package com.superme.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "school_classes")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SchoolClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Column(nullable = false)
    private String name;   // "Class 1", "Class 7"
}
