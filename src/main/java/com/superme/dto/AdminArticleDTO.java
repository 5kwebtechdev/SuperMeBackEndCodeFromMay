package com.superme.dto;

import com.superme.enums.AgeGroup;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminArticleDTO {
    private Long id;
    private String title;
    private String description;
    private Integer coins;
    private List<String> tags;
    private AgeGroup ageGroup;
    private String thumbnailUrl;
    private String content;
    private String time_duration;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    private ArticleStatistics statistics;

   }