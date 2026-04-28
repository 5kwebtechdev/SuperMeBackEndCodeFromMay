package com.superme.model;

import com.superme.enums.AnswerType;
import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "question")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "challenge_id")
    @JsonBackReference("challenge-questions")
    private Challenge challenge;

    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Column(name = "question_image_url")
    private String questionImageUrl;

    @Column(columnDefinition = "TEXT")  // HINT FIELD FROM DATABASE
    private String hint;

    @Column(name = "hint_count",nullable = false)
    private Integer hintCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_type", nullable = false)
    private AnswerType answerType; // MCQ, TRUE_FALSE, VISUALS

    @Column(nullable = false)
    @Builder.Default
    private Integer points = 10;

    @Column(name = "time_limit")
    private Integer timeLimit; // in seconds

    @Column(name = "attachment_url")
    private String attachmentUrl;

    @Column(name = "attachment_type")
    private String attachmentType;

    // Audit fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    @ToString.Exclude
    private User createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    @ToString.Exclude
    private User updatedBy;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("optionOrder ASC")
    @Builder.Default
    @JsonManagedReference("question-options")
    @ToString.Exclude
    private List<QuestionOption> options = new ArrayList<>();

    // ============================================================================
    // LIFECYCLE METHODS
    // ============================================================================

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        this.createdAt = now;
        this.updatedAt = now;
        if (this.points == null) this.points = 10;
        if (this.answerType == null) this.answerType = AnswerType.MCQ;
        if (this.hintCount == null) this.hintCount = 0;  // 🔥 ADD THIS

    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now(ZoneId.systemDefault());
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    public boolean hasImage() {
        return questionImageUrl != null && !questionImageUrl.trim().isEmpty();
    }

    public boolean hasAttachment() {
        return attachmentUrl != null && !attachmentUrl.trim().isEmpty();
    }

    public boolean hasHint() {
        return hint != null && !hint.trim().isEmpty();
    }

    public List<QuestionOption> getCorrectOptions() {
        if (options == null) return new ArrayList<>();
        return options.stream()
                .filter(opt -> Boolean.TRUE.equals(opt.getIsCorrect()))
                .collect(Collectors.toList());
    }

    public int getCorrectOptionsCount() {
        return getCorrectOptions().size();
    }

    public boolean isSingleChoice() {
        return answerType == AnswerType.MCQ || answerType == AnswerType.TRUE_FALSE;
    }

    public boolean isMultipleChoice() {
        // Assuming MCQ can be single or multiple based on correct answers count
        return getCorrectOptionsCount() > 1;
    }

    public boolean isVisual() {
        return answerType == AnswerType.VISUALS;
    }

    public boolean isTrueFalse() {
        return answerType == AnswerType.TRUE_FALSE;
    }

    @Override
    public String toString() {
        return "Question{" +
                "id=" + id +
                ", order=" + questionOrder +
                ", points=" + points +
                ", hint=" + (hasHint() ? "Yes" : "No") +
                ", options=" + (options != null ? options.size() : 0) +
                '}';
    }
}