package com.superme.repository;

import com.superme.enums.AgeGroup;
import com.superme.enums.Status;
import com.superme.model.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    
    List<Article> findByAgeGroup(AgeGroup ageGroup);
    
    List<Article> findByStatus(Article.Status status);
    
    long countByStatus(Article.Status status);


    
    @Query("SELECT a FROM Article a WHERE a.title LIKE %:keyword% OR a.description LIKE %:keyword% OR a.content LIKE %:keyword%")
    List<Article> findByKeyword(@Param("keyword") String keyword);
    
    @Query("SELECT DISTINCT tag FROM Article a JOIN a.tags tag")
    List<String> findAllTags();

    @Query("""
    SELECT a FROM Article a
    WHERE a.status = :status
      AND (
            :keyword IS NULL
         OR LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
         OR LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
         OR LOWER(a.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
""")
    List<Article> searchArticles(
            String keyword,
            Article.Status status
    );

    @Query("SELECT DISTINCT a.timeDuration FROM Article a WHERE a.timeDuration IS NOT NULL")
    List<String> findDistinctDurationCategories();
}