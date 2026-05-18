// AcademicCategoryService.java
package com.superme.service;

import com.superme.dto.CategoryRequestDTO;
import com.superme.dto.CategoryResponseDTO;
import com.superme.model.AcdemicCategory;
import com.superme.repository.CategoryRepository;
import com.superme.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AcademicCategoryService {

    private final CategoryRepository categoryRepository;
    private final CourseRepository courseRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.base-url}")
    private String filebaseurl;

    // CREATE
    @Transactional
    public CategoryResponseDTO createCategory(CategoryRequestDTO requestDTO) {
        // Validate
        if (requestDTO.getCategoryName() == null || requestDTO.getCategoryName().trim().isEmpty()) {
            throw new IllegalArgumentException("Category name is required");
        }

        String categoryName = requestDTO.getCategoryName().trim();

        // Check duplicate
        if (categoryRepository.existsByCategoryName(categoryName)) {
            throw new IllegalArgumentException("Category already exists: " + categoryName);
        }

        // Save thumbnail if provided
        String thumbnailUrl = null;
        if (requestDTO.getThumbnail() != null && !requestDTO.getThumbnail().isEmpty()) {
            thumbnailUrl = saveFileLocally(requestDTO.getThumbnail(), "CategoryThumbNail");
        }

        // Create
        AcdemicCategory acdemicCategory = AcdemicCategory.builder()
                .categoryName(categoryName)
                .description(requestDTO.getDescription())
                .status(requestDTO.getStatus() != null ? requestDTO.getStatus() : "ACTIVE")
                .thumbnailUrl(thumbnailUrl)
                .build();

        AcdemicCategory savedAcdemicCategory = categoryRepository.save(acdemicCategory);
        return convertToDTO(savedAcdemicCategory);
    }

    // READ ALL (For Admin)
    public List<CategoryResponseDTO> getAllCategories() {
        return categoryRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // READ ACTIVE (For Users)
    public List<CategoryResponseDTO> getActiveCategories() {
        return categoryRepository.findActiveCategories().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // GET BY ID
    public CategoryResponseDTO getCategoryById(Long id) {
        AcdemicCategory acdemicCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + id));
        return convertToDTO(acdemicCategory);
    }

    // UPDATE
//    @Transactional
//    public CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO requestDTO) {
//        AcdemicCategory acdemicCategory = categoryRepository.findById(id)
//                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + id));
//
//        String newCategoryName = requestDTO.getCategoryName().trim();
//
//        // Check duplicate name (if changed)
//        if (!acdemicCategory.getCategoryName().equals(newCategoryName)
//                && categoryRepository.existsByCategoryName(newCategoryName)) {
//            throw new IllegalArgumentException("Category name already exists: " + newCategoryName);
//        }
//
//        // Update thumbnail if provided
//        if (requestDTO.getThumbnail() != null && !requestDTO.getThumbnail().isEmpty()) {
//            // Delete old thumbnail if it exists
//            if (acdemicCategory.getThumbnailUrl() != null) {
//                deleteLocalFile(acdemicCategory.getThumbnailUrl());
//            }
//            String thumbnailUrl = saveFileLocally(requestDTO.getThumbnail(), "CategoryThumbNail");
//            acdemicCategory.setThumbnailUrl(thumbnailUrl);
//        }
//
//        // Update other fields
//        acdemicCategory.setCategoryName(newCategoryName);
//        acdemicCategory.setDescription(requestDTO.getDescription());
//
//        if (requestDTO.getStatus() != null) {
//            acdemicCategory.setStatus(requestDTO.getStatus());
//        }
//
//        AcdemicCategory updatedAcdemicCategory = categoryRepository.save(acdemicCategory);
//        return convertToDTO(updatedAcdemicCategory);
//    }


    // AcademicCategoryService.java
    @Transactional
    public CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO requestDTO) {
        AcdemicCategory acdemicCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + id));

        String newCategoryName = requestDTO.getCategoryName().trim();

        // Check duplicate name (if changed)
        if (!acdemicCategory.getCategoryName().equals(newCategoryName)
                && categoryRepository.existsByCategoryName(newCategoryName)) {
            throw new IllegalArgumentException("Category name already exists: " + newCategoryName);
        }

        // Handle thumbnail update
        if (requestDTO.getThumbnail() != null && !requestDTO.getThumbnail().isEmpty()) {
            // New thumbnail provided: delete old thumbnail and save the new one
            if (acdemicCategory.getThumbnailUrl() != null) {
                deleteLocalFile(acdemicCategory.getThumbnailUrl());
            }
            String thumbnailUrl = saveFileLocally(requestDTO.getThumbnail(), "CategoryThumbNail");
            acdemicCategory.setThumbnailUrl(thumbnailUrl);
        } else if (requestDTO.getExistingThumbnailUrl() != null && !requestDTO.getExistingThumbnailUrl().isEmpty()) {
            // No new thumbnail, but existingThumbnailUrl is provided: retain the existing thumbnail
            // Extract the filename from the URL (e.g., "1779129636468_22a0626b-d259-49b6-8849-7e3540bd7d1d.png")
            String filename = extractFilenameFromUrl(requestDTO.getExistingThumbnailUrl());
            acdemicCategory.setThumbnailUrl(filename);
        } else {
            // No new thumbnail and no existingThumbnailUrl: set thumbnail to null
            if (acdemicCategory.getThumbnailUrl() != null) {
                deleteLocalFile(acdemicCategory.getThumbnailUrl());
                acdemicCategory.setThumbnailUrl(null);
            }
        }

        // Update other fields
        acdemicCategory.setCategoryName(newCategoryName);
        acdemicCategory.setDescription(requestDTO.getDescription());

        if (requestDTO.getStatus() != null) {
            acdemicCategory.setStatus(requestDTO.getStatus());
        }

        AcdemicCategory updatedAcdemicCategory = categoryRepository.save(acdemicCategory);
        return convertToDTO(updatedAcdemicCategory);
    }

    // Helper method to extract filename from URL
    private String extractFilenameFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        // Extract the last part of the URL (e.g., "1779129636468_22a0626b-d259-49b6-8849-7e3540bd7d1d.png")
        String[] parts = url.split("/");
        return parts[parts.length - 1];
    }



    // DELETE
    @Transactional
    public void deleteCategory(Long id) {
        AcdemicCategory acdemicCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + id));

        // Check if used in courses
        long courseCount = courseRepository.countByCategory(acdemicCategory.getCategoryName());
        if (courseCount > 0) {
            throw new IllegalStateException(
                    "Cannot delete. Category has " + courseCount + " courses"
            );
        }

        // Delete thumbnail if it exists
        if (acdemicCategory.getThumbnailUrl() != null) {
            deleteLocalFile(acdemicCategory.getThumbnailUrl());
        }

        categoryRepository.delete(acdemicCategory);
    }

    // Helper method to save file locally
// AcademicCategoryService.java
    public String saveFileLocally(MultipartFile file, String subfolder) {
        try {
            Path uploadPath = Paths.get(uploadDir, subfolder);

            // Create directory if it doesn't exist
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String filename = System.currentTimeMillis() + "_" + UUID.randomUUID().toString() +
                    file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));

            // Save file
            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath);

            // Return only the filename (e.g., "1779127492618_f21da882-6961-42d8-b1d4-738a6fa1d18b.png")
            return filename;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save file locally: " + e.getMessage(), e);
        }
    }

    // AcademicCategoryService.java
    private void deleteLocalFile(String filename) {
        if (filename == null || filename.isBlank()) return;
        try {
            Path filePath = Paths.get(uploadDir, "CategoryThumbNail", filename);
            Files.deleteIfExists(filePath);
        } catch (Exception ignored) {
            // Log error if needed
        }
    }

    // AcademicCategoryService.java
    private CategoryResponseDTO convertToDTO(AcdemicCategory acdemicCategory) {
        CategoryResponseDTO dto = new CategoryResponseDTO();
        dto.setId(acdemicCategory.getId());
        dto.setCategoryName(acdemicCategory.getCategoryName());
        dto.setDescription(acdemicCategory.getDescription());
        dto.setStatus(acdemicCategory.getStatus());

        // Construct the download URL for the thumbnail
        if (acdemicCategory.getThumbnailUrl() != null) {
            dto.setThumbnailUrl(filebaseurl+"/v1/category/download/thumbnail/" + acdemicCategory.getThumbnailUrl());
        } else {
            dto.setThumbnailUrl(null);
        }

        // Count courses
        long courseCount = courseRepository.countByCategory(acdemicCategory.getCategoryName());
        dto.setCourseCount((int) courseCount);

        return dto;
    }
}