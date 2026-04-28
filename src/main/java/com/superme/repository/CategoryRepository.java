package com.superme.repository;

import com.superme.model.AcdemicCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<AcdemicCategory, Long> {

    Optional<AcdemicCategory> findByCategoryName(String categoryName);

    List<AcdemicCategory> findAllByOrderByCreatedAtDesc();

    // FIX: Use AcdemicCategory instead of Category
    @Query("SELECT c FROM AcdemicCategory c WHERE c.status = 'ACTIVE' ORDER BY c.createdAt DESC")
    List<AcdemicCategory> findActiveCategories();

    boolean existsByCategoryName(String categoryName);

    List<AcdemicCategory> findByCategoryNameContainingIgnoreCase(String name);

    // Add derived query for status (backup option)
    List<AcdemicCategory> findByStatusOrderByCreatedAtDesc(String status);
}