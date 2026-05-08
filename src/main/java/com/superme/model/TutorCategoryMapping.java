package com.superme.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import lombok.*;
@Entity
@Table(name = "tutor_category_mappings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tutor_id", "category_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TutorCategoryMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "tutor_id")
    private Long tutorId;

    @NotNull
    private Integer categoryId;

    @Column(columnDefinition = "TEXT")
    private String selectedFieldOptions;

    private String expertiseLevel;
    private Integer yearsOfExperience;

    @Builder.Default
    private Boolean isAcceptingStudents = true;

    private Integer maxStudentsPerBatch;

    @Builder.Default
    private Integer currentStudents = 0;

    private BigDecimal categoryHourlyRate;
    private BigDecimal categoryBatchRate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    private Integer studentCount = 0;

    private BigDecimal categoryRating;

    @Builder.Default
    private Integer totalReviews = 0;

    private LocalDateTime lastClassTaught;

    @Builder.Default
    private Integer classesCompleted = 0;

    @Builder.Default
    private Integer coursesCompleted = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}