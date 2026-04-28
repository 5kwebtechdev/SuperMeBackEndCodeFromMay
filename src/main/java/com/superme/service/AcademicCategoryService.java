package com.superme.service;

import com.superme.dto.CategoryRequestDTO;
import com.superme.dto.CategoryResponseDTO;
import com.superme.model.AcdemicCategory;
import com.superme.repository.CategoryRepository;
import com.superme.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AcademicCategoryService {

    private final CategoryRepository categoryRepository;
    private final CourseRepository courseRepository;

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

        // Create
        AcdemicCategory acdemicCategory = AcdemicCategory.builder()
                .categoryName(categoryName)
                .description(requestDTO.getDescription())
                .status(requestDTO.getStatus() != null ? requestDTO.getStatus() : "ACTIVE")
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

        // Update
        acdemicCategory.setCategoryName(newCategoryName);
        acdemicCategory.setDescription(requestDTO.getDescription());

        if (requestDTO.getStatus() != null) {
            acdemicCategory.setStatus(requestDTO.getStatus());
        }

        AcdemicCategory updatedAcdemicCategory = categoryRepository.save(acdemicCategory);
        return convertToDTO(updatedAcdemicCategory);
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

        categoryRepository.delete(acdemicCategory);
    }

    // Helper method
    private CategoryResponseDTO convertToDTO(AcdemicCategory acdemicCategory) {
        CategoryResponseDTO dto = new CategoryResponseDTO();
        dto.setId(acdemicCategory.getId());
        dto.setCategoryName(acdemicCategory.getCategoryName());
        dto.setDescription(acdemicCategory.getDescription());
        dto.setStatus(acdemicCategory.getStatus());


        // Count courses
        long courseCount = courseRepository.countByCategory(acdemicCategory.getCategoryName());
        dto.setCourseCount((int) courseCount);

        return dto;
    }
}