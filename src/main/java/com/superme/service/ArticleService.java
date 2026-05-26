package com.superme.service;

import com.superme.config.FileStorageConfig;
import com.superme.dto.ArticleResponseDto;
import com.superme.exception.BusinessException;
import com.superme.model.Article;
import com.superme.model.User;
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

    public ArticleResponseDto markArticleAsRead(Long id, User user) {
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
//                article.getThumbnailUrl(),
                 (buildFileUrl(article.getThumbnailUrl())),
                article.getContent(),
                article.getDurationMinutes(),
                article.getStatus(),
                article.getCreatedAt(),
                article.getUpdatedAt(),
                article.getPublishedAt()
        );
    }







    @Autowired
    private FileStorageConfig fileStorageConfig;

    private String buildFileUrl(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }

        // extract filename from /uploads/tempimg.png
        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        return fileStorageConfig.getBaseUrl() + "/v1/articles/download/" + fileName;
//        return fileStorageConfig.getBaseUrl() + "/api/files/download/" + fileName;
    }

    
}
