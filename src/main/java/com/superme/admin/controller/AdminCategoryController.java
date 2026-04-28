package com.superme.admin.controller;

import com.superme.dto.CategoryRequestDTO;
import com.superme.dto.CategoryResponseDTO;
import com.superme.service.AcademicCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/category")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final AcademicCategoryService academicCategoryService;

    @Operation(
            summary = "Create new category",
            description = "Creates a new category. Requires admin authentication token.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<CategoryResponseDTO> createCategory(
            @Valid @RequestBody CategoryRequestDTO requestDTO) {
        CategoryResponseDTO category = academicCategoryService.createCategory(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }

    @Operation(
            summary = "Get all categories",
            description = "Returns all categories (including inactive). Requires admin authentication token.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<CategoryResponseDTO>> getAllCategories() {
        List<CategoryResponseDTO> categories = academicCategoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @Operation(
            summary = "Update category",
            description = "Updates an existing category. Requires admin authentication token.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<CategoryResponseDTO> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequestDTO requestDTO) {
        CategoryResponseDTO category = academicCategoryService.updateCategory(id, requestDTO);
        return ResponseEntity.ok(category);
    }

    @Operation(
            summary = "Delete category",
            description = "Deletes a category. Requires admin authentication token.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        academicCategoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}