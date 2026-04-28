package com.superme.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArticleStatistics {
    private Long totalArticles;
    private Long completedArticles;
    private Long pendingArticles;
}
