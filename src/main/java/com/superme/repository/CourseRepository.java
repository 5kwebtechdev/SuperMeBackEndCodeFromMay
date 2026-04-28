package com.superme.repository;

import com.superme.enums.CourseDifficulty;
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

    // Remove CourseCategory enum methods since Course uses String category
    // List<Course> findByCategory(CourseCategory category); // REMOVE THIS

    List<Course> findByStatus(Status status);

    List<Course> findByDifficulty(CourseDifficulty difficulty);

    @Query("SELECT COUNT(c) FROM Course c")
    Long countTotalCourses();

    @Query("SELECT c.status, COUNT(c) FROM Course c GROUP BY c.status")
    List<Object[]> countCoursesByStatus();

    // FIX: Use String category instead of CourseCategory
    @Query("SELECT c.category, COUNT(c) FROM Course c GROUP BY c.category")
    List<Object[]> countCoursesByCategory();

    // FIX: Return String instead of CourseCategory
    @Query("SELECT DISTINCT c.category FROM Course c ORDER BY c.category")
    List<String> findDistinctCategories();

    Long countByStatus(Status status);

    @Query("SELECT AVG(c.duration) FROM Course c")
    Double findAverageDuration();

    Page<Course> findAll(Specification<Course> spec, Pageable pageable);

    // Keep these - they use String category
    List<Course> findByCategory(String category);

    long countByCategory(String category);

    List<Course> findByCategoryAndEnabledTrue(String category);

    @Query("SELECT DISTINCT c.category FROM Course c WHERE c.enabled = true")
    List<String> findDistinctActiveCategories();

    List<Course> findByCategoryAndStatus(String category, String status);
}