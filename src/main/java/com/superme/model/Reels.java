package com.superme.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "reels")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reels {
    @Column(nullable = true)
    private Long createdBy;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 40)
    @Column(nullable = false, length = 40)
    private String title;

    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String category;

    @Size(max = 200)
    private String hashtags;

    @Column(columnDefinition = "TEXT")
    private String description; // Video description/alt text

    @Column(nullable = false)
    private Integer duration; // duration in seconds

    @NotBlank
    @Column(nullable = false)
    private String videoUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean aiLabel = false;

    @ElementCollection
    @CollectionTable(name = "reel_likes", joinColumns = @JoinColumn(name = "reel_id"))
    @Column(name = "user_id")
    @Builder.Default
    private Set<Long> likedBy = new HashSet<>(); // store userIds

    @ElementCollection
    @CollectionTable(name = "reel_saves", joinColumns = @JoinColumn(name = "reel_id"))
    @Column(name = "user_id")
    @Builder.Default
    private Set<Long> savedBy = new HashSet<>(); // store userIds

    @Column(nullable = false)
    @Builder.Default
    private Integer shareCount = 0;

    @Column(name = "created_date", nullable = false)
    private java.time.LocalDate createdDate;

    @Column(name = "created_time", nullable = false)
    private java.time.LocalTime createdTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    @PrePersist
    protected void onCreate() {
        this.createdDate = java.time.LocalDate.now();
        this.createdTime = java.time.LocalTime.now();
        if (this.status == null) {
            this.status = ApprovalStatus.PENDING;
        }
    }

    public void like(Long userId) {
        this.likedBy.add(userId);
    }

    public void save(Long userId) {
        this.savedBy.add(userId);
    }

    public void share() {
        this.shareCount++;
    }
}