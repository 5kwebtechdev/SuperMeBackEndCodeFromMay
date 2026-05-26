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

    @Autowired
    private ArticleFileStorageService articleFileStorageService;

    // Extracts bare filename from whatever is stored (full path, relative path, or already just filename).
    private String extractFilename(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) return null;
        int lastSlash = Math.max(storedPath.lastIndexOf('/'), storedPath.lastIndexOf('\\'));
        return lastSlash >= 0 ? storedPath.substring(lastSlash + 1) : storedPath;
    }

    // ============================================================================
    // MAIN ADMIN SCREEN METHODS
    // ============================================================================

    public AdminArticleOverviewResponseDTO getAdminArticleOverview(
            int page, int size, String sortBy, String sortDir,
            String searchText, List<AgeGroup> filterAgeGroups, String status) {

        Page<AdminArticleDTO> dataTable = getArticleDataTable(
                page, size, sortBy, sortDir, searchText, filterAgeGroups, status);

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
            String searchText, List<AgeGroup> filterAgeGroups, String status) {

        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);

        Article.Status statusFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFilter = Article.Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        final Article.Status finalStatusFilter = statusFilter;

        List<AdminArticleDTO> filteredArticles = articleRepository.findAllByDeletedFalse(sort).stream()
                .filter(article -> finalStatusFilter == null || article.getStatus() == finalStatusFilter)
                .filter(article -> applySearchCriteria(article, searchText))
                .filter(article -> applyFilterCriteria(article, filterAgeGroups))
                .map(this::convertToAdminArticleDTO)
                .collect(Collectors.toList());

        Pageable pageable = PageRequest.of(page, size, sort);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + size, filteredArticles.size());
        List<AdminArticleDTO> pageContent = start >= filteredArticles.size()
                ? new ArrayList<>()
                : filteredArticles.subList(start, end);

        return new PageImpl<>(pageContent, pageable, filteredArticles.size());
    }

    // ============================================================================
    // STAT CARDS
    // ============================================================================

    public ArticleStatistics getArticleStatsForCards() {
        long totalArticles = articleRepository.countByDeletedFalse();
        long publishedArticles = articleRepository.countByStatusAndDeletedFalse(Article.Status.PUBLISHED);
        long draftArticles = articleRepository.countByStatusAndDeletedFalse(Article.Status.DRAFT);

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

            List<Article> activeArticles = articleRepository.findAllByDeletedFalse();

            List<String> titleSuggestions = activeArticles.stream()
                    .map(Article::getTitle)
                    .filter(title -> title != null && title.toLowerCase().contains(searchQuery))
                    .distinct()
                    .limit(5)
                    .collect(Collectors.toList());
            suggestions.addAll(titleSuggestions);

            try {
                Long id = Long.parseLong(query);
                articleRepository.findByIdAndDeletedFalse(id)
                        .ifPresent(article -> suggestions.add("ID: " + id + " - " + article.getTitle()));
            } catch (NumberFormatException ignored) {
            }

            List<String> descSuggestions = activeArticles.stream()
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

//    private Article convertToEntity(AdminArticleDTO dto) {
//        Article article = new Article();
//
//        // Check if it's an update
//        if (dto.getId() != null ) {
//            article.setId(dto.getId());
//            article.setCreatedAt(dto.getCreatedAt()); // Keep original createdAt
//        } else {
//            // New article
//            article.setCreatedAt(LocalDateTime.now());
//        }
//
//        // If updating existing article (PUT), ID should be copied
//        article.setId(dto.getId());
//
//        article.setTitle(dto.getTitle());
//        article.setDescription(dto.getDescription());
//        article.setCoins(dto.getCoins());
//        article.setContent(dto.getContent());
//        article.setThumbnailUrl(dto.getThumbnailUrl());
//        article.setDurationMinutes(dto.getDurationMinutes());
//
//        // Tags (List<String>)
//        article.setTags(dto.getTags());
//
//        // AgeGroup (String -> Enum)
//        if (dto.getAgeGroup() != null) {
//            article.setAgeGroup(dto.getAgeGroup());
//        }
//
//        // Status (String -> Enum)
//        if (dto.getStatus() != null) {
//            article.setStatus(Article.Status.valueOf(dto.getStatus()));
//        }
//
//        article.setUpdatedAt(LocalDateTime.now());
//        article.setPublishedAt(dto.getPublishedAt());
//
//        return article;
//    }
//
//    private AdminArticleDTO convertToDTO(Article article) {
//        AdminArticleDTO dto = new AdminArticleDTO();
//
//        dto.setId(article.getId());
//        dto.setTitle(article.getTitle());
//        dto.setCoins(article.getCoins());
//        dto.setDescription(article.getDescription());
//        dto.setContent(article.getContent());
//        dto.setThumbnailUrl(article.getThumbnailUrl());
//        dto.setThumbnailUrl(article.getTimeDuration());
//
//        // Tags
//        dto.setTags(article.getTags());
//
//        // AgeGroup enum -> String
//        if (article.getAgeGroup() != null) {
//            dto.setAgeGroup(article.getAgeGroup());
//
//        }
//
//        // Status enum -> String
//        if (article.getStatus() != null) {
//            dto.setStatus(article.getStatus().name());
//        }
//
//        // Dates
//        dto.setCreatedAt(article.getCreatedAt());
//        dto.setUpdatedAt(article.getUpdatedAt());
//        dto.setPublishedAt(article.getPublishedAt());
//
//        return dto;
//    }



    public Optional<AdminArticleDTO> getArticleById(Long id) {
        return articleRepository.findByIdAndDeletedFalse(id)
                .map(this::convertToAdminArticleDTO);
    }

//    public Article updateArticle(Long id, Article articleDetails) {
//        Optional<Article> optionalArticle = articleRepository.findById(id);
//        if (optionalArticle.isPresent()) {
//            Article article = optionalArticle.get();
//            article.setTitle(articleDetails.getTitle());
//            article.setCoins(articleDetails.getCoins());
//            article.setDescription(articleDetails.getDescription());
//            article.setTags(articleDetails.getTags());
//            article.setAgeGroup(articleDetails.getAgeGroup());
//            article.setThumbnailUrl(articleDetails.getThumbnailUrl());
//            article.setContent(articleDetails.getContent());
//            article.setTimeDuration(articleDetails.getTimeDuration());
//            article.setStatus(articleDetails.getStatus());
//            article.setUpdatedAt(java.time.LocalDateTime.now());
//
//            return articleRepository.save(article);
//        }
//        throw new RuntimeException("Article not found with id: " + id);
//    }







    public void deleteArticle(Long id) {
        Article article = articleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Article not found with id: " + id));
        article.softDelete();
        articleRepository.save(article);
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

//    public String uploadThumbnail(Long id, MultipartFile file) {
//        String fileUrl = "https://cloudinary.com/uploads/" + id + "/thumbnail/" + file.getOriginalFilename();
//
//        Optional<Article> optionalArticle = articleRepository.findById(id);
//        if (optionalArticle.isPresent()) {
//            Article article = optionalArticle.get();
//            article.setThumbnailUrl(fileUrl);
//            article.setUpdatedAt(java.time.LocalDateTime.now());
//            articleRepository.save(article);
//        }
//
//        return fileUrl;
//    }

    // ============================================================================
    // PRIVATE HELPER METHODS
    // ============================================================================

    // ============================================================================
    // BLOCKS JSON → HTML (used on update)
    // ============================================================================

    public String blocksJsonToHtml(String blocksJson) {
        if (blocksJson == null || blocksJson.isBlank()) return "";
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(blocksJson);
            if (!root.isArray()) return blocksJson;

            StringBuilder html = new StringBuilder();
            for (com.fasterxml.jackson.databind.JsonNode node : root) {
                String type    = node.path("type").asText("");
                String content = node.path("content").asText("");
                switch (type) {
                    case "heading"   -> html.append("<h2>").append(content).append("</h2>\r\n");
                    case "paragraph" -> html.append("<p>").append(content).append("</p>\r\n");
                    case "points"    -> html.append("<li>").append(content).append("</li>\r\n");
                    case "callout"   -> html.append("<div class=\"callout\">").append(content).append("</div>\r\n");
                    case "image"     -> html.append("<img src=\"").append(content).append("\" />\r\n");
                    default          -> html.append("<p>").append(content).append("</p>\r\n");
                }
            }
            return html.toString().trim();
        } catch (Exception e) {
            return blocksJson;
        }
    }

    // ============================================================================
    // HTML CONTENT → BLOCK PARSER
    // ============================================================================

    public List<com.superme.dto.ContentBlock> parseContentToBlocks(String html) {
        List<com.superme.dto.ContentBlock> blocks = new ArrayList<>();
        if (html == null || html.isBlank()) return blocks;

        // If content is still stored as a JSON block array (migration fallback), parse it directly
        if (html.trim().startsWith("[")) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(html);
                if (root.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode node : root) {
                        blocks.add(new com.superme.dto.ContentBlock(
                                node.path("type").asText(""),
                                node.path("content").asText("")
                        ));
                    }
                    return blocks;
                }
            } catch (Exception ignored) {}
        }

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "<h2(?:[^>]*)>(.*?)</h2>" +
            "|<p(?:[^>]*)>(.*?)</p>" +
            "|<li(?:[^>]*)>(.*?)</li>" +
            "|<div[^>]+class=[\"']callout[\"'][^>]*>(.*?)</div>" +
            "|<img[^>]+src=[\"']([^\"']+)[\"'][^>]*/?>",
            java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE
        );

        java.util.regex.Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                blocks.add(new com.superme.dto.ContentBlock("heading",   stripHtml(matcher.group(1))));
            } else if (matcher.group(2) != null) {
                blocks.add(new com.superme.dto.ContentBlock("paragraph", stripHtml(matcher.group(2))));
            } else if (matcher.group(3) != null) {
                blocks.add(new com.superme.dto.ContentBlock("points",    stripHtml(matcher.group(3))));
            } else if (matcher.group(4) != null) {
                blocks.add(new com.superme.dto.ContentBlock("callout",   stripHtml(matcher.group(4))));
            } else if (matcher.group(5) != null) {
                blocks.add(new com.superme.dto.ContentBlock("image",     matcher.group(5).trim()));
            }
        }
        return blocks;
    }

    private String stripHtml(String input) {
        if (input == null) return "";
        return input.replaceAll("<[^>]+>", "").replaceAll("&nbsp;", " ").trim();
    }

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
        dto.setThumbnailUrl(articleFileStorageService.getThumbnailUrl(extractFilename(article.getThumbnailUrl())));
        dto.setAttachmentUrl(articleFileStorageService.getAttachmentUrl(extractFilename(article.getAttachmentUrl())));
        dto.setContent(article.getContent());
        dto.setDurationMinutes(article.getDurationMinutes());
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
        return articleRepository.findAllByDeletedFalse();
    }

    public Page<Article> getAllArticles(int page, int size, String sortBy, String sortDir) {
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return articleRepository.findAll(pageable);
    }

    public List<Article> searchArticles(String keyword) {
        return (keyword == null || keyword.trim().isEmpty()) ? articleRepository.findAllByDeletedFalse()
                : articleRepository.findByKeyword(keyword);
    }

    public List<Article> getArticlesByAgeGroup(AgeGroup ageGroup) {
        return ageGroup == null ? articleRepository.findAllByDeletedFalse() : articleRepository.findByAgeGroupAndDeletedFalse(ageGroup);
    }

    public List<Article> getArticlesByStatus(Article.Status status) {
        return status == null ? articleRepository.findAllByDeletedFalse() : articleRepository.findByStatusAndDeletedFalse(status);
    }

    public List<Integer> getDurationMinutes() {
        return articleRepository.findDistinctDurationMinutes();
    }

















    // Add these methods to your AdminArticleService







    public void updateArticleContent(Long articleId, String content) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article not found"));
        article.setContent(content);
        articleRepository.save(article);
    }





























// Add these methods to your AdminArticleService class

    public Article getArticleEntityById(Long id) {
        return articleRepository.findByIdAndDeletedFalse(id).orElse(null);
    }

    public Article updateArticleEntity(Long id, Article article) {
        article.setId(id);
        article.setUpdatedAt(LocalDateTime.now());
        return articleRepository.save(article);
    }

    public AdminArticleDTO convertToDTO(Article article) {
        if (article == null) return null;

        AdminArticleDTO dto = new AdminArticleDTO();
        dto.setId(article.getId());
        dto.setTitle(article.getTitle());
        dto.setDescription(article.getDescription());
        dto.setCoins(article.getCoins());
        dto.setContent(article.getContent());
        dto.setThumbnailUrl(articleFileStorageService.getThumbnailUrl(extractFilename(article.getThumbnailUrl())));
        dto.setAttachmentUrl(articleFileStorageService.getAttachmentUrl(extractFilename(article.getAttachmentUrl())));
        dto.setDurationMinutes(article.getDurationMinutes());
        dto.setTags(article.getTags());
        dto.setAgeGroup(article.getAgeGroup());
        dto.setStatus(article.getStatus() != null ? article.getStatus().name() : "DRAFT");
        dto.setCreatedAt(article.getCreatedAt());
        dto.setUpdatedAt(article.getUpdatedAt());
        dto.setPublishedAt(article.getPublishedAt());

        return dto;
    }

    public Article convertToEntity(AdminArticleDTO dto) {
        if (dto == null) return null;

        Article article = new Article();
        article.setId(dto.getId());
        article.setTitle(dto.getTitle());
        article.setDescription(dto.getDescription());
        article.setCoins(dto.getCoins());
        article.setContent(dto.getContent());
        article.setThumbnailUrl(dto.getThumbnailUrl());
        article.setDurationMinutes(dto.getDurationMinutes());
        article.setTags(dto.getTags());
        article.setAgeGroup(dto.getAgeGroup());

        if (dto.getStatus() != null) {
            article.setStatus(Article.Status.valueOf(dto.getStatus()));
        }

        article.setCreatedAt(dto.getCreatedAt());
        article.setUpdatedAt(dto.getUpdatedAt());
        article.setPublishedAt(dto.getPublishedAt());

        return article;
    }



























    // ============================================================================
// FILE UPLOAD OPERATIONS - USING PROJECT UPLOADS FOLDER
// ============================================================================

    public String uploadThumbnail(Long articleId, MultipartFile file) {
        String filename = articleFileStorageService.saveThumbnail(file);
        articleRepository.findByIdAndDeletedFalse(articleId).ifPresent(article -> {
            article.setThumbnailUrl(filename);
            articleRepository.save(article);
        });
        return articleFileStorageService.getThumbnailUrl(filename);
    }

    public String uploadAttachment(Long articleId, MultipartFile file) {
        String filename = articleFileStorageService.saveAttachment(file);
        articleRepository.findByIdAndDeletedFalse(articleId).ifPresent(article -> {
            article.setAttachmentUrl(filename);
            articleRepository.save(article);
        });
        return articleFileStorageService.getAttachmentUrl(filename);
    }

    public List<String> uploadContentImages(Long articleId, List<MultipartFile> files) {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            String filename = articleFileStorageService.saveContentImage(file);
            urls.add(articleFileStorageService.getContentImageUrl(filename));
        }
        return urls;
    }



}
