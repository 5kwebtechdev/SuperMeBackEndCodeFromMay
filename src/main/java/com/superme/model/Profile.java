package com.superme.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName; // Full name of the child

    @Column(nullable = false)
    private String petName; // Pet name of the child

    @Column(nullable = false)
    private LocalDate dob; // Date of birth of the child stored as epoch timestamp (in milliseconds)

    @Column(nullable = false)
    private String gender; // Gender of the child (male or female)

    @Column(nullable = false)
    private String avatar; // Avatar selection for the child

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Reference to the user (parent) who created this profile
}