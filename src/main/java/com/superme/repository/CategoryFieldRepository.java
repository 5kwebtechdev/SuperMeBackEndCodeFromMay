package com.superme.repository;

import com.superme.model.CategoryField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryFieldRepository extends JpaRepository<CategoryField, Long> {
    List<CategoryField> findByCategoryIdOrderByDisplayOrder(Long categoryId);
    Optional<CategoryField> findByCategoryIdAndFieldKey(Long categoryId, String fieldKey);
    List<CategoryField> findByCategoryId(Long categoryId);
}
