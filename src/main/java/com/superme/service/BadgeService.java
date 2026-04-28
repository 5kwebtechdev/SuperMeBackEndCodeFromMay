package com.superme.service;

import com.superme.dto.*;
import com.superme.exception.BusinessException;
import com.superme.model.*;
import com.superme.enums.*;
import com.superme.repository.*;
import com.superme.websocket.BadgeAwardedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final BadgeProgressRepository badgeProgressRepository;
    private final BadgeDefinitionRepository badgeDefinitionRepository;
    private final UserRepository userRepository;
    private final HabitRepository habitRepository;
    private final TaskRepository taskRepository;
    private final ChallengeRepository challengeRepository;
    private final ReelsRepository reelsRepository;
    private final FeatureUsageRepository featureUsageRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;


    private static final String APP_LINK = "https://superme.app/download";

    // Badge level configurations
    private static final Map<String, List<Integer>> BADGE_LEVELS = createBadgeLevels();
    // Celebration constants
    private static final String CELEBRATION_TITLE = "Shine Bright!";

    private static Map<String, List<Integer>> createBadgeLevels() {
        Map<String, List<Integer>> levels = new HashMap<>();
        levels.put("STREAK_KEEPER", Arrays.asList(7, 21, 50, 100, 365));
        levels.put("COIN_COLLECTOR", Arrays.asList(500, 2000, 6000, 15000, 40000));
        levels.put("QUIZ_ACE", Arrays.asList(10, 30, 75, 200, 500));
        levels.put("PET_COLLECTOR", Arrays.asList(3, 6, 10, 15, 20));
        levels.put("CONTENT_CREATOR", Arrays.asList(5, 15, 35, 80, 180));
        levels.put("KNOWLEDGE_SEEKER", Arrays.asList(25, 70, 180, 450, 1000));
        levels.put("HABIT_MASTER", Arrays.asList(100, 300, 800, 2000, 5000));
        levels.put("DAILY_CHAMPION", Arrays.asList(7, 14, 30, 90, 180));
        levels.put("FEATURE_EXPLORER", Arrays.asList(1, 10, 30, 75, 200));
        levels.put("PUZZLE_MASTER", Arrays.asList(50, 140, 350, 850, 2000));
        levels.put("PET_STYLIST", Arrays.asList(10, 25, 55, 120, 250));
        levels.put("TASK_TERMINATOR", Arrays.asList(50, 140, 350, 850, 2000));
        levels.put("LEARNING_ENTHUSIAST", Arrays.asList(14, 30, 60, 100, 200));
        levels.put("SOCIAL_STAR", Arrays.asList(100, 350, 1000, 3000, 8000));
        return levels;
    }

    @Transactional
    public void triggerBadgeUpdate(
            Long userId,
            ActivityType activityType,
            int coinsEarned,
            String feature
    ) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("Badge trigger skipped: user not found {}", userId);
                return;
            }

            // Eligibility check (CHILD / SELF only)
            if (!isUserEligibleForBadges(user)) {
                return;
            }

            switch (activityType) {

                // Learning activities
                case QUIZ_COMPLETED, PUZZLE_COMPLETED, ARTICLE_COMPLETED -> {
                    updateLearningStreak(user);
                    updateActivityStreak(user);
                }

                // Non-learning activities
                case HABIT_COMPLETED, TASK_COMPLETED,LESSON_COMPLETED -> {
                    updateActivityStreak(user);
                }
            }

            // 🔑 FEATURE EXPLORER tracking
            updateFeatureUsage(userId, activityType);

            // 🔑 SINGLE evaluation point
            checkAndAwardBadges(userId);

            log.info(
                    "Badge evaluation triggered for user {}, activity={}, coins={}, feature={}",
                    userId, activityType, coinsEarned, feature
            );

        } catch (Exception e) {
            log.error(
                    "Error triggering badge update for user {}, activity={}",
                    userId, activityType, e
            );
        }
    }

    /**
     * MAIN METHOD: Get badges with celebration card for badge page
     */
    @Transactional(readOnly = true)
    public BadgeText getBadgesForUser(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return createBadgeTextWithMotivation();
        }

        User user = userOpt.get();

        if (!isUserEligibleForBadges(user)) {
            return createBadgeTextWithMotivation();
        }

        List<BadgeResponseDTO> badgeResponses = new ArrayList<>();
        List<BadgeDefinition> allDefinitions = badgeDefinitionRepository.findByActiveTrueWithAgeGroups();

        for (BadgeDefinition definition : allDefinitions) {
//            if (definition.getApplicableAgeGroups() == null ||
//                    !definition.getApplicableAgeGroups().contains(user.getAgeGroup())) {
//                continue;
//            }

            // Use level-specific targets instead of final target
            BadgeLevelInfo levelInfo = getCurrentLevelAndProgress(user, definition);
            int currentLevel = levelInfo.getCurrentLevel();
            int currentProgress = levelInfo.getCurrentProgress();
            int targetForCurrentLevel = levelInfo.getTargetForCurrentLevel();

            boolean isMaxLevel = currentLevel >= 5 && currentProgress >= targetForCurrentLevel;

            // Check if user has earned ANY level of this badge
            boolean hasEarnedAnyLevel = badgeRepository.existsByUserAndBadgeType(user, definition.getName());
            boolean isEarned = hasEarnedAnyLevel || (currentProgress >= targetForCurrentLevel && !isMaxLevel);

            String levelText = buildLevelText(currentLevel, 5);
            String achievementText = getAchievementScreenText(definition.getName(),currentLevel);
            String description = getBadgeDescription(definition.getName(), currentLevel, currentProgress, targetForCurrentLevel);

            BadgeResponseDTO response = BadgeResponseDTO.builder()
                    .title(definition.getDisplayName())
                    .levelText(levelText)
                    .description(description)
                    .achievementScreenText(achievementText)
                    .icon("/badges/" + definition.getName().toLowerCase() + ".png")
                    .progress(currentProgress)
                    .target(targetForCurrentLevel) // Use LEVEL-SPECIFIC target
                    .isEarned(isEarned)
                    .isMaxLevel(isMaxLevel)
                    .currentLevel(currentLevel)
                    .totalLevels(5)
                    .celebrationTitle(buildCelebrationTitle(definition, currentLevel))
                    .celebrationMessage(buildCelebrationMessage(definition, currentLevel))
                    .shareMessage(buildShareMessage(definition.getName(), currentLevel))
                    .build();

            badgeResponses.add(response);
        }

        List<BadgeResponseDTO> sortedBadges = sortBadgesByProgress(badgeResponses);
        CelebrationDTO celebrationCard = buildCelebrationCard(user);

        return BadgeText.builder()
                .celebrationCard(celebrationCard)
                .badges(sortedBadges)
                .build();
    }

    /**
     * Get badge description with dynamic progress text
     */
    private String getBadgeDescription(
            String badgeName,
            int currentLevel,
            int currentProgress,
            int target
    ) {
        // ✅ CASE 1: Not started → show static instruction
        if (currentProgress == 0) {
            return getStaticLevelDescription(badgeName, currentLevel);
        }

        // ✅ CASE 2: In progress → show remaining text
        if (currentProgress < target) {
            int remaining = target - currentProgress;
            return remaining + " more " + getUnit(badgeName) + " to earn this badge";
        }

        // ✅ CASE 3: Completed → show static instruction (or next-level text)
        return getStaticLevelDescription(badgeName, currentLevel);
    }

    private String getStaticLevelDescription(String badgeName, int currentLevel) {

    // Return initial text for current level when not started or completed
        return switch (badgeName) {
            case "STREAK_KEEPER" -> switch (currentLevel) {
                case 1 -> "Open the app for 7 days.";
                case 2 -> "Open the app for 21 days.";
                case 3 -> "Open the app for 50 days.";
                case 4 -> "Open the app for 100 days.";
                case 5 -> "Open the app for 365 days.";
                default -> "";
            };
            case "COIN_COLLECTOR" -> switch (currentLevel) {
                case 1 -> "Earn 500 coins.";
                case 2 -> "Earn 2,000 coins.";
                case 3 -> "Earn 6,000 coins.";
                case 4 -> "Earn 15,000 coins.";
                case 5 -> "Earn 40,000 coins.";
                default -> "";
            };
            case "QUIZ_ACE" -> switch (currentLevel) {
                case 1 -> "Ace 10 quizzes perfectly.";
                case 2 -> "Ace 30 quizzes perfectly.";
                case 3 -> "Ace 75 quizzes perfectly.";
                case 4 -> "Ace 200 quizzes perfectly.";
                case 5 -> "Ace 500 quizzes perfectly.";
                default -> "";
            };
            case "PET_COLLECTOR" -> switch (currentLevel) {
                case 1 -> "Purchase 3 different pets.";
                case 2 -> "Purchase 6 different pets.";
                case 3 -> "Purchase 10 different pets.";
                case 4 -> "Purchase 15 different pets.";
                case 5 -> "Purchase 20 different pets.";
                default -> "";
            };
            case "CONTENT_CREATOR" -> switch (currentLevel) {
                case 1 -> "Upload 5 reels";
                case 2 -> "Upload 15 reels";
                case 3 -> "Upload 35 reels";
                case 4 -> "Upload 80 reels";
                case 5 -> "Upload 180 reels";
                default -> "";
            };
            case "KNOWLEDGE_SEEKER" -> switch (currentLevel) {
                case 1 -> "Complete 25 articles.";
                case 2 -> "Complete 70 articles.";
                case 3 -> "Complete 180 articles.";
                case 4 -> "Complete 450 articles.";
                case 5 -> "Complete 1,000 articles.";
                default -> "";
            };
            case "HABIT_MASTER" -> switch (currentLevel) {
                case 1 -> "Complete 100 total habits.";
                case 2 -> "Complete 300 total habits.";
                case 3 -> "Complete 800 total habits.";
                case 4 -> "Complete 2,000 total habits.";
                case 5 -> "Complete 5,000 total habits.";
                default -> "";
            };
            case "DAILY_CHAMPION" -> switch (currentLevel) {
                case 1 -> "Complete activities for 7 days.";
                case 2 -> "Complete activities for 14 days.";
                case 3 -> "Complete activities for 30 days.";
                case 4 -> "Complete activities for 90 days.";
                case 5 -> "Complete activities for 180 days.";
                default -> "";
            };
            case "FEATURE_EXPLORER" -> switch (currentLevel) {
                case 1 -> "Use habits, tasks, quiz, puzzle, article once.";
                case 2 -> "Use each section 10 times.";
                case 3 -> "Use each section 30 times.";
                case 4 -> "Use each section 75 times.";
                case 5 -> "Use each section 200 times.";
                default -> "";
            };
            case "PUZZLE_MASTER" -> switch (currentLevel) {
                case 1 -> "Complete 50 puzzles successfully.";
                case 2 -> "Complete 140 puzzles successfully.";
                case 3 -> "Complete 350 puzzles successfully.";
                case 4 -> "Complete 850 puzzles successfully.";
                case 5 -> "Complete 2,000 puzzles successfully.";
                default -> "";
            };
            case "PET_STYLIST" -> switch (currentLevel) {
                case 1 -> "Purchase 10 different pet accessories.";
                case 2 -> "Purchase 25 different pet accessories.";
                case 3 -> "Purchase 55 different pet accessories.";
                case 4 -> "Purchase 120 different pet accessories.";
                case 5 -> "Purchase 250 different pet accessories.";
                default -> "";
            };
            case "TASK_TERMINATOR" -> switch (currentLevel) {
                case 1 -> "Complete 50 tasks successfully.";
                case 2 -> "Complete 140 tasks successfully.";
                case 3 -> "Complete 350 tasks successfully.";
                case 4 -> "Complete 850 tasks successfully.";
                case 5 -> "Complete 2,000 tasks successfully.";
                default -> "";
            };
            case "LEARNING_ENTHUSIAST" -> switch (currentLevel) {
                case 1 -> "Complete learning activities for 14 days.";
                case 2 -> "Complete learning activities for 30 days.";
                case 3 -> "Complete learning activities for 60 days.";
                case 4 -> "Complete learning activities for 100 days.";
                case 5 -> "Complete learning activities for 200 days.";
                default -> "";
            };
            case "SOCIAL_STAR" -> switch (currentLevel) {
                case 1 -> "Receive 100+ total likes on reels.";
                case 2 -> "Receive 350+ total likes on reels.";
                case 3 -> "Receive 1,000+ total likes on reels.";
                case 4 -> "Receive 3,000+ total likes on reels.";
                case 5 -> "Receive 8,000+ total likes on reels.";
                default -> "";
            };
            default -> "";
        };
    }

    /**
     * Get unit for progress text
     */
    private String getUnit(String badgeName) {
        return switch (badgeName) {
            case "STREAK_KEEPER", "DAILY_CHAMPION", "LEARNING_ENTHUSIAST" -> "days";
            case "COIN_COLLECTOR" -> "coins";
            case "QUIZ_ACE" -> "quizzes";
            case "PET_COLLECTOR" -> "pets";
            case "CONTENT_CREATOR" -> "reels";
            case "KNOWLEDGE_SEEKER" -> "articles";
            case "HABIT_MASTER" -> "habits";
            case "PUZZLE_MASTER" -> "puzzles";
            case "PET_STYLIST" -> "accessories";
            case "TASK_TERMINATOR" -> "tasks";
            case "SOCIAL_STAR" -> "likes";
            case "FEATURE_EXPLORER" -> "activities";
            default -> "";
        };
    }

    /**
     * Build celebration card for user - ALWAYS show celebration with motivational message
     */
    private CelebrationDTO buildCelebrationCard(User user) {

        List<Badge> earnedBadges =
                badgeRepository.findByUserOrderByEarnedAtDesc(user);

        if (earnedBadges.isEmpty()) {
            return createMotivationalCelebrationDTO();
        }

        return createCelebrationDTO(earnedBadges);
    }


    /**
     * Create celebration DTO when badges are earned
     */
    private CelebrationDTO createCelebrationDTO(List<Badge> newBadges) {

        // Collect badge names for UI
        List<String> badgeNames = newBadges.stream()
                .map(Badge::getName)
                .collect(Collectors.toList());

        // Most recent badge = first one (already sorted by earnedAt desc)
        Badge mostRecentBadge = newBadges.get(0);

        int otherBadgesCount = badgeNames.size() - 1;

        String message;
        if (otherBadgesCount == 0) {
            message = String.format(
                    "You just earned the \"%s\" badge! Show it off to your friends.",
                    mostRecentBadge.getName()
            );
        } else if (otherBadgesCount == 1) {
            message = String.format(
                    "You just earned the \"%s\" and 1 other badge! Show it off to your friends.",
                    mostRecentBadge.getName()
            );
        } else {
            message = String.format(
                    "You just earned the \"%s\" and %d other badges! Show it off to your friends.",
                    mostRecentBadge.getName(),
                    otherBadgesCount
            );
        }

        // ✅ Use the ONE canonical share message builder
        String shareMessage = buildShareMessage(
                mostRecentBadge.getBadgeType(),
                mostRecentBadge.getLevel()
        );

        return CelebrationDTO.builder()
                .title(CELEBRATION_TITLE)      // "Shine Bright!"
                .message(message)
                .shareMessage(shareMessage)
                .earnedBadges(badgeNames)
                .showCelebration(true)
                .build();
    }


    /**
     * Create motivational celebration DTO when no badges are earned
     */
    private CelebrationDTO createMotivationalCelebrationDTO() {
        return CelebrationDTO.builder()
                .title(CELEBRATION_TITLE)
                .message("Keep going, You're doing great!!")
                .shareMessage("I'm making progress on SuperMe! 🚀")
                .earnedBadges(Collections.emptyList())
                .showCelebration(true)
                .build();
    }

    /**
     * Create BadgeText with motivational message
     */
    private BadgeText createBadgeTextWithMotivation() {
        return BadgeText.builder()
                .celebrationCard(createMotivationalCelebrationDTO())
                .badges(Collections.emptyList())
                .build();
    }

    /**
     * Create share message for a badge - EXACT FORMAT AS SPECIFIED
     */
    private String createShareMessage(Badge badge) {
        String badgeName = badge.getName();
        String achievementText = buildShareMessage(badge.getName(), badge.getLevel());

        return String.format(
                "🎉 I just earned the %s badge on SuperMe! 🎉%n%nLevel %d Milestone: %s%n%nDownload SuperMe: %s",
                badgeName, badge.getLevel(), achievementText, APP_LINK
        );
    }



    /**
     * Get achievement text for share message
     */
    public String getAchievementScreenText(String badgeName, int level) {

        return switch (badgeName) {

            // 1️⃣ STREAK_KEEPER
            case "STREAK_KEEPER" -> switch (level) {
                case 1 -> "You opened the app for 7 consecutive days.";
                case 2 -> "You opened the app for 21 consecutive days.";
                case 3 -> "You opened the app for 50 consecutive days.";
                case 4 -> "You opened the app for 100 consecutive days.";
                case 5 -> "You opened the app for 365 consecutive days.";
                default -> "";
            };

            // 2️⃣ DAILY_CHAMPION
            case "DAILY_CHAMPION" -> switch (level) {
                case 1 -> "You completed daily activities for 7 days.";
                case 2 -> "You completed daily activities for 14 days.";
                case 3 -> "You completed daily activities for 30 days.";
                case 4 -> "You completed daily activities for 90 days.";
                case 5 -> "You completed daily activities for 180 days.";
                default -> "";
            };

            // 3️⃣ LEARNING_ENTHUSIAST
            case "LEARNING_ENTHUSIAST" -> switch (level) {
                case 1 -> "You completed learning activities for 14 days.";
                case 2 -> "You completed learning activities for 30 days.";
                case 3 -> "You completed learning activities for 60 days.";
                case 4 -> "You completed learning activities for 100 days.";
                case 5 -> "You completed learning activities for 200 days.";
                default -> "";
            };

            // 4️⃣ COIN_COLLECTOR
            case "COIN_COLLECTOR" -> switch (level) {
                case 1 -> "You collected 500 coins.";
                case 2 -> "You collected 2,000 coins.";
                case 3 -> "You collected 6,000 coins.";
                case 4 -> "You collected 15,000 coins.";
                case 5 -> "You collected 40,000 coins.";
                default -> "";
            };

            // 5️⃣ QUIZ_ACE
            case "QUIZ_ACE" -> switch (level) {
                case 1 -> "You aced 10 quizzes perfectly.";
                case 2 -> "You aced 30 quizzes perfectly.";
                case 3 -> "You aced 75 quizzes perfectly.";
                case 4 -> "You aced 200 quizzes perfectly.";
                case 5 -> "You aced 500 quizzes perfectly.";
                default -> "";
            };

            // 6️⃣ CONTENT_CREATOR
            case "CONTENT_CREATOR" -> switch (level) {
                case 1 -> "You successfully uploaded 5 reels.";
                case 2 -> "You successfully uploaded 15 reels.";
                case 3 -> "You successfully uploaded 35 reels.";
                case 4 -> "You successfully uploaded 80 reels.";
                case 5 -> "You successfully uploaded 180 reels.";
                default -> "";
            };

            // 7️⃣ KNOWLEDGE_SEEKER
            case "KNOWLEDGE_SEEKER" -> switch (level) {
                case 1 -> "You completed 25 articles.";
                case 2 -> "You completed 70 articles.";
                case 3 -> "You completed 180 articles.";
                case 4 -> "You completed 450 articles.";
                case 5 -> "You completed 1,000 articles.";
                default -> "";
            };

            // 8️⃣ HABIT_MASTER
            case "HABIT_MASTER" -> switch (level) {
                case 1 -> "You completed 100 habits.";
                case 2 -> "You completed 300 habits.";
                case 3 -> "You completed 800 habits.";
                case 4 -> "You completed 2,000 habits.";
                case 5 -> "You completed 5,000 habits.";
                default -> "";
            };

            // 9️⃣ TASK_TERMINATOR
            case "TASK_TERMINATOR" -> switch (level) {
                case 1 -> "You completed 50 tasks.";
                case 2 -> "You completed 140 tasks.";
                case 3 -> "You completed 350 tasks.";
                case 4 -> "You completed 850 tasks.";
                case 5 -> "You completed 2,000 tasks.";
                default -> "";
            };

            // 🔟 PUZZLE_MASTER
            case "PUZZLE_MASTER" -> switch (level) {
                case 1 -> "You completed 50 puzzles.";
                case 2 -> "You completed 140 puzzles.";
                case 3 -> "You completed 350 puzzles.";
                case 4 -> "You completed 850 puzzles.";
                case 5 -> "You completed 2,000 puzzles.";
                default -> "";
            };

            // 1️⃣1️⃣ FEATURE_EXPLORER
            case "FEATURE_EXPLORER" -> switch (level) {
                case 1 -> "You explored all app features at least once.";
                case 2 -> "You used every feature 10 times.";
                case 3 -> "You used every feature 30 times.";
                case 4 -> "You used every feature 75 times.";
                case 5 -> "You used every feature 200 times.";
                default -> "";
            };

            // 1️⃣2️⃣ SOCIAL_STAR
            case "SOCIAL_STAR" -> switch (level) {
                case 1 -> "You received over 100 likes on your reels.";
                case 2 -> "You received over 350 likes on your reels.";
                case 3 -> "You received over 1,000 likes on your reels.";
                case 4 -> "You received over 3,000 likes on your reels.";
                case 5 -> "You received over 8,000 likes on your reels.";
                default -> "";
            };

            // 1️⃣3️⃣ PET_COLLECTOR  (UNCHANGED LOGIC)
            case "PET_COLLECTOR" -> switch (level) {
                case 1 -> "You collected 3 different pets.";
                case 2 -> "You collected 6 different pets.";
                case 3 -> "You collected 10 different pets.";
                case 4 -> "You collected 15 different pets.";
                case 5 -> "You collected 20 different pets.";
                default -> "";
            };

            // 1️⃣4️⃣ PET_STYLIST (UNCHANGED LOGIC)
            case "PET_STYLIST" -> switch (level) {
                case 1 -> "You unlocked 10 pet accessories.";
                case 2 -> "You unlocked 25 pet accessories.";
                case 3 -> "You unlocked 55 pet accessories.";
                case 4 -> "You unlocked 120 pet accessories.";
                case 5 -> "You unlocked 250 pet accessories.";
                default -> "";
            };

            default -> "";
        };
    }



    private String buildShareMessage(
            String badgeName,
            int level
    ) {
        String achievementText =
                getAchievementTextForShare(badgeName, level);

        return String.format(
                "🎉 I just earned the %s badge on SuperMe! 🎉%n%n" +
                        "Level %d Milestone: %s%n%n" +
                        "Download SuperMe: %s",
                badgeName,
                level,
                achievementText,
                APP_LINK
        );
    }


    private String getAchievementTextForShare(String badgeName, int level) {

        return switch (badgeName) {

            case "STREAK_KEEPER" -> switch (level) {
                case 1 -> "7 days in a row! Consistency unlocked 🔥";
                case 2 -> "21-day streak going strong 💪";
                case 3 -> "50 days of discipline! 🚀";
                case 4 -> "100-day streak achieved 🏆";
                case 5 -> "365 days strong — legendary streak 👑";
                default -> "";
            };

            case "DAILY_CHAMPION" -> switch (level) {
                case 1 -> "7 days of daily activity done! 💥";
                case 2 -> "14 days of showing up 💪";
                case 3 -> "30 days consistent! 🔥";
                case 4 -> "90 days unstoppable 🚀";
                case 5 -> "180 days of pure dedication 🏆";
                default -> "";
            };

            case "LEARNING_ENTHUSIAST" -> switch (level) {
                case 1 -> "14 days of learning streak 📚";
                case 2 -> "30 learning days completed 🚀";
                case 3 -> "60 days smarter 💡";
                case 4 -> "100 days of growth 🧠";
                case 5 -> "200 days of learning mastery 🏆";
                default -> "";
            };

            case "COIN_COLLECTOR" -> switch (level) {
                case 1 -> "500 coins collected 💰";
                case 2 -> "2,000 coins and counting 💸";
                case 3 -> "6,000 coins stacked 🪙";
                case 4 -> "15,000 coins milestone 🎉";
                case 5 -> "40,000 coins legend status 👑";
                default -> "";
            };

            case "QUIZ_ACE" -> switch (level) {
                case 1 -> "10 quizzes aced 🎯";
                case 2 -> "30 perfect quizzes 🔥";
                case 3 -> "75 quizzes mastered 🧠";
                case 4 -> "200 quiz wins 🚀";
                case 5 -> "500 quizzes aced — unreal 🏆";
                default -> "";
            };

            case "CONTENT_CREATOR" -> switch (level) {
                case 1 -> "Uploaded my first 5 reels 🎬";
                case 2 -> "15 reels live 🚀";
                case 3 -> "35 reels done 🎨";
                case 4 -> "80 reels — creator mode ON 🔥";
                case 5 -> "180 reels strong 💯";
                default -> "";
            };

            case "KNOWLEDGE_SEEKER" -> switch (level) {
                case 1 -> "25 articles explored 📖";
                case 2 -> "70 articles completed 🚀";
                case 3 -> "180 articles mastered 🧠";
                case 4 -> "450 articles deep 💡";
                case 5 -> "1,000 articles — knowledge beast 🏆";
                default -> "";
            };

            case "HABIT_MASTER" -> switch (level) {
                case 1 -> "100 habits done 💪";
                case 2 -> "300 habits crushed 🔥";
                case 3 -> "800 habits strong 🚀";
                case 4 -> "2,000 habits mastered 🏆";
                case 5 -> "5,000 habits legend 👑";
                default -> "";
            };

            case "TASK_TERMINATOR" -> switch (level) {
                case 1 -> "50 tasks completed ✅";
                case 2 -> "140 tasks smashed 💥";
                case 3 -> "350 tasks conquered 🔥";
                case 4 -> "850 tasks unstoppable 🚀";
                case 5 -> "2,000 tasks beast mode 🏆";
                default -> "";
            };

            case "PUZZLE_MASTER" -> switch (level) {
                case 1 -> "50 puzzles solved 🧩";
                case 2 -> "140 puzzles cracked 🔥";
                case 3 -> "350 puzzles mastered 🧠";
                case 4 -> "850 puzzles genius mode 🚀";
                case 5 -> "2,000 puzzles legend 🏆";
                default -> "";
            };

            case "FEATURE_EXPLORER" -> switch (level) {
                case 1 -> "Explored all features 🚀";
                case 2 -> "Used every feature 10 times 🔥";
                case 3 -> "30× feature mastery 💡";
                case 4 -> "75× full app power 💪";
                case 5 -> "200× feature legend 🏆";
                default -> "";
            };

            case "SOCIAL_STAR" -> switch (level) {
                case 1 -> "100 likes achieved ⭐";
                case 2 -> "350 likes milestone 💥";
                case 3 -> "1,000 likes moment 🎉";
                case 4 -> "3,000 likes — fame rising 😎";
                case 5 -> "8,000 likes superstar 🌟";
                default -> "";
            };

            case "PET_COLLECTOR" -> switch (level) {
                case 1 -> "Collected 3 pets 🐶";
                case 2 -> "6 pets unlocked 🐾";
                case 3 -> "10 pets squad 🐕";
                case 4 -> "15 pets family 🏡";
                case 5 -> "20 pets legend 🏆";
                default -> "";
            };

            case "PET_STYLIST" -> switch (level) {
                case 1 -> "10 pet accessories unlocked 🎀";
                case 2 -> "25 accessories styled ✨";
                case 3 -> "55 accessories collected 🐾";
                case 4 -> "120 accessories mastered 🔥";
                case 5 -> "250 accessories fashion king 👑";
                default -> "";
            };

            default -> "";
        };
    }



    // Helper methods for level calculation and text building
    private int calculateCurrentLevel(int currentProgress, List<Integer> levelTargets) {
        if (currentProgress <= 0) return 0;

        for (int i = levelTargets.size() - 1; i >= 0; i--) {
            if (currentProgress >= levelTargets.get(i)) {
                return i + 1;
            }
        }
        return 1;
    }

    private String buildLevelText(int currentLevel, int maxLevel) {
        return "Level " + currentLevel;
    }

    private String buildCelebrationTitle(BadgeDefinition definition, int currentLevel) {
        if (currentLevel == 1) return "First Level Unlocked!";
        if (currentLevel >= definition.getMaxLevel()) return "Master Achievement!";
        return "Level " + currentLevel + " Completed!";
    }

    private String buildCelebrationMessage(BadgeDefinition definition, int currentLevel) {
        if (currentLevel == 1) return "You've started your " + definition.getDisplayName() + " journey!";
        if (currentLevel >= definition.getMaxLevel()) {
            return "Congratulations! You've mastered " + definition.getDisplayName() + "!";
        }
        return "Awesome! You've reached level " + currentLevel + " in " + definition.getDisplayName() + "!";
    }

    /**
     * BADGE ORDERING LOGIC - EXACTLY AS SPECIFIED
     */
    private List<BadgeResponseDTO> sortBadgesByProgress(List<BadgeResponseDTO> badges) {
        return badges.stream()
                .sorted((b1, b2) -> {

                    boolean b1Max = Boolean.TRUE.equals(b1.getIsMaxLevel());
                    boolean b2Max = Boolean.TRUE.equals(b2.getIsMaxLevel());

                    if (b1Max && !b2Max) return 1;
                    if (b2Max && !b1Max) return -1;

                    // ✅ ONLY JUST-COMPLETED LEVEL MOVES DOWN
                    boolean b1JustCompleted =
                            b1.getProgress().equals(b1.getTarget()) && !b1Max;
                    boolean b2JustCompleted =
                            b2.getProgress().equals(b2.getTarget()) && !b2Max;

                    if (b1JustCompleted && !b2JustCompleted) return 1;
                    if (b2JustCompleted && !b1JustCompleted) return -1;

                    double p1 = (double) b1.getProgress() / b1.getTarget();
                    double p2 = (double) b2.getProgress() / b2.getTarget();

                    if (p1 != p2) {
                        return Double.compare(p2, p1);
                    }
                    return b1.getTitle().compareTo(b2.getTitle());
                })
                .toList();
    }


    /**
     * Get current level and progress for a badge - FIXED VERSION
     * Now shows the NEXT level user is working toward
     */
    private BadgeLevelInfo getCurrentLevelAndProgress(User user, BadgeDefinition definition) {
        log.info("Inside getCurrentLevelAndProgress for badge: {}", definition.getName());
        String badgeName = definition.getName();
        List<Integer> levelTargets = BADGE_LEVELS.get(badgeName);
        int currentProgress = calculateProgress(user, definition);

        // Find which level the user is currently working on
        int currentLevel = 1;
        int targetForCurrentLevel = levelTargets.get(0); // Start with level 1 target

        // Check each level to see if user has completed it
        for (int i = 0; i < levelTargets.size(); i++) {
            int levelTarget = levelTargets.get(i);

            if (currentProgress >= levelTarget) {
                // User has completed this level
                if (i < levelTargets.size() - 1) {
                    // Move to next level
                    currentLevel = i + 2;
                    targetForCurrentLevel = levelTargets.get(i + 1);
                } else {
                    // User has completed all levels
                    currentLevel = levelTargets.size();
                    targetForCurrentLevel = levelTargets.get(levelTargets.size() - 1);
                    break;
                }
            } else {
                // Found a level not yet completed
                currentLevel = i + 1;
                targetForCurrentLevel = levelTarget;
                break;
            }
        }

        log.info("Finished getCurrentLevelAndProgress for badge: {}. CurrentLevel: {}, CurrentProgress: {}, TargetForCurrentLevel: {}",
                definition.getName(), currentLevel, currentProgress, targetForCurrentLevel);

        return new BadgeLevelInfo(currentLevel, currentProgress, targetForCurrentLevel);

    }

    /**
     * Calculate progress for a specific badge definition
     */
    private int calculateProgress(User user, BadgeDefinition definition) {
        return switch (definition.getName()) {
            case "STREAK_KEEPER" -> user.getCurrentStreak();
            case "COIN_COLLECTOR" -> user.getCoins();
            case "QUIZ_ACE" -> challengeRepository.countPerfectChallengesByUser(user.getId());
            case "PET_COLLECTOR" -> user.getPet() != null ? 1 : 0; // Simplified - you'll need to track different pets
            case "CONTENT_CREATOR" -> reelsRepository.countReelsByUser(user.getId());
            case "KNOWLEDGE_SEEKER" -> challengeRepository.countCompletedArticlesByUser(user.getId());
            case "HABIT_MASTER" -> habitRepository.countCompletedHabitsByUser(user.getId());
            case "DAILY_CHAMPION" -> user.getDailyActivityStreak(); // Use current streak for daily activity
            case "FEATURE_EXPLORER" -> {FeatureUsage usage = featureUsageRepository.findById(user.getId()).orElse(null);
                yield usage == null ? 0 : usage.minUsage(); }
            case "PUZZLE_MASTER" -> challengeRepository.countCompletedPuzzlesByUser(user.getId());
            case "PET_STYLIST" -> calculatePetAccessoriesCount(user);
            case "TASK_TERMINATOR" -> taskRepository.countCompletedTasksByUser(user.getId());
            case "LEARNING_ENTHUSIAST" -> user.getLearningStreak();
            case "SOCIAL_STAR" -> {
                List<Reels> userReels = reelsRepository.findReelsByUser(user.getId());
                yield userReels.stream().mapToInt(reel -> reel.getLikedBy().size()).sum();
            }
            default -> 0;
        };
    }

    private void updateActivityStreak(User user) {
        LocalDate today = LocalDate.now();

        if (today.equals(user.getLastActivityDate())) return;

        if (today.equals(
                Optional.ofNullable(user.getLastActivityDate())
                        .map(d -> d.plusDays(1))
                        .orElse(null)
        )) {
            user.setActivityStreak(user.getActivityStreak() + 1);
        } else {
            user.setActivityStreak(1);
        }

        user.setLastActivityDate(today);
    }


    private void updateLearningStreak(User user) {
        LocalDate today = LocalDate.now();

        if (today.equals(user.getLastLearningDate())) return;

        if (today.equals(
                Optional.ofNullable(user.getLastLearningDate())
                        .map(d -> d.plusDays(1))
                        .orElse(null)
        )) {
            user.setLearningStreak(user.getLearningStreak() + 1);
        } else {
            user.setLearningStreak(1);
        }

        user.setLastLearningDate(today);
    }


    private void updateFeatureUsage(Long userId, ActivityType activityType) {

        FeatureUsage usage = featureUsageRepository
                .findById(userId)
                .orElseGet(() -> {
                    FeatureUsage f = new FeatureUsage();
                    f.setUserId(userId);
                    return f;
                });

        switch (activityType) {
            case HABIT_COMPLETED -> usage.setHabitsCount(usage.getHabitsCount() + 1);
            case TASK_COMPLETED -> usage.setTasksCount(usage.getTasksCount() + 1);
            case QUIZ_COMPLETED -> usage.setQuizCount(usage.getQuizCount() + 1);
            case PUZZLE_COMPLETED -> usage.setPuzzleCount(usage.getPuzzleCount() + 1);
            case ARTICLE_COMPLETED -> usage.setArticleCount(usage.getArticleCount() + 1);
            case LESSON_COMPLETED -> usage.setArticleCount(usage.getArticleCount() + 1);
            default -> {
                // ignore
            }
        }

        featureUsageRepository.save(usage);
    }


    /**
     * Calculate pet accessories count
     */
    private int calculatePetAccessoriesCount(User user) {
        // You'll need to implement proper pet accessories tracking
        // For now, return a basic count
        return user.getPet() != null ? 1 : 0;
    }

    /**
     * Check if user is eligible for badges (CHILD or SELF relationship)
     */
    private boolean isUserEligibleForBadges(User user) {
        return user.getRelationship() == Relationship.CHILD || user.getRelationship() == Relationship.SELF;
    }


    /**
     * Main method to check and award badges for a user with multi-level support
     * UPDATED: Now checks ALL levels and awards badges for any completed level
     */
    @Transactional
    public void checkAndAwardBadges(Long userId) {
        log.info("Inside checkAndAwardBadges for userId: {}", userId);
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return;

        User user = userOpt.get();
        if (!isUserEligibleForBadges(user)) {
            return;
        }

        List<BadgeDefinition> allDefinitions = badgeDefinitionRepository.findByActiveTrue();

        log.info("All badge definitions to check for user {}: {}", user.getId(),
                allDefinitions.stream().map(BadgeDefinition::getName).collect(Collectors.toList())
        );

        for (BadgeDefinition definition : allDefinitions) {

            BadgeLevelInfo levelInfo = getCurrentLevelAndProgress(user, definition);
            int currentProgress = levelInfo.getCurrentProgress();
            List<Integer> levelTargets = BADGE_LEVELS.get(definition.getName());

            log.info("Checking badge: {} for user: {}. CurrentProgress: {}, LevelTargets: {}",
                    definition.getName(), user.getId(), currentProgress, levelTargets);

            // Check ALL levels (1-5) to see if any have been passed
            for (int level = 1; level <= levelTargets.size(); level++) {
                int levelTarget = levelTargets.get(level - 1);
                log.info("Checking level {} for badge {}: User progress: {}, Level target: {}",
                        level, definition.getName(), currentProgress, levelTarget);

                // If user has reached or exceeded this level's target
                if (currentProgress >= levelTarget) {
                    String levelBadgeName = definition.getDisplayName() + " - Level " + level;
                    log.info("User {} has reached level {} for badge {}. Checking if badge '{}' has already been awarded.",
                            user.getId(), level, definition.getName(), levelBadgeName);

                    // Check if this specific level badge has already been awarded
                    if (!badgeRepository.existsByUserAndName(user, levelBadgeName)) {
                        // Award this level's badge
                        awardBadge(user, definition, level);
                        log.info("🎉 Awarded badge: {} to user: {}", levelBadgeName, user.getId());
                    }
                }
            }

            // Update the badge progress to current level
            updateBadgeProgress(user, definition, currentProgress, levelInfo.getCurrentLevel());
        }
        log.info("Finished checkAndAwardBadges for userId: {}", userId);
    }

    private void sendRealtimeBadgeEvent(Badge badge) {

        BadgeRealtimeEvent event = BadgeRealtimeEvent.builder()
                .badgeId(badge.getId())
                .title("Level " + badge.getLevel() + " - Badge Unlocked!")
                .badgeName(
                        badge.getName().replace(" - Level " + badge.getLevel(), "")
                )
                .message(
                        getAchievementScreenText(badge.getBadgeType(), badge.getLevel())
                )
                .icon(badge.getIcon())
                .build();

        messagingTemplate.convertAndSend(
                "/topic/badges/" + badge.getUser().getId(),
                event
        );
    }


    /**
     * Award a badge to a user with level support
     */
    public void awardBadge(User user, BadgeDefinition definition, int level) {
        log.info("Inside awardBadge for user: {}, badge: {}, level: {}", user.getId(), definition.getName(), level);
        String levelBadgeName = definition.getDisplayName() + " - Level " + level;
        String achievementText = getAchievementText(definition.getName(), level);

        log.info("Awarding badge: {} to user: {}. AchievementText: {}", levelBadgeName, user.getId(), achievementText);
        Badge badge = Badge.builder()
                .name(levelBadgeName)
                .description(achievementText)
                .icon("/badges/" + definition.getName().toLowerCase() + "_l" + level + ".png")
                .earnedAt(LocalDateTime.now())
                .user(user)
                .level(level)
                .badgeType(definition.getName())
                .popupShown(false)
                .build();
        badgeRepository.save(badge);
        // 🔔 publish domain event (safe)
        applicationEventPublisher.publishEvent(
                new BadgeAwardedEvent(badge)
        );
        log.info("Badge saved and event published for badge: {} to user: {}", levelBadgeName, user.getId());

        // Update progress with LEVEL-SPECIFIC target
        List<Integer> levelTargets = BADGE_LEVELS.get(definition.getName());
        int targetForCurrentLevel = levelTargets.get(level - 1);

        BadgeProgress progress = badgeProgressRepository
                .findByUserAndBadgeName(user, definition.getName())
                .orElse(BadgeProgress.builder()
                        .user(user)
                        .badgeName(definition.getName())
                        .badgeType(definition.getBadgeType())
                        .currentProgress(calculateProgress(user, definition))
                        .targetProgress(targetForCurrentLevel)
                        .currentLevel(level)
                        .build());

        progress.setCurrentLevel(level);
        progress.setCurrentProgress(calculateProgress(user, definition));
        progress.setTargetProgress(targetForCurrentLevel);
        badgeProgressRepository.save(progress);

        triggerCelebration(user, definition, level);

        log.info("Triggered celebration for badge: {} to user: {}", levelBadgeName, user.getId());

        // Log the badge earning for debugging
        log.info("✅ Badge earned - User: {}, Badge: {}, Level: {}",
                user.getId(), definition.getDisplayName(), level);
    }

    /**
     * Trigger celebration for earned badge
     */
    private void triggerCelebration(User user, BadgeDefinition definition, int level) {
        log.info("🎉 Celebration triggered for user {}: {} Level {}",
                user.getId(), definition.getDisplayName(), level);
    }

    /**
     * Update badge progress
     */
    private void updateBadgeProgress(User user, BadgeDefinition definition, int currentProgress, int currentLevel) {
        log.info("Inside updateBadgeProgress for user: {}, badge: {}. CurrentProgress: {}, CurrentLevel: {}",
                user.getId(), definition.getName(), currentProgress, currentLevel);
        try {
            List<Integer> levelTargets = BADGE_LEVELS.get(definition.getName());
            int targetForCurrentLevel = levelTargets.get(currentLevel - 1);

            log.info("Updating progress for badge: {}. CurrentProgress: {}, TargetForCurrentLevel: {}",
                    definition.getName(), currentProgress, targetForCurrentLevel);

            BadgeProgress progress = badgeProgressRepository
                    .findByUserAndBadgeName(user, definition.getName())
                    .orElse(BadgeProgress.builder()
                            .user(user)
                            .badgeName(definition.getName())
                            .badgeType(definition.getBadgeType())
                            .currentProgress(currentProgress)
                            .targetProgress(targetForCurrentLevel)
                            .currentLevel(currentLevel)
                            .build());

            progress.setCurrentProgress(currentProgress);
            progress.setCurrentLevel(currentLevel);
            progress.setTargetProgress(targetForCurrentLevel);
            badgeProgressRepository.save(progress);
            log.info("✅ Updated progress for badge: {} for user: {}. CurrentProgress: {}, CurrentLevel: {}, TargetProgress: {}",
                    definition.getName(), user.getId(), currentProgress, currentLevel, targetForCurrentLevel);
        } catch (Exception e) {
            log.error("❌ Error updating progress for badge {}: {}", definition.getName(), e.getMessage());
        }
    }

    /**
     * Get achievement text for earned badges
     */
    private String getAchievementText(String badgeName, int level) {
        return getAchievementTextForShare(badgeName, level); // Reuse the same method
    }

    /**
     * Scheduled daily badge check
     */
    @Scheduled(cron = "0 0 6 * * ?")
    @Transactional
    public void dailyBadgeScan() {
        log.info("🔄 Starting daily badge scan for eligible users...");
        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            try {
                if (isUserEligibleForBadges(user)) {
                    checkAndAwardBadges(user.getId());
                }
            } catch (Exception e) {
                log.error("❌ Error checking badges for user {}: {}", user.getId(), e.getMessage());
            }
        }
        log.info("✅ Daily badge scan completed");
    }
    /**
     * ORIGINAL METHODS (KEPT FOR BACKWARD COMPATIBILITY)
     */
    public void awardBadge(User user,
                           BadgeDefinition definition,
                           int level,
                           int currentProgress) {

        String levelBadgeName = definition.getDisplayName() + " - Level " + level;
        String achievementText = getAchievementText(definition.getName(), level);

        Badge badge = Badge.builder()
                .name(levelBadgeName)
                .description(achievementText)
                .icon("/badges/" + definition.getName().toLowerCase() + "_l" + level + ".png")
                .earnedAt(LocalDateTime.now())
                .user(user)
                .level(level)
                .badgeType(definition.getName())
                .build();

        badgeRepository.save(badge);

        List<Integer> levelTargets = BADGE_LEVELS.get(definition.getName());
        int targetForLevel = levelTargets.get(level - 1);

        BadgeProgress progress = badgeProgressRepository
                .findByUserAndBadgeName(user, definition.getName())
                .orElse(BadgeProgress.builder()
                        .user(user)
                        .badgeName(definition.getName())
                        .badgeType(definition.getBadgeType())
                        .build());

        progress.setCurrentLevel(level);
        progress.setCurrentProgress(currentProgress);
        progress.setTargetProgress(targetForLevel);

        badgeProgressRepository.save(progress);
    }


    public List<Badge> getBadgesByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found with ID: " + userId));
        return badgeRepository.findByUser(user);
    }

    /**
     * Helper class to store badge level information
     */
    private static class BadgeLevelInfo {
        private final int currentLevel;
        private final int currentProgress;
        private final int targetForCurrentLevel;

        public BadgeLevelInfo(int currentLevel, int currentProgress, int targetForCurrentLevel) {
            this.currentLevel = currentLevel;
            this.currentProgress = currentProgress;
            this.targetForCurrentLevel = targetForCurrentLevel;
        }

        public int getCurrentLevel() {
            return currentLevel;
        }

        public int getCurrentProgress() {
            return currentProgress;
        }

        public int getTargetForCurrentLevel() {
            return targetForCurrentLevel;
        }
    }

    /**
     * Get user badges for display in habit responses (limited to 4 milestone badges)
     */
    public List<BadgeResponseDTO> getUserBadges(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return List.of();
        }

        User user = userOpt.get();

        if (!isUserEligibleForBadges(user)) {
            return List.of();
        }

        List<BadgeResponseDTO> badgeResponses = new ArrayList<>();

        // Only get the 4 specific milestone badges
        List<String> milestoneBadgeNames = List.of("STREAK_KEEPER", "COIN_COLLECTOR", "TASK_TERMINATOR", "HABIT_MASTER");
        List<BadgeDefinition> milestoneDefinitions = badgeDefinitionRepository.findByNameIn(milestoneBadgeNames);

        for (BadgeDefinition definition : milestoneDefinitions) {
            // Calculate current progress automatically from user data
            BadgeLevelInfo levelInfo = getCurrentLevelAndProgress(user, definition);
            int currentProgress = levelInfo.getCurrentProgress();
            int targetForCurrentLevel = levelInfo.getTargetForCurrentLevel();
            int currentLevel = levelInfo.getCurrentLevel();

            boolean isMaxLevel = currentLevel >= 5 && currentProgress >= targetForCurrentLevel;

            // Check if user has earned ANY level of this badge
            boolean hasEarnedAnyLevel = badgeRepository.existsByUserAndBadgeType(user, definition.getName());
            boolean isEarned = hasEarnedAnyLevel || (currentProgress >= targetForCurrentLevel && !isMaxLevel);

            String levelText = buildLevelText(currentLevel, 5);
            String achievementText = getAchievementScreenText(definition.getName(),currentLevel);
            String description = getBadgeDescription(definition.getName(), currentLevel, currentProgress, targetForCurrentLevel);

            BadgeResponseDTO response = BadgeResponseDTO.builder()
                    .title(definition.getDisplayName())
                    .levelText(levelText)
                    .description(description)
                    .achievementScreenText(achievementText)
                    .icon("/badges/" + definition.getName().toLowerCase() + ".png")
                    .progress(currentProgress)  // Automatically calculated from user data
                    .target(targetForCurrentLevel) // Automatically set from level configuration
                    .isEarned(isEarned)
                    .isMaxLevel(isMaxLevel)
                    .currentLevel(currentLevel)
                    .totalLevels(5)
                    .celebrationTitle(buildCelebrationTitle(definition, currentLevel))
                    .celebrationMessage(buildCelebrationMessage(definition, currentLevel))
                    .shareMessage(buildShareMessage(definition.getName(), currentLevel))
                    .build();

            badgeResponses.add(response);
        }

        return badgeResponses;
    }

    /**
     * Get CHALLENGE-RELATED badges specifically for challenge responses
     * This is separate from the habit/task badges method
     */
    public List<BadgeResponseDTO> getChallengeUserBadges(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return List.of();
        }

        User user = userOpt.get();

        if (!isUserEligibleForBadges(user)) {
            return List.of();
        }

        List<BadgeResponseDTO> badgeResponses = new ArrayList<>();

        // Get CHALLENGE-RELATED badges only (different from habit/task badges)
        List<String> challengeBadgeNames = List.of(
                "QUIZ_ACE", "KNOWLEDGE_SEEKER", "LEARNING_ENTHUSIAST",
                "PUZZLE_MASTER", "TASK_TERMINATOR", "FEATURE_EXPLORER",
                "DAILY_CHAMPION"
        );

        List<BadgeDefinition> challengeDefinitions = badgeDefinitionRepository.findByNameIn(challengeBadgeNames);

        for (BadgeDefinition definition : challengeDefinitions) {
            // Calculate current progress automatically from user data
            BadgeLevelInfo levelInfo = getCurrentLevelAndProgress(user, definition);
            int currentProgress = levelInfo.getCurrentProgress();
            int targetForCurrentLevel = levelInfo.getTargetForCurrentLevel();
            int currentLevel = levelInfo.getCurrentLevel();

            boolean isMaxLevel = currentLevel >= 5 && currentProgress >= targetForCurrentLevel;

            // Check if user has earned ANY level of this badge
            boolean hasEarnedAnyLevel = badgeRepository.existsByUserAndBadgeType(user, definition.getName());
            boolean isEarned = hasEarnedAnyLevel || (currentProgress >= targetForCurrentLevel && !isMaxLevel);

            String levelText = buildLevelText(currentLevel, 5);
            String achievementText = getAchievementScreenText(definition.getName(),currentLevel);
            String description = getBadgeDescription(definition.getName(), currentLevel, currentProgress, targetForCurrentLevel);

            BadgeResponseDTO response = BadgeResponseDTO.builder()
                    .title(definition.getDisplayName())
                    .levelText(levelText)
                    .description(description)
                    .achievementScreenText(achievementText)
                    .icon("/badges/" + definition.getName().toLowerCase() + ".png")
                    .progress(currentProgress)  // Automatically calculated from user data
                    .target(targetForCurrentLevel) // Automatically set from level configuration
                    .isEarned(isEarned)
                    .isMaxLevel(isMaxLevel)
                    .currentLevel(currentLevel)
                    .totalLevels(5)
                    .celebrationTitle(buildCelebrationTitle(definition, currentLevel))
                    .celebrationMessage(buildCelebrationMessage(definition, currentLevel))
                    .shareMessage(buildShareMessage(definition.getName(), currentLevel))
                    .build();

            badgeResponses.add(response);
        }

        return badgeResponses;
    }

    /**
     * Get ALL badges for user (comprehensive list for badges page)
     * This is for the main badges screen
     */
    public List<BadgeResponseDTO> getAllUserBadges(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return List.of();
        }

        User user = userOpt.get();

        if (!isUserEligibleForBadges(user)) {
            return List.of();
        }

        List<BadgeResponseDTO> badgeResponses = new ArrayList<>();
        List<BadgeDefinition> allDefinitions = badgeDefinitionRepository.findByActiveTrueWithAgeGroups();

        for (BadgeDefinition definition : allDefinitions) {
            if (definition.getApplicableAgeGroups() == null ||
                    !definition.getApplicableAgeGroups().contains(user.getAgeGroup())) {
                continue;
            }

            BadgeLevelInfo levelInfo = getCurrentLevelAndProgress(user, definition);
            int currentLevel = levelInfo.getCurrentLevel();
            int currentProgress = levelInfo.getCurrentProgress();
            int targetForCurrentLevel = levelInfo.getTargetForCurrentLevel();

            boolean isMaxLevel = currentLevel >= 5 && currentProgress >= targetForCurrentLevel;

            boolean hasEarnedAnyLevel = badgeRepository.existsByUserAndBadgeType(user, definition.getName());
            boolean isEarned = hasEarnedAnyLevel || (currentProgress >= targetForCurrentLevel && !isMaxLevel);

            String levelText = buildLevelText(currentLevel, 5);
            String achievementText = getAchievementScreenText(definition.getName(),currentLevel);
            String description = getBadgeDescription(definition.getName(), currentLevel, currentProgress, targetForCurrentLevel);

            BadgeResponseDTO response = BadgeResponseDTO.builder()
                    .title(definition.getDisplayName())
                    .levelText(levelText)
                    .description(description)
                    .achievementScreenText(achievementText)
                    .icon("/badges/" + definition.getName().toLowerCase() + ".png")
                    .progress(currentProgress)
                    .target(targetForCurrentLevel)
                    .isEarned(isEarned)
                    .isMaxLevel(isMaxLevel)
                    .currentLevel(currentLevel)
                    .totalLevels(5)
                    .celebrationTitle(buildCelebrationTitle(definition, currentLevel))
                    .celebrationMessage(buildCelebrationMessage(definition, currentLevel))
                    .shareMessage(buildShareMessage(definition.getName(), currentLevel))
                    .build();

            badgeResponses.add(response);
        }

        List<BadgeResponseDTO> sortedBadges = sortBadgesByProgress(badgeResponses);
        return sortedBadges;
    }

    public BadgePopupDTO getPendingBadgePopup(Long userId) {

        return badgeRepository
                .findFirstByUserIdAndPopupShownFalseOrderByEarnedAtDesc(userId)
                .map(badge -> BadgePopupDTO.builder()
                        .badgeId(badge.getId())
                        .title("Level " + badge.getLevel() + " - Badge Unlocked!")
                        .badgeName(
                                badge.getName().replace(" - Level " + badge.getLevel(), "")
                        )
                        .message(
                                getAchievementScreenText(badge.getBadgeType(), badge.getLevel())
                        )
                        .icon(badge.getIcon())
                        .showPopup(true)
                        .build()
                )
                .orElse(
                        BadgePopupDTO.builder()
                                .showPopup(false)
                                .build()
                );
    }

    @Transactional
    public void acknowledgeBadgePopup(Long badgeId, Long userId) {

        System.out.println("Acknowledging badge popup for badgeId: " + badgeId + ", userId: " + userId);

        Badge badge = badgeRepository
                .findByIdAndUserIdAndPopupShownFalse(badgeId, userId)
                .orElseThrow(() ->
                        new BusinessException("Invalid or already acknowledged badge popup")
                );

        badge.setPopupShown(true);
        badgeRepository.save(badge);
    }

}