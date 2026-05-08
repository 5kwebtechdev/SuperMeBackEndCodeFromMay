package com.superme.service;

import com.superme.admin.model.Admin;
import com.superme.dto.*;
import com.superme.enums.*;
import com.superme.exception.BusinessException;
import com.superme.exception.ResourceNotFoundException;
import com.superme.model.*;
import com.superme.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * AdminChallengeService — merged, updated to work with multi-question Challenge model.
 *
 * - Multi-question CRUD (Challenge -> Question -> QuestionOption)
 * - Admin analytics and breakdowns
 * - Export (CSV / JSON)
 * - Soft delete / bulk delete / bulk status update
 * - Admin view conversion to AdminChallengeDTO
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AdminChallengeService {

    private final ChallengeRepository challengeRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final ChallengeAttachmentRepository challengeAttachmentRepository;
    private final UserChallengeCompletionRepository userChallengeCompletionRepository;

    // -------------------------------
    // Controller-specific (multi-question)
    // -------------------------------

    @Transactional
    public MultiQuestionChallengeResponseDTO createChallenge(MultiQuestionChallengeRequestDTO request, Admin user) {
        try {
            // 1️⃣ Convert DTO to entity
            Challenge challenge = convertToEntity(request);
            challenge.setCreatedByAdminId(user.getId());
            // 2️⃣ Set parent references for questions and attachments
            if (challenge.getQuestions() != null) {
                challenge.getQuestions().forEach(q -> q.setChallenge(challenge));
            }
            if (challenge.getAttachments() != null) {
                challenge.getAttachments().forEach(a -> a.setChallenge(challenge));
            }

            // 3️⃣ Save challenge — JPA will persist ageGroups and child entities in correct order
            Challenge saved = challengeRepository.save(challenge);

            // 4️⃣ Convert saved entity to response DTO
            return convertToResponseDTO(saved);

        } catch (IllegalArgumentException e) {
            throw new BusinessException("Validation error: " + e.getMessage());
        } catch (Exception e) {
            throw new BusinessException("Failed to create challenge: " + e.getMessage());
        }
    }


    public List<MultiQuestionChallengeResponseDTO> addChallengeList(List<MultiQuestionChallengeRequestDTO> dtos,Admin user) {
        List<MultiQuestionChallengeResponseDTO> responses = new ArrayList<>();
        for (MultiQuestionChallengeRequestDTO dto : dtos) {
            responses.add(createChallenge(dto,user));
        }
        return responses;
    }

//    public MultiQuestionChallengeResponseDTO updateChallenge(Long id, MultiQuestionChallengeRequestDTO request) {
//        try {
//            request.validateQuestions();
//            Challenge existing = challengeRepository.findById(id)
//                    .orElseThrow(() -> new ResourceNotFoundException("Challenge not found with id: " + id));
//
//            Challenge updated = convertToEntity(request);
//            updated.setId(existing.getId());
//            updated.setCreatedAt(existing.getCreatedAt());
//            updated.setCreatedBy(existing.getCreatedBy());
//            updated.setUpdatedAt(LocalDateTime.now());
//
//            // remove old questions/options/attachments
//            deleteChallengeQuestionsAndOptions(id);
//
//            Challenge saved = saveChallengeWithQuestions(updated);
//            return convertToResponseDTO(saved);
//        } catch (ResourceNotFoundException e) {
//            throw e;
//        } catch (IllegalArgumentException e) {
//            throw new BusinessException("Validation error: " + e.getMessage());
//        } catch (Exception e) {
//            throw new BusinessException("Failed to update challenge: " + e.getMessage());
//        }
//    }




    public MultiQuestionChallengeResponseDTO updateChallenge(Long id, MultiQuestionChallengeRequestDTO request) {

        request.validateQuestions();

        Challenge existing = challengeRepository.findByIdWithQuestions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge not found with id: " + id));

        // -------------------------
        // ✅ Update primitive fields
        // -------------------------
        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setDescriptionExpanded(request.getDescriptionExpanded());
        existing.setCategory(request.getCategory());
        existing.setDifficulty(request.getDifficulty());
        existing.setTopic(request.getTopic());
        existing.setStatus(request.getStatus());
        existing.setCoins(request.getCoins());
        existing.setCoinsForCorrectAnswer(request.getCoinsForCorrectAnswer());
        existing.setTrophies(request.getTrophies());
        existing.setTimeDuration(request.getTimeDuration());
        existing.setSectionTitle(request.getSectionTitle());
        existing.setPositiveFeedback(request.getPositiveFeedback());
        existing.setNegativeFeedback(request.getNegativeFeedback());
        existing.setNegativeFeedbackTryAgain(request.getNegativeFeedbackTryAgain());
        existing.setThumbnailImageUrl(request.getThumbnailImageUrl());
        existing.setInnerImageUrl(request.getInnerImageUrl());
        existing.setAgeGroups(request.getAgeGroups());
        existing.setEnabled(request.isEnabled());
        existing.setUpdatedAt(LocalDateTime.now());

        // -------------------------
        // ✅ HANDLE QUESTIONS (safe way)
        // -------------------------
        existing.getQuestions().clear(); // IMPORTANT for orphanRemoval

        for (QuestionRequestDTO qdto : request.getQuestions()) {
            Question q = convertQuestionRequestToEntity(qdto);
            q.setChallenge(existing);

            if (q.getOptions() != null) {
                q.getOptions().forEach(opt -> opt.setQuestion(q));
            }

            existing.getQuestions().add(q);
        }

        // -------------------------
        // ✅ HANDLE ATTACHMENTS (FIX HERE)
        // -------------------------
        if (existing.getAttachments() != null) {
            existing.getAttachments().clear(); // VERY IMPORTANT
        } else {
            existing.setAttachments(new ArrayList<>());
        }

        if (request.getAttachments() != null) {
            for (ChallengeAttachmentRequestDTO dto : request.getAttachments()) {
                ChallengeAttachment att = convertAttachmentRequestToEntity(dto);
                att.setChallenge(existing);
                existing.getAttachments().add(att);
            }
        }

        // -------------------------
        // ✅ SAVE (only once)
        // -------------------------
        Challenge saved = challengeRepository.save(existing);

        return convertToResponseDTO(saved);
    }







    public MultiQuestionChallengeResponseDTO getChallengeById(Long id) {
        Challenge challenge = challengeRepository.findByIdWithQuestions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge not found with id: " + id));
        return convertToResponseDTO(challenge);
    }

    public List<MultiQuestionChallengeResponseDTO> getAllChallenges() {
        List<Challenge> challenges = challengeRepository.findAllWithQuestions();
        return challenges.stream().map(this::convertToResponseDTO).collect(Collectors.toList());
    }

    // -------------------------------
    // Save/delete helpers (multi-question) - FIXED VERSION
    // -------------------------------

    private Challenge saveChallengeWithQuestions(Challenge challenge) {
        if (challenge.getEnabled() == null) challenge.setEnabled(true);

        // Save challenge WITHOUT children first to get an ID
        // Clear collections temporarily to avoid cascade issues
        List<Question> tempQuestions = challenge.getQuestions();
        List<ChallengeAttachment> tempAttachments = challenge.getAttachments();

        challenge.setQuestions(null);
        challenge.setAttachments(null);

        // Save challenge to get ID
        Challenge savedChallenge = challengeRepository.save(challenge);

        // Restore collections
        savedChallenge.setQuestions(tempQuestions);
        savedChallenge.setAttachments(tempAttachments);

        // Save questions
        if (tempQuestions != null && !tempQuestions.isEmpty()) {
            for (Question q : tempQuestions) {
                q.setChallenge(savedChallenge);
                Question savedQ = questionRepository.save(q);

                // Save options
                if (q.getOptions() != null && !q.getOptions().isEmpty()) {
                    for (QuestionOption opt : q.getOptions()) {
                        opt.setQuestion(savedQ);
                        questionOptionRepository.save(opt);
                    }
                }
            }
        }

        // Save attachments
        if (tempAttachments != null && !tempAttachments.isEmpty()) {
            for (ChallengeAttachment att : tempAttachments) {
                att.setChallenge(savedChallenge);
                challengeAttachmentRepository.save(att);
            }
        }

        // Return the fully saved challenge
        return challengeRepository.findByIdWithQuestions(savedChallenge.getId())
                .orElse(savedChallenge);
    }

    private void deleteChallengeQuestionsAndOptions(Long challengeId) {
        List<Question> questions = questionRepository.findByChallengeId(challengeId);
        for (Question q : questions) {
            questionOptionRepository.deleteByQuestionId(q.getId());
        }
        questionRepository.deleteByChallengeId(challengeId);
        challengeAttachmentRepository.deleteByChallengeId(challengeId);
    }

    // -------------------------------
    // Conversion: Request DTO -> Entity - FIXED VERSION
    // -------------------------------

    private Challenge convertToEntity(MultiQuestionChallengeRequestDTO request) {
        Challenge challenge = Challenge.builder()
                .name(request.getName())
                .description(request.getDescription())
                .descriptionExpanded(request.getDescriptionExpanded())
                .category(request.getCategory())
                .difficulty(request.getDifficulty())
                .topic(request.getTopic())
                .status(request.getStatus())
                .coins(request.getCoins())
                .coinsForCorrectAnswer(request.getCoinsForCorrectAnswer())
                .trophies(request.getTrophies())
                .timeDuration(request.getTimeDuration())
                .sectionTitle(request.getSectionTitle())
                .positiveFeedback(request.getPositiveFeedback())
                .negativeFeedback(request.getNegativeFeedback())
                .negativeFeedbackTryAgain(request.getNegativeFeedbackTryAgain())
                .thumbnailImageUrl(request.getThumbnailImageUrl())
                .innerImageUrl(request.getInnerImageUrl())
                .ageGroups(request.getAgeGroups())
                .enabled(request.isEnabled())
                .build();

        // -----------------------
// Map ageGroups as List
// -----------------------
        if (request.getAgeGroups() != null && !request.getAgeGroups().isEmpty()) {
            // DTO already contains List<AgeGroup>, just copy it
            challenge.setAgeGroups(new ArrayList<>(request.getAgeGroups()));
        }


        // -----------------------
        // Map questions
        // -----------------------
        if (request.getQuestions() != null) {
            List<Question> questions = request.getQuestions().stream()
                    .map(this::convertQuestionRequestToEntity)
                    .collect(Collectors.toList());

            // Set challenge reference on each question
            for (Question q : questions) {
                q.setChallenge(challenge);
            }
            challenge.setQuestions(questions);
        }

        // -----------------------
        // Map attachments
        // -----------------------
        if (request.getAttachments() != null) {
            List<ChallengeAttachment> attachments = request.getAttachments().stream()
                    .map(this::convertAttachmentRequestToEntity)
                    .collect(Collectors.toList());

            // Set challenge reference on each attachment
            for (ChallengeAttachment a : attachments) {
                a.setChallenge(challenge);
            }
            challenge.setAttachments(attachments);
        }

        return challenge;
    }


    private Question convertQuestionRequestToEntity(QuestionRequestDTO dto) {
        Question q = Question.builder()
                .questionOrder(dto.getQuestionOrder())
                .questionText(dto.getQuestionText())
                .questionImageUrl(dto.getQuestionImageUrl())
                .hint(dto.getHint())
                .answerType(dto.getAnswerType())
                .points(dto.getPoints())
                .timeLimit(dto.getTimeLimit())
                .attachmentUrl(dto.getAttachmentUrl())
                .attachmentType(dto.getAttachmentType())
                .build();

        if (dto.getOptions() != null) {
            List<QuestionOption> opts = dto.getOptions().stream()
                    .map(this::convertOptionRequestToEntity)
                    .collect(Collectors.toList());
            q.setOptions(opts);

            // Set question reference on each option
            for (QuestionOption option : opts) {
                option.setQuestion(q);
            }
        }
        return q;
    }

    private QuestionOption convertOptionRequestToEntity(QuestionOptionRequestDTO dto) {
        return QuestionOption.builder()
                .optionText(dto.getOptionText())
                .optionImageUrl(dto.getOptionImageUrl())
                .optionOrder(dto.getOptionOrder())
                .isCorrect(dto.getIsCorrect())
                .explanation(dto.getExplanation())
                .build();
    }

    private ChallengeAttachment convertAttachmentRequestToEntity(ChallengeAttachmentRequestDTO dto) {
        return ChallengeAttachment.builder()
                .fileName(dto.getFileName())
                .fileUrl(dto.getFileUrl())
                .fileType(dto.getFileType())
                .fileSize(dto.getFileSize())
                .build();
        // NOTE: Challenge reference will be set later in saveChallengeWithQuestions
    }

    // -------------------------------
    // Conversion: Entity -> Response DTO (multi-question)
    // -------------------------------

    private MultiQuestionChallengeResponseDTO convertToResponseDTO(Challenge challenge) {
        MultiQuestionChallengeResponseDTO resp = MultiQuestionChallengeResponseDTO.builder()
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
                .positiveFeedback(challenge.getPositiveFeedback())
                .negativeFeedback(challenge.getNegativeFeedback())
                .negativeFeedbackTryAgain(challenge.getNegativeFeedbackTryAgain())
                .thumbnailImageUrl(challenge.getThumbnailImageUrl())
                .innerImageUrl(challenge.getInnerImageUrl())
                .enabled(challenge.getEnabled() != null ? challenge.getEnabled() : true)
                .build();

        if (challenge.getQuestions() != null) {
            List<QuestionResponseDTO> qDtos = challenge.getQuestions().stream()
                    .map(this::convertQuestionToResponseDTO)
                    .collect(Collectors.toList());
            resp.setQuestions(qDtos);
            resp.setTotalQuestions(qDtos.size());
            int totalPoints = qDtos.stream().mapToInt(q -> q.getPoints() != null ? q.getPoints() : 0).sum();
            resp.setTotalPoints(totalPoints);
        }

        if (challenge.getAttachments() != null) {
            List<ChallengeAttachmentResponseDTO> attDtos = challenge.getAttachments().stream()
                    .map(this::convertAttachmentToResponseDTO)
                    .collect(Collectors.toList());
            resp.setAttachments(attDtos);
        }

        List<UserChallengeCompletion> completions = userChallengeCompletionRepository.findByChallenge_Id(challenge.getId());
        resp.setCompletionCount((long) completions.size());
        if (!completions.isEmpty()) {
            double avgScore = completions.stream().mapToInt(UserChallengeCompletion::getScore).average().orElse(0.0);
            resp.setAverageScore(avgScore);
        }

        return resp;
    }

    private QuestionResponseDTO convertQuestionToResponseDTO(Question q) {
        QuestionResponseDTO dto = QuestionResponseDTO.builder()
                .questionId(q.getId())
                .questionOrder(q.getQuestionOrder())
                .questionText(q.getQuestionText())
                .questionImageUrl(q.getQuestionImageUrl())
                .hint(q.getHint())
                .answerType(q.getAnswerType())
                .points(q.getPoints())
                .timeLimit(q.getTimeLimit())
                .attachmentUrl(q.getAttachmentUrl())
                .attachmentType(q.getAttachmentType())
                .createdAt(q.getCreatedAt())
                .updatedAt(q.getUpdatedAt())
                .build();

        if (q.getOptions() != null) {
            List<QuestionOptionResponseDTO> opts = q.getOptions().stream()
                    .map(this::convertOptionToResponseDTO)
                    .collect(Collectors.toList());
            dto.setOptions(opts);
        }
        return dto;
    }

    private QuestionOptionResponseDTO convertOptionToResponseDTO(QuestionOption opt) {
        return QuestionOptionResponseDTO.builder()
                .optionId(opt.getId())
                .optionText(opt.getOptionText())
                .optionImageUrl(opt.getOptionImageUrl())
                .optionOrder(opt.getOptionOrder())
                .isCorrect(opt.getIsCorrect())
                .explanation(opt.getExplanation())
                .createdAt(opt.getCreatedAt())
                .updatedAt(opt.getUpdatedAt())
                .build();
    }

    private ChallengeAttachmentResponseDTO convertAttachmentToResponseDTO(ChallengeAttachment att) {
        return ChallengeAttachmentResponseDTO.builder()
                .attachmentId(att.getId())
                .fileName(att.getFileName())
                .fileUrl(att.getFileUrl())
                .fileType(att.getFileType())
                .fileSize(att.getFileSize())
                .createdAt(att.getCreatedAt())
                .updatedAt(att.getUpdatedAt())
                .build();
    }

    // -------------------------------
    // Admin style views (AdminChallengeDTO)
    // -------------------------------

    /**
     * Returns AdminChallengeDTO view for all challenges (used in /overview, search, export)
     */
    public List<AdminChallengeDTO> getAllChallengeViews() {
        List<Challenge> challenges = challengeRepository.findAllWithQuestions();
        return challenges.stream().map(this::convertToAdminDTO).collect(Collectors.toList());
    }

    private AdminChallengeDTO convertToAdminDTO(Challenge challenge) {
        AdminChallengeDTO dto = new AdminChallengeDTO();
        dto.setChallengeId(challenge.getId());
        dto.setName(challenge.getName());
        dto.setDescription(challenge.getDescription());
        dto.setDescriptionExpanded(challenge.getDescriptionExpanded());

        dto.setType(challenge.getCategory() != null ? challenge.getCategory().getDisplayName() : null);
        dto.setTypeValue(challenge.getCategory() != null ? challenge.getCategory().name() : null);

        dto.setDifficultyLevel(challenge.getDifficulty() != null ? challenge.getDifficulty().getDisplayName() : null);
        dto.setDifficultyValue(challenge.getDifficulty() != null ? challenge.getDifficulty().name() : null);

        dto.setAgeGroups(challenge.getAgeGroups());
        dto.setAgeGroupDisplayNames(challenge.getAgeGroups() != null ?
                challenge.getAgeGroups().stream().map(AgeGroup::getDisplayName).collect(Collectors.toList()) :
                new ArrayList<>());

        dto.setTopic(challenge.getTopic());
        dto.setCoins(challenge.getCoins());
        dto.setCoinsForCorrectAnswer(challenge.getCoinsForCorrectAnswer());
        dto.setTrophies(challenge.getTrophies());
        dto.setTimeDuration(challenge.getTimeDuration());
        dto.setSectionTitle(challenge.getSectionTitle());
        dto.setSectionTitleDisplay(challenge.getSectionTitle() != null ? challenge.getSectionTitle().getDisplayName() : null);

        // Get hint from first question if available
        String hint = null;
        if (challenge.getQuestions() != null && !challenge.getQuestions().isEmpty()) {
            Question firstQuestion = challenge.getQuestions().get(0);
            hint = firstQuestion.getHint();
        }
        dto.setHint(hint);

        dto.setThumbnailImageUrl(challenge.getThumbnailImageUrl());
        dto.setInnerImageUrl(challenge.getInnerImageUrl());

        dto.setHasAttachments(challenge.getAttachments() != null && !challenge.getAttachments().isEmpty());
        dto.setAttachments(challenge.getAttachments() != null ?
                challenge.getAttachments().stream().map(this::convertAttachmentToResponseDTO).collect(Collectors.toList()) :
                null);

        dto.setCreatedBy(String.valueOf(challenge.getCreatedByAdminId()));
        dto.setCreatedAt(challenge.getCreatedAt());
        dto.setUpdatedBy(String.valueOf(challenge.getUpdatedByUserId()));
        dto.setUpdatedAt(challenge.getUpdatedAt());

        dto.setPositiveFeedback(challenge.getPositiveFeedback());
        dto.setNegativeFeedback(challenge.getNegativeFeedback());
        dto.setNegativeFeedbackTryAgain(challenge.getNegativeFeedbackTryAgain());

        dto.setStatus(challenge.getStatus());
        dto.setStatusDisplay(challenge.getStatus() != null ? challenge.getStatus().getDisplayName() : null);

        dto.setEnabled(challenge.getEnabled() != null ? challenge.getEnabled() : true);

        // Question-based metrics
        int numberOfQuestions = challenge.getQuestions() != null ? challenge.getQuestions().size() : 0;
        dto.setNumberOfQuestions(numberOfQuestions);
        dto.setMultiQuestion(numberOfQuestions > 1);

        // Collect answer types used across questions (distinct)
        List<AnswerType> answerTypes = (challenge.getQuestions() == null) ? new ArrayList<>() :
                challenge.getQuestions().stream()
                        .map(Question::getAnswerType)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList());
        dto.setAnswerTypes(answerTypes);
        dto.setAnswerTypeDisplayNames(answerTypes.stream().map(AnswerType::getDisplayName).collect(Collectors.toList()));

        // Completion stats (by challenge)
        List<UserChallengeCompletion> completions = userChallengeCompletionRepository.findByChallenge_Id(challenge.getId());
        dto.setCompletionCount((long) completions.size());
        dto.setAverageScore(completions.stream().mapToInt(UserChallengeCompletion::getScore).average().orElse(0.0));
        dto.setCompletionRate(completions.size() > 0 ? 100.0 : 0.0);

        // Check if any question option is image-based
        boolean anyImageOption = challenge.getQuestions() != null &&
                challenge.getQuestions().stream()
                        .flatMap(q -> q.getOptions() != null ? q.getOptions().stream() : Stream.empty())
                        .anyMatch(opt -> opt.getOptionImageUrl() != null && !opt.getOptionImageUrl().isEmpty());
        dto.setHasImageOptions(anyImageOption);

        dto.setHasImageQuestion(challenge.getQuestions() != null &&
                challenge.getQuestions().stream().anyMatch(Question::hasImage));
        dto.setHasMultipleAgeGroups(challenge.getAgeGroups() != null && challenge.getAgeGroups().size() > 1);

        return dto;
    }

    // -------------------------------
    // Admin analytics / dashboard
    // -------------------------------

    public ChallengeStatsResponse getDashboardStats() {
        long totalChallenges = challengeRepository.count();
        long totalQuestions = questionRepository.count();

        long published = challengeRepository.countByStatus(Status.APPROVED);
        long drafts = challengeRepository.countByStatus(Status.DRAFT);
        long underReview = challengeRepository.countByStatus(Status.VERIFICATION_PENDING);

        long totalCompletions = userChallengeCompletionRepository.count();
        double avgCompletionRate = totalChallenges > 0 ? (double) totalCompletions / totalChallenges * 100.0 : 0.0;

        return ChallengeStatsResponse.builder()
                .totalChallenges(totalChallenges)
                .totalChallengesPublished(published)
                .totalDrafts(drafts)
                .totalUnderReview(underReview)
                .averageCompletionRate(avgCompletionRate)
                .build();
    }

    public Map<String, Object> getChallengeBreakdowns() {
        Map<String, Object> breakdowns = new HashMap<>();

        List<Challenge> challenges = challengeRepository.findAllWithQuestions();

        // category breakdown
        Map<Category, Long> categoryBreakdown = challenges.stream()
                .collect(Collectors.groupingBy(Challenge::getCategory, Collectors.counting()));
        breakdowns.put("categoryBreakdown", categoryBreakdown);

        // difficulty breakdown
        Map<Difficulty, Long> difficultyBreakdown = challenges.stream()
                .collect(Collectors.groupingBy(Challenge::getDifficulty, Collectors.counting()));
        breakdowns.put("difficultyBreakdown", difficultyBreakdown);

        // age group breakdown
        Map<String, Long> ageGroupBreakdown = new HashMap<>();
        for (Challenge c : challenges) {
            if (c.getAgeGroups() != null) {
                for (AgeGroup ag : c.getAgeGroups()) {
                    ageGroupBreakdown.merge(ag.getDisplayName(), 1L, Long::sum);
                }
            }
        }
        breakdowns.put("ageGroupBreakdown", ageGroupBreakdown);

        // answer type breakdown (aggregate from questions)
        Map<AnswerType, Long> answerTypeBreakdown = challenges.stream()
                .flatMap(ch -> ch.getQuestions() != null ? ch.getQuestions().stream() : Stream.empty())
                .map(Question::getAnswerType)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(at -> at, Collectors.counting()));
        breakdowns.put("answerTypeBreakdown", answerTypeBreakdown);

        // questions distribution (by number of questions per challenge)
        Map<String, Long> questionsDistribution = challenges.stream()
                .collect(Collectors.groupingBy(
                        ch -> getQuestionRange(ch.getQuestions() != null ? ch.getQuestions().size() : 0),
                        Collectors.counting()));
        breakdowns.put("questionsDistribution", questionsDistribution);

        // completionStats (challengeId -> count)
        Map<Long, Long> completionStats = userChallengeCompletionRepository.findAll().stream()
                .filter(c -> c.getChallenge() != null)
                .collect(Collectors.groupingBy(c -> c.getChallenge().getId(), Collectors.counting()));
        breakdowns.put("completionStats", completionStats);

        return breakdowns;
    }

    // -------------------------------
    // Filter option providers
    // -------------------------------

    public List<Map<String, String>> getAvailableTypes() {
        return Arrays.stream(Category.values())
                .map(cat -> Map.of("value", cat.name(), "label", cat.getDisplayName()))
                .collect(Collectors.toList());
    }

    public List<Map<String, String>> getAvailableDifficulties() {
        return Arrays.stream(Difficulty.values())
                .map(d -> Map.of("value", d.name(), "label", d.getDisplayName()))
                .collect(Collectors.toList());
    }

    public List<Map<String, String>> getAvailableAgeGroups() {
        return Arrays.stream(AgeGroup.values())
                .map(a -> Map.of("value", a.name(), "label", a.getDisplayName()))
                .collect(Collectors.toList());
    }

    public List<Map<String, String>> getAvailableAnswerTypes() {
        return Arrays.stream(AnswerType.values())
                .map(a -> Map.of("value", a.name(), "label", a.getDisplayName()))
                .collect(Collectors.toList());
    }

    public List<Map<String, String>> getAvailableStatuses() {
        return Arrays.stream(Status.values())
                .map(s -> Map.of("value", s.name(), "label", s.getDisplayName()))
                .collect(Collectors.toList());
    }

    // -------------------------------
    // Search / Filter / Export
    // -------------------------------

    public List<AdminChallengeDTO> searchChallenges(String query, int limit, int offset) {
        List<AdminChallengeDTO> all = getAllChallengeViews();
        List<AdminChallengeDTO> filtered = all.stream()
                .filter(dto -> dto.matchesSearchTerm(query))
                .collect(Collectors.toList());
        int start = Math.max(0, offset);
        int end = Math.min(filtered.size(), start + limit);
        return start < filtered.size() ? filtered.subList(start, end) : Collections.emptyList();
    }

    public String exportChallengeData(AdminChallengeDTO.ChallengeFilterCriteria criteria, String format) {
        List<AdminChallengeDTO> all = getAllChallengeViews();
        List<AdminChallengeDTO> filtered = (criteria == null) ? all :
                all.stream().filter(dto -> dto.matchesFilters(criteria)).collect(Collectors.toList());

        if ("csv".equalsIgnoreCase(format)) return generateCSV(filtered);
        if ("json".equalsIgnoreCase(format)) return generateJSON(filtered);
        throw new IllegalArgumentException("Unsupported export format: " + format);
    }

    private String generateCSV(List<AdminChallengeDTO> challenges) {
        StringBuilder sb = new StringBuilder();
        sb.append("Challenge ID,Name,Description,Type,Difficulty,Age Groups,Questions,Answer Types,Status,Created At,Updated At\n");
        for (AdminChallengeDTO ch : challenges) {
            sb.append(ch.getChallengeId()).append(",")
                    .append("\"").append(escapeCsv(ch.getName())).append("\",")
                    .append("\"").append(escapeCsv(ch.getDescription())).append("\",")
                    .append(ch.getType() != null ? ch.getType() : "").append(",")
                    .append(ch.getDifficultyLevel() != null ? ch.getDifficultyLevel() : "").append(",")
                    .append("\"").append(escapeCsv(ch.getAgeGroupsAsString())).append("\",")
                    .append(ch.getNumberOfQuestions() != null ? ch.getNumberOfQuestions() : 0).append(",")
                    .append("\"").append(escapeCsv(String.join(", ", ch.getAnswerTypeDisplayNames() == null ? Collections.emptyList() : ch.getAnswerTypeDisplayNames()))).append("\",")
                    .append(ch.getStatusDisplay() != null ? ch.getStatusDisplay() : "").append(",")
                    .append(ch.getFormattedCreatedAt() != null ? ch.getFormattedCreatedAt() : "").append(",")
                    .append(ch.getFormattedUpdatedAt() != null ? ch.getFormattedUpdatedAt() : "").append("\n");
        }
        return sb.toString();
    }

    private String generateJSON(List<AdminChallengeDTO> challenges) {
        StringBuilder json = new StringBuilder();
        json.append("[");
        for (int i = 0; i < challenges.size(); i++) {
            AdminChallengeDTO ch = challenges.get(i);
            json.append("{")
                    .append("\"challengeId\":").append(ch.getChallengeId()).append(",")
                    .append("\"name\":\"").append(escapeJson(ch.getName())).append("\",")
                    .append("\"description\":\"").append(escapeJson(ch.getDescription())).append("\",")
                    .append("\"type\":\"").append(escapeJson(ch.getType())).append("\",")
                    .append("\"difficulty\":\"").append(escapeJson(ch.getDifficultyLevel())).append("\",")
                    .append("\"ageGroups\":\"").append(escapeJson(ch.getAgeGroupsAsString())).append("\",")
                    .append("\"questions\":").append(ch.getNumberOfQuestions() != null ? ch.getNumberOfQuestions() : 0).append(",")
                    .append("\"answerTypes\":\"").append(escapeJson(String.join(", ", ch.getAnswerTypeDisplayNames() == null ? Collections.emptyList() : ch.getAnswerTypeDisplayNames()))).append("\",")
                    .append("\"status\":\"").append(escapeJson(ch.getStatusDisplay())).append("\",")
                    .append("\"createdAt\":\"").append(escapeJson(ch.getFormattedCreatedAt())).append("\",")
                    .append("\"updatedAt\":\"").append(escapeJson(ch.getFormattedUpdatedAt())).append("\"")
                    .append("}");
            if (i < challenges.size() - 1) json.append(",");
        }
        json.append("]");
        return json.toString();
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        return s.replace("\"", "\"\"");
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // -------------------------------
    // Soft delete / bulk operations
    // -------------------------------

    public void softDeleteChallenge(Long id) {
        Challenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge with ID " + id + " not found"));
        if (Boolean.FALSE.equals(challenge.getEnabled())) {
            throw new BusinessException("Challenge is already deleted");
        }
        challenge.setEnabled(false);
        challenge.setUpdatedAt(LocalDateTime.now());
        challengeRepository.save(challenge);
    }

    public void bulkDeleteChallenges(List<Long> challengeIds) {
        List<Challenge> list = challengeRepository.findAllById(challengeIds);
        list.forEach(ch -> {
            ch.setEnabled(false);
            ch.setUpdatedAt(LocalDateTime.now());
        });
        challengeRepository.saveAll(list);
    }

    public void bulkUpdateStatus(List<Long> challengeIds, Status newStatus) {
        List<Challenge> list = challengeRepository.findAllById(challengeIds);
        list.forEach(ch -> {
            ch.setStatus(newStatus);
            ch.setUpdatedAt(LocalDateTime.now());
        });
        challengeRepository.saveAll(list);
    }

    public Challenge updateChallengeStatus(StatusUpdateRequest request) {
        Challenge challenge = challengeRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Challenge not found with id: " + request.getId()));
        challenge.setStatus(request.getStatus());
        challenge.setUpdatedAt(LocalDateTime.now());
        return challengeRepository.save(challenge);
    }

    // -------------------------------
    // Utilities
    // -------------------------------

    public List<AdminChallengeDTO> getAllChallengeViewsForFilter(AdminChallengeDTO.ChallengeFilterCriteria criteria, int page, int size) {
        // Simple implementation: get all and filter in-memory (controller uses pagination parameters)
        List<AdminChallengeDTO> all = getAllChallengeViews();
        List<AdminChallengeDTO> filtered = (criteria == null) ? all : all.stream().filter(dto -> dto.matchesFilters(criteria)).collect(Collectors.toList());
        int start = Math.min(page * size, filtered.size());
        int end = Math.min(start + size, filtered.size());
        return filtered.subList(start, end);
    }

    private String getQuestionRange(int count) {
        if (count == 0) return "0";
        if (count <= 5) return "1-5";
        if (count <= 10) return "6-10";
        if (count <= 20) return "11-20";
        return "20+";
    }
}