package com.superme.model;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@Entity
@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // Include only non-null fields in JSON
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    private java.time.LocalDate creationDate;

    @Column(nullable = false)
    private java.time.LocalTime creationTime;

    // Change these from NOT NULL to NULLABLE (optional)
    @Column(nullable = true)  // Was: nullable = false
    private String emotions;

    @Column(nullable = true)  // Was: nullable = false
    private String sleep;

    @Column(nullable = true)  // Was: nullable = false
    private String health;

    @Column(nullable = true)  // Was: nullable = false
    private String hobbies;

    @Column(nullable = true)  // Was: nullable = false
    private String food;

    @Column(nullable = true)  // Was: nullable = false
    private String social;

    @Column(nullable = true)  // Was: nullable = false
    private String school;

    @Column(nullable = true)
    private String notes;

//    @Column(nullable = true, length = 1000)
//    private List<String> attachmentUrls; // S3 URL or key for the uploaded image

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalAttachment> attachments;
 }