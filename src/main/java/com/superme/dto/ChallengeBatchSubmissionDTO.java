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
public class ChallengeBatchSubmissionDTO {
    private Map<Long, String> questionAnswers; // questionId -> selectedOption(s)
    private Long timeTaken; // in seconds
    private List<Long> skippedQuestions;
}