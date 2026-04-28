package com.superme.service;

import com.superme.exception.ResourceNotFoundException;
import com.superme.model.CategoryField;
import com.superme.model.FieldOption;
import com.superme.model.TutorCategory;
import com.superme.repository.CategoryFieldRepository;
import com.superme.repository.FieldOptionRepository;
import com.superme.repository.TutorCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CategoryFieldService {

    private final CategoryFieldRepository fieldRepository;
    private final TutorCategoryRepository categoryRepository;
    private final FieldOptionRepository optionRepository;

    /**
     * Get all fields for a category
     */
    public List<CategoryField> getFieldsByCategory(Long categoryId) {
        log.info("Fetching fields for category: {}", categoryId);
        validateCategoryExists(categoryId);
        return fieldRepository.findByCategoryIdOrderByDisplayOrder(categoryId);
    }

    /**
     * Get field by ID
     */
    public CategoryField getFieldById(Long fieldId) {
        log.info("Fetching field with id: {}", fieldId);
        return fieldRepository.findById(fieldId)
                .orElseThrow(() -> new RuntimeException("Field not found with id: " + fieldId));
    }

    /**
     * Create new field for category
     */
    public CategoryField createField(Long categoryId, CategoryField field) {
        log.info("Creating new field for category: {}", categoryId);

        TutorCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));

        // Check duplicate field key
        if (fieldRepository.findByCategoryIdAndFieldKey(categoryId, field.getFieldKey()).isPresent()) {
            throw new RuntimeException("Field '" + field.getFieldKey() + "' already exists for this category");
        }

        field.setCategory(category);
        CategoryField saved = fieldRepository.save(field);
        log.info("Field created successfully with id: {}", saved.getId());
        return saved;
    }

    /**
     * Update field
     */
    public CategoryField updateField(Long fieldId, CategoryField fieldDetails) {
        log.info("Updating field with id: {}", fieldId);

        CategoryField field = getFieldById(fieldId);

        // Check if key is being changed
        if (!field.getFieldKey().equals(fieldDetails.getFieldKey())) {
            if (fieldRepository.findByCategoryIdAndFieldKey(field.getCategory().getId(), fieldDetails.getFieldKey()).isPresent()) {
                throw new RuntimeException("Field '" + fieldDetails.getFieldKey() + "' already exists for this category");
            }
        }

        field.setFieldKey(fieldDetails.getFieldKey());
        field.setFieldLabel(fieldDetails.getFieldLabel());
        field.setFieldType(fieldDetails.getFieldType());
        field.setIsRequired(fieldDetails.getIsRequired());
        field.setIsFilterable(fieldDetails.getIsFilterable());
        field.setDisplayOrder(fieldDetails.getDisplayOrder());
        field.setValidationRules(fieldDetails.getValidationRules());

        CategoryField updated = fieldRepository.save(field);
        log.info("Field updated successfully with id: {}", fieldId);
        return updated;
    }

    /**
     * Delete field
     */
    public void deleteField(Long fieldId) {
        log.info("Deleting field with id: {}", fieldId);

        CategoryField field = getFieldById(fieldId);

        // Delete associated options
        List<FieldOption> options = optionRepository.findByFieldIdOrderByDisplayOrder(fieldId);
        optionRepository.deleteAll(options);

        fieldRepository.delete(field);
        log.info("Field deleted successfully with id: {}", fieldId);
    }

    private void validateCategoryExists(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found with id: " + categoryId);
        }
    }
}
