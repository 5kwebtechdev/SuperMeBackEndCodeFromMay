package com.superme.service;

import com.superme.dto.AdminArticleDTO;
import com.superme.dto.AdminArticleOverviewResponseDTO;
import com.superme.dto.ArticleStatistics;
import com.superme.enums.AgeGroup;
import com.superme.model.Article;
import com.superme.repository.ArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminArticleService {

    @Autowired
    private ArticleRepository articleRepository;

    // ============================================================================
    // MAIN ADMIN SCREEN METHODS
    // ============================================================================

    public AdminArticleOverviewResponseDTO getAdminArticleOverview(
            int page, int size, String sortBy, String sortDir,
            String searchText, List<AgeGroup> filterAgeGroups) {

        Page<AdminArticleDTO> dataTable = getArticleDataTable(
                page, size, sortBy, sortDir, searchText, filterAgeGroups);

        ArticleStatistics stats = getArticleStatsForCards();
        AdminArticleOverviewResponseDTO.FilterOptions filterOptions = getFilterOptions();

        AdminArticleOverviewResponseDTO response = new AdminArticleOverviewResponseDTO();
        response.setArticles(dataTable.getContent());
        response.setStatistics(stats);
        response.setFilterOptions(filterOptions);
        response.setTotalElements(dataTable.getTotalElements());
        response.setTotalPages(dataTable.getTotalPages());
        response.setCurrentPage(dataTable.getNumber());
        response.setPageSize(dataTable.getSize());
        response.setHasNext(dataTable.hasNext());
        response.setHasPrevious(dataTable.hasPrevious());

        return response;
    }

    public Page<AdminArticleDTO> getArticleDataTable(
            int page, int size, String sortBy, String sortDir,
            String searchText, List<AgeGroup> filterAgeGroups) {

        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Article> articlePage = articleRepository.findAll(pageable);

        List<AdminArticleDTO> filteredArticles = articlePage.getContent().stream()
                .filter(article -> applySearchCriteria(article, searchText))
                .filter(article -> applyFilterCriteria(article, filterAgeGroups))
                .map(this::convertToAdminArticleDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(filteredArticles, pageable, articlePage.getTotalElements());
    }

    // ============================================================================
    // STAT CARDS
    // ============================================================================

    public ArticleStatistics getArticleStatsForCards() {
        long totalArticles = articleRepository.count();
        long publishedArticles = articleRepository.countByStatus(Article.Status.PUBLISHED);
        long draftArticles = articleRepository.countByStatus(Article.Status.DRAFT);

        return new ArticleStatistics(
                totalArticles, publishedArticles, draftArticles);
    }

    // ============================================================================
    // SEARCH FUNCTIONALITY
    // ============================================================================

    public List<String> getSearchSuggestions(String query) {
        List<String> suggestions = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            String searchQuery = query.toLowerCase();

            // Search by title
            List<String> titleSuggestions = articleRepository.findAll().stream()
                    .map(Article::getTitle)
                    .filter(title -> title != null && title.toLowerCase().contains(searchQuery))
                    .distinct()
                    .limit(5)
                    .collect(Collectors.toList());
            suggestions.addAll(titleSuggestions);

            // Search by ID (if query is numeric)
            try {
                Long id = Long.parseLong(query);
                articleRepository.findById(id)
                        .ifPresent(article -> suggestions.add("ID: " + id + " - " + article.getTitle()));
            } catch (NumberFormatException ignored) {
            }

            // Search by description
            List<String> descSuggestions = articleRepository.findAll().stream()
                    .filter(article -> article.getDescription() != null &&
                            article.getDescription().toLowerCase().contains(searchQuery))
                    .map(article -> article.getTitle() + " (Description match)")
                    .distinct()
                    .limit(3)
                    .collect(Collectors.toList());
            suggestions.addAll(descSuggestions);
        }

        return suggestions.stream().distinct().limit(10).collect(Collectors.toList());
    }

    // ============================================================================
    // FILTER OPTIONS
    // ============================================================================

    public AdminArticleOverviewResponseDTO.FilterOptions getFilterOptions() {
        List<String> ageGroups = Arrays.stream(AgeGroup.values())
                .map(Enum::name)
                .collect(Collectors.toList());

        List<String> statuses = Arrays.stream(Article.Status.values())
                .map(Enum::name)
                .collect(Collectors.toList());

        List<String> durationCategories = Arrays.stream(Article.DurationCategory.values())
                .map(Enum::name)
                .collect(Collectors.toList());

        return new AdminArticleOverviewResponseDTO.FilterOptions(
                ageGroups, durationCategories, statuses);
    }

    // ============================================================================
    // CRUD OPERATIONS
    // ============================================================================

    public AdminArticleDTO createArticle(AdminArticleDTO dto) {
        // Convert DTO -> Entity
        Article article = convertToEntity(dto);

        // Set Audit Fields
        article.setCreatedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());

        // Save Entity
        Article saved = articleRepository.save(article);

        // Convert back to DTO
        return convertToDTO(saved);
    }

    private Article convertToEntity(AdminArticleDTO dto) {
        Article article = new Article();

        // Check if it's an update
        if (dto.getId() != null ) {
            article.setId(dto.getId());
            article.setCreatedAt(dto.getCreatedAt()); // Keep original createdAt
        } else {
            // New article
            article.setCreatedAt(LocalDateTime.now());
        }

        // If updating existing article (PUT), ID should be copied
        article.setId(dto.getId());

        article.setTitle(dto.getTitle());
        article.setDescription(dto.getDescription());
        article.setCoins(dto.getCoins());
        article.setContent(dto.getContent());
        article.setThumbnailUrl(dto.getThumbnailUrl());
        article.setTimeDuration(dto.getTime_duration());

        // Tags (List<String>)
        article.setTags(dto.getTags());

        // AgeGroup (String -> Enum)
        if (dto.getAgeGroup() != null) {
            article.setAgeGroup(dto.getAgeGroup());
        }

        // Status (String -> Enum)
        if (dto.getStatus() != null) {
            article.setStatus(Article.Status.valueOf(dto.getStatus()));
        }

        article.setUpdatedAt(LocalDateTime.now());
        article.setPublishedAt(dto.getPublishedAt());

        return article;
    }

    private AdminArticleDTO convertToDTO(Article article) {
        AdminArticleDTO dto = new AdminArticleDTO();

        dto.setId(article.getId());
        dto.setTitle(article.getTitle());
        dto.setCoins(article.getCoins());
        dto.setDescription(article.getDescription());
        dto.setContent(article.getContent());
        dto.setThumbnailUrl(article.getThumbnailUrl());
        dto.setThumbnailUrl(article.getTimeDuration());

        // Tags
        dto.setTags(article.getTags());

        // AgeGroup enum -> String
        if (article.getAgeGroup() != null) {
            dto.setAgeGroup(article.getAgeGroup());

        }

        // Status enum -> String
        if (article.getStatus() != null) {
            dto.setStatus(article.getStatus().name());
        }

        // Dates
        dto.setCreatedAt(article.getCreatedAt());
        dto.setUpdatedAt(article.getUpdatedAt());
        dto.setPublishedAt(article.getPublishedAt());

        return dto;
    }



    public Optional<AdminArticleDTO> getArticleById(Long id) {
        return articleRepository.findById(id)
                .map(this::convertToAdminArticleDTO);
    }

    public Article updateArticle(Long id, Article articleDetails) {
        Optional<Article> optionalArticle = articleRepository.findById(id);
        if (optionalArticle.isPresent()) {
            Article article = optionalArticle.get();
            article.setTitle(articleDetails.getTitle());
            article.setCoins(articleDetails.getCoins());
            article.setDescription(articleDetails.getDescription());
            article.setTags(articleDetails.getTags());
            article.setAgeGroup(articleDetails.getAgeGroup());
            article.setThumbnailUrl(articleDetails.getThumbnailUrl());
            article.setContent(articleDetails.getContent());
            article.setTimeDuration(articleDetails.getTimeDuration());
            article.setStatus(articleDetails.getStatus());
            article.setUpdatedAt(java.time.LocalDateTime.now());

            return articleRepository.save(article);
        }
        throw new RuntimeException("Article not found with id: " + id);
    }

    public void deleteArticle(Long id) {
        if (articleRepository.existsById(id)) {
            articleRepository.deleteById(id);
        } else {
            throw new RuntimeException("Article not found with id: " + id);
        }
    }

    // ============================================================================
    // PUBLISH/DRAFT OPERATIONS
    // ============================================================================

    public Article publishArticle(Long id) {
        Optional<Article> optionalArticle = articleRepository.findById(id);
        if (optionalArticle.isPresent()) {
            Article article = optionalArticle.get();
            article.publish();
            return articleRepository.save(article);
        }
        throw new RuntimeException("Article not found with id: " + id);
    }

    public Article saveDraft(Long id) {
        Optional<Article> optionalArticle = articleRepository.findById(id);
        if (optionalArticle.isPresent()) {
            Article article = optionalArticle.get();
            article.saveDraft();
            return articleRepository.save(article);
        }
        throw new RuntimeException("Article not found with id: " + id);
    }

    // ============================================================================
    // FILE UPLOAD OPERATIONS
    // ============================================================================

    public String uploadThumbnail(Long id, MultipartFile file) {
        String fileUrl = "https://cloudinary.com/uploads/" + id + "/thumbnail/" + file.getOriginalFilename();

        Optional<Article> optionalArticle = articleRepository.findById(id);
        if (optionalArticle.isPresent()) {
            Article article = optionalArticle.get();
            article.setThumbnailUrl(fileUrl);
            article.setUpdatedAt(java.time.LocalDateTime.now());
            articleRepository.save(article);
        }

        return fileUrl;
    }

    // ============================================================================
    // PRIVATE HELPER METHODS
    // ============================================================================

    private boolean applySearchCriteria(Article article, String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return true;
        }

        String searchLower = searchText.toLowerCase();

        // Search by ID
        if (article.getId() != null && article.getId().toString().contains(searchText)) {
            return true;
        }

        // Search by title
        if (article.getTitle() != null && article.getTitle().toLowerCase().contains(searchLower)) {
            return true;
        }

        // Search by description
        if (article.getDescription() != null && article.getDescription().toLowerCase().contains(searchLower)) {
            return true;
        }

        return false;
    }

    private boolean applyFilterCriteria(Article article, List<AgeGroup> filterAgeGroups) {

        if (filterAgeGroups != null && !filterAgeGroups.isEmpty() &&
                !filterAgeGroups.contains(article.getAgeGroup())) {
            return false;
        }

        return true;
    }

    private AdminArticleDTO convertToAdminArticleDTO(Article article) {
        AdminArticleDTO dto = new AdminArticleDTO();
        dto.setId(article.getId());
        dto.setCoins(article.getCoins());
        dto.setTitle(article.getTitle());
        dto.setDescription(article.getDescription());
        dto.setTags(article.getTags());
        dto.setAgeGroup(article.getAgeGroup());
        dto.setThumbnailUrl(article.getThumbnailUrl());
        dto.setContent(article.getContent());
        dto.setTime_duration(article.getTimeDuration());
        dto.setStatus(article.getStatus().getDisplayName());
        dto.setCreatedAt(article.getCreatedAt());
        dto.setUpdatedAt(article.getUpdatedAt());
        dto.setPublishedAt(article.getPublishedAt());
        return dto;
    }

    // ============================================================================
    // ADDITIONAL UTILITY METHODS
    // ============================================================================

    public List<Article> getAllArticles() {
        return articleRepository.findAll();
    }

    public Page<Article> getAllArticles(int page, int size, String sortBy, String sortDir) {
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return articleRepository.findAll(pageable);
    }

    public List<Article> searchArticles(String keyword) {
        return (keyword == null || keyword.trim().isEmpty()) ? articleRepository.findAll()
                : articleRepository.findByKeyword(keyword);
    }

    public List<Article> getArticlesByAgeGroup(AgeGroup ageGroup) {
        return ageGroup == null ? articleRepository.findAll() : articleRepository.findByAgeGroup(ageGroup);
    }

    public List<Article> getArticlesByStatus(Article.Status status) {
        return status == null ? articleRepository.findAll() : articleRepository.findByStatus(status);
    }

    public List<String> getDurationCategories() {
        return articleRepository.findDistinctDurationCategories();
    }
}
