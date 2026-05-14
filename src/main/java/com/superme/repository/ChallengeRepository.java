package com.superme.repository;

import com.superme.enums.Category;
import com.superme.enums.Status;
import com.superme.model.Challenge;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long>, JpaSpecificationExecutor<Challenge> {

    // ============================================================================
    // FETCH CHALLENGES WITH QUESTIONS AND OPTIONS (CORRECTED)
    // ============================================================================


    long countByCategory(Category category);
    /**
     * Fetch all challenges with their questions and options eagerly loaded
     */
    @Query("SELECT DISTINCT c FROM Challenge c " +
            "LEFT JOIN FETCH c.questions q " +
            "LEFT JOIN FETCH q.options " +
            "WHERE c.enabled = true")
    List<Challenge> findAllWithQuestionsAndOptions();

    /**
     * Fetch visible challenges with questions and options for users
     */
    @Query("SELECT DISTINCT c FROM Challenge c " +
            "LEFT JOIN FETCH c.questions q " +
            "LEFT JOIN FETCH q.options " +
            "WHERE c.enabled = true")
    List<Challenge> findVisibleChallengesWithQuestionsAndOptions();

    /**
     * Fetch challenge by ID with questions and options
     */
    @Query("SELECT DISTINCT c FROM Challenge c " +
            "LEFT JOIN FETCH c.questions q " +
            "LEFT JOIN FETCH q.options " +
            "WHERE c.id = :id")
    Optional<Challenge> findByIdWithQuestionsAndOptions(@Param("id") Long id);

    /**
     * Fetch challenges by category with questions and options
     */
    @Query("SELECT DISTINCT c FROM Challenge c " +
            "LEFT JOIN FETCH c.questions q " +
            "LEFT JOIN FETCH q.options " +
            "WHERE c.category = :category AND c.enabled = true")
    List<Challenge> findByCategoryWithQuestionsAndOptions(@Param("category") String category);

    /**
     * Fetch challenges for specific age group with questions and options
     */
    @Query("SELECT DISTINCT c FROM Challenge c " +
            "LEFT JOIN FETCH c.questions q " +
            "LEFT JOIN FETCH q.options " +
            "WHERE :ageGroup MEMBER OF c.ageGroups AND c.enabled = true")
    List<Challenge> findByAgeGroupWithQuestionsAndOptions(@Param("ageGroup") String ageGroup);

    /**
     * Fetch all challenges (including disabled ones) with questions and options - for admin use
     */
    @Query("SELECT DISTINCT c FROM Challenge c " +
            "LEFT JOIN FETCH c.questions q " +
            "LEFT JOIN FETCH q.options")
    List<Challenge> findAllWithQuestionsAndOptionsIncludingDisabled();

    // ============================================================================
    // SIMPLER METHODS (QUESTIONS ONLY)
    // ============================================================================

    /**
     * Fetch all challenges with questions only
     */
    @Query("SELECT DISTINCT c FROM Challenge c LEFT JOIN FETCH c.questions")
    List<Challenge> findAllWithQuestions();

    /**
     * Fetch challenge by ID with questions only
     */
    @Query("SELECT c FROM Challenge c LEFT JOIN FETCH c.questions WHERE c.id = :id")
    Optional<Challenge> findByIdWithQuestions(@Param("id") Long id);

    /**
     * Fetch challenges by enabled status with questions
     */
    @Query("SELECT c FROM Challenge c LEFT JOIN FETCH c.questions WHERE c.enabled = :enabled")
    List<Challenge> findByEnabledWithQuestions(@Param("enabled") boolean enabled);

    /**
     * Fetch challenges by status with questions
     */
    @Query("SELECT c FROM Challenge c LEFT JOIN FETCH c.questions WHERE c.status = :status")
    List<Challenge> findByStatusWithQuestions(@Param("status") Status status);

    // ============================================================================
    // BASIC METHODS
    // ============================================================================

    List<Challenge> findByEnabledTrue();
    List<Challenge> findByCategory(String category);

    // ============================================================================
    // STATISTICS AND ANALYTICS
    // ============================================================================

    @Query("SELECT COUNT(c) FROM Challenge c")
    Long countTotalChallenges();

    @Query("SELECT c.category, COUNT(c) FROM Challenge c GROUP BY c.category")
    List<Object[]> countChallengesByCategory();

    @Query("SELECT c.difficulty, COUNT(c) FROM Challenge c GROUP BY c.difficulty")
    List<Object[]> countChallengesByDifficulty();

    @Query("SELECT ag, COUNT(c) FROM Challenge c JOIN c.ageGroups ag GROUP BY ag")
    List<Object[]> countChallengesByAgeGroup();

    long countByStatus(Status status);

    // User challenge completion counts
    @Query("SELECT COUNT(uc) FROM UserChallengeCompletion uc WHERE uc.user.id = :userId")
    int countCompletedChallengesByUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(uc) FROM UserChallengeCompletion uc WHERE uc.user.id = :userId AND uc.score = (SELECT COUNT(o) FROM Question q JOIN q.options o WHERE q.challenge = uc.challenge)")
    int countPerfectChallengesByUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(uc) FROM UserChallengeCompletion uc WHERE uc.user.id = :userId AND uc.challenge.category = 'ARTICLE'")
    int countCompletedArticlesByUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(uc) FROM UserChallengeCompletion uc WHERE uc.user.id = :userId AND uc.challenge.category = 'PUZZLE'")
    int countCompletedPuzzlesByUser(@Param("userId") Long userId);

    // ============================================================================
    // SPECIFICATION AND PAGINATION
    // ============================================================================

    List<Challenge> findAll(Specification<Challenge> spec);
    Page<Challenge> findAll(Specification<Challenge> spec, Pageable pageable);

    /**
     * Find challenges with questions by IDs
     */
    @Query("SELECT DISTINCT c FROM Challenge c " +
            "LEFT JOIN FETCH c.questions q " +
            "LEFT JOIN FETCH q.options " +
            "WHERE c.id IN :challengeIds")
    List<Challenge> findByIdsWithQuestionsAndOptions(@Param("challengeIds") List<Long> challengeIds);

    @Query("""
    SELECT c FROM Challenge c
    WHERE c.enabled = true
      AND c.status = :status
      AND (:category IS NULL OR c.category = :category)
      AND (
            :keyword IS NULL
         OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
         OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
         OR LOWER(c.topic) LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
""")
    List<Challenge> searchChallenges(
            String keyword,
            Category category,
            Status status
    );

}