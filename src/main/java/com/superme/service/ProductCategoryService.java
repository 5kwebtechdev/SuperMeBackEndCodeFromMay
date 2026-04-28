package com.superme.service;

import com.superme.exception.ResourceNotFoundException;
import com.superme.dto.*;
import com.superme.model.ProductCategory;
import com.superme.repository.ProductCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProductCategoryService {

    private final ProductCategoryRepository categoryRepository;

    public List<ProductCategoryDto> getAll() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public ProductCategoryDto getById(Long id) {
        return toDto(findEntity(id));
    }

    public ProductCategoryDto create(ProductCategoryCreateRequest request) {
        categoryRepository.findByNameIgnoreCase(request.getName())
                .ifPresent(c -> { throw new RuntimeException("Product category already exists: " + request.getName()); });

        ProductCategory entity = ProductCategory.builder()
                .name(request.getName())
                .displayName(request.getDisplayName())
                .build();

        return toDto(categoryRepository.save(entity));
    }

    public ProductCategoryDto update(Long id, ProductCategoryUpdateRequest request) {
        ProductCategory existing = findEntity(id);
        existing.setName(request.getName());
        existing.setDisplayName(request.getDisplayName());
        return toDto(categoryRepository.save(existing));
    }

    public void delete(Long id) {
        ProductCategory existing = findEntity(id);
        categoryRepository.delete(existing);
    }

    private ProductCategory findEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product category not found: " + id));
    }

    private ProductCategoryDto toDto(ProductCategory c) {
        return ProductCategoryDto.builder()
                .id(c.getId())
                .name(c.getName())
                .displayName(c.getDisplayName())
                .build();
    }
}
