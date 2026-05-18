package com.superme.model;

import com.superme.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Course entity for academic content management.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    // Lombok will generate constructors, getters, setters, builder

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_name", nullable = false)
    private String courseName;

    @Column(columnDefinition = "TEXT")
    private String description;

//    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private String category;


    @Column(nullable = false)
    private String difficulty;

    @Column(name = "no_of_lessons", nullable = false)
    private Integer noOfLessons;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER, targetClass = AgeGroup.class)
    @CollectionTable(
            name = "course_age_groups",
            joinColumns = @JoinColumn(name = "course_id")
    )
    @Column(name = "age_group", nullable = false)
    @Enumerated(EnumType.STRING)
    private List<AgeGroup> ageGroups = new ArrayList<>();

    @Column(nullable = false)
    private Integer duration; // in minutes

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Format format;

    @Column(nullable = false)
    private Integer totalCoins;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "attachment_url")
    private String attachmentUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @com.fasterxml.jackson.annotation.JsonManagedReference
    private List<Lesson> lessons;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(ZoneId.systemDefault());
        if (this.status == null) {
            this.status = Status.DRAFT;
        }
    }
}
