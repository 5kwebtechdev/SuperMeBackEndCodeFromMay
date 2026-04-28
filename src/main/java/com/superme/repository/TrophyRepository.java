package com.superme.repository;

import com.superme.enums.Category;
import com.superme.enums.Difficulty;
import com.superme.model.Trophy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrophyRepository extends JpaRepository<Trophy, Long> {

    List<Trophy> findByUserIdOrderByEarnedDateDesc(String userId);

    List<Trophy> findByUserIdAndEarnedDateBetweenOrderByEarnedDateDesc(
            String userId, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT t FROM Trophy t WHERE t.userId = :userId AND YEAR(t.earnedDate) = :year AND MONTH(t.earnedDate) = :month ORDER BY t.earnedDate DESC")
    List<Trophy> findByUserIdAndYearAndMonth(@Param("userId") String userId,
                                             @Param("year") int year,
                                             @Param("month") int month);

    @Query("SELECT SUM(t.trophyCount) FROM Trophy t WHERE t.userId = :userId")
    Integer getTotalTrophyCountByUserId(@Param("userId") String userId);

    boolean existsByUserIdAndChallengeId(String userId, String challengeId);

    Optional<Trophy> findByUserIdAndChallengeId(String userId, String challengeId);

    // Updated to use Category instead of ContentType
    @Query("SELECT t FROM Trophy t WHERE t.userId = :userId AND t.category = :category ORDER BY t.earnedDate DESC")
    List<Trophy> findByUserIdAndCategory(@Param("userId") String userId,
                                         @Param("category") Category category);

    // Updated to use Difficulty instead of DifficultyLevel
    @Query("SELECT t FROM Trophy t WHERE t.userId = :userId AND t.difficulty = :difficulty ORDER BY t.earnedDate DESC")
    List<Trophy> findByUserIdAndDifficulty(@Param("userId") String userId,
                                           @Param("difficulty") Difficulty difficulty);
}