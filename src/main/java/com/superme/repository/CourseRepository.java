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

    List<Course> findByEnabled(boolean enabled);

    long countByEnabled(boolean enabled);

    long countByStatusAndEnabled(Status status, boolean enabled);

    @Query("SELECT DISTINCT c.category FROM Course c WHERE c.enabled = true ORDER BY c.category")
    List<String> findDistinctCategories();

    Long countByStatus(Status status);

    @Query("SELECT AVG(c.duration) FROM Course c WHERE c.enabled = true")
    Double findAverageDuration();

    Page<Course> findAll(Specification<Course> spec, Pageable pageable);

    long countByCategory(String category);

}