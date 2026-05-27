package com.superme.dto;

import com.superme.enums.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class MultiQuestionChallengeRequestDTO {
    // Used for edit — null on create
    private Long challengeId;

    // Challenge fields
    private String name;
    private String description;
    private String descriptionExpanded;
    private Category category;
    private Difficulty difficulty;
    private String topic;
    private String timeDuration;
    private Integer coins;
    private Integer adminId;
    private Integer coinsForCorrectAnswer;
    private Integer trophies;
    private SectionTitle sectionTitle;
    private Status status;
    private Boolean enabled;
    private QuestionMode questionMode;

    // Age groups - accept as List<String> first
    private List<String> ageGroups;

    // File uploads — both optional on edit
    private MultipartFile thumbnail;
    private List<MultipartFile> attachments;

    // INDIVIDUAL mode: one visual for the single question
    private MultipartFile questionVisual;

    // Attachment IDs to explicitly remove on edit
    private List<Long> removedAttachmentIds;

    // Multi-question mode: list of questions
    private List<QuestionDTO> questions;

    // Individual question mode: flat single-question fields
    private String questionText;
    private String answerType;
    private String hint;
    private String positiveFeedback;
    private String negativeFeedback;
    private String negativeFeedbackTryAgain;
    private List<OptionDTO> options;

    // Helper method to convert age groups to enum
    public List<AgeGroup> getAgeGroupEnums() {
        if (ageGroups == null) return new ArrayList<>();
        return ageGroups.stream()
                .map(age -> {
                    try {
                        return AgeGroup.valueOf(age);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(age -> age != null)
                .collect(Collectors.toList());
    }

    @Data
    public static class QuestionDTO {
        private String questionText;
        private String answerType;
        private String hint;
        private String positiveFeedback;
        private String negativeFeedback;
        private String negativeFeedbackTryAgain;
        private List<OptionDTO> options;
        // MULTI mode: one visual per question
        private MultipartFile questionVisual;
    }

    @Data
    public static class OptionDTO {
        private String optionText;
        private MultipartFile optionFile;
        private Integer optionOrder;
        private Boolean isCorrect;
    }
}