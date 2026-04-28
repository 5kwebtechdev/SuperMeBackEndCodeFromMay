package com.superme.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.superme.enums.AgeGroup;
import com.superme.enums.Relationship;
import com.superme.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class User {

    // ─────────────────────────────
    // Basic Information
    // ─────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String email; // Nullable: Can register with phone only

    @Column(unique = true)
    private String phone; // Nullable: Optional but must be unique

    @Column(nullable = false)
    private String gender;

    @Column(nullable = false)
    private LocalDate dateOfBirth; // Required

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Relationship relationship;

    // ─────────────────────────────
    // Authentication & Account Status
    // ─────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean isLoggedIn = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(unique = true)
    private String emailVerificationToken;

    @Column
    private LocalDateTime lastLoginDate;

    @Column
    private LocalDateTime createdDateTime;

    @Column
    private String referralCode; // Optional referral code


    // ─────────────────────────────
    // Gamification Fields
    // ─────────────────────────────
    @Column(nullable = false)
    @Builder.Default
    private int coins = 0;

    @Column(nullable = false)
    @Builder.Default
    private int currentStreak = 0;

    @Column(nullable = false)
    @Builder.Default
    private int highestStreak = 0;

    // ─────────────────────────────
    // Derived Attributes
    // ─────────────────────────────
    @Column(nullable = false)
    @Builder.Default
    private int age = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AgeGroup ageGroup = AgeGroup.BELOW_11;

    // ─────────────────────────────
    // Relationships
    // ─────────────────────────────
    @ManyToOne
    @JsonBackReference
    private Family family;

    @OneToOne
    @JoinColumn(name = "avatar_id")
    private Avatar avatar;

    @ManyToOne
    @JoinColumn(name = "pet_id")
    private Pet pet;

    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<CalendarEvent> calendarEvents;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<CoinTransaction> coinHistory;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reward> rewards;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Badge> badges;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Note> notes;

    @Column(nullable = false)
    @Builder.Default
    private Integer dailyActivityStreak = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer learningActivityStreak = 0;

    private LocalDate lastDailyBonusDate;

    // Used ONLY for streak calculation
    private LocalDate lastStreakDate;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalActiveDays = 0;

    private LocalDate lastMissedStreakDate;
    private Integer lastStreakCount;

    // Learning Enthusiast
    @Column(nullable = false)
    @Builder.Default
    private Integer learningStreak = 0;

    private LocalDate lastLearningDate;

    // Daily Champion
    @Column(nullable = false)
    @Builder.Default
    private Integer activityStreak = 0;
    private LocalDate lastActivityDate;
    // ─────────────────────────────
    // Lifecycle Methods
    // ─────────────────────────────
    @PrePersist
    @PreUpdate
    private void updateAgeAndAgeGroup() {
        if (this.dateOfBirth != null) {
            this.age = Period.between(this.dateOfBirth, LocalDate.now()).getYears();
            this.ageGroup = AgeGroup.fromAge(this.age);
        }
    }

    // ─────────────────────────────
    // Utility Methods
    // ─────────────────────────────
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        updateAgeAndAgeGroup();
    }

    public boolean isUsingEmail() {
        return email != null && !email.isBlank();
    }

    public boolean isUsingPhone() {
        return phone != null && !phone.isBlank();
    }

    public void setCreatedDateTimeOnRegister() {
        this.createdDateTime = LocalDateTime.now();
    }

    // Optional lightweight constructor
    public User(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
