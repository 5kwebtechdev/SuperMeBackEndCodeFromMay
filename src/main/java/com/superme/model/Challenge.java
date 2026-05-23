package com.superme.model;

import com.superme.admin.model.Admin;
import com.superme.enums.*;
import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "challenge")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String descriptionExpanded;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category; // ARTICLE, PUZZLE, QUIZ

    @Column(nullable = false, length = 100)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty; // EASY, HARD, MEDIUM

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.DRAFT; // APPROVED, DRAFT, PUBLISHED, REJECTED, VERIFICATION_PENDING

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER, targetClass = AgeGroup.class)
    @CollectionTable(
            name = "challenge_age_groups",
            joinColumns = @JoinColumn(name = "challenge_id")
    )
    @Column(name = "age_group", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private List<AgeGroup> ageGroups = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private Integer coins = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer coinsForCorrectAnswer = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer trophies = 0;

//    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private String timeDuration; // LONG, MEDIUM, SHORT

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SectionTitle sectionTitle; // RECENTLY_ADDED, TODAYS_CHALLENGE, TRENDING

    @Column
    private String thumbnailImageUrl;

    @Column
    private String innerImageUrl;

    // Audit fields
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "created_by",insertable = true,updatable = false)
//    @ToString.Exclude
////    private User createdBy;
//    private Admin createdBy;

    @Column(nullable = false,insertable = true,updatable = false)
    private LocalDateTime createdAt;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "updated_by",insertable = false,updatable = true)
//    @ToString.Exclude
//    private User updatedBy;


    private Long createdByAdminId;
    private Long updatedByUserId;



    @Column(insertable = false,updatable = true)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_mode")
    private QuestionMode questionMode;

    // ============================================================================
    // RELATIONSHIPS
    // ============================================================================

    @OneToMany(mappedBy = "challenge", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionOrder ASC")
    @Builder.Default
    @JsonManagedReference("challenge-questions")
    @ToString.Exclude
    private List<Question> questions = new ArrayList<>();

    @OneToMany(mappedBy = "challenge", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonManagedReference("challenge-attachments")
    @ToString.Exclude
    private List<ChallengeAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "challenge", cascade = CascadeType.ALL)
    @Builder.Default
    @ToString.Exclude
    private List<UserChallengeCompletion> completions = new ArrayList<>();

    // ============================================================================
    // LIFECYCLE METHODS
    // ============================================================================

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        this.createdAt = now;
        this.updatedAt = now;

        // Set defaults
        if (this.status == null) this.status = Status.DRAFT;
        if (this.coins == null) this.coins = 0;
        if (this.coinsForCorrectAnswer == null) this.coinsForCorrectAnswer = 0;
        if (this.trophies == null) this.trophies = 0;
        if (this.enabled == null) this.enabled = true;

    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now(ZoneId.systemDefault());
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    public int getTotalQuestions() {
        return questions != null ? questions.size() : 0;
    }

    public int getTotalPoints() {
        if (questions == null) return 0;
        return questions.stream()
                .mapToInt(Question::getPoints)
                .sum();
    }

    public int getEstimatedTimeMinutes() {
        if (questions == null) return 0;
        return questions.stream()
                .mapToInt(q -> q.getTimeLimit() != null ? q.getTimeLimit() : 30)
                .sum();
    }

    public String getAgeGroupsDisplay() {
        if (ageGroups == null || ageGroups.isEmpty()) {
            return "All Ages";
        }
        return ageGroups.stream()
                .map(AgeGroup::getDisplayName)
                .collect(Collectors.joining(", "));
    }

    public boolean isPublished() {
        return status == Status.APPROVED && enabled;
    }

    public boolean isDraft() {
        return status == Status.DRAFT;
    }

    public boolean needsReview() {
        return status == Status.VERIFICATION_PENDING;
    }

    @Override
    public String toString() {
        return "Challenge{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", category=" + category +
                ", questions=" + getTotalQuestions() +
                ", difficulty=" + difficulty +
                '}';
    }
}