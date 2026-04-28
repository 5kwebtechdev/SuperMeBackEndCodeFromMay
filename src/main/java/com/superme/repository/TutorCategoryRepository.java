package com.superme.repository;

import com.superme.model.TutorCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TutorCategoryRepository extends JpaRepository<TutorCategory, Long> {
    Optional<TutorCategory> findByCategoryKey(String categoryKey);
    Optional<TutorCategory> findByCategoryName(String categoryName);
    List<TutorCategory> findByIsActiveTrueOrderByDisplayOrder();
}
