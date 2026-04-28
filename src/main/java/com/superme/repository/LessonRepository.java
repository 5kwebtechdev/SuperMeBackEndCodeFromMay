package com.superme.repository;

import com.superme.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository interface for Lesson entity operations.
 */
@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByCourseIdOrderByLessonOrderAsc(Long courseId);

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.course.id = :courseId")
    Long countLessonsByCourseId(Long courseId);

    void deleteByCourseId(Long courseId);
}