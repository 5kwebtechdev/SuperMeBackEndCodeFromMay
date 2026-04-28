package com.superme.repository;

import com.superme.model.ProductCategoryField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductCategoryFieldRepository extends JpaRepository<ProductCategoryField, Long> {
    List<ProductCategoryField> findByCategoryIdOrderByDisplayOrder(Long categoryId);
    Optional<ProductCategoryField> findByCategoryIdAndFieldKey(Long categoryId, String fieldKey);
}
