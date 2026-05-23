package com.superme.dto;

import com.superme.enums.*;
import com.superme.model.ChallengeAttachment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for admin challenge overview operations.
 * Contains challenge information optimized for data table display and filtering.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminChallengeDTO {

    private Long challengeId;
    private String name;
    private String description;
    private String descriptionExpanded;

    // Classification
    private String type;              // Category display name
    private String typeValue;         // Category enum value
    private String difficultyLevel;
    private String difficultyValue;   // Difficulty enum value
    private List<AgeGroup> ageGroups;
    private List<String> ageGroupDisplayNames;
    private String topic;
    private Status status;
    private String statusDisplay;

    // Challenge Content
    private List<QuestionResponseDTO> questions;
    private Integer numberOfQuestions;

    // 🔥 MULTI-ANSWER TYPE PER CHALLENGE (each question has its own)
    private List<AnswerType> answerTypes;
    private List<String> answerTypeDisplayNames;

    private String timeDuration;
    private SectionTitle sectionTitle;
    private String sectionTitleDisplay;
    private String hint;

    // Rewards
    private Integer coins;
    private Integer coinsForCorrectAnswer;
    private Integer trophies;

    // Feedback
    private String positiveFeedback;
    private String negativeFeedback;
    private String negativeFeedbackTryAgain;

    // Images
    private String thumbnailImageUrl;
    private String innerImageUrl;

    // Audit
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private boolean enabled;

    // Dashboard Stats
    private Long completionCount;
    private Double averageScore;
    private Double completionRate;

    // Attachments
    private List<ChallengeAttachmentResponseDTO> attachments;

    // Flags
    private boolean hasImageQuestion;
    private boolean hasImageOptions;
    private boolean hasAttachments;
    private boolean hasMultipleAgeGroups;
    private boolean isMultiQuestion;

    // ========================================================================
    // BUSINESS LOGIC
    // ========================================================================

    public boolean matchesSearchTerm(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) return true;

        String term = searchTerm.toLowerCase().trim();

        boolean idMatch = false;
        try {
            long searchId = Long.parseLong(term);
            idMatch = challengeId != null && challengeId.equals(searchId);
        } catch (NumberFormatException ignored) {}

        return idMatch
                || (name != null && name.toLowerCase().contains(term))
                || (topic != null && topic.toLowerCase().contains(term));
    }

    public boolean matchesFilters(ChallengeFilterCriteria criteria) {
        if (criteria == null) return true;

        if (criteria.getType() != null &&
                !criteria.getType().isEmpty() &&
                !criteria.getType().equalsIgnoreCase(typeValue))
            return false;

        if (criteria.getDifficulty() != null &&
                !criteria.getDifficulty().isEmpty() &&
                !criteria.getDifficulty().equalsIgnoreCase(difficultyValue))
            return false;

        if (criteria.getAgeGroup() != null &&
                !criteria.getAgeGroup().isEmpty()) {
            boolean match = ageGroups.stream().anyMatch(
                    age -> age.name().equalsIgnoreCase(criteria.getAgeGroup())
            );
            if (!match) return false;
        }

        if (criteria.getStatus() != null &&
                !criteria.getStatus().isEmpty() &&
                !criteria.getStatus().equalsIgnoreCase(status.name()))
            return false;

        // 🔥 Updated for MULTIPLE ANSWER TYPES
        if (criteria.getAnswerType() != null &&
                !criteria.getAnswerType().isEmpty()) {

            boolean contains = answerTypes != null &&
                    answerTypes.stream()
                            .anyMatch(at -> at.name().equalsIgnoreCase(criteria.getAnswerType()));

            if (!contains) return false;
        }

        if (criteria.getEnabled() != null && enabled != criteria.getEnabled())
            return false;

        if (criteria.getIsMultiQuestion() != null &&
                isMultiQuestion != criteria.getIsMultiQuestion())
            return false;

        return true;
    }

    // ========================================================================
    // UTILITIES
    // ========================================================================

    public String getAgeGroupsAsString() {
        return (ageGroupDisplayNames == null || ageGroupDisplayNames.isEmpty())
                ? "All Ages"
                : String.join(", ", ageGroupDisplayNames);
    }

    public String getQuestionsSummary() {
        if (questions == null || questions.isEmpty()) return "No questions";

        long imageCount = questions.stream()
                .filter(q -> q.getQuestionImageUrl() != null && !q.getQuestionImageUrl().isEmpty())
                .count();

        return String.format("%d questions (%d with images)", questions.size(), imageCount);
    }

    public String getOptionsSummary() {
        if (questions == null || questions.isEmpty()) return "No options";

        long total = 0;
        long correct = 0;

        for (QuestionResponseDTO q : questions) {
            if (q.getOptions() != null) {
                total += q.getOptions().size();
                correct += q.getOptions().stream()
                        .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                        .count();
            }
        }

        return String.format("%d total options (%d correct)", total, correct);
    }

    public String getAttachmentsSummary() {
        if (attachments == null || attachments.isEmpty()) return "No attachments";

        long img = attachments.stream()
                .filter(a -> a.getFileType() == ChallengeAttachment.FileType.JPG ||
                        a.getFileType() == ChallengeAttachment.FileType.PNG)
                .count();
        long pdf = attachments.stream()
                .filter(a -> a.getFileType() == ChallengeAttachment.FileType.PDF)
                .count();

        return String.format("%d files (%d images, %d PDFs)", attachments.size(), img, pdf);
    }

    public String getCompletionStats() {
        if (completionCount == null || completionCount == 0) return "No completions";

        return String.format("%d completions (%.1f%%)",
                completionCount,
                (completionRate != null ? completionRate * 100 : 0));
    }

    public String getFormattedCreatedAt() {
        return formatDateTime(createdAt);
    }

    public String getFormattedUpdatedAt() {
        return formatDateTime(updatedAt);
    }

    private String formatDateTime(LocalDateTime dt) {
        if (dt == null) return "N/A";
        return String.format("%04d-%02d-%02d", dt.getYear(), dt.getMonthValue(), dt.getDayOfMonth());
    }

    public boolean isPublished() {
        return status == Status.PUBLISHED;
    }

    public boolean needsReview() {
        return status == Status.VERIFICATION_PENDING;
    }

    public boolean isDraft() {
        return status == Status.DRAFT;
    }

    public boolean isRejected() {
        return status == Status.REJECTED;
    }

    // ========================================================================
    // FILTER CLASS
    // ========================================================================

    public static class ChallengeFilterCriteria {
        private String searchTerm;
        private String type;
        private String difficulty;
        private String ageGroup;
        private String status;
        private String answerType;
        private Boolean enabled;
        private Boolean isMultiQuestion;
        private LocalDateTime createdAfter;
        private LocalDateTime createdBefore;

        public boolean hasFilters() {
            return (searchTerm != null && !searchTerm.trim().isEmpty()) ||
                    (type != null && !type.trim().isEmpty()) ||
                    (difficulty != null && !difficulty.trim().isEmpty()) ||
                    (ageGroup != null && !ageGroup.trim().isEmpty()) ||
                    (status != null && !status.trim().isEmpty()) ||
                    (answerType != null && !answerType.trim().isEmpty()) ||
                    enabled != null ||
                    isMultiQuestion != null ||
                    createdAfter != null ||
                    createdBefore != null;
        }

        // getters + setters auto-generated by Lombok? No => manually generated?
        // If you want Lombok here, I can add.
        public String getSearchTerm() { return searchTerm; }
        public void setSearchTerm(String searchTerm) { this.searchTerm = searchTerm; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
        public String getAgeGroup() { return ageGroup; }
        public void setAgeGroup(String ageGroup) { this.ageGroup = ageGroup; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getAnswerType() { return answerType; }
        public void setAnswerType(String answerType) { this.answerType = answerType; }
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
        public Boolean getIsMultiQuestion() { return isMultiQuestion; }
        public void setIsMultiQuestion(Boolean isMultiQuestion) { this.isMultiQuestion = isMultiQuestion; }
        public LocalDateTime getCreatedAfter() { return createdAfter; }
        public void setCreatedAfter(LocalDateTime createdAfter) { this.createdAfter = createdAfter; }
        public LocalDateTime getCreatedBefore() { return createdBefore; }
        public void setCreatedBefore(LocalDateTime createdBefore) { this.createdBefore = createdBefore; }
    }
}
