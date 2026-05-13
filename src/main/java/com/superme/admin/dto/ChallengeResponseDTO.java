package com.superme.admin.dto;

import com.superme.enums.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeResponseDTO {
    private Long challengeId;
    private String name;
    private String description;
    private String descriptionExpanded;
    private Category category;
    private Difficulty difficulty;
    private List<AgeGroup> ageGroups;
    private String topic;
    private Status status;
    private Integer coins;
    private Integer coinsForCorrectAnswer;
    private Integer trophies;
    private String timeDuration;
    private SectionTitle sectionTitle;
    private String positiveFeedback;
    private String negativeFeedback;
    private String negativeFeedbackTryAgain;
    private String thumbnailImageUrl;
    private String innerImageUrl;
    private Boolean enabled;
    private List<QuestionResponseDTO> questions;
    private List<AttachmentResponseDTO> attachments;
    private Integer totalQuestions;
    private Integer totalPoints;
    
    // Display fields
    private String difficultyDisplay;
    private String statusDisplay;
    private Boolean published;
    private String ageGroupsDisplay;
    private String categoryDisplay;
}