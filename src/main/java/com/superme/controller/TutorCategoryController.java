package com.superme.controller;

import com.superme.model.TutorCategory;
import com.superme.service.TutorCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tutors/categories")
@RequiredArgsConstructor
@Slf4j
public class TutorCategoryController {

    private final TutorCategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<TutorCategory>> getAllCategories() {
        log.info("GET /v1/categories");
        return ResponseEntity.ok(categoryService.getAllActiveCategories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TutorCategory> getCategoryById(@PathVariable Long id) {
        log.info("GET /api/v1/categories/{}", id);
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @GetMapping("/key/{categoryKey}")
    public ResponseEntity<TutorCategory> getCategoryByKey(@PathVariable String categoryKey) {
        log.info("GET /api/v1/categories/key/{}", categoryKey);
        return ResponseEntity.ok(categoryService.getCategoryByKey(categoryKey));
    }

    @PostMapping
    public ResponseEntity<TutorCategory> createCategory(@RequestBody TutorCategory category) {
        log.info("POST /api/v1/categories - Creating: {}", category.getCategoryName());
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TutorCategory> updateCategory(@PathVariable Long id, @RequestBody TutorCategory categoryDetails) {
        log.info("PUT /api/v1/categories/{}", id);
        return ResponseEntity.ok(categoryService.updateCategory(id, categoryDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        log.info("DELETE /api/v1/categories/{}", id);
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
