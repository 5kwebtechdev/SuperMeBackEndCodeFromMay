package com.superme.service;

import com.superme.exception.ResourceNotFoundException;
import com.superme.dto.*;
import com.superme.model.ProductCategory;
import com.superme.model.ProductCategoryField;
import com.superme.model.ProductFieldOption;
import com.superme.repository.ProductCategoryFieldRepository;
import com.superme.repository.ProductCategoryRepository;
import com.superme.repository.ProductFieldOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProductCategoryFieldService {

    private final ProductCategoryFieldRepository fieldRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductFieldOptionRepository optionRepository;

    public List<ProductCategoryFieldDto> getFieldsByCategory(Long categoryId) {
        validateCategoryExists(categoryId);
        return fieldRepository.findByCategoryIdOrderByDisplayOrder(categoryId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public ProductCategoryFieldDto getFieldById(Long fieldId) {
        return toDto(findField(fieldId));
    }

    public ProductCategoryFieldDto createField(Long categoryId, ProductCategoryFieldCreateRequest request) {
        ProductCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Product category not found: " + categoryId));

        fieldRepository.findByCategoryIdAndFieldKey(categoryId, request.getFieldKey())
                .ifPresent(f -> { throw new RuntimeException("Field already exists for this category: " + request.getFieldKey()); });

        ProductCategoryField field = ProductCategoryField.builder()
                .category(category)
                .fieldType(request.getFieldType())
                .fieldKey(request.getFieldKey())
                .fieldLabel(request.getFieldLabel())
                .isRequired(request.getIsRequired())
                .isFilterable(request.getIsFilterable())
                .displayOrder(request.getDisplayOrder())
                .validationRules(request.getValidationRules())
                .build();

        ProductCategoryField saved = fieldRepository.save(field);
        saveOptions(saved, request.getOptions());
        return toDto(saved);
    }

    public ProductCategoryFieldDto updateField(Long fieldId, ProductCategoryFieldUpdateRequest request) {
        ProductCategoryField field = findField(fieldId);

        if (!field.getFieldKey().equals(request.getFieldKey())) {
            fieldRepository.findByCategoryIdAndFieldKey(field.getCategory().getId(), request.getFieldKey())
                    .ifPresent(f -> { throw new RuntimeException("Field already exists for this category: " + request.getFieldKey()); });
        }

        field.setFieldType(request.getFieldType());
        field.setFieldKey(request.getFieldKey());
        field.setFieldLabel(request.getFieldLabel());
        field.setIsRequired(request.getIsRequired());
        field.setIsFilterable(request.getIsFilterable());
        field.setDisplayOrder(request.getDisplayOrder());
        field.setValidationRules(request.getValidationRules());

        ProductCategoryField saved = fieldRepository.save(field);
        saveOptions(saved, request.getOptions());
        return toDto(saved);
    }

    public void deleteField(Long fieldId) {
        ProductCategoryField field = findField(fieldId);
        List<ProductFieldOption> options = optionRepository.findByFieldIdOrderByDisplayOrder(fieldId);
        optionRepository.deleteAll(options);
        fieldRepository.delete(field);
    }

    // ── helpers ──

    private void validateCategoryExists(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Product category not found: " + categoryId);
        }
    }

    private ProductCategoryField findField(Long fieldId) {
        return fieldRepository.findById(fieldId)
                .orElseThrow(() -> new ResourceNotFoundException("Product field not found: " + fieldId));
    }

    private void saveOptions(ProductCategoryField field, List<ProductFieldOptionDto> optionDtos) {
        List<ProductFieldOption> existing = optionRepository.findByFieldIdOrderByDisplayOrder(field.getId());
        optionRepository.deleteAll(existing);

        if (optionDtos == null) return;

        for (ProductFieldOptionDto dto : optionDtos) {
            ProductFieldOption opt = ProductFieldOption.builder()
                    .field(field)
                    .value(dto.getValue())
                    .label(dto.getLabel())
                    .displayOrder(dto.getDisplayOrder())
                    .build();
            optionRepository.save(opt);
        }
    }

    private ProductCategoryFieldDto toDto(ProductCategoryField f) {
        List<ProductFieldOption> options = optionRepository.findByFieldIdOrderByDisplayOrder(f.getId());
        List<ProductFieldOptionDto> optionDtos = options.stream()
                .map(o -> ProductFieldOptionDto.builder()
                        .id(o.getId())
                        .value(o.getValue())
                        .label(o.getLabel())
                        .displayOrder(o.getDisplayOrder())
                        .build())
                .toList();

        return ProductCategoryFieldDto.builder()
                .id(f.getId())
                .categoryId(f.getCategory().getId())
                .fieldType(f.getFieldType())
                .fieldKey(f.getFieldKey())
                .fieldLabel(f.getFieldLabel())
                .isRequired(f.getIsRequired())
                .isFilterable(f.getIsFilterable())
                .displayOrder(f.getDisplayOrder())
                .validationRules(f.getValidationRules())
                .options(optionDtos)
                .build();
    }
}
