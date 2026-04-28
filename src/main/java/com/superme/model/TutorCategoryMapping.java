package com.superme.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import lombok.*;
//
///**
// * Entity to map tutors to categories with their expertise and field options.
// * One tutor can teach multiple categories.
// * One category can have multiple tutors.
// */
//@Entity
//@Table(name = "tutor_category_mappings", uniqueConstraints = {
//        @UniqueConstraint(columnNames = {"tutor_id", "category_id"})
//})
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class TutorCategoryMapping {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Integer id;
//
//    @Column(name = "tutor_id", nullable = false)
//    private Long tutorId;
//
//    @Column(name = "category_id", nullable = false)
//    private Integer categoryId;
//
//    // JSON field storing selected field options
//    // Example: {"classes": ["CLASS_9", "CLASS_10"], "subjects": ["MATHEMATICS"]}
//    @Column(name = "selected_field_options", columnDefinition = "JSON")
//    private String selectedFieldOptions;
//
//    // Expertise level
//    @Column(name = "expertise_level", length = 50)
//    private String expertiseLevel; // BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
//
//    // Experience in years
//    @Column(name = "years_of_experience")
//    private Integer yearsOfExperience;
//
//    // Availability
//    @Column(name = "is_accepting_students")
//    @Builder.Default
//    private Boolean isAcceptingStudents = true;
//
//    @Column(name = "max_students_per_batch")
//    private Integer maxStudentsPerBatch;
//
//    @Column(name = "current_students")
//    @Builder.Default
//    private Integer currentStudents = 0;
//
//    // Category-specific rates
//    @Column(name = "category_hourly_rate", precision = 10, scale = 2)
//    private BigDecimal categoryHourlyRate;
//
//    @Column(name = "category_batch_rate", precision = 10, scale = 2)
//    private BigDecimal categoryBatchRate;
//
//    // Description
//    @Column(name = "description", columnDefinition = "TEXT")
//    private String description;
//
//    // Stats for this category
//    @Column(name = "student_count")
//    @Builder.Default
//    private Integer studentCount = 0;
//
//    @Column(name = "category_rating", precision = 3, scale = 2)
//    private BigDecimal categoryRating;
//
//    @Column(name = "total_reviews")
//    @Builder.Default
//    private Integer totalReviews = 0;
//
//    @Column(name = "last_class_taught")
//    private LocalDateTime lastClassTaught;
//
//    @Column(name = "classes_completed")
//    @Builder.Default
//    private Integer classesCompleted = 0;
//
//    @Column(name = "courses_completed")
//    @Builder.Default
//    private Integer coursesCompleted = 0;
//
//    @Column(name = "created_at", nullable = false)
//    private LocalDateTime createdAt;
//
//    @Column(name = "updated_at")
//    private LocalDateTime updatedAt;
//
//    @PrePersist
//    protected void onCreate() {
//        LocalDateTime now = LocalDateTime.now();
//        this.createdAt = now;
//        this.updatedAt = now;
//
//        if (this.isAcceptingStudents == null) {
//            this.isAcceptingStudents = true;
//        }
//        if (this.currentStudents == null) {
//            this.currentStudents = 0;
//        }
//        if (this.studentCount == null) {
//            this.studentCount = 0;
//        }
//        if (this.classesCompleted == null) {
//            this.classesCompleted = 0;
//        }
//        if (this.coursesCompleted == null) {
//            this.coursesCompleted = 0;
//        }
//    }
//
//    @PreUpdate
//    protected void onUpdate() {
//        this.updatedAt = LocalDateTime.now();
//    }
//}
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