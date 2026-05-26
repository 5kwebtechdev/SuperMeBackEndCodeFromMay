package com.superme.repository;

import com.superme.enums.AgeGroup;
import com.superme.model.Article;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    // Soft-delete-aware finders
    List<Article> findAllByDeletedFalse(Sort sort);

    List<Article> findAllByDeletedFalse();

    Optional<Article> findByIdAndDeletedFalse(Long id);

    List<Article> findByAgeGroupAndDeletedFalse(AgeGroup ageGroup);

    List<Article> findByStatusAndDeletedFalse(Article.Status status);

    long countByDeletedFalse();

    long countByStatusAndDeletedFalse(Article.Status status);

    // Legacy (kept for any internal use, include all rows)
    List<Article> findByAgeGroup(AgeGroup ageGroup);

    List<Article> findByStatus(Article.Status status);

    long countByStatus(Article.Status status);

    @Query("SELECT a FROM Article a WHERE a.deleted = false AND (a.title LIKE %:keyword% OR a.description LIKE %:keyword% OR a.content LIKE %:keyword%)")
    List<Article> findByKeyword(@Param("keyword") String keyword);

    @Query("SELECT DISTINCT tag FROM Article a JOIN a.tags tag WHERE a.deleted = false")
    List<String> findAllTags();

    @Query("""
    SELECT a FROM Article a
    WHERE a.deleted = false
      AND a.status = :status
      AND (
            :keyword IS NULL
         OR LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
         OR LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
         OR LOWER(a.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
""")
    List<Article> searchArticles(
            @Param("keyword") String keyword,
            @Param("status") Article.Status status
    );

    @Query("SELECT DISTINCT a.durationMinutes FROM Article a WHERE a.deleted = false AND a.durationMinutes IS NOT NULL")
    List<Integer> findDistinctDurationMinutes();
}