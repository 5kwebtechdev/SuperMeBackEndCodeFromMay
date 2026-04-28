package com.superme.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonBackReference;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "question_option")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @JsonBackReference("question-options")
    private Question question;

    @Column(name = "option_text", length = 500)
    private String optionText;

    @Column(name = "option_image_url")
    private String optionImageUrl;

    @Column(name = "option_order", nullable = false)
    private Integer optionOrder;

    @Column(name = "is_correct", nullable = false)
    @Builder.Default
    private Boolean isCorrect = false;

    // ADD THIS FIELD
    @Column(name = "hint", columnDefinition = "TEXT", length = 500)
    private String hint;

    @Column(columnDefinition = "TEXT")
    private String explanation;

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
        if (this.isCorrect == null) this.isCorrect = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now(ZoneId.systemDefault());
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    public boolean isImageOption() {
        return optionImageUrl != null && !optionImageUrl.trim().isEmpty();
    }

    public String getDisplayContent() {
        return isImageOption() ? optionImageUrl : optionText;
    }

    @Override
    public String toString() {
        return "QuestionOption{" +
                "id=" + id +
                ", order=" + optionOrder +
                ", isCorrect=" + isCorrect +
                '}';
    }
}