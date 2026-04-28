package com.superme.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ReelCreateRequest {
    @NotBlank
    @Size(max = 40)
    private String title;
    private String hashtags;
    private String category;
    private String description;
    private Integer duration;
    private String videoUrl;
    private Boolean aiLabel;
    private Long createdBy;
    private Long createdAt;
    private java.time.LocalDate createdLocalDate;
    private java.time.LocalTime createdLocalTime;

    // Getters and setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHashtags() {
        return hashtags;
    }

    public void setHashtags(String hashtags) {
        this.hashtags = hashtags;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public Boolean getAiLabel() {
        return aiLabel;
    }

    public void setAiLabel(Boolean aiLabel) {
        this.aiLabel = aiLabel;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
        if (createdAt != null) {
            java.time.Instant instant = java.time.Instant.ofEpochMilli(createdAt);
            this.createdLocalDate = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            this.createdLocalTime = instant.atZone(java.time.ZoneId.systemDefault()).toLocalTime();
        } else {
            this.createdLocalDate = null;
            this.createdLocalTime = null;
        }
    }

    public java.time.LocalDate getCreatedLocalDate() {
        return createdLocalDate;
    }

    public java.time.LocalTime getCreatedLocalTime() {
        return createdLocalTime;
    }
}
