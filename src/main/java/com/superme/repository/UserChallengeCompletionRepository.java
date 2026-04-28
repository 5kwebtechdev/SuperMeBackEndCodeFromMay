package com.superme.repository;

import com.superme.model.Challenge;
import com.superme.model.UserChallengeCompletion;
import com.superme.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserChallengeCompletionRepository extends JpaRepository<UserChallengeCompletion, Long> {

    // Get all completions for a user (by entity)
    List<UserChallengeCompletion> findByUser(User user);

    // OR: Get all completions for a user (by ID)
    @Query("SELECT ucc FROM UserChallengeCompletion ucc WHERE ucc.user.id = :userId")
    List<UserChallengeCompletion> findByUserId(Long userId);

    @Query("SELECT COUNT(DISTINCT ucc.challenge.id) FROM UserChallengeCompletion ucc")
    long countCompletedChallenges();

    // Find completion by user and challenge
    Optional<UserChallengeCompletion> findByUserAndChallenge(User user, Challenge challenge);

    // Find completion by user ID and challenge ID
    @Query("SELECT ucc FROM UserChallengeCompletion ucc WHERE ucc.user.id = :userId AND ucc.challenge.id = :challengeId")
    Optional<UserChallengeCompletion> findByUserIdAndChallengeId(@Param("userId") Long userId, @Param("challengeId") Long challengeId);

    // Check if user has completed a specific challenge
    boolean existsByUserAndChallenge(User user, Challenge challenge);

    // Check if user has completed a specific challenge by IDs
    @Query("SELECT COUNT(ucc) > 0 FROM UserChallengeCompletion ucc WHERE ucc.user.id = :userId AND ucc.challenge.id = :challengeId")
    boolean existsByUserIdAndChallengeId(@Param("userId") Long userId, @Param("challengeId") Long challengeId);

    // Get completion score for a user and challenge
    @Query("SELECT ucc.score FROM UserChallengeCompletion ucc WHERE ucc.user.id = :userId AND ucc.challenge.id = :challengeId")
    Optional<Integer> findScoreByUserIdAndChallengeId(@Param("userId") Long userId, @Param("challengeId") Long challengeId);


    // Add this method
    @Query("SELECT ucc FROM UserChallengeCompletion ucc WHERE ucc.challenge.id = :challengeId")
    List<UserChallengeCompletion> findByChallengeId(@Param("challengeId") Long challengeId);

    // Or if you prefer named query method:
    List<UserChallengeCompletion> findByChallenge_Id(Long challengeId);

    // Add this method to your UserChallengeCompletionRepository
    @Query("SELECT COUNT(ucc) FROM UserChallengeCompletion ucc WHERE ucc.user.id = :userId AND ucc.challenge.id = :challengeId")
    long countByUserIdAndChallengeId(@Param("userId") Long userId, @Param("challengeId") Long challengeId);
}
