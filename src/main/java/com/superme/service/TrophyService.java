package com.superme.service;

import com.superme.dto.TrophyResponse;
import com.superme.dto.TrophySummary;
import com.superme.enums.Category;
import com.superme.enums.Difficulty;
import com.superme.model.Trophy;
import com.superme.dto.ChallengeResultDTO;
import com.superme.repository.TrophyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrophyService {

    private final TrophyRepository trophyRepository;

    // Overloaded method without categoryIconUrl for backward compatibility
    @Transactional
    public Trophy awardTrophyForChallenge(ChallengeResultDTO challengeResult, String userId,
                                          Category category, Difficulty difficulty) {
        // Generate a challengeId for backward compatibility
        String challengeId = "challenge_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
        return awardTrophyForChallenge(challengeId, challengeResult, userId, category, difficulty, null);
    }

    // Overloaded method without challengeId for backward compatibility
    @Transactional
    public Trophy awardTrophyForChallenge(ChallengeResultDTO challengeResult, String userId,
                                          Category category, Difficulty difficulty,
                                          String categoryIconUrl) {
        // Generate a challengeId for backward compatibility
        String challengeId = "challenge_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
        return awardTrophyForChallenge(challengeId, challengeResult, userId, category, difficulty, categoryIconUrl);
    }

    @Transactional
    public Trophy awardTrophyForChallenge(String challengeId, ChallengeResultDTO challengeResult, String userId,
                                          Category category, Difficulty difficulty,
                                          String categoryIconUrl) {
        // Use the provided challengeId (this should come from ChallengeService)
        // Get title - you might need to pass this too or fetch from Challenge entity
        String title = "Challenge Completed";

        // Check if user already earned trophy for this challenge
        if (trophyRepository.existsByUserIdAndChallengeId(userId, challengeId)) {
            log.info("User {} already earned trophy for challenge {}", userId, challengeId);
            return trophyRepository.findByUserIdAndChallengeId(userId, challengeId)
                    .orElseThrow(() -> new RuntimeException("Trophy not found"));
        }

        // Only award trophy if challenge was completed successfully
        if (!challengeResult.isSuccess()) {
            throw new IllegalArgumentException("Challenge was not completed successfully");
        }

        // Use the trophies from ChallengeResultDTO
        int trophyCount = challengeResult.getTrophiesEarned();

        Trophy trophy = Trophy.builder()
                .userId(userId)
                .challengeId(challengeId)
                .title(title)
                .trophyCount(trophyCount)
                .category(category)
                .difficulty(difficulty)
                .categoryIconUrl(categoryIconUrl) // This should be challenge.getThumbnailImageUrl()
                .earnedDate(LocalDateTime.now())
                .build();

        Trophy savedTrophy = trophyRepository.save(trophy);
        log.info("Awarded {} trophies to user {} for completing challenge {}",
                trophyCount, userId, challengeId);

        return savedTrophy;
    }

    private int getTrophyCountByDifficulty(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 2;
            case MEDIUM -> 3;
            case HARD -> 4;
        };
    }

    // Overloaded method without challengeIds for backward compatibility
    @Transactional
    public List<Trophy> awardTrophiesForChallenges(List<ChallengeResultDTO> challengeResults,
                                                   String userId, Category category,
                                                   Difficulty difficulty, String categoryIconUrl) {
        List<Trophy> trophies = new ArrayList<>();

        for (ChallengeResultDTO result : challengeResults) {
            if (result.isSuccess()) {
                // Generate unique challengeId for each result
                String challengeId = "challenge_" + System.currentTimeMillis() + "_" +
                        UUID.randomUUID().toString().substring(0, 8);
                Trophy trophy = awardTrophyForChallenge(challengeId, result, userId,
                        category, difficulty, categoryIconUrl);
                trophies.add(trophy);
            }
        }

        return trophies;
    }

    // Process multiple challenge results (for batch operations)
    @Transactional
    public List<Trophy> awardTrophiesForChallenges(List<String> challengeIds,
                                                   List<ChallengeResultDTO> challengeResults,
                                                   String userId, Category category,
                                                   Difficulty difficulty, String categoryIconUrl) {
        // Ensure both lists have the same size
        if (challengeIds.size() != challengeResults.size()) {
            throw new IllegalArgumentException("challengeIds and challengeResults must have the same size");
        }

        List<Trophy> trophies = new ArrayList<>();

        for (int i = 0; i < challengeResults.size(); i++) {
            ChallengeResultDTO result = challengeResults.get(i);
            String challengeId = challengeIds.get(i);

            if (result.isSuccess()) {
                Trophy trophy = awardTrophyForChallenge(challengeId, result, userId,
                        category, difficulty, categoryIconUrl);
                trophies.add(trophy);
            }
        }

        return trophies;
    }

    public List<Trophy> getUserTrophies(String userId) {
        return trophyRepository.findByUserIdOrderByEarnedDateDesc(userId);
    }

    public List<Trophy> getUserTrophiesByMonth(String userId, int year, int month) {
        return trophyRepository.findByUserIdAndYearAndMonth(userId, year, month);
    }

    public Integer getUserTotalTrophyCount(String userId) {
        Integer total = trophyRepository.getTotalTrophyCountByUserId(userId);
        return total != null ? total : 0;
    }

    public boolean hasUserCompletedChallenge(String userId, String challengeId) {
        return trophyRepository.existsByUserIdAndChallengeId(userId, challengeId);
    }

    public List<Trophy> getUserTrophiesByCategory(String userId, Category category) {
        return trophyRepository.findByUserIdAndCategory(userId, category);
    }

    public List<Trophy> getUserTrophiesByDifficulty(String userId, Difficulty difficulty) {
        return trophyRepository.findByUserIdAndDifficulty(userId, difficulty);
    }

    // Get user's trophy summary including total count and recent trophies
    public TrophySummary getTrophySummary(String userId) {
        Integer totalTrophies = getUserTotalTrophyCount(userId);
        List<Trophy> recentTrophies = getUserTrophies(userId).stream()
                .limit(5) // Last 5 trophies
                .collect(Collectors.toList());

        return TrophySummary.builder()
                .totalTrophies(totalTrophies)
                .recentTrophies(recentTrophies.stream()
                        .map(TrophyResponse::fromEntity)
                        .collect(Collectors.toList()))
                .build();
    }
}