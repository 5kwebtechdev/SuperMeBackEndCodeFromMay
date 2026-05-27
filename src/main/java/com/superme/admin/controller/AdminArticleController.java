//package com.superme.admin.controller;
//
//import com.superme.dto.AdminArticleDTO;
//import com.superme.dto.AdminArticleOverviewResponseDTO;
//import com.superme.dto.ArticleStatistics;
//import com.superme.enums.AgeGroup;
//import com.superme.model.Article;
//import com.superme.service.AdminArticleService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Page;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
//@RestController
//@RequestMapping("/admin/articles")
//@CrossOrigin(origins = "*")
//public class AdminArticleController {
//
//    @Autowired
//    private AdminArticleService adminArticleService;
//
//    // ============================================================================
//    // MAIN ADMIN OVERVIEW ENDPOINT
//    // ============================================================================
//
//    @GetMapping("/overview")
//    public ResponseEntity<AdminArticleOverviewResponseDTO> getAdminArticleOverview(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size,
//            @RequestParam(defaultValue = "createdAt") String sortBy,
//            @RequestParam(defaultValue = "desc") String sortDir,
//            @RequestParam(required = false) String searchText,
//            @RequestParam(required = false) List<AgeGroup> filterAgeGroups) {
//
//        AdminArticleOverviewResponseDTO response = adminArticleService.getAdminArticleOverview(
//                page, size, sortBy, sortDir, searchText, filterAgeGroups);
//
//        return ResponseEntity.ok(response);
//    }
//
//    // ============================================================================
//    // DATA TABLE ENDPOINTS
//    // ============================================================================
//
//    @GetMapping("/datatable")
//    public ResponseEntity<Page<AdminArticleDTO>> getArticleDataTable(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size,
//            @RequestParam(defaultValue = "createdAt") String sortBy,
//            @RequestParam(defaultValue = "desc") String sortDir,
//            @RequestParam(required = false) String searchText,
//            @RequestParam(required = false) List<AgeGroup> filterAgeGroups) {
//
//        Page<AdminArticleDTO> articles = adminArticleService.getArticleDataTable(
//                page, size, sortBy, sortDir, searchText, filterAgeGroups);
//
//        return ResponseEntity.ok(articles);
//    }
//
//    // ============================================================================
//    // STATS AND FILTER OPTIONS
//    // ============================================================================
//
//    @GetMapping("/stats")
//    public ResponseEntity<ArticleStatistics> getArticleStats() {
//        ArticleStatistics stats = adminArticleService.getArticleStatsForCards();
//        return ResponseEntity.ok(stats);
//    }
//
//    @GetMapping("/filter-options")
//    public ResponseEntity<AdminArticleOverviewResponseDTO.FilterOptions> getFilterOptions() {
//        AdminArticleOverviewResponseDTO.FilterOptions options = adminArticleService.getFilterOptions();
//        return ResponseEntity.ok(options);
//    }
//
//    // ============================================================================
//    // SEARCH SUGGESTIONS
//    // ============================================================================
//
//    @GetMapping("/search-suggestions")
//    public ResponseEntity<List<String>> getSearchSuggestions(@RequestParam String query) {
//        List<String> suggestions = adminArticleService.getSearchSuggestions(query);
//        return ResponseEntity.ok(suggestions);
//    }
//
//    // ============================================================================
//    // CRUD OPERATIONS
//    // ============================================================================
//
//    @PostMapping("/save-article")
//    public ResponseEntity<AdminArticleDTO> createArticle(@RequestBody AdminArticleDTO article) {
//        try {
//            AdminArticleDTO createdArticle = adminArticleService.createArticle(article);
//            return ResponseEntity.status(HttpStatus.CREATED).body(createdArticle);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
//        }
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<AdminArticleDTO> getArticleById(@PathVariable Long id) {
//        Optional<AdminArticleDTO> article = adminArticleService.getArticleById(id);
//        return article.map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }
//
//    @PutMapping("/{id}")
//    public ResponseEntity<Article> updateArticle(@PathVariable Long id, @RequestBody Article articleDetails) {
//        try {
//            Article updatedArticle = adminArticleService.updateArticle(id, articleDetails);
//            return ResponseEntity.ok(updatedArticle);
//        } catch (RuntimeException e) {
//            return ResponseEntity.notFound().build();
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
//        }
//    }
//
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
//        try {
//            adminArticleService.deleteArticle(id);
//            return ResponseEntity.noContent().build();
//        } catch (RuntimeException e) {
//            return ResponseEntity.notFound().build();
//        }
//    }
//
//    // ============================================================================
//    // PUBLISH/DRAFT OPERATIONS
//    // ============================================================================
//
//    @PostMapping("/{id}/publish")
//    public ResponseEntity<Article> publishArticle(@PathVariable Long id) {
//        try {
//            Article publishedArticle = adminArticleService.publishArticle(id);
//            return ResponseEntity.ok(publishedArticle);
//        } catch (RuntimeException e) {
//            return ResponseEntity.notFound().build();
//        }
//    }
//
//    @PostMapping("/{id}/draft")
//    public ResponseEntity<Article> saveDraft(@PathVariable Long id) {
//        try {
//            Article draftArticle = adminArticleService.saveDraft(id);
//            return ResponseEntity.ok(draftArticle);
//        } catch (RuntimeException e) {
//            return ResponseEntity.notFound().build();
//        }
//    }
//
//    // ============================================================================
//    // FILE UPLOAD OPERATIONS
//    // ============================================================================
//
//    @PostMapping("/{id}/thumbnail")
//    public ResponseEntity<Map<String, String>> uploadThumbnail(
//            @PathVariable Long id,
//            @RequestParam("file") MultipartFile file) {
//
//        try {
//            String thumbnailUrl = adminArticleService.uploadThumbnail(id, file);
//            Map<String, String> response = Map.of("thumbnailUrl", thumbnailUrl);
//            return ResponseEntity.ok(response);
//        } catch (RuntimeException e) {
//            return ResponseEntity.notFound().build();
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
//        }
//    }
//
//    // ============================================================================
//    // SEARCH AND FILTER OPERATIONS
//    // ============================================================================
//
//    @GetMapping("/search")
//    public ResponseEntity<List<Article>> searchArticles(@RequestParam(required = false) String keyword) {
//        List<Article> articles = adminArticleService.searchArticles(keyword);
//        return ResponseEntity.ok(articles);
//    }
//
//    @GetMapping("/by-age-group")
//    public ResponseEntity<List<Article>> getArticlesByAgeGroup(
//            @RequestParam(required = false) AgeGroup ageGroup) {
//        List<Article> articles = adminArticleService.getArticlesByAgeGroup(ageGroup);
//        return ResponseEntity.ok(articles);
//    }
//
//    @GetMapping("/by-status")
//    public ResponseEntity<List<Article>> getArticlesByStatus(@RequestParam(required = false) Article.Status status) {
//        List<Article> articles = adminArticleService.getArticlesByStatus(status);
//        return ResponseEntity.ok(articles);
//    }
//
//    // ============================================================================
//    // BULK OPERATIONS
//    // ============================================================================
//
//    @PostMapping("/bulk-create")
//    public ResponseEntity<List<AdminArticleDTO>> createBulkArticles(@RequestBody List<AdminArticleDTO> articles) {
//        try {
//            List<AdminArticleDTO> createdArticles = articles.stream()
//                    .map(adminArticleService::createArticle)
//                    .toList();
//            return ResponseEntity.status(HttpStatus.CREATED).body(createdArticles);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
//        }
//    }
//
//    @DeleteMapping("/bulk-delete")
//    public ResponseEntity<Void> deleteBulkArticles(@RequestBody List<Long> ids) {
//        try {
//            ids.forEach(adminArticleService::deleteArticle);
//            return ResponseEntity.noContent().build();
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
//        }
//    }
//
//    // ============================================================================
//    // VALIDATION ENDPOINTS
//    // ============================================================================
//
//    @GetMapping("/validate-title")
//    public ResponseEntity<Map<String, Boolean>> validateTitle(@RequestParam String title) {
//        boolean isUnique = adminArticleService.getAllArticles().stream()
//                .noneMatch(article -> article.getTitle().equalsIgnoreCase(title));
//        Map<String, Boolean> response = Map.of("isUnique", isUnique);
//        return ResponseEntity.ok(response);
//    }
//
//    @GetMapping("/duration-categories")
//    public ResponseEntity<List<String>> getDurationCategories() {
//        List<String> durationCategories = adminArticleService.getDurationCategories();
//        return ResponseEntity.ok(durationCategories);
//    }
//
//    // ============================================================================
//    // EXCEPTION HANDLING
//    // ============================================================================
//
//    @ExceptionHandler(RuntimeException.class)
//    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException e) {
//        Map<String, String> error = Map.of("error", e.getMessage());
//        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
//    }
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<Map<String, String>> handleException(Exception e) {
//        Map<String, String> error = Map.of("error", "Internal server error");
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
//    }
//}
package com.superme.admin.controller;

import com.superme.dto.AdminArticleDTO;
import com.superme.dto.AdminArticleOverviewResponseDTO;
import com.superme.dto.ArticleStatistics;
import com.superme.dto.ContentBlock;
import com.superme.enums.AgeGroup;
import com.superme.model.Article;
import com.superme.service.AdminArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/articles")
@CrossOrigin(origins = "*")
public class AdminArticleController {

    @Autowired
    private AdminArticleService adminArticleService;

    @Autowired
    private com.superme.service.ArticleFileStorageService articleFileStorageService;

    // ============================================================================
    // SAVE ARTICLE - HANDLES MULTIPART FORM DATA (FIXED)
    // ============================================================================

    @PostMapping(value = "/save-article", consumes = {"multipart/form-data"})
    public ResponseEntity<?> saveArticle(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("ageGroup") String ageGroup,
            @RequestParam("durationMinutes") Integer durationMinutes,
            @RequestParam("coins") Integer coins,
            @RequestParam("status") String status,
            @RequestParam("content") String content,
            @RequestParam(value = "section", required = false) String section,
            @RequestParam(value = "useTagAsTitle", required = false) Boolean useTagAsTitle,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestParam(value = "attachment", required = false) MultipartFile attachment,
            @RequestParam(value = "contentImages", required = false) List<MultipartFile> contentImages) {

        try {
            // Create DTO from form parameters
            AdminArticleDTO dto = new AdminArticleDTO();
            dto.setTitle(title);
            dto.setDescription(description);
            dto.setCoins(coins);
            dto.setContent(content);
            dto.setDurationMinutes(durationMinutes);

            // Handle tags (comma-separated string to list)
            if (tags != null && !tags.isEmpty()) {
                List<String> tagList = Arrays.stream(tags.split(","))
                        .map(String::trim)
                        .filter(t -> !t.isEmpty())
                        .collect(Collectors.toList());
                dto.setTags(tagList);
            } else {
                dto.setTags(new ArrayList<>());
            }

            // Set age group
            try {
                dto.setAgeGroup(AgeGroup.valueOf(ageGroup));
            } catch (IllegalArgumentException e) {
                dto.setAgeGroup(AgeGroup.BELOW_11);
            }

            // Set status
            dto.setStatus(status);

            // Set optional fields
            dto.setSection(section != null ? section : "Discover Something New!");
            dto.setUseTagAsTitle(useTagAsTitle != null ? useTagAsTitle : false);

            // Call service to create article
            AdminArticleDTO createdArticle = adminArticleService.createArticle(dto);

            // Handle thumbnail upload if present
            if (thumbnail != null && !thumbnail.isEmpty()) {
                String thumbnailUrl = adminArticleService.uploadThumbnail(createdArticle.getId(), thumbnail);
                createdArticle.setThumbnailUrl(thumbnailUrl);
            }

            // Handle attachment if present
            if (attachment != null && !attachment.isEmpty()) {
                String attachmentUrl = adminArticleService.uploadAttachment(createdArticle.getId(), attachment);
                createdArticle.setAttachmentUrl(attachmentUrl);
            }

            // Handle content images if present
            if (contentImages != null && !contentImages.isEmpty()) {
                List<String> contentImageUrls = adminArticleService.uploadContentImages(createdArticle.getId(), contentImages);
                // Replace placeholder images in content
                String updatedContent = replaceImagePlaceholders(content, contentImageUrls);
                createdArticle.setContent(updatedContent);
                adminArticleService.updateArticleContent(createdArticle.getId(), updatedContent);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(createdArticle);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to create article: " + e.getMessage()));
        }
    }

    private String replaceImagePlaceholders(String content, List<String> imageUrls) {
        if (content == null || imageUrls == null || imageUrls.isEmpty()) {
            return content;
        }

        String updatedContent = content;
        for (int i = 0; i < imageUrls.size(); i++) {
            updatedContent = updatedContent.replace("CONTENT_IMAGE_" + i, imageUrls.get(i));
        }
        return updatedContent;
    }

    // ============================================================================
    // UPDATED GET ARTICLE BY ID - WITH PROPER RESPONSE
    // ============================================================================

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getArticleById(@PathVariable Long id) {
        try {
            Optional<AdminArticleDTO> articleOpt = adminArticleService.getArticleById(id);
            if (articleOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Article not found"));
            }

            AdminArticleDTO dto = articleOpt.get();
            List<ContentBlock> contentBlocks = adminArticleService.parseContentToBlocks(dto.getContent());

            Map<String, Object> data = new java.util.LinkedHashMap<>();
            data.put("id",             dto.getId());
            data.put("title",          dto.getTitle());
            data.put("description",    dto.getDescription());
            data.put("coins",          dto.getCoins());
            data.put("content",        contentBlocks);
            data.put("thumbnailUrl",   dto.getThumbnailUrl());
            data.put("attachmentUrl",  dto.getAttachmentUrl());
            data.put("durationMinutes", dto.getDurationMinutes());
            data.put("tags",           dto.getTags());
            data.put("ageGroup",       dto.getAgeGroup());
            data.put("status",         dto.getStatus());
            data.put("section",        dto.getSection());
            data.put("useTagAsTitle",  dto.getUseTagAsTitle());
            data.put("createdAt",      dto.getCreatedAt());
            data.put("updatedAt",      dto.getUpdatedAt());
            data.put("publishedAt",    dto.getPublishedAt());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", data,
                    "message", "Article retrieved successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ============================================================================
    // UPDATE ARTICLE WITH MULTIPART FORM DATA
    // ============================================================================

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<Map<String, Object>> updateArticle(
            @PathVariable Long id,
            @RequestParam(value = "title",          required = false) String title,
            @RequestParam(value = "description",    required = false) String description,
            @RequestParam(value = "ageGroup",       required = false) String ageGroup,
            @RequestParam(value = "durationMinutes",required = false) Integer durationMinutes,
            @RequestParam(value = "coins",          required = false) Integer coins,
            @RequestParam(value = "status",         required = false) String status,
            @RequestParam(value = "content",        required = false) String content,
            @RequestParam(value = "tag",            required = false) String tag,
            @RequestParam(value = "section",        required = false) String section,
            @RequestParam(value = "useTagAsTitle",  required = false) Boolean useTagAsTitle,
            @RequestParam(value = "thumbnail",      required = false) MultipartFile thumbnail) {

        try {
            Article existingArticle = adminArticleService.getArticleEntityById(id);
            if (existingArticle == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Article not found with id: " + id));
            }

            if (title != null)          existingArticle.setTitle(title);
            if (description != null)    existingArticle.setDescription(description);
            if (durationMinutes != null) existingArticle.setDurationMinutes(durationMinutes);
            if (coins != null)          existingArticle.setCoins(coins);

            if (ageGroup != null) {
                try {
                    existingArticle.setAgeGroup(AgeGroup.valueOf(ageGroup.toUpperCase()));
                } catch (IllegalArgumentException ignored) {}
            }

            if (status != null) {
                try {
                    existingArticle.setStatus(Article.Status.valueOf(status.toUpperCase()));
                } catch (IllegalArgumentException ignored) {}
            }

            // content arrives as a JSON block array — convert back to HTML before storing
            if (content != null) {
                String htmlContent = content.trim().startsWith("[")
                        ? adminArticleService.blocksJsonToHtml(content)
                        : content;
                existingArticle.setContent(htmlContent);
            }

            if (tag != null && !tag.isEmpty()) {
                List<String> tagList = Arrays.stream(tag.split(","))
                        .map(String::trim)
                        .filter(t -> !t.isEmpty())
                        .collect(Collectors.toList());
                existingArticle.setTags(tagList);
            }

            if (thumbnail != null && !thumbnail.isEmpty()) {
                String thumbnailUrl = adminArticleService.uploadThumbnail(id, thumbnail);
                existingArticle.setThumbnailUrl(thumbnailUrl);
            }

            Article updatedArticle = adminArticleService.updateArticleEntity(id, existingArticle);
            AdminArticleDTO responseDTO = adminArticleService.convertToDTO(updatedArticle);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", responseDTO,
                    "message", "Article updated successfully"
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "Failed to update article: " + e.getMessage()));
        }
    }

    // ============================================================================
    // OTHER EXISTING ENDPOINTS (keep your existing ones)
    // ============================================================================

    @GetMapping("/overview")
    public ResponseEntity<AdminArticleOverviewResponseDTO> getAdminArticleOverview(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortField,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String searchText,
            @RequestParam(required = false) List<AgeGroup> filterAgeGroups,
            @RequestParam(required = false) String status) {

        String resolvedSortBy = mapArticleSortField(sortField);
        String resolvedSortDir = (sortDir != null && !sortDir.isBlank()) ? sortDir.trim() : "desc";

        AdminArticleOverviewResponseDTO response = adminArticleService.getAdminArticleOverview(
                page, size, resolvedSortBy, resolvedSortDir, searchText, filterAgeGroups, status);
        return ResponseEntity.ok(response);
    }

    // Maps frontend sortField values to JPA entity field names
    private String mapArticleSortField(String sortField) {
        if (sortField == null || sortField.isBlank()) return "id";
        return switch (sortField.trim()) {
            case "id"           -> "id";
            case "title"        -> "title";
            case "timeDuration" -> "durationMinutes";
            case "ageGroup"     -> "ageGroup";
            case "status"       -> "status";
            case "createdAt"    -> "createdAt";
            case "updatedAt"    -> "updatedAt";
            default             -> "id";
        };
    }

    @GetMapping("/datatable")
    public ResponseEntity<Page<AdminArticleDTO>> getArticleDataTable(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String searchText,
            @RequestParam(required = false) List<AgeGroup> filterAgeGroups,
            @RequestParam(required = false) String status) {

        Page<AdminArticleDTO> articles = adminArticleService.getArticleDataTable(
                page, size, sortBy, sortDir, searchText, filterAgeGroups, status);
        return ResponseEntity.ok(articles);
    }

    @GetMapping("/stats")
    public ResponseEntity<ArticleStatistics> getArticleStats() {
        ArticleStatistics stats = adminArticleService.getArticleStatsForCards();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/filter-options")
    public ResponseEntity<AdminArticleOverviewResponseDTO.FilterOptions> getFilterOptions() {
        AdminArticleOverviewResponseDTO.FilterOptions options = adminArticleService.getFilterOptions();
        return ResponseEntity.ok(options);
    }

    @GetMapping("/search-suggestions")
    public ResponseEntity<List<String>> getSearchSuggestions(@RequestParam String query) {
        List<String> suggestions = adminArticleService.getSearchSuggestions(query);
        return ResponseEntity.ok(suggestions);
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<Map<String, Object>> publishArticle(@PathVariable Long id) {
        try {
            Article publishedArticle = adminArticleService.publishArticle(id);
            AdminArticleDTO responseDTO = adminArticleService.convertToDTO(publishedArticle);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", responseDTO,
                    "message", "Article published successfully"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Article not found with id: " + id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteArticle(@PathVariable Long id) {
        try {
            adminArticleService.deleteArticle(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Article deleted successfully"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Article not found with id: " + id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/bulk-create")
    public ResponseEntity<List<AdminArticleDTO>> createBulkArticles(@RequestBody List<AdminArticleDTO> articles) {
        try {
            List<AdminArticleDTO> createdArticles = articles.stream()
                    .map(adminArticleService::createArticle)
                    .collect(Collectors.toList());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdArticles);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/bulk-delete")
    public ResponseEntity<Void> deleteBulkArticles(@RequestBody List<Long> ids) {
        try {
            ids.forEach(adminArticleService::deleteArticle);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }


    @GetMapping("/download/thumbnail/{fileName:.+}")
    public ResponseEntity<Resource> downloadThumbnail(@PathVariable String fileName) {
        return serveFile(articleFileStorageService.getThumbnailsDir(), fileName, "inline");
    }

    @GetMapping("/download/attachment/{fileName:.+}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable String fileName) {
        return serveFile(articleFileStorageService.getAttachmentsDir(), fileName, "attachment");
    }

    @GetMapping("/download/content/{fileName:.+}")
    public ResponseEntity<Resource> downloadContentImage(@PathVariable String fileName) {
        return serveFile(articleFileStorageService.getContentDir(), fileName, "inline");
    }

    private ResponseEntity<Resource> serveFile(String directory, String fileName, String disposition) {
        try {
            Path path = Paths.get(directory).resolve(fileName).normalize();
            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }











}