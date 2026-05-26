package com.superme.service;

import com.superme.dto.LearningSearchResponse;
import com.superme.enums.Category;
import com.superme.enums.Status;
import com.superme.model.Article;
import com.superme.model.Challenge;
import com.superme.repository.ArticleRepository;
import com.superme.repository.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LearningSearchService {

    private final ChallengeRepository challengeRepository;
    private final ArticleRepository articleRepository;

    public List<LearningSearchResponse> search(String keyword, String category) {

        String searchKey =
                (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        // ✅ Convert once
        Category categoryEnum = null;
        if (category != null && !category.isBlank()) {
            categoryEnum = Category.valueOf(category.toUpperCase());
        }

        List<LearningSearchResponse> response = new ArrayList<>();

        // =======================
        // ARTICLE
        // =======================
        if (categoryEnum == null || categoryEnum == Category.ARTICLE) {

            articleRepository
                    .searchArticles(searchKey, Article.Status.PUBLISHED)
                    .forEach(a ->
                            response.add(
                                    LearningSearchResponse.builder()
                                            .id(a.getId())
                                            .title(a.getTitle())
                                            .description(a.getDescription())
                                            .coins(a.getCoins())
                                            .type("ARTICLE")
                                            .ageGroups(List.of(a.getAgeGroup().name()))
                                            .durationMinutes(a.getDurationMinutes())
                                            .thumbnailUrl(a.getThumbnailUrl())
                                            .status(a.getStatus().name())
                                            .build()
                            )
                    );
        }

        // =======================
        // CHALLENGE (QUIZ / PUZZLE)
        // =======================
        if (categoryEnum == null ||
                categoryEnum == Category.QUIZ ||
                categoryEnum == Category.PUZZLE) {

            challengeRepository
                    .searchChallenges(searchKey, categoryEnum, Status.PUBLISHED)
                    .forEach(c ->
                            response.add(
                                    LearningSearchResponse.builder()
                                            .id(c.getId())
                                            .title(c.getName())
                                            .description(c.getDescription())
                                            .coins(c.getCoins())
                                            .type(c.getCategory().name())
                                            .ageGroups(
                                                    c.getAgeGroups()
                                                            .stream()
                                                            .map(Enum::name)
                                                            .toList()
                                            )
                                            .thumbnailUrl(c.getThumbnailImageUrl())
                                            .status(c.getStatus().name())
                                            .build()
                            )
                    );
        }

        return response;
    }
}



