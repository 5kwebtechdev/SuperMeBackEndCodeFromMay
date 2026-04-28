package com.superme.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LearningSearchResponse {

    private Long id;
    private String title;
    private String description;
    private Integer coins;
    private String type;           // QUIZ | PUZZLE | ARTICLE
    private List<String> ageGroups;
    private String timeDuration;
    private String thumbnailUrl;
    private String status;
}
