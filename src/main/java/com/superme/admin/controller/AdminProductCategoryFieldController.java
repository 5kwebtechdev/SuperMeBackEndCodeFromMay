package com.superme.admin.controller;

import com.superme.dto.*;
import com.superme.service.ProductCategoryFieldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/product-category-fields")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductCategoryFieldController {

    private final ProductCategoryFieldService fieldService;

    // For Add/Edit Product to know which fields to show for a category
    @GetMapping("/{categoryId}/fields")
    public ResponseEntity<List<ProductCategoryFieldDto>> getFieldsByCategory(
            @PathVariable Long categoryId) {
        return ResponseEntity.ok(fieldService.getFieldsByCategory(categoryId));
    }

    @GetMapping("/field/{fieldId}")
    public ResponseEntity<ProductCategoryFieldDto> getFieldById(@PathVariable Long fieldId) {
        return ResponseEntity.ok(fieldService.getFieldById(fieldId));
    }

    @PostMapping("/{categoryId}/fields")
    public ResponseEntity<ProductCategoryFieldDto> createField(
            @PathVariable Long categoryId,
            @RequestBody ProductCategoryFieldCreateRequest request) {
        return ResponseEntity.ok(fieldService.createField(categoryId, request));
    }

    @PutMapping("/field/{fieldId}")
    public ResponseEntity<ProductCategoryFieldDto> updateField(
            @PathVariable Long fieldId,
            @RequestBody ProductCategoryFieldUpdateRequest request) {
        return ResponseEntity.ok(fieldService.updateField(fieldId, request));
    }

    @DeleteMapping("/field/{fieldId}")
    public ResponseEntity<Void> deleteField(@PathVariable Long fieldId) {
        fieldService.deleteField(fieldId);
        return ResponseEntity.noContent().build();
    }
}
