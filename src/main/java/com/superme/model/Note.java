package com.superme.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ElementCollection
    @CollectionTable(name = "note_tags", joinColumns = @JoinColumn(name = "note_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(nullable = false)
    private java.time.LocalDate createdDate;
    @Column(nullable = false)
    private java.time.LocalTime createdTime;

    @Column(nullable = false)
    private java.time.LocalDate updatedDate;
    @Column(nullable = false)
    private java.time.LocalTime updatedTime;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Lombok will generate constructors, getters, setters, builder

    @PrePersist
    protected void onCreate() {
        java.time.LocalDate nowDate = java.time.LocalDate.now();
        java.time.LocalTime nowTime = java.time.LocalTime.now();
        this.createdDate = nowDate;
        this.createdTime = nowTime;
        this.updatedDate = nowDate;
        this.updatedTime = nowTime;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = java.time.LocalDate.now();
        this.updatedTime = java.time.LocalTime.now();
    }
}