package com.superme.admin.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponseDTO {
    private Long id;
    private String questionText;
    private String questionImageUrl;
    private String answerType;
    private String hint;
    private String positiveFeedback;
    private String negativeFeedback;
    private String negativeFeedbackTryAgain;
    private List<OptionResponseDTO> options;
}