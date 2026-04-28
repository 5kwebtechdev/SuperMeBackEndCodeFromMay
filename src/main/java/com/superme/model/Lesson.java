package com.superme.model;

import com.superme.enums.Format;
import jakarta.persistence.*;
import lombok.*;

/**
 * Lesson entity for course content.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lesson_title", nullable = false)
    private String lessonTitle;

    @Column(name = "lesson_description", columnDefinition = "TEXT")
    private String lessonDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Format format;

    @Column(nullable = false)
    private Integer duration; // in minutes

    @Column(nullable = false)
    private Integer coins;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl; // S3 image URL

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "lesson_order")
    private Integer lessonOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonBackReference
    private Course course;

}