package com.superme.repository;


import com.superme.model.TutorCategoryMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TutorCategoryMappingRepository extends JpaRepository<TutorCategoryMapping, Integer> {

    // Get all categories for a tutor
    List<TutorCategoryMapping> findByTutorId(Long tutorId);

    // Get specific category mapping for a tutor
    Optional<TutorCategoryMapping> findByTutorIdAndCategoryId(Long tutorId, Integer categoryId);

    // Check if tutor already teaches a category
    boolean existsByTutorIdAndCategoryId(Long tutorId, Integer categoryId);

    // Get all tutors teaching a category
    List<TutorCategoryMapping> findByCategoryId(Integer categoryId);

    // Find tutors accepting students in a category
    List<TutorCategoryMapping> findByCategoryIdAndIsAcceptingStudentsTrue(Integer categoryId);

    // Count categories for a tutor
    long countByTutorId(Long tutorId);

    // Get categories sorted by expertise level
    @Query("SELECT tcm FROM TutorCategoryMapping tcm WHERE tcm.tutorId = :tutorId ORDER BY tcm.expertiseLevel DESC")
    List<TutorCategoryMapping> findByTutorIdOrderByExpertise(@Param("tutorId") Long tutorId);
}

