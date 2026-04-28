package com.superme.dto;

import com.superme.enums.AgeGroup;
import com.superme.model.Article;
import java.time.LocalDateTime;
import java.util.List;

public class ArticleResponseDto {

    private Long id;
    private String title;
    private String description;
    private Integer coins;
    private List<String> tags;
    private AgeGroup ageGroup;
    private String thumbnailUrl;
    private String content;
    private String timeDuration;
    private Article.Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;

    public ArticleResponseDto(Long id, String title, String description, Integer coins, List<String> tags,
                               AgeGroup ageGroup, String thumbnailUrl, String content, String timeDuration,
                               Article.Status status, LocalDateTime createdAt, LocalDateTime updatedAt,
                               LocalDateTime publishedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.coins = coins;
        this.tags = tags;
        this.ageGroup = ageGroup;
        this.thumbnailUrl = thumbnailUrl;
        this.content = content;
        this.timeDuration = timeDuration;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.publishedAt = publishedAt;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCoins() {
        return coins;
    }

    public void setCoins(Integer coins) {
        this.coins = coins;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public AgeGroup getAgeGroup() {
        return ageGroup;
    }

    public void setAgeGroup(AgeGroup ageGroup) {
        this.ageGroup = ageGroup;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTimeDuration() {
        return timeDuration;
    }

    public void setTimeDuration(String timeDuration) {
        this.timeDuration = timeDuration;
    }

    public Article.Status getStatus() {
        return status;
    }

    public void setStatus(Article.Status status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
