package com.superme.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ChallengeDTO {
    // Basic Challenge Info
    private Long id;
    private String name;
    private String description;
    private String descriptionExpanded;

    // Classification
    private String category; // ARTICLE, PUZZLE, QUIZ
    private String topic;
    private String difficulty; // EASY, MEDIUM, HARD
    private String status; // DRAFT, VERIFICATION_PENDING, APPROVED, REJECTED
    private List<String> ageGroups;

    // Rewards
    private Integer coins;
    private Integer coinsForCorrectAnswer;
    private Integer trophies;
    private Integer rewardCoins;

    // Challenge Content
    private String timeDuration; // SHORT, MEDIUM, LONG
    private String sectionTitle; // RECENTLY_ADDED, TODAYS_CHALLENGE, TRENDING
    private String questionText;
    private String questionImageUrl; // New field for question image
    private String answerType; // MCC, TRUE_FALSE, VISUALS
    private String hint;

    // Images
    private String thumbnailImageUrl;
    private String innerImageUrl;
    private String imageUrl; // Legacy field - maps to thumbnail

    // Feedback
    private String positiveFeedback;
    private String negativeFeedback;
    private String negativeFeedbackTryAgain;

    // Options - dynamically populated based on answerType
    private List<ChallengeOptionDTO> options;

    // Attachments - for additional files
    private List<String> attachments;

    // Legacy option fields for backward compatibility (for MCC type)
    private String option1;
    private String option2;
    private String option3;
    private String option4;

    // Legacy image option fields for VISUALS type
    private String option1Image;
    private String option2Image;
    private String option3Image;
    private String option4Image;

    // Completion info
    private String correctOption;
    private String correctOptionImage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String createdBy; // Only username
    private String completedBy;
    private boolean visible; // Maps to enabled field
    private Integer score; // From completion record

    // Frontend display helpers
    public boolean isMCQ() {
        return "MCC".equalsIgnoreCase(answerType);
    }

    public boolean isTrueFalse() {
        return "TRUE_FALSE".equalsIgnoreCase(answerType);
    }

    public boolean isVisual() {
        return "VISUALS".equalsIgnoreCase(answerType);
    }

    public boolean isQuiz() {
        return "QUIZ".equalsIgnoreCase(category);
    }

    public boolean isPuzzle() {
        return "PUZZLE".equalsIgnoreCase(category);
    }

    public boolean isArticle() {
        return "ARTICLE".equalsIgnoreCase(category);
    }

    // Check if question is image-based
    public boolean isImageQuestion() {
        return questionImageUrl != null && !questionImageUrl.trim().isEmpty();
    }

    // Get question display content
    public String getQuestionDisplay() {
        if (isImageQuestion()) {
            return questionImageUrl;
        }
        return questionText;
    }

    // Get expected number of options based on answer type
    public int getExpectedOptionCount() {
        if (isTrueFalse()) {
            return 2; // True and False
        } else if (isMCQ()) {
            return 4; // Multiple Choice with 4 options
        } else if (isVisual()) {
            return 4; // Visual questions with 4 image options
        }
        return 0;
    }

    // Check if options are properly configured
    public boolean hasValidOptions() {
        if (options == null || options.isEmpty()) return false;

        if (isTrueFalse()) {
            return options.size() >= 2;
        } else if (isMCQ()) {
            return options.size() >= 4;
        } else if (isVisual()) {
            // For visual questions, check if we have image options
            boolean hasImageOptions = options.stream()
                    .anyMatch(opt -> opt != null && opt.getOptionImageUrl() != null && !opt.getOptionImageUrl().trim().isEmpty());
            return options.size() >= 4 && hasImageOptions;
        }
        return !options.isEmpty();
    }

    // Check if this challenge has image-based options
    public boolean hasImageOptions() {
        if (options == null) return false;
        return options.stream()
                .anyMatch(opt -> opt != null && opt.getOptionImageUrl() != null && !opt.getOptionImageUrl().trim().isEmpty());
    }

//    // Get primary image URL for display
//    public String getPrimaryImageUrl() {
//        if (thumbnailImageUrl != null) {
//            return thumbnailImageUrl;
//        }
//        if (innerImageUrl != null) {
//            return innerImageUrl;
//        }
//        if (imageUrl != null) {
//            return imageUrl;
//        }
//        if (attachments != null && !attachments.isEmpty()) {
//            return attachments.get(0).getFileUrl();
//        }
//        return null;
//    }

    // Get correct option text - FIXED with null safety
    public String getCorrectOptionText() {
        if (options == null) {
            return null;
        }

        return options.stream()
                .filter(Objects::nonNull)
                .filter(opt -> Boolean.TRUE.equals(opt.getIsCorrect()))
                .map(ChallengeOptionDTO::getOptionText)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null); // Use orElse(null) instead of get()
    }

    // Get correct option image URL - FIXED with null safety
    public String getCorrectOptionImageUrl() {
        if (options == null) {
            return null;
        }

        return options.stream()
                .filter(Objects::nonNull)
                .filter(opt -> Boolean.TRUE.equals(opt.getIsCorrect()))
                .map(ChallengeOptionDTO::getOptionImageUrl)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null); // Use orElse(null) instead of get()
    }

    // Additional safe getters for potentially null fields
    public List<String> getAgeGroups() {
        return ageGroups != null ? ageGroups : List.of();
    }

    public List<ChallengeOptionDTO> getOptions() {
        return options != null ? options : List.of();
    }

//    public List<ChallengeAttachmentDTO> getAttachments() {
//        return attachments != null ? attachments : List.of();
//    }

    public Integer getCoins() {
        return coins != null ? coins : 0;
    }

    public Integer getCoinsForCorrectAnswer() {
        return coinsForCorrectAnswer != null ? coinsForCorrectAnswer : 0;
    }

    public Integer getTrophies() {
        return trophies != null ? trophies : 0;
    }

    public Integer getRewardCoins() {
        return rewardCoins != null ? rewardCoins : 0;
    }

    public Integer getScore() {
        return score != null ? score : 0;
    }

    // Safe check for completion status
    public boolean isCompleted() {
        return completedAt != null;
    }

    // Get display name with fallback
    public String getDisplayName() {
        return name != null ? name : "Unnamed Challenge";
    }

    // Get display description with fallback
    public String getDisplayDescription() {
        if (description != null && !description.trim().isEmpty()) {
            return description;
        }
        return descriptionExpanded != null ? descriptionExpanded : "No description available";
    }
}