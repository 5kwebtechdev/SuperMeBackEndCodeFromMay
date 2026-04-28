package com.superme.dto;

import com.superme.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for GET responses - INCLUDES audit fields
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiQuestionChallengeResponseDTO {

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

    private boolean enabled;

    private List<QuestionResponseDTO> questions = new ArrayList<>();
    private List<ChallengeAttachmentResponseDTO> attachments = new ArrayList<>();


    private boolean completed;
    private LocalDateTime completedAt;
    private Integer score;
    private boolean recentlyCompleted;

    // Audit fields (INCLUDED in response)
//    private String createdBy;
//    private LocalDateTime createdAt;
//    private String updatedBy;
//    private LocalDateTime updatedAt;

    // Statistics
    private Integer totalQuestions;
    private Integer totalPoints;
    private Integer estimatedTimeMinutes;

    // Completion statistics (optional)
    private Long completionCount;
    private Double averageScore;

    // Helper methods for display
    public String getCategoryDisplay() {
        return category != null ? category.getDisplayName() : "";
    }

    public String getDifficultyDisplay() {
        return difficulty != null ? difficulty.getDisplayName() : "";
    }

    public String getStatusDisplay() {
        return status != null ? status.getDisplayName() : "";
    }

    public String getAgeGroupsDisplay() {
        if (ageGroups == null || ageGroups.isEmpty()) return "All Ages";
        return ageGroups.stream()
                .map(AgeGroup::getDisplayName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    public boolean isPublished() {
        return status == Status.APPROVED && enabled;
    }
}