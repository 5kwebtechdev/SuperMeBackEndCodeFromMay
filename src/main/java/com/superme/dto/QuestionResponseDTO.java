package com.superme.dto;

import com.superme.enums.AnswerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponseDTO {

    private Long questionId;
    private Integer questionOrder;
    private String questionText;
    private String questionImageUrl;
    private String hint;  // ADDED HINT FIELD
    private AnswerType answerType;

    private Integer points;
    private Integer timeLimit;
    private String attachmentUrl;
    private String attachmentType;


    //    @Column(columnDefinition = "TEXT")
    private String positiveFeedback;
//
//    @Column(columnDefinition = "TEXT")
    private String negativeFeedback;
//
//    @Column(columnDefinition = "TEXT")
    private String negativeFeedbackTryAgain;

    // Audit fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;




    // Options
    @Builder.Default
    private List<QuestionOptionResponseDTO> options = new ArrayList<>();

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

    public long getCorrectOptionsCount() {
        return options.stream()
                .filter(opt -> Boolean.TRUE.equals(opt.getIsCorrect()))
                .count();
    }

    public boolean isSingleChoice() {
        return answerType == AnswerType.MCQ && getCorrectOptionsCount() == 1;
    }

    public boolean isMultipleChoice() {
        return answerType == AnswerType.MCQ && getCorrectOptionsCount() > 1;
    }

    public boolean isTrueFalse() {
        return answerType == AnswerType.TRUE_FALSE;
    }

    public boolean isVisual() {
        return answerType == AnswerType.VISUALS;
    }
}