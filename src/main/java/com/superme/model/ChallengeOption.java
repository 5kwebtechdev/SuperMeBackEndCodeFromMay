package com.superme.model;

import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import com.fasterxml.jackson.annotation.JsonBackReference;

/**
 * Challenge Option entity representing answer choices for challenges.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengeOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    @JsonBackReference
    private Challenge challenge;

    @Column(nullable = false, length = 500)
    private String optionText;

    // New field for image-based options
    @Column(nullable = true)
    private String optionImageUrl;

    @Column(nullable = false)
    private Integer optionOrder; // 1, 2, 3, 4 for ordering

    @Column(nullable = false)
    @Builder.Default
    private Boolean isCorrect = false;

    // Audit fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ============================================================================
    // LIFECYCLE METHODS
    // ============================================================================

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now(ZoneId.systemDefault());
    }

    @Override
    public String toString() {
        return "ChallengeOption{" +
                "id=" + id +
                ", optionOrder=" + optionOrder +
                ", optionText='" + optionText + '\'' +
                ", optionImageUrl='" + optionImageUrl + '\'' +
                ", isCorrect=" + isCorrect +
                '}';
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public String getOptionText() {
        return optionText;
    }

    public String getOptionImageUrl() {
        return optionImageUrl;
    }

    /**
     * Returns the display content for the option - image URL if available, otherwise text.
     */
    public String getDisplayContent() {
        if (optionImageUrl != null && !optionImageUrl.trim().isEmpty()) {
            return optionImageUrl;
        }
        return optionText;
    }

    /**
     * Checks if this option is image-based.
     */
    public boolean isImageOption() {
        return optionImageUrl != null && !optionImageUrl.trim().isEmpty();
    }
}