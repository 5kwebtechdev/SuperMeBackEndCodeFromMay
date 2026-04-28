package com.superme.repository;

import com.superme.model.ProductFieldOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductFieldOptionRepository extends JpaRepository<ProductFieldOption, Long> {
    List<ProductFieldOption> findByFieldIdOrderByDisplayOrder(Long fieldId);
}
