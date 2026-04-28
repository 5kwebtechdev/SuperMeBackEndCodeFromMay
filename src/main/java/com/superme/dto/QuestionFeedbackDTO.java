package com.superme.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionFeedbackDTO {
    private Long questionId;
    private String questionText;
    private boolean correct;
    private String explanation;
    private String correctAnswer;
    private String userAnswer;
    private int pointsEarned;
}