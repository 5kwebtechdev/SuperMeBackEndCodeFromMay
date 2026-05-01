package com.superme.controller;

import com.superme.dto.ArticleResponseDto;
import com.superme.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/articles")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE, RequestMethod.OPTIONS })
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @GetMapping
    public ResponseEntity<?> getAllArticles() {


        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        String userIdStr = (String) auth.getPrincipal();
        String role = auth.getAuthorities().iterator().next().getAuthority();

        // 🚫 Block ADMIN
        if ("ROLE_ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body("Admins are not allowed to access this endpoint");
        }


        List<ArticleResponseDto> articles = articleService.getAllArticles();
        return ResponseEntity.ok(articles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponseDto> getArticle(@PathVariable Long id) {
        return articleService.getArticleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ArticleResponseDto> completeArticle(@PathVariable Long id, @RequestParam Long userId) {
        try {
            ArticleResponseDto article = articleService.markArticleAsRead(id, userId);
            return ResponseEntity.ok(article);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}