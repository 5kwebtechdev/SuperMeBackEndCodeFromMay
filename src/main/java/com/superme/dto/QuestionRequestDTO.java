package com.superme.dto;

import com.superme.enums.AnswerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionRequestDTO {

    private Integer questionOrder;

    public Integer getQuestionOrder() {
        return questionOrder;
    }

    public void setQuestionOrder(Integer questionOrder) {
        this.questionOrder = questionOrder;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getQuestionImageUrl() {
        return questionImageUrl;
    }

    public void setQuestionImageUrl(String questionImageUrl) {
        this.questionImageUrl = questionImageUrl;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public AnswerType getAnswerType() {
        return answerType;
    }

    public void setAnswerType(AnswerType answerType) {
        this.answerType = answerType;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public Integer getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(Integer timeLimit) {
        this.timeLimit = timeLimit;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public String getAttachmentType() {
        return attachmentType;
    }

    public void setAttachmentType(String attachmentType) {
        this.attachmentType = attachmentType;
    }

    public List<QuestionOptionRequestDTO> getOptions() {
        return options;
    }

    public void setOptions(List<QuestionOptionRequestDTO> options) {
        this.options = options;
    }

    @NotBlank(message = "Question text is required")
    @Size(min = 5, max = 1000, message = "Question text must be between 5 and 1000 characters")
    private String questionText;

    private String questionImageUrl;

    @Size(max = 500, message = "Hint cannot exceed 500 characters")
    private String hint;  // ADDED HINT FIELD

    @NotNull(message = "Answer type is required")
    private AnswerType answerType;

    @Builder.Default
    @PositiveOrZero(message = "Points must be zero or positive")
    private Integer points = 10;

    @PositiveOrZero(message = "Time limit must be zero or positive")
    private Integer timeLimit;

    private String attachmentUrl;
    private String attachmentType;

    @NotNull(message = "Options are required")
    @Size(min = 2, message = "At least 2 options are required")
    private List<QuestionOptionRequestDTO> options = new ArrayList<>();

    // Enhanced validation
    public void validate() {
        if (questionText == null || questionText.trim().isEmpty()) {
            throw new IllegalArgumentException("Question text is required");
        }
        if (answerType == null) {
            throw new IllegalArgumentException("Answer type is required");
        }
        if (options == null || options.size() < 2) {
            throw new IllegalArgumentException("At least 2 options are required");
        }

        // Set default order if not provided
        for (int i = 0; i < options.size(); i++) {
            QuestionOptionRequestDTO option = options.get(i);
            if (option.getOptionOrder() == null) {
                option.setOptionOrder(i + 1);
            }
        }

        // Validate based on answer type
        long correctCount = options.stream()
                .filter(opt -> Boolean.TRUE.equals(opt.getIsCorrect()))
                .count();

        switch (answerType) {
            case MCQ:
                // MCQ can have 1 or more correct answers
                if (correctCount < 1) {
                    throw new IllegalArgumentException("MCQ questions must have at least 1 correct option");
                }
                break;

            case TRUE_FALSE:
                // TRUE_FALSE must have exactly 2 options
                if (options.size() != 2) {
                    throw new IllegalArgumentException("TRUE_FALSE questions must have exactly 2 options");
                }
                if (correctCount != 1) {
                    throw new IllegalArgumentException("TRUE_FALSE questions must have exactly 1 correct option");
                }
                break;

            case VISUALS:
                // VISUALS can have image options
                if (correctCount < 1) {
                    throw new IllegalArgumentException("VISUALS questions must have at least 1 correct option");
                }
                break;
        }
    }

    // Helper methods
    public boolean hasImage() {
        return questionImageUrl != null && !questionImageUrl.trim().isEmpty();
    }

    public boolean hasHint() {
        return hint != null && !hint.trim().isEmpty();
    }

    public boolean hasAttachment() {
        return attachmentUrl != null && !attachmentUrl.trim().isEmpty();
    }
}