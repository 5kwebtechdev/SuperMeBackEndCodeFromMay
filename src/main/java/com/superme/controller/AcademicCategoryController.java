 package com.superme.controller;

import com.superme.dto.CategoryRequestDTO;
import com.superme.dto.CategoryResponseDTO;
import com.superme.service.AcademicCategoryService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
public class AcademicCategoryController {

    private final AcademicCategoryService academicCategoryService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @GetMapping
    public ResponseEntity<List<CategoryResponseDTO>> getActiveCategories() {
        List<CategoryResponseDTO> categories = academicCategoryService.getActiveCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/all")
    public ResponseEntity<List<CategoryResponseDTO>> getAllCategories() {
        List<CategoryResponseDTO> categories = academicCategoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> getCategoryById(@PathVariable Long id) {
        CategoryResponseDTO category = academicCategoryService.getCategoryById(id);
        return ResponseEntity.ok(category);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CategoryResponseDTO> createCategory(
            @RequestParam("categoryName") @NotBlank String categoryName,
            @RequestParam("description") String description,
            @RequestParam("status") String status,
            @RequestPart("thumbnail") MultipartFile thumbnail) {

        CategoryRequestDTO requestDTO = new CategoryRequestDTO();
        requestDTO.setCategoryName(categoryName);
        requestDTO.setDescription(description);
        requestDTO.setStatus(status);
        requestDTO.setThumbnail(thumbnail);

        CategoryResponseDTO createdCategory = academicCategoryService.createCategory(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);
    }

//    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<CategoryResponseDTO> updateCategory(
//            @PathVariable Long id,
//            @RequestParam("categoryName") String categoryName,
//            @RequestParam("description") String description,
//            @RequestParam("status") String status,
//            @RequestPart("thumbnail") MultipartFile thumbnail) {
//
//        CategoryRequestDTO requestDTO = new CategoryRequestDTO();
//        requestDTO.setCategoryName(categoryName);
//        requestDTO.setDescription(description);
//        requestDTO.setStatus(status);
//        requestDTO.setThumbnail(thumbnail);
//
//        CategoryResponseDTO updatedCategory = academicCategoryService.updateCategory(id, requestDTO);
//        return ResponseEntity.ok(updatedCategory);
//    }




    // AcademicCategoryController.java
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CategoryResponseDTO> updateCategory(
            @PathVariable Long id,
            @RequestParam("categoryName") String categoryName,
            @RequestParam("description") String description,
            @RequestParam("status") String status,
            @RequestParam(value = "existingThumbnailUrl", required = false) String existingThumbnailUrl,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail) {

        CategoryRequestDTO requestDTO = new CategoryRequestDTO();
        requestDTO.setCategoryName(categoryName);
        requestDTO.setDescription(description);
        requestDTO.setStatus(status);
        requestDTO.setThumbnail(thumbnail);
        requestDTO.setExistingThumbnailUrl(existingThumbnailUrl);

        CategoryResponseDTO updatedCategory = academicCategoryService.updateCategory(id, requestDTO);
        return ResponseEntity.ok(updatedCategory);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        academicCategoryService.deleteCategory(id);
        return ResponseEntity.ok().build();
    }

    // Download thumbnail API
    @GetMapping("/download/thumbnail/{filePath:.+}")
    public ResponseEntity<byte[]> downloadThumbnail(@PathVariable String filePath) {
        try {
            // Resolve the full path: uploadDir/CategoryThumbNail/filePath
            Path path = Paths.get(uploadDir, "CategoryThumbNail", filePath);

            if (!Files.exists(path)) {
                throw new RuntimeException("File not found: " + filePath);
            }

            byte[] fileBytes = Files.readAllBytes(path);

            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .header("Content-Disposition", "inline; filename=\"" + path.getFileName() + "\"")
                    .body(fileBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error while downloading file: " + e.getMessage(), e);
        }
    }
}