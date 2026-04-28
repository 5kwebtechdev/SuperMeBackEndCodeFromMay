package com.superme.repository;

import com.superme.model.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {
    Optional<LessonProgress> findByUserIdAndLessonId(Long userId, Long lessonId);
    List<LessonProgress> findByUserIdAndLesson_CourseId(Long userId, Long courseId);
    boolean existsByUserIdAndLessonIdAndCompletedTrue(Long userId, Long lessonId);

    @Query("SELECT lp FROM LessonProgress lp " +
            "WHERE lp.userId = :userId AND lp.lesson.course.id = :courseId")
    List<LessonProgress> findByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);
}