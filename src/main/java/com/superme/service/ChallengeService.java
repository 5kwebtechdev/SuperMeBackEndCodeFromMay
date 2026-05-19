package com.superme.service;

import com.superme.config.FileStorageConfig;
import com.superme.enums.*;
import com.superme.exception.BusinessException;
import com.superme.enums.Category;
import com.superme.exception.InternalServerErrorException;
import com.superme.model.*;
import com.superme.repository.*;
import com.superme.dto.*;
import com.superme.specification.ChallengeSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChallengeService {
    private final ChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final CoinTransactionRepository coinTransactionRepository;
    private final UserChallengeCompletionRepository completionRepository;
    private final BadgeService badgeService;
    private final TrophyService trophyService;
    private final CoinService coinService;
    private final QuestionRepository questionRepository;

    // Track hints used per user per challenge
    private final Map<String, Integer> userChallengeHintCount = new ConcurrentHashMap<>();

    // ==================== BATCH SUBMISSION METHODS ====================

    /**
     * Submit all challenge questions at once
     */
    public ChallengeResultDTO submitChallengeBatch(User user, Long challengeId, ChallengeBatchSubmissionDTO submission) {
        try {
            Challenge challenge = challengeRepository.findById(challengeId)
                    .orElseThrow(() -> new BusinessException("Challenge not found"));

            // Check if already completed
            if (isChallengeCompletedByUser(user.getId(), challengeId)) {
                return buildAlreadyCompletedResult(challenge, user);
            }

            // Calculate results for all questions
            ChallengeBatchResult batchResult = calculateBatchResults(challenge, submission.getQuestionAnswers());

            // Process successful submission if any correct answers
            if (batchResult.getTotalCorrect() > 0) {
                ChallengeResultDTO resultDTO = processBatchSubmission(user, challenge, batchResult);

                // Check and award badges based on challenge performance
                checkAndAwardChallengeBadges(user, challenge, batchResult,resultDTO.getCoinsEarned());

                return resultDTO;
            } else {
                return buildIncorrectBatchResult(challenge, batchResult, user);
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error submitting challenge batch for user {} and challenge {}", user.getId(), challengeId, e);
            throw new BusinessException("Error submitting challenge batch");
        }
    }

    /**
     * Calculate batch results for all questions
     */
    private ChallengeBatchResult calculateBatchResults(Challenge challenge, Map<Long, String> questionAnswers) {
        int totalCorrect = 0;
        int totalPoints = 0;
        Map<Long, Boolean> questionResults = new HashMap<>();
        List<QuestionFeedbackDTO> feedbacks = new ArrayList<>();

        for (Question question : challenge.getQuestions()) {
            Long questionId = question.getId();
            String userAnswer = questionAnswers.get(questionId);
            boolean isCorrect = checkAnswerCorrect(question, userAnswer);

            questionResults.put(questionId, isCorrect);

            if (isCorrect) {
                totalCorrect++;
                totalPoints += question.getPoints();
            }

            QuestionFeedbackDTO feedback = createQuestionFeedback(question, isCorrect, userAnswer);
            feedbacks.add(feedback);
        }

        double percentageScore = ((double) totalCorrect / challenge.getQuestions().size()) * 100;

        return ChallengeBatchResult.builder()
                .totalCorrect(totalCorrect)
                .totalQuestions(challenge.getQuestions().size())
                .totalPoints(totalPoints)
                .percentageScore(percentageScore)
                .questionResults(questionResults)
                .questionFeedbacks(feedbacks)
                .build();
    }

    /**
     * Process batch submission and award rewards
     */
    private ChallengeResultDTO processBatchSubmission(User user, Challenge challenge, ChallengeBatchResult batchResult) {
        int coinsToAward = calculateCoinsEarned(batchResult.getTotalCorrect());
        int trophiesToAward = calculateTrophiesEarned(challenge.getDifficulty(),
                batchResult.getTotalCorrect() == batchResult.getTotalQuestions());

        // Award coins
        coinService.addCoins(user, coinsToAward, "CHALLENGE_BATCH_REWARD",challenge.getName());

        // Award trophies
        Trophy trophy = awardTrophiesBatch(user, challenge, batchResult, coinsToAward, trophiesToAward);

        // Record completion
        recordChallengeCompletionBatch(user, challenge, batchResult.getTotalCorrect());

        // Get badges after awarding - using the CORRECT method name
        List<BadgeResponseDTO> challengeBadges = getChallengeRelatedBadges(user, challenge);

        // Generate celebration text
        String celebrationText = generateCelebrationText(batchResult.getTotalCorrect(), batchResult.getTotalQuestions());

        return ChallengeResultDTO.builder()
                .success(true)
                .celebrationText(celebrationText)
                .correctCount(batchResult.getTotalCorrect())
                .totalQuestions(batchResult.getTotalQuestions())
                .coinsEarned(coinsToAward)
                .trophiesEarned(trophiesToAward)
                .totalCoins(user.getCoins() + coinsToAward)
                .challengeCompleted(true)
                .percentageScore(batchResult.getPercentageScore())
                .questionResults(batchResult.getQuestionResults())
                .questionFeedbacks(batchResult.getQuestionFeedbacks())
                .overallFeedback(getOverallFeedback(batchResult.getPercentageScore()))
                .firstCompletion(isFirstCompletion(user.getId(), challenge.getId()))
                .badges(challengeBadges)
                .build();
    }

    /**
     * Award badges for challenge completion
     */
    private void awardBadgesForChallenge(
            User user,
            Challenge challenge,
            boolean isPerfectScore,
            int coinsToAward) {
        try {
            ActivityType activityType;

            switch (challenge.getCategory()) {

                case QUIZ -> activityType = ActivityType.QUIZ_COMPLETED;

                case PUZZLE -> activityType = ActivityType.PUZZLE_COMPLETED;

                case ARTICLE -> activityType = ActivityType.ARTICLE_COMPLETED;

                default -> activityType = ActivityType.CHALLENGE_COMPLETED;
            }

            // ✅ SINGLE badge trigger (after persistence)
            badgeService.triggerBadgeUpdate(
                    user.getId(),
                    activityType,
                    coinsToAward,                  // challenges may or may not award coins
                    "CHALLENGE"
            );

            log.info(
                    "Triggered badge evaluation for user {} after completing challenge {} (type: {}, perfect: {})",
                    user.getId(),
                    challenge.getName(),
                    activityType,
                    isPerfectScore
            );

        } catch (Exception e) {
            log.error("Error triggering badges for challenge completion: {}", e.getMessage(), e);
        }
    }


    /**
     * Create question feedback DTO
     */
    private QuestionFeedbackDTO createQuestionFeedback(Question question, boolean isCorrect, String userAnswer) {
        String correctAnswer = getCorrectAnswerText(question);

        return QuestionFeedbackDTO.builder()
                .questionId(question.getId())
                .questionText(question.getQuestionText())
                .correct(isCorrect)
                .explanation(getExplanationForQuestion(question, isCorrect))
                .correctAnswer(correctAnswer)
                .userAnswer(userAnswer != null ? userAnswer : "No answer")
                .pointsEarned(isCorrect ? question.getPoints() : 0)
                .build();
    }

    /**
     * Get correct answer text for a question
     */
    private String getCorrectAnswerText(Question question) {
        return question.getOptions().stream()
                .filter(QuestionOption::getIsCorrect)
                .map(QuestionOption::getOptionText)
                .findFirst()
                .orElse("No correct answer found");
    }

    /**
     * Get explanation for question - FIXED VERSION (handles null explanation)
     */
    private String getExplanationForQuestion(Question question, boolean isCorrect) {
        if (isCorrect) {
            Optional<QuestionOption> correctOption = question.getOptions().stream()
                    .filter(QuestionOption::getIsCorrect)
                    .findFirst();

            if (correctOption.isPresent() && correctOption.get().getExplanation() != null) {
                return correctOption.get().getExplanation();
            }
            return "Well done!";
        } else {
            String correctAnswer = getCorrectAnswerText(question);
            return "The correct answer is: " + correctAnswer;
        }
    }

    /**
     * Generate celebration text based on performance
     */
    private String generateCelebrationText(int correct, int total) {
        double percentage = ((double) correct / total) * 100;

        if (percentage == 100) {
            return "Perfect Score! You're a Genius! 🌟";
        } else if (percentage >= 80) {
            return "Excellent Work! You're a Star! ⭐";
        } else if (percentage >= 60) {
            return "Great Job! You're Getting Better! 👍";
        } else if (percentage >= 40) {
            return "Good Effort! Keep Practicing! 💪";
        } else {
            return "Nice Try! Every Step Counts! 🎯";
        }
    }

    /**
     * Get overall feedback based on percentage score
     */
    private String getOverallFeedback(double percentageScore) {
        if (percentageScore == 100) {
            return "Perfect! You answered all questions correctly!";
        } else if (percentageScore >= 80) {
            return "Excellent! You're doing great!";
        } else if (percentageScore >= 60) {
            return "Good job! Keep practicing!";
        } else if (percentageScore >= 40) {
            return "Not bad! You're getting there!";
        } else {
            return "Keep trying! Practice makes perfect!";
        }
    }

    /**
     * Check and award badges for batch submission
     */
    private void checkAndAwardChallengeBadges(User user, Challenge challenge, ChallengeBatchResult batchResult, int coinsEarned) {
        String category = challenge.getCategory().name();
        boolean isPerfectScore = batchResult.getTotalCorrect() == batchResult.getTotalQuestions();

        log.info("Checking badges for user {} completing {} challenge (perfect: {})",
                user.getId(), category, isPerfectScore);

        // Trigger badge service calls
        awardBadgesForChallenge(user, challenge, isPerfectScore, coinsEarned);
    }

    /**
     * Award trophies for batch submission - FIXED VERSION
     */
    private Trophy awardTrophiesBatch(User user, Challenge challenge, ChallengeBatchResult batchResult,
                                      int coinsEarned, int trophiesEarned) {
        try {
            // Create proper ChallengeResultDTO for trophy service
            ChallengeResultDTO trophyResult = ChallengeResultDTO.builder()
                    .success(true)
                    .celebrationText("Challenge completed!")
                    .correctCount(batchResult.getTotalCorrect())
                    .totalQuestions(batchResult.getTotalQuestions())
                    .coinsEarned(coinsEarned)
                    .trophiesEarned(trophiesEarned)
                    .totalCoins(user.getCoins() + coinsEarned)
                    .challengeCompleted(true)
                    .percentageScore(batchResult.getPercentageScore())
                    .overallFeedback("Well done!")
                    .firstCompletion(true)
                    .build();

            return trophyService.awardTrophyForChallenge(
                    trophyResult,
                    user.getId().toString(),
                    challenge.getCategory(),
                    challenge.getDifficulty(),
                    challenge.getThumbnailImageUrl()
            );
        } catch (Exception e) {
            log.error("Error awarding trophy for batch submission: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Record batch completion
     */
    private void recordChallengeCompletionBatch(User user, Challenge challenge, int score) {
        try {
            UserChallengeCompletion completion = new UserChallengeCompletion();
            completion.setUser(user);
            completion.setChallenge(challenge);
            completion.setCompletedAt(LocalDateTime.now(ZoneId.systemDefault()));
            completion.setScore(score);
            completionRepository.save(completion);
        } catch (Exception e) {
            log.error("Error recording batch completion: {}", e.getMessage());
        }
    }

    /**
     * Get challenge-related badges for the user - UPDATED VERSION
     */
    private List<BadgeResponseDTO> getChallengeRelatedBadges(User user, Challenge challenge) {
        try {
            // Use the new method to get CHALLENGE-RELATED badges only
            List<BadgeResponseDTO> challengeBadges = badgeService.getChallengeUserBadges(user.getId());

            if (challengeBadges == null || challengeBadges.isEmpty()) {
                log.info("No challenge-related badges found for user {}", user.getId());
                return new ArrayList<>();
            }

            log.info("Found {} challenge-related badges for user {}", challengeBadges.size(), user.getId());

            // Filter to show only relevant badges based on challenge type
            String category = challenge.getCategory().name();

            List<BadgeResponseDTO> relevantBadges = challengeBadges.stream()
                    .filter(badge -> badge != null && badge.getTitle() != null)
                    .filter(badge -> isRelevantForChallenge(badge.getTitle(), category))
                    .sorted((b1, b2) -> {
                        // Sort by relevance: show earned badges first, then by progress
                        boolean b1Earned = Boolean.TRUE.equals(b1.getIsEarned());
                        boolean b2Earned = Boolean.TRUE.equals(b2.getIsEarned());

                        if (b1Earned && !b2Earned) return -1;
                        if (!b1Earned && b2Earned) return 1;

                        // Both earned or both not earned, sort by progress percentage
                        double progress1 = (double) b1.getProgress() / b1.getTarget();
                        double progress2 = (double) b2.getProgress() / b2.getTarget();

                        return Double.compare(progress2, progress1); // Higher progress first
                    })
                    .limit(2) // Show only 2 most relevant badges
                    .collect(Collectors.toList());

            log.info("Filtered to {} relevant badges for challenge category: {}",
                    relevantBadges.size(), category);

            return relevantBadges;

        } catch (Exception e) {
            log.error("Error getting challenge-related badges for user {}: {}",
                    user.getId(), e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Check if badge is relevant for this specific challenge type
     */
    private boolean isRelevantForChallenge(String badgeTitle, String challengeCategory) {
        if (badgeTitle == null || challengeCategory == null) {
            return false;
        }

        String badgeUpper = badgeTitle.toUpperCase();

        return switch (challengeCategory) {
            case "QUIZ" ->
                    badgeUpper.contains("QUIZ") ||
                            badgeUpper.contains("KNOWLEDGE") ||
                            badgeUpper.contains("LEARNING") ||
                            badgeUpper.contains("FEATURE EXPLORER") ||
                            badgeUpper.contains("DAILY CHAMPION");
            case "PUZZLE" ->
                    badgeUpper.contains("PUZZLE") ||
                            badgeUpper.contains("TASK TERMINATOR") ||
                            badgeUpper.contains("FEATURE EXPLORER") ||
                            badgeUpper.contains("DAILY CHAMPION");
            case "ARTICLE" ->
                    badgeUpper.contains("KNOWLEDGE") ||
                            badgeUpper.contains("LEARNING") ||
                            badgeUpper.contains("FEATURE EXPLORER") ||
                            badgeUpper.contains("DAILY CHAMPION");
            default ->
                    badgeUpper.contains("QUIZ") ||
                            badgeUpper.contains("PUZZLE") ||
                            badgeUpper.contains("KNOWLEDGE") ||
                            badgeUpper.contains("LEARNING") ||
                            badgeUpper.contains("DAILY CHAMPION");
        };
    }

    /**
     * Build incorrect batch result
     */
    private ChallengeResultDTO buildIncorrectBatchResult(Challenge challenge, ChallengeBatchResult batchResult, User user) {
        return ChallengeResultDTO.builder()
                .success(false)
                .celebrationText("Nice try! Better luck next time!")
                .correctCount(batchResult.getTotalCorrect())
                .totalQuestions(batchResult.getTotalQuestions())
                .coinsEarned(0)
                .trophiesEarned(0)
                .totalCoins(user.getCoins())
                .challengeCompleted(false)
                .percentageScore(batchResult.getPercentageScore())
                .questionResults(batchResult.getQuestionResults())
                .questionFeedbacks(batchResult.getQuestionFeedbacks())
                .overallFeedback(getOverallFeedback(batchResult.getPercentageScore()))
                .build();
    }

    // ==================== EXISTING METHODS (KEPT AS IS) ====================

    /**
     * Deduct coins and provide a hint for a quiz/puzzle challenge.
     * Allows only 2 hints per user per challenge. Deducts 3 coins per hint.
     * Returns true if hint is provided, false if limit reached or insufficient coins.
     */
    @Transactional
    public HintDto useHint(User user, Long questionId) {
        HintDto hintDto = new HintDto();
        String key = user.getId() + ":" + questionId;
       Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException("Question not found"));

//        if (question.getHintCount() >= 2) {
//            throw new BusinessException("Hint limit reached");
//        }

        try {
            // Use CoinService to spend coins - automatically creates transaction
            coinService.spendCoins(user, 3, "HINT_USED");
//            question.setHintCount(question.getHintCount() + 1);
            questionRepository.save(question);
            hintDto.setHintCount(question.getHintCount());
            hintDto.setHint(question.getHint());
            return hintDto;
        } catch (Exception e) {
            log.error("Error using hint for user {} on question {}: {}", user.getId(), questionId, e.getMessage());
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    /**
     * Get all questions/options for a challenge for a user
     */
    @Transactional(readOnly = true)
    public List<QuestionResponseDTO> getChallengeQuestionsForUser(User user, Long challengeId) {
        try {
            // Fetch challenge
            Challenge challenge = challengeRepository.findById(challengeId)
                    .orElseThrow(() -> new BusinessException("Challenge not found"));

            if (challenge.getQuestions() == null || challenge.getQuestions().isEmpty()) {
                throw new BusinessException("No questions found for this challenge");
            }

            boolean isChallengeCompleted = user != null && isChallengeCompletedByUser(user.getId(), challengeId);

            // Convert each question to DTO with its options
            return challenge.getQuestions().stream()
                    .sorted(Comparator.comparing(Question::getQuestionOrder))
                    .map(question -> {
                        // Build the question DTO
                        QuestionResponseDTO dto = QuestionResponseDTO.builder()
                                .questionId(question.getId())
                                .questionOrder(question.getQuestionOrder())
                                .questionText(question.getQuestionText())
                                .questionImageUrl(question.getQuestionImageUrl())
                                .hint(question.getHint())
                                .answerType(question.getAnswerType())
                                .points(question.getPoints())
                                .timeLimit(question.getTimeLimit())
                                .attachmentUrl(question.getAttachmentUrl())
                                .attachmentType(question.getAttachmentType())
                                .build();

                        // Convert options if they exist
                        if (question.getOptions() != null && !question.getOptions().isEmpty()) {
                            List<QuestionOptionResponseDTO> optionDTOs = question.getOptions().stream()
                                    .sorted(Comparator.comparing(QuestionOption::getOptionOrder))
                                    .map(opt -> {
                                        QuestionOptionResponseDTO optionDTO = QuestionOptionResponseDTO.builder()
                                                .optionId(opt.getId())
                                                .optionText(opt.getOptionText())
                                                .optionImageUrl(opt.getOptionImageUrl())
                                                .optionOrder(opt.getOptionOrder())
                                                .explanation(opt.getExplanation())
                                                .build();

                                        // Only show isCorrect if challenge is completed or for admin review
                                        if (isChallengeCompleted) {
                                            optionDTO.setIsCorrect(opt.getIsCorrect());
                                        } else {
                                            // Hide correct answers for non-completed challenges
                                            optionDTO.setIsCorrect(null);
                                        }

                                        return optionDTO;
                                    })
                                    .collect(Collectors.toList());
                            dto.setOptions(optionDTOs);
                        } else {
                            dto.setOptions(new ArrayList<>());
                        }

                        return dto;
                    })
                    .collect(Collectors.toList());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting challenge questions for user {} and challenge {}",
                    user != null ? user.getId() : "null", challengeId, e);
            throw new BusinessException("Error retrieving challenge questions");
        }
    }

    /**
     * Get challenge result for a user
     */
    @Transactional(readOnly = true)
    public ChallengeResultDTO getChallengeResultForUser(User user, Long challengeId) {
        try {
            Challenge challenge = challengeRepository.findById(challengeId)
                    .orElseThrow(() -> new BusinessException("Challenge not found"));

            boolean completed = isChallengeCompletedByUser(user.getId(), challengeId);
            int score = completed ? getCompletionScore(user, challenge) : 0;
            boolean perfectScore = completed && isPerfectCompletion(user, challenge);
            int coinsEarned = completed ? calculateCoinsEarned(score) : 0;
            int trophiesEarned = completed ? calculateTrophiesEarned(challenge.getDifficulty(), perfectScore) : 0;

            // Get badges earned for this challenge
            List<BadgeResponseDTO> badges = completed ? getChallengeRelatedBadges(user, challenge) : List.of();
            String feedback = generateHistoricalFeedback(completed, score, getTotalQuestions(challenge), perfectScore);

            return ChallengeResultDTO.builder()
                    .success(completed)
                    .celebrationText(completed ? "Challenge Completed!" : "Not yet completed")
                    .correctCount(score)
                    .totalQuestions(getTotalQuestions(challenge))
                    .coinsEarned(coinsEarned)
                    .trophiesEarned(trophiesEarned)
                    .totalCoins(user.getCoins() + coinsEarned)
                    .challengeCompleted(completed)
                    .percentageScore(completed ? ((double) score / getTotalQuestions(challenge)) * 100 : 0)
                    .badges(badges)
                    .overallFeedback(feedback)
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting challenge result for user {} and challenge {}", user.getId(), challengeId, e);
            throw new BusinessException("Error retrieving challenge result");
        }
    }

    /**
     * Get all challenges for user
     */
    @Transactional(readOnly = true)
    public List<MultiQuestionChallengeResponseDTO> getAllChallengesForUser(User user) {
        try {
            // Configuration - these could be moved to application.properties
            Boolean hideCompleted = true;
            Integer daysToHideAfterChallengeCompleted = 7;

            // Get all enabled challenges
            List<Challenge> challenges = challengeRepository.findByEnabledTrue();

            if (!hideCompleted) {
                // If hideCompleted is false, return all challenges
                return challenges.stream()
                        .map(this::convertToMultiQuestionResponseDTO)
                        .collect(Collectors.toList());
            }

            // Get all completed challenges for this user
            List<UserChallengeCompletion> completedChallenges = completionRepository.findByUser(user);

            // Create a map of challengeId -> completionDate for quick lookup
            Map<Long, LocalDateTime> completedChallengeMap = completedChallenges.stream()
                    .collect(Collectors.toMap(
                            cc -> cc.getChallenge().getId(),
                            UserChallengeCompletion::getCompletedAt
                    ));

            // Get current date for comparison
            LocalDateTime now = LocalDateTime.now();

            // Filter challenges based on completion status
            List<Challenge> filteredChallenges = challenges.stream()
                    .filter(challenge -> {
                        // Check if challenge is completed by this user
                        if (!completedChallengeMap.containsKey(challenge.getId())) {
                            return true; // Not completed, show it
                        }

                        // Challenge is completed, check if it should be hidden
                        LocalDateTime completionDate = completedChallengeMap.get(challenge.getId());

                        // Calculate days between completion and now
                        long daysSinceCompletion = ChronoUnit.DAYS.between(completionDate, now);

                        // Show challenge only if completed outside the hide period
                        // (if completed 7+ days ago, show it; if completed within 7 days, hide it)
                        return daysSinceCompletion >= daysToHideAfterChallengeCompleted;
                    })
                    .collect(Collectors.toList());

            return filteredChallenges.stream()
                    .map(this::convertToMultiQuestionResponseDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting all challenges for user {}", user.getId(), e);
            throw new BusinessException("Error retrieving challenges");
        }
    }

    private MultiQuestionChallengeResponseDTO convertToMultiQuestionResponseDTO(Challenge challenge) {
        MultiQuestionChallengeResponseDTO dto = MultiQuestionChallengeResponseDTO.builder()
                .challengeId(challenge.getId())
                .name(challenge.getName())
                .description(challenge.getDescription())
                .descriptionExpanded(challenge.getDescriptionExpanded())
                .category(challenge.getCategory())
                .difficulty(challenge.getDifficulty())
                .ageGroups(challenge.getAgeGroups())
                .topic(challenge.getTopic())
                .status(challenge.getStatus())
                .coins(challenge.getCoins())
                .coinsForCorrectAnswer(challenge.getCoinsForCorrectAnswer())
                .trophies(challenge.getTrophies())
                .timeDuration(challenge.getTimeDuration())
                .sectionTitle(challenge.getSectionTitle())
//                .positiveFeedback(challenge.getPositiveFeedback())
//                .negativeFeedback(challenge.getNegativeFeedback())
//                .negativeFeedbackTryAgain(challenge.getNegativeFeedbackTryAgain())
//                .thumbnailImageUrl(challenge.getThumbnailImageUrl())
                .thumbnailImageUrl(buildFileUrl(challenge.getThumbnailImageUrl()))
                .innerImageUrl(challenge.getInnerImageUrl())
                .enabled(challenge.getEnabled() != null ? challenge.getEnabled() : true)
                .questions(convertQuestionsToResponseDTO(challenge.getQuestions()))
                .attachments(convertAttachmentsToResponseDTO(challenge.getAttachments()))
                .totalQuestions(challenge.getQuestions().size())
                .totalPoints(challenge.getQuestions().stream()
                        .mapToInt(Question::getPoints)
                        .sum())
                .build();

        return dto;
    }

    @Autowired
    private FileStorageConfig fileStorageConfig;

    private String buildFileUrl(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }

        // extract filename from /uploads/tempimg.png
        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        return fileStorageConfig.getBaseUrl() + "/v1/challenges/download/" + fileName;
//        return fileStorageConfig.getBaseUrl() + "/api/files/download/" + fileName;
    }


    private List<QuestionResponseDTO> convertQuestionsToResponseDTO(List<Question> questions) {
        if (questions == null) {
            return new ArrayList<>();
        }

        return questions.stream()
                .sorted(Comparator.comparing(Question::getQuestionOrder))
                .map(this::convertQuestionToResponseDTO)
                .collect(Collectors.toList());
    }

    private QuestionResponseDTO convertQuestionToResponseDTO(Question question) {
        return QuestionResponseDTO.builder()
                .questionId(question.getId())
                .questionOrder(question.getQuestionOrder())
                .questionText(question.getQuestionText())
                .questionImageUrl(question.getQuestionImageUrl())
                .hint(question.getHint())
                .answerType(question.getAnswerType())
                .points(question.getPoints())
                .timeLimit(question.getTimeLimit())
                .attachmentUrl(question.getAttachmentUrl())
                .attachmentType(question.getAttachmentType())
                .options(convertOptionsToResponseDTO(question.getOptions()))
                .positiveFeedback(question.getPositiveFeedback())
                .negativeFeedback(question.getNegativeFeedback())
                .negativeFeedbackTryAgain(question.getNegativeFeedbackTryAgain())
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }

    private List<QuestionOptionResponseDTO> convertOptionsToResponseDTO(List<QuestionOption> options) {
        if (options == null) {
            return new ArrayList<>();
        }

        return options.stream()
                .sorted(Comparator.comparing(QuestionOption::getOptionOrder))
                .map(option -> QuestionOptionResponseDTO.builder()
                        .optionId(option.getId())
                        .optionText(option.getOptionText())
                        .optionImageUrl(option.getOptionImageUrl())
                        .optionOrder(option.getOptionOrder())
                        .isCorrect(option.getIsCorrect())
                        .explanation(option.getExplanation())
                        .createdAt(option.getCreatedAt())
                        .updatedAt(option.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private List<ChallengeAttachmentResponseDTO> convertAttachmentsToResponseDTO(List<ChallengeAttachment> attachments) {
        if (attachments == null) {
            return new ArrayList<>();
        }

        return attachments.stream()
                .map(attachment -> ChallengeAttachmentResponseDTO.builder()
                        .attachmentId(attachment.getId())
                        .fileName(attachment.getFileName())
                        .fileUrl(attachment.getFileUrl())
                        .fileType(attachment.getFileType())
                        .fileSize(attachment.getFileSize())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get challenges suitable for user's age
     */
    @Transactional(readOnly = true)
    public List<MultiQuestionChallengeResponseDTO> getChallengesForUserAge(User user, int age) {
        try {
            List<Challenge> challenges = challengeRepository.findByEnabledTrue();

            // Convert age to AgeGroup
            AgeGroup userAgeGroup = AgeGroup.fromAge(age);

            // Filter challenges by age group
            return challenges.stream()
                    .filter(challenge -> isSuitableForAgeGroup(challenge, userAgeGroup))
                    .map(this::convertToMultiQuestionResponseDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting challenges for user age {}", age, e);
            throw new BusinessException("Error retrieving age-appropriate challenges");
        }
    }

    /**
     * Check if a challenge is suitable for a given age group
     */
    private boolean isSuitableForAgeGroup(Challenge challenge, AgeGroup userAgeGroup) {
        if (challenge.getAgeGroups() == null || challenge.getAgeGroups().isEmpty()) {
            return true; // No age restriction, suitable for all
        }

        // Check if user's age group is in the challenge's allowed age groups
        return challenge.getAgeGroups().contains(userAgeGroup);
    }

    /**
     * Get challenges by specific age group
     */
    @Transactional(readOnly = true)
    public List<MultiQuestionChallengeResponseDTO> getChallengesByAgeGroup(User user, AgeGroup ageGroup) {
        try {
            List<Challenge> allChallenges = challengeRepository.findByEnabledTrue();

            // Filter by age group manually
            List<Challenge> filteredChallenges = allChallenges.stream()
                    .filter(challenge -> challenge.getAgeGroups() != null &&
                            challenge.getAgeGroups().contains(ageGroup))
                    .collect(Collectors.toList());

            return filteredChallenges.stream()
                    .map(this::convertToMultiQuestionResponseDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting challenges for age group {}", ageGroup, e);
            throw new BusinessException("Error retrieving challenges for age group");
        }
    }

    /**
     * Submit challenge answers and process rewards
     */
    public ChallengeResultDTO submitChallenge(User user, Long challengeId, String selectedOption) {
        try {
            Challenge challenge = challengeRepository.findById(challengeId)
                    .orElseThrow(() -> new BusinessException("Challenge not found"));

            // Prevent awarding coins multiple times
            if (isChallengeCompletedByUser(user.getId(), challengeId)) {
                return buildAlreadyCompletedResult(challenge, user);
            }

            // Parse selected answers and calculate score
            ChallengeSubmissionResult result = calculateSubmissionResult(challenge, selectedOption);

            if (result.getCorrectCount() > 0) {

                return processSuccessfulSubmission(user, challenge, result);
            } else {
                return buildIncorrectResult(challenge, result);
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error submitting challenge for user {} and challenge {}", user.getId(), challengeId, e);
            throw new BusinessException("Error submitting challenge");
        }
    }

    private void awardChallengeBadges(
            User user,
            int score,
            int totalQuestions
    ) {
        try {
            // Determine activity type
            ActivityType activityType = ActivityType.CHALLENGE_COMPLETED;

            // SINGLE badge trigger (after persistence)
            badgeService.triggerBadgeUpdate(
                    user.getId(),
                    activityType,
                    0,              // coins (already handled elsewhere)
                    "CHALLENGE"
            );

            log.info(
                    "Badge evaluation triggered for user {} after challenge completion (score {}/{})",
                    user.getId(),
                    score,
                    totalQuestions
            );

        } catch (Exception e) {
            log.error(
                    "Error triggering badge update for challenge completion for user {}",
                    user.getId(),
                    e
            );
        }
    }


    /**
     * Process successful challenge submission with full reward tracking
     */
    private ChallengeResultDTO processSuccessfulSubmission(
            User user,
            Challenge challenge,
            ChallengeSubmissionResult result
    ) {
        try {
            int coinsEarned = calculateCoinsEarned(result.getCorrectCount());
            int trophiesEarned = calculateTrophiesEarned(
                    challenge.getDifficulty(),
                    result.isPerfectScore()
            );

            // 1. Award coins
            awardCoins(user, coinsEarned, challenge.getName(), result.getCorrectCount());

            // 2. Award trophies
            Trophy trophy = awardTrophies(user, challenge, result, coinsEarned, trophiesEarned);

            // 3. Record completion (streak / history / analytics)
            recordChallengeCompletion(user, challenge, result.getCorrectCount());

            // 4. 🔑 SINGLE badge trigger
            badgeService.triggerBadgeUpdate(
                    user.getId(),
                    ActivityType.CHALLENGE_COMPLETED,
                    coinsEarned,
                    "CHALLENGE"
            );

            log.info(
                    "User {} completed challenge {}: {} coins, {} trophies, {} correct answers",
                    user.getId(),
                    challenge.getName(),
                    coinsEarned,
                    trophiesEarned,
                    result.getCorrectCount()
            );

            return buildSuccessResult(
                    challenge,
                    result,
                    coinsEarned,
                    trophiesEarned,
                    Collections.emptyList() // badges are resolved via badge APIs
            );

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error(
                    "Error processing successful submission for user {} and challenge {}",
                    user.getId(),
                    challenge.getId(),
                    e
            );
            throw new BusinessException("Error processing challenge submission");
        }
    }


    /**
     * Calculate submission results
     */
    private ChallengeSubmissionResult calculateSubmissionResult(Challenge challenge, String selectedOption) {
        try {
            List<String> selectedAnswers = selectedOption != null
                    ? Arrays.stream(selectedOption.split(",")).map(String::trim).toList()
                    : List.of();

            if (challenge.getQuestions() == null || challenge.getQuestions().isEmpty()) {
                return new ChallengeSubmissionResult(0, false, 0);
            }

            int totalQuestions = challenge.getQuestions().size();
            int correctCount = 0;
            boolean perfectScore = selectedAnswers.size() == totalQuestions;

            // Parse selected answers as questionIndex:optionText pairs
            Map<Integer, String> answerMap = parseSelectedAnswers(selectedAnswers);

            // Check each question
            for (int i = 0; i < totalQuestions; i++) {
                if (i >= challenge.getQuestions().size()) break;

                Question question = challenge.getQuestions().get(i);
                String userAnswer = answerMap.get(i);

                if (userAnswer != null) {
                    boolean isCorrect = checkAnswerCorrect(question, userAnswer);
                    if (isCorrect) {
                        correctCount++;
                    } else {
                        perfectScore = false;
                    }
                } else {
                    // Question not answered
                    perfectScore = false;
                }
            }

            // If not all questions answered, it's not perfect
            if (selectedAnswers.size() < totalQuestions) {
                perfectScore = false;
            }

            return new ChallengeSubmissionResult(correctCount, perfectScore, totalQuestions);

        } catch (Exception e) {
            log.error("Error calculating submission result for challenge {}", challenge.getId(), e);
            throw new BusinessException("Error calculating submission result");
        }
    }

    /**
     * Parse selected answers
     */
    private Map<Integer, String> parseSelectedAnswers(List<String> selectedAnswers) {
        Map<Integer, String> answerMap = new HashMap<>();

        for (int i = 0; i < selectedAnswers.size(); i++) {
            String answer = selectedAnswers.get(i);
            if (answer.contains(":")) {
                String[] parts = answer.split(":", 2);
                try {
                    int questionIndex = Integer.parseInt(parts[0].trim());
                    String answerText = parts[1].trim();
                    answerMap.put(questionIndex, answerText);
                } catch (NumberFormatException e) {
                    answerMap.put(i, answer);
                }
            } else {
                answerMap.put(i, answer);
            }
        }

        return answerMap;
    }

    /**
     * Check if the user's answer is correct for a specific question
     */
    private boolean checkAnswerCorrect(Question question, String userAnswer) {
        if (question.getOptions() == null || question.getOptions().isEmpty() || userAnswer == null) {
            return false;
        }

        // For TRUE_FALSE questions
        if (question.getAnswerType() == AnswerType.TRUE_FALSE) {
            return checkTrueFalseAnswer(question, userAnswer);
        }

        // For MCQ/VISUALS questions
        return question.getOptions().stream()
                .filter(QuestionOption::getIsCorrect)
                .anyMatch(correctOption ->
                        correctOption.getOptionText() != null &&
                                correctOption.getOptionText().equalsIgnoreCase(userAnswer.trim()));
    }

    /**
     * Check TRUE_FALSE answer (special handling)
     */
    private boolean checkTrueFalseAnswer(Question question, String userAnswer) {
        if (question.getOptions() == null || question.getOptions().size() < 2) {
            return false;
        }

        QuestionOption trueOption = question.getOptions().get(0);
        QuestionOption falseOption = question.getOptions().get(1);

        Boolean isTrueCorrect = trueOption.getIsCorrect();
        Boolean isFalseCorrect = falseOption.getIsCorrect();

        if ("true".equalsIgnoreCase(userAnswer.trim()) || "True".equals(userAnswer.trim())) {
            return Boolean.TRUE.equals(isTrueCorrect);
        } else if ("false".equalsIgnoreCase(userAnswer.trim()) || "False".equals(userAnswer.trim())) {
            return Boolean.TRUE.equals(isFalseCorrect);
        }

        return false;
    }

    /**
     * Coin rewards using CoinService - automatically tracks transaction
     */
    private void awardCoins(User user, int coinsEarned, String challengeName, int correctCount) {
        try {
            coinService.addCoins(user, coinsEarned, "CHALLENGE_REWARD",challengeName);
        } catch (BusinessException e) {
            log.error("Business error awarding coins to user {} for challenge completion", user.getId(), e);
            throw e;
        } catch (Exception e) {
            log.error("Error awarding coins to user {} for challenge completion", user.getId(), e);
            throw new BusinessException("Error awarding coins");
        }
    }

    /**
     * Trophy awards using TrophyService
     */
    private Trophy awardTrophies(User user, Challenge challenge, ChallengeSubmissionResult result, int coinsEarned, int trophiesEarned) {
        try {
            ChallengeResultDTO trophyResult = ChallengeResultDTO.builder()
                    .success(true)
                    .celebrationText("Challenge completed!")
                    .correctCount(result.getCorrectCount())
                    .totalQuestions(result.getTotalQuestions())
                    .coinsEarned(coinsEarned)
                    .trophiesEarned(trophiesEarned)
                    .totalCoins(user.getCoins() + coinsEarned)
                    .challengeCompleted(true)
                    .percentageScore(((double) result.getCorrectCount() / result.getTotalQuestions()) * 100)
                    .overallFeedback("Well done!")
                    .firstCompletion(true)
                    .build();

            return trophyService.awardTrophyForChallenge(
                    trophyResult,
                    user.getId().toString(),
                    challenge.getCategory(),
                    challenge.getDifficulty(),
                    challenge.getThumbnailImageUrl()
            );
        } catch (BusinessException e) {
            log.error("Business error awarding trophy for user {} and challenge {}", user.getId(), challenge.getId(), e);
            throw e;
        } catch (Exception e) {
            log.error("Error awarding trophy for user {} and challenge {}", user.getId(), challenge.getId(), e);
            throw new BusinessException("Error awarding trophy");
        }
    }

    /**
     * Record challenge completion for streak calculation
     */
    private void recordChallengeCompletion(User user, Challenge challenge, int score) {
        try {
            UserChallengeCompletion completion = new UserChallengeCompletion();
            completion.setUser(user);
            completion.setChallenge(challenge);
            completion.setCompletedAt(LocalDateTime.now(ZoneId.systemDefault()));
            completion.setScore(score);
            completionRepository.save(completion);
        } catch (Exception e) {
            log.error("Error recording challenge completion for user {} and challenge {}", user.getId(), challenge.getId(), e);
            throw new BusinessException("Error recording completion");
        }
    }

    /**
     * Check if challenge is completed by user
     */
    private boolean isChallengeCompletedByUser(Long userId, Long challengeId) {
        try {
            List<UserChallengeCompletion> userCompletions = completionRepository.findByUserId(userId);
            return userCompletions.stream()
                    .anyMatch(completion -> completion.getChallenge().getId().equals(challengeId));
        } catch (Exception e) {
            log.error("Error checking challenge completion for user {} and challenge {}", userId, challengeId, e);
            throw new BusinessException("Error checking completion status");
        }
    }

    /**
     * Get completion score for user and challenge
     */
    private int getCompletionScore(User user, Challenge challenge) {
        try {
            List<UserChallengeCompletion> userCompletions = completionRepository.findByUser(user);
            return userCompletions.stream()
                    .filter(completion -> completion.getChallenge().getId().equals(challenge.getId()))
                    .findFirst()
                    .map(UserChallengeCompletion::getScore)
                    .orElse(0);
        } catch (Exception e) {
            log.error("Error getting completion score for user {} and challenge {}", user.getId(), challenge.getId(), e);
            throw new BusinessException("Error retrieving completion score");
        }
    }

    /**
     * Check if user has perfect completion
     */
    private boolean isPerfectCompletion(User user, Challenge challenge) {
        try {
            int score = getCompletionScore(user, challenge);
            return score == getTotalQuestions(challenge);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error checking perfect completion for user {} and challenge {}", user.getId(), challenge.getId(), e);
            throw new BusinessException("Error checking perfect completion");
        }
    }

    /**
     * Get single challenge by id for a user
     */
    @Transactional(readOnly = true)
    public MultiQuestionChallengeResponseDTO getChallengeByIdForUser(User user, Long challengeId) {
        try {
            Challenge challenge = challengeRepository.findById(challengeId)
                    .orElseThrow(() -> new BusinessException("Challenge not found"));

            return convertToMultiQuestionResponseDTO(challenge);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting challenge by id for user {} and challenge {}", user.getId(), challengeId, e);
            throw new BusinessException("Error retrieving challenge");
        }
    }

    /**
     * Build success result DTO
     */
    private ChallengeResultDTO buildSuccessResult(Challenge challenge, ChallengeSubmissionResult result,
                                                  int coinsEarned, int trophiesEarned,
                                                  List<String> earnedBadges) {
        String feedback = generateFeedback(result, earnedBadges);

        List<BadgeResponseDTO> badgeDTOs = earnedBadges.stream()
                .map(badgeName -> BadgeResponseDTO.builder()
                        .title(badgeName)
                        .build())
                .collect(Collectors.toList());

        return ChallengeResultDTO.builder()
                .success(true)
                .celebrationText("Well done!")
                .correctCount(result.getCorrectCount())
                .totalQuestions(result.getTotalQuestions())
                .coinsEarned(coinsEarned)
                .trophiesEarned(trophiesEarned)
                .totalCoins(coinsEarned)
                .challengeCompleted(true)
                .percentageScore(((double) result.getCorrectCount() / result.getTotalQuestions()) * 100)
                .badges(badgeDTOs)
                .overallFeedback(feedback)
                .build();
    }

    /**
     * Build already completed result
     */
    private ChallengeResultDTO buildAlreadyCompletedResult(Challenge challenge, User user) {
        int previousScore = getCompletionScore(user, challenge);
        int totalQuestions = getTotalQuestions(challenge);

        return ChallengeResultDTO.builder()
                .success(false)
                .celebrationText("Challenge already completed!")
                .correctCount(previousScore)
                .totalQuestions(totalQuestions)
                .coinsEarned(0)
                .trophiesEarned(0)
                .totalCoins(user.getCoins())
                .challengeCompleted(true)
                .percentageScore(((double) previousScore / totalQuestions) * 100)
                .overallFeedback("Challenge already completed! Your previous score: " + previousScore + "/" + totalQuestions)
                .build();
    }

    /**
     * Build incorrect result
     */
    private ChallengeResultDTO buildIncorrectResult(Challenge challenge, ChallengeSubmissionResult result) {
        return ChallengeResultDTO.builder()
                .success(false)
                .celebrationText("Better luck next time!")
                .correctCount(result.getCorrectCount())
                .totalQuestions(result.getTotalQuestions())
                .coinsEarned(0)
                .trophiesEarned(0)
                .totalCoins(0)
                .challengeCompleted(false)
                .percentageScore(((double) result.getCorrectCount() / result.getTotalQuestions()) * 100)
                .overallFeedback(generateIncorrectFeedback(result))
                .build();
    }

    /**
     * Generate appropriate feedback based on performance
     */
    private String generateFeedback(ChallengeSubmissionResult result, List<String> earnedBadges) {
        StringBuilder feedback = new StringBuilder();

        if (result.isPerfectScore()) {
            feedback.append("Perfect score! 🎉 ");
        } else if (result.getCorrectCount() >= result.getTotalQuestions() * 0.8) {
            feedback.append("Excellent work! ");
        } else if (result.getCorrectCount() >= result.getTotalQuestions() * 0.6) {
            feedback.append("Good job! ");
        } else {
            feedback.append("Nice try! ");
        }

        feedback.append("You got ").append(result.getCorrectCount())
                .append(" out of ").append(result.getTotalQuestions()).append(" correct.");

        if (!earnedBadges.isEmpty()) {
            feedback.append(" Earned badges: ").append(String.join(", ", earnedBadges));
        }

        return feedback.toString();
    }

    private String generateIncorrectFeedback(ChallengeSubmissionResult result) {
        return "You got " + result.getCorrectCount() + " out of " + result.getTotalQuestions() +
                " correct. Keep practicing!";
    }

    private String generateHistoricalFeedback(boolean completed, int score, int totalQuestions, boolean perfectScore) {
        if (!completed) {
            return "Challenge not yet completed.";
        }

        if (perfectScore) {
            return "Perfect score! " + score + "/" + totalQuestions;
        } else if (score >= totalQuestions * 0.8) {
            return "Excellent! " + score + "/" + totalQuestions;
        } else {
            return "Completed with score: " + score + "/" + totalQuestions;
        }
    }

    /**
     * Coin calculation logic
     */
    private int calculateCoinsEarned(int correctCount) {
        return 5 * correctCount; // 5 coins per correct answer
    }

    /**
     * Trophy calculation logic
     */
    private int calculateTrophiesEarned(Difficulty difficulty, boolean perfectScore) {
        int baseTrophies = getBaseTrophyCount(difficulty);
        return perfectScore ? baseTrophies + 2 : baseTrophies;
    }

    private int getBaseTrophyCount(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 1;
            case MEDIUM -> 3;
            case HARD -> 5;
        };
    }

    /**
     * Helper method to get total questions based on questions list
     */
    private int getTotalQuestions(Challenge challenge) {
        return challenge.getQuestions() != null ? challenge.getQuestions().size() : 0;
    }

    /**
     * Helper class for submission results
     */
    private static class ChallengeSubmissionResult {
        private final int correctCount;
        private final boolean perfectScore;
        private final int totalQuestions;

        public ChallengeSubmissionResult(int correctCount, boolean perfectScore, int totalQuestions) {
            this.correctCount = correctCount;
            this.perfectScore = perfectScore;
            this.totalQuestions = totalQuestions;
        }

        public int getCorrectCount() { return correctCount; }
        public boolean isPerfectScore() { return perfectScore; }
        public int getTotalQuestions() { return totalQuestions; }
    }

    /**
     * Search challenges with multiple filter criteria USING SPECIFICATIONS
     */
    @Transactional(readOnly = true)
    public List<MultiQuestionChallengeResponseDTO> searchChallenges(
            User user,
            String name,
            String description,
            Category category,
            String topic,
            Difficulty difficulty,
            Status status,
            List<AgeGroup> ageGroups,
            Integer minCoins,
            Integer maxCoins,
            Integer coinsForCorrectAnswer,
            Integer minTrophies,
            Integer maxTrophies,
            String timeDuration,
            SectionTitle sectionTitle,
            AnswerType answerType,
            Boolean enabled,
            LocalDate createdAfter,
            LocalDate createdBefore,
            LocalDate updatedAfter,
            LocalDate updatedBefore,
            String sortBy,
            String sortDir) {

        try {
            Specification<Challenge> spec = ChallengeSpecification.filter(
                    name, description, category, topic, difficulty, status,
                    ageGroups, minCoins, maxCoins, coinsForCorrectAnswer,
                    minTrophies, maxTrophies, timeDuration, sectionTitle,
                    answerType, enabled, createdAfter, createdBefore,
                    updatedAfter, updatedBefore, sortBy, sortDir);

            List<Challenge> filteredChallenges = challengeRepository.findAll(spec);

            return filteredChallenges.stream()
                    .map(this::convertToMultiQuestionResponseDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error searching challenges for user {}", user.getId(), e);
            throw new BusinessException("Error searching challenges");
        }
    }

    /**
     * Check if this is the first completion
     */
    private boolean isFirstCompletion(Long userId, Long challengeId) {
        try {
            return completionRepository.countByUserIdAndChallengeId(userId, challengeId) == 1;
        } catch (Exception e) {
            log.error("Error checking first completion for user {} and challenge {}", userId, challengeId, e);
            return false;
        }
    }
}