package com.superme.repository;

import com.superme.model.ProductSizeRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductSizeRowRepository extends JpaRepository<ProductSizeRow, Long> {
    List<ProductSizeRow> findByProductId(Long productId);
}
