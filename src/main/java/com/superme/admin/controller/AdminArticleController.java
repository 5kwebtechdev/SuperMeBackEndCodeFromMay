package com.superme.admin.controller;

import com.superme.dto.AdminArticleDTO;
import com.superme.dto.AdminArticleOverviewResponseDTO;
import com.superme.dto.ArticleStatistics;
import com.superme.enums.AgeGroup;
import com.superme.model.Article;
import com.superme.service.AdminArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/admin/articles")
@CrossOrigin(origins = "*")
public class AdminArticleController {

    @Autowired
    private AdminArticleService adminArticleService;

    // ============================================================================
    // MAIN ADMIN OVERVIEW ENDPOINT
    // ============================================================================

    @GetMapping("/overview")
    public ResponseEntity<AdminArticleOverviewResponseDTO> getAdminArticleOverview(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String searchText,
            @RequestParam(required = false) List<AgeGroup> filterAgeGroups) {

        AdminArticleOverviewResponseDTO response = adminArticleService.getAdminArticleOverview(
                page, size, sortBy, sortDir, searchText, filterAgeGroups);

        return ResponseEntity.ok(response);
    }

    // ============================================================================
    // DATA TABLE ENDPOINTS
    // ============================================================================

    @GetMapping("/datatable")
    public ResponseEntity<Page<AdminArticleDTO>> getArticleDataTable(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String searchText,
            @RequestParam(required = false) List<AgeGroup> filterAgeGroups) {

        Page<AdminArticleDTO> articles = adminArticleService.getArticleDataTable(
                page, size, sortBy, sortDir, searchText, filterAgeGroups);

        return ResponseEntity.ok(articles);
    }

    // ============================================================================
    // STATS AND FILTER OPTIONS
    // ============================================================================

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

    // ============================================================================
    // SEARCH SUGGESTIONS
    // ============================================================================

    @GetMapping("/search-suggestions")
    public ResponseEntity<List<String>> getSearchSuggestions(@RequestParam String query) {
        List<String> suggestions = adminArticleService.getSearchSuggestions(query);
        return ResponseEntity.ok(suggestions);
    }

    // ============================================================================
    // CRUD OPERATIONS
    // ============================================================================

    @PostMapping("/save-article")
    public ResponseEntity<AdminArticleDTO> createArticle(@RequestBody AdminArticleDTO article) {
        try {
            AdminArticleDTO createdArticle = adminArticleService.createArticle(article);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdArticle);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminArticleDTO> getArticleById(@PathVariable Long id) {
        Optional<AdminArticleDTO> article = adminArticleService.getArticleById(id);
        return article.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Article> updateArticle(@PathVariable Long id, @RequestBody Article articleDetails) {
        try {
            Article updatedArticle = adminArticleService.updateArticle(id, articleDetails);
            return ResponseEntity.ok(updatedArticle);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        try {
            adminArticleService.deleteArticle(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ============================================================================
    // PUBLISH/DRAFT OPERATIONS
    // ============================================================================

    @PostMapping("/{id}/publish")
    public ResponseEntity<Article> publishArticle(@PathVariable Long id) {
        try {
            Article publishedArticle = adminArticleService.publishArticle(id);
            return ResponseEntity.ok(publishedArticle);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/draft")
    public ResponseEntity<Article> saveDraft(@PathVariable Long id) {
        try {
            Article draftArticle = adminArticleService.saveDraft(id);
            return ResponseEntity.ok(draftArticle);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ============================================================================
    // FILE UPLOAD OPERATIONS
    // ============================================================================

    @PostMapping("/{id}/thumbnail")
    public ResponseEntity<Map<String, String>> uploadThumbnail(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        try {
            String thumbnailUrl = adminArticleService.uploadThumbnail(id, file);
            Map<String, String> response = Map.of("thumbnailUrl", thumbnailUrl);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    // ============================================================================
    // SEARCH AND FILTER OPERATIONS
    // ============================================================================

    @GetMapping("/search")
    public ResponseEntity<List<Article>> searchArticles(@RequestParam(required = false) String keyword) {
        List<Article> articles = adminArticleService.searchArticles(keyword);
        return ResponseEntity.ok(articles);
    }

    @GetMapping("/by-age-group")
    public ResponseEntity<List<Article>> getArticlesByAgeGroup(
            @RequestParam(required = false) AgeGroup ageGroup) {
        List<Article> articles = adminArticleService.getArticlesByAgeGroup(ageGroup);
        return ResponseEntity.ok(articles);
    }

    @GetMapping("/by-status")
    public ResponseEntity<List<Article>> getArticlesByStatus(@RequestParam(required = false) Article.Status status) {
        List<Article> articles = adminArticleService.getArticlesByStatus(status);
        return ResponseEntity.ok(articles);
    }

    // ============================================================================
    // BULK OPERATIONS
    // ============================================================================

    @PostMapping("/bulk-create")
    public ResponseEntity<List<AdminArticleDTO>> createBulkArticles(@RequestBody List<AdminArticleDTO> articles) {
        try {
            List<AdminArticleDTO> createdArticles = articles.stream()
                    .map(adminArticleService::createArticle)
                    .toList();
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

    // ============================================================================
    // VALIDATION ENDPOINTS
    // ============================================================================

    @GetMapping("/validate-title")
    public ResponseEntity<Map<String, Boolean>> validateTitle(@RequestParam String title) {
        boolean isUnique = adminArticleService.getAllArticles().stream()
                .noneMatch(article -> article.getTitle().equalsIgnoreCase(title));
        Map<String, Boolean> response = Map.of("isUnique", isUnique);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/duration-categories")
    public ResponseEntity<List<String>> getDurationCategories() {
        List<String> durationCategories = adminArticleService.getDurationCategories();
        return ResponseEntity.ok(durationCategories);
    }

    // ============================================================================
    // EXCEPTION HANDLING
    // ============================================================================

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException e) {
        Map<String, String> error = Map.of("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        Map<String, String> error = Map.of("error", "Internal server error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
