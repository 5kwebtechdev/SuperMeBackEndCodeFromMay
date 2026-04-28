package com.superme.repository;

import com.superme.model.CourseProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseProgressRepository extends JpaRepository<CourseProgress, Long> {
    Optional<CourseProgress> findByUserIdAndCourseId(Long userId, Long courseId);

    List<CourseProgress> findByUserId(Long userId);
    List<CourseProgress> findByCourseId(Long courseId);
    boolean existsByUserIdAndCourseIdAndCompletedTrue(Long userId, Long courseId);

    @Query("SELECT COUNT(lp) FROM LessonProgress lp " +
            "WHERE lp.userId = :userId AND lp.lesson.course.id = :courseId AND lp.completed = true")
    Integer countCompletedLessonsByUserAndCourse(@Param("userId") Long userId, @Param("courseId") Long courseId);
}
