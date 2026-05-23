package com.superme.admin.dto;

import com.superme.enums.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class MultiQuestionChallengeRequestDTO {
    // Challenge fields
    private String name;
    private String description;
    private String descriptionExpanded;
    private Category category;
    private Difficulty difficulty;
    private String topic;
    private String timeDuration;
    private Integer coins;
    private Integer coinsForCorrectAnswer;
    private Integer trophies;
    private SectionTitle sectionTitle;
    private Status status;
    private QuestionMode questionMode;
    
    // Age groups
    private List<AgeGroup> ageGroups;
    
    // File uploads
    private MultipartFile thumbnail;
    private List<MultipartFile> attachments;
    
    // Questions - these will come as separate parameters and need to be mapped
    private List<QuestionDTO> questions;
    
    @Data
    public static class QuestionDTO {
        private String questionText;
        private String answerType;
        private String hint;
        private String positiveFeedback;
        private String negativeFeedback;
        private String negativeFeedbackTryAgain;
        private List<OptionDTO> options;
    }
    
    @Data
    public static class OptionDTO {
        private String optionText;
        private Integer optionOrder;
        private Boolean isCorrect;
    }
}