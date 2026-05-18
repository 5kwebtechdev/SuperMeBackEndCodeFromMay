package com.superme.repository;

import com.superme.enums.Status;
import com.superme.model.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {



    List<Course> findByStatus(Status status);






    // FIX: Return String instead of CourseCategory
    @Query("SELECT DISTINCT c.category FROM Course c ORDER BY c.category")
    List<String> findDistinctCategories();

    Long countByStatus(Status status);

    @Query("SELECT AVG(c.duration) FROM Course c")
    Double findAverageDuration();

    Page<Course> findAll(Specification<Course> spec, Pageable pageable);

    // Keep these - they use String category

    long countByCategory(String category);



}