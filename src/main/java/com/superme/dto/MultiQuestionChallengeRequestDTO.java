package com.superme.dto;

import com.superme.enums.*;
import com.superme.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for creating/updating challenges with multiple questions
 * NO audit fields (id, createdAt, updatedAt, createdBy, updatedBy)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiQuestionChallengeRequestDTO {



    @NotBlank(message = "Challenge name is required")
    @Size(min = 3, max = 200, message = "Name must be between 3 and 200 characters")
    private String name;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 500, message = "Description must be between 10 and 500 characters")
    private String description;

    private String descriptionExpanded;

    @NotNull(message = "Category is required")
    private Category category;

    @NotNull(message = "Difficulty is required")
    private Difficulty difficulty;

    @NotNull(message = "At least one age group is required")
    @Size(min = 1, message = "At least one age group is required")
    private List<AgeGroup> ageGroups;

    @NotBlank(message = "Topic is required")
    @Size(min = 2, max = 100, message = "Topic must be between 2 and 100 characters")
    private String topic;

    @Builder.Default
    private Status status = Status.DRAFT;

    @Builder.Default
    private Integer coins = 0;

    @Builder.Default
    private Integer coinsForCorrectAnswer = 0;

    @Builder.Default
    private Integer trophies = 0;

    @NotNull(message = "Time duration is required")
    private String timeDuration;

    @NotNull(message = "Section title is required")
    private SectionTitle sectionTitle;

    private String positiveFeedback;
    private String negativeFeedback;
    private String negativeFeedbackTryAgain;

    private String thumbnailImageUrl;
    private String innerImageUrl;

    @Builder.Default
    private boolean enabled = true;

    @NotNull(message = "At least one question is required")
    @Size(min = 1, message = "At least one question is required")
    private List<QuestionRequestDTO> questions = new ArrayList<>();

    private List<ChallengeAttachmentRequestDTO> attachments = new ArrayList<>();

    // Helper methods
    public int getTotalQuestions() {
        return questions != null ? questions.size() : 0;
    }

    public int getTotalPoints() {
        if (questions == null) return 0;
        return questions.stream()
                .mapToInt(QuestionRequestDTO::getPoints)
                .sum();
    }

    public void validateQuestions() {
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("At least one question is required");
        }

        for (int i = 0; i < questions.size(); i++) {
            QuestionRequestDTO question = questions.get(i);
            if (question.getQuestionOrder() == null) {
                question.setQuestionOrder(i + 1);
            }
            question.validate();
        }
    }
}