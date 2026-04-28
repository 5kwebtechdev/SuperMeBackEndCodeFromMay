package com.superme.dto;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Map;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeBatchResult {
    private int totalCorrect;
    private int totalQuestions;
    private int totalPoints;
    private double percentageScore;
    private Map<Long, Boolean> questionResults; // questionId -> isCorrect
    private List<QuestionFeedbackDTO> questionFeedbacks;
}
