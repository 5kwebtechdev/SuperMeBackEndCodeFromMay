package com.superme.service;

import com.superme.model.CategoryField;
import com.superme.model.TutorCategory;
import com.superme.repository.CategoryFieldRepository;
import com.superme.repository.TutorCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TutorCategoryService {

    private final TutorCategoryRepository categoryRepository;
    private final CategoryFieldRepository fieldRepository;

    /**
     * Get all active categories ordered by display order
     */
    public List<TutorCategory> getAllActiveCategories() {
        log.info("Fetching all active categories");
        return categoryRepository.findByIsActiveTrueOrderByDisplayOrder();
    }

    /**
     * Get category by ID with all fields
     */
    public TutorCategory getCategoryById(Long id) {
        log.info("Fetching category with id: {}", id);
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
    }

    /**
     * Get category by key
     */
    public TutorCategory getCategoryByKey(String categoryKey) {
        log.info("Fetching category with key: {}", categoryKey);
        return categoryRepository.findByCategoryKey(categoryKey)
                .orElseThrow(() -> new RuntimeException("Category not found with key: " + categoryKey));
    }

    /**
     * Create new category
     */
    public TutorCategory createCategory(TutorCategory category) {
        log.info("Creating new category: {}", category.getCategoryName());

        // Check for duplicate
        if (categoryRepository.findByCategoryKey(category.getCategoryKey()).isPresent()) {
            throw new RuntimeException("Category with key '" + category.getCategoryKey() + "' already exists");
        }

        category.setIsActive(true);
        TutorCategory saved = categoryRepository.save(category);
        log.info("Category created successfully with id: {}", saved.getId());
        return saved;
    }

    /**
     * Update category
     */
    public TutorCategory updateCategory(Long id, TutorCategory categoryDetails) {
        log.info("Updating category with id: {}", id);

        TutorCategory category = getCategoryById(id);

        // Check if key is being changed
        if (!category.getCategoryKey().equals(categoryDetails.getCategoryKey())) {
            if (categoryRepository.findByCategoryKey(categoryDetails.getCategoryKey()).isPresent()) {
                throw new RuntimeException("Category with key '" + categoryDetails.getCategoryKey() + "' already exists");
            }
        }

        category.setCategoryName(categoryDetails.getCategoryName());
        category.setCategoryKey(categoryDetails.getCategoryKey());
        category.setDescription(categoryDetails.getDescription());
        category.setIconUrl(categoryDetails.getIconUrl());
        category.setDisplayOrder(categoryDetails.getDisplayOrder());
        category.setIsActive(categoryDetails.getIsActive());

        TutorCategory updated = categoryRepository.save(category);
        log.info("Category updated successfully with id: {}", id);
        return updated;
    }

    /**
     * Delete category (soft delete)
     */
    public void deleteCategory(Long id) {
        log.info("Deleting category with id: {}", id);

        TutorCategory category = getCategoryById(id);
        category.setIsActive(false);
        categoryRepository.save(category);

        log.info("Category deleted successfully with id: {}", id);
    }

    /**
     * Get all fields for a category
     */
    public List<CategoryField> getFieldsByCategory(Long categoryId) {
        log.info("Fetching fields for category: {}", categoryId);
        getCategoryById(categoryId); // Verify category exists
        return fieldRepository.findByCategoryIdOrderByDisplayOrder(categoryId);
    }
}
