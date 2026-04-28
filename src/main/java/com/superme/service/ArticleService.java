package com.superme.service;

import com.superme.dto.ArticleResponseDto;
import com.superme.exception.BusinessException;
import com.superme.model.Article;
import com.superme.repository.ArticleRepository;
import com.superme.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ArticleService {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    public List<ArticleResponseDto> getAllArticles() {
        return articleRepository.findAll().stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    public Optional<ArticleResponseDto> getArticleById(Long id) {
        return articleRepository.findById(id).map(this::toResponseDto);
    }

    public ArticleResponseDto markArticleAsRead(Long id, Long userId) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Article not found"));

        article.setStatus(Article.Status.READ);
        Article savedArticle = articleRepository.save(article);

        return toResponseDto(savedArticle);
    }

    private ArticleResponseDto toResponseDto(Article article) {
        return new ArticleResponseDto(
                article.getId(),
                article.getTitle(),
                article.getDescription(),
                article.getCoins(),
                article.getTags(),
                article.getAgeGroup(),
                article.getThumbnailUrl(),
                article.getContent(),
                article.getTimeDuration(),
                article.getStatus(),
                article.getCreatedAt(),
                article.getUpdatedAt(),
                article.getPublishedAt()
        );
    }
}
