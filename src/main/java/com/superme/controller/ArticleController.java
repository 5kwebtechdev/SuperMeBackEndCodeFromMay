package com.superme.controller;

import com.superme.config.FileStorageConfig;
import com.superme.dto.ArticleResponseDto;
import com.superme.exception.ResourceNotFoundException;
import com.superme.model.User;
import com.superme.service.ArticleService;
import com.superme.service.UserService;
import com.superme.util.UserJwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    @Autowired
    private UserService userService;

    @PostMapping("/{id}/complete")
    public ResponseEntity<ArticleResponseDto> completeArticle(@PathVariable Long id,
//                                                              @RequestParam Long userId,
                                                              @RequestHeader("Authorization") String token
    ) {
        Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        try {
            ArticleResponseDto article = articleService.markArticleAsRead(id, user);
            return ResponseEntity.ok(article);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Autowired
    private FileStorageConfig fileStorageConfig;

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {

        try {
            Path filePath = Paths.get(fileStorageConfig.getUploadDir())
                    .resolve(fileName)
                    .normalize();

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                throw new RuntimeException("File not found");
            }

            String contentType = "image/png"; // default

            // optional: detect type dynamically
            try {
                contentType = Files.probeContentType(filePath);
            } catch (Exception ignored) {}

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            throw new RuntimeException("Error reading file", e);
        }
    }





}