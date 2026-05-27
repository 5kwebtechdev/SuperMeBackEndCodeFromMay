package com.superme.service;
import com.superme.admin.dto.*;
import com.superme.admin.dto.QuestionResponseDTO;
import com.superme.admin.model.Admin;
import com.superme.dto.*;
import com.superme.dto.MultiQuestionChallengeRequestDTO;
import com.superme.enums.*;
import com.superme.exception.BusinessException;
import com.superme.exception.ResourceNotFoundException;
import com.superme.model.*;
import com.superme.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneId;
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
    private final ChallengeFileStorageService challengeFileStorageService;

    // Extracts the bare filename from a stored value that may be a full path or already just a filename.
    private String extractFilename(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) return null;
        int lastSlash = Math.max(storedPath.lastIndexOf('/'), storedPath.lastIndexOf('\\'));
        return lastSlash >= 0 ? storedPath.substring(lastSlash + 1) : storedPath;
    }

    private String buildThumbnailDownloadUrl(String storedPath) {
        return challengeFileStorageService.getThumbnailUrl(extractFilename(storedPath));
    }

    private String buildAttachmentDownloadUrl(String storedPath) {
        return challengeFileStorageService.getAttachmentUrl(extractFilename(storedPath));
    }

    private String buildQuestionVisualDownloadUrl(String storedPath) {
        return challengeFileStorageService.getQuestionVisualUrl(extractFilename(storedPath));
    }

    private String buildQuestionOptionDownloadUrl(String storedPath) {
        return challengeFileStorageService.getQuestionOptionUrl(extractFilename(storedPath));
    }

    // -------------------------------
    // Controller-specific (multi-question)
    // -------------------------------

    @Transactional
//    public MultiQuestionChallengeResponseDTO createChallenge(MultiQuestionChallengeRequestDTO request, Admin user) {
//        try {
//            // 1️⃣ Convert DTO to entity
//            Challenge challenge = convertToEntity(request);
//            challenge.setCreatedByAdminId(user.getId());
//            // 2️⃣ Set parent references for questions and attachments
//            if (challenge.getQuestions() != null) {
//                challenge.getQuestions().forEach(q -> q.setChallenge(challenge));
//            }
//            if (challenge.getAttachments() != null) {
//                challenge.getAttachments().forEach(a -> a.setChallenge(challenge));
//            }
//
//            // 3️⃣ Save challenge — JPA will persist ageGroups and child entities in correct order
//            Challenge saved = challengeRepository.save(challenge);
//
//            // 4️⃣ Convert saved entity to response DTO
//            return convertToResponseDTO(saved);
//
//        } catch (IllegalArgumentException e) {
//            throw new BusinessException("Validation error: " + e.getMessage());
//        } catch (Exception e) {
//            throw new BusinessException("Failed to create challenge: " + e.getMessage());
//        }
//    }


    public List<ChallengeResponseDTO > addChallengeList(List<MultiQuestionChallengeRequestDTO> dtos,Admin user) {
        List<ChallengeResponseDTO > responses = new ArrayList<>();
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




//    public MultiQuestionChallengeResponseDTO updateChallenge(Long id, MultiQuestionChallengeRequestDTO request) {
//
//        request.validateQuestions();
//
//        Challenge existing = challengeRepository.findByIdWithQuestions(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Challenge not found with id: " + id));
//
//        // -------------------------
//        // ✅ Update primitive fields
//        // -------------------------
//        existing.setName(request.getName());
//        existing.setDescription(request.getDescription());
//        existing.setDescriptionExpanded(request.getDescriptionExpanded());
//        existing.setCategory(request.getCategory());
//        existing.setDifficulty(request.getDifficulty());
//        existing.setTopic(request.getTopic());
//        existing.setStatus(request.getStatus());
//        existing.setCoins(request.getCoins());
//        existing.setCoinsForCorrectAnswer(request.getCoinsForCorrectAnswer());
//        existing.setTrophies(request.getTrophies());
//        existing.setTimeDuration(request.getTimeDuration());
//        existing.setSectionTitle(request.getSectionTitle());
//        existing.setPositiveFeedback(request.getPositiveFeedback());
//        existing.setNegativeFeedback(request.getNegativeFeedback());
//        existing.setNegativeFeedbackTryAgain(request.getNegativeFeedbackTryAgain());
//        existing.setThumbnailImageUrl(request.getThumbnailImageUrl());
//        existing.setInnerImageUrl(request.getInnerImageUrl());
//        existing.setAgeGroups(request.getAgeGroups());
//        existing.setEnabled(request.isEnabled());
//        existing.setUpdatedAt(LocalDateTime.now());
//
//        // -------------------------
//        // ✅ HANDLE QUESTIONS (safe way)
//        // -------------------------
//        existing.getQuestions().clear(); // IMPORTANT for orphanRemoval
//
//        for (QuestionRequestDTO qdto : request.getQuestions()) {
//            Question q = convertQuestionRequestToEntity(qdto);
//            q.setChallenge(existing);
//
//            if (q.getOptions() != null) {
//                q.getOptions().forEach(opt -> opt.setQuestion(q));
//            }
//
//            existing.getQuestions().add(q);
//        }
//
//        // -------------------------
//        // ✅ HANDLE ATTACHMENTS (FIX HERE)
//        // -------------------------
//        if (existing.getAttachments() != null) {
//            existing.getAttachments().clear(); // VERY IMPORTANT
//        } else {
//            existing.setAttachments(new ArrayList<>());
//        }
//
//        if (request.getAttachments() != null) {
//            for (ChallengeAttachmentRequestDTO dto : request.getAttachments()) {
//                ChallengeAttachment att = convertAttachmentRequestToEntity(dto);
//                att.setChallenge(existing);
//                existing.getAttachments().add(att);
//            }
//        }
//
//        // -------------------------
//        // ✅ SAVE (only once)
//        // -------------------------
//        Challenge saved = challengeRepository.save(existing);
//
//        return convertToResponseDTO(saved);
//    }







    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE CHALLENGE
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public ChallengeResponseDTO updateChallenge(MultiQuestionChallengeRequestDTO request, Admin admin) {
        if (request.getChallengeId() == null) {
            throw new BusinessException("challengeId is required for update.");
        }

        Challenge challenge = challengeRepository.findByIdWithQuestions(request.getChallengeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Challenge not found with id: " + request.getChallengeId()));

        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());

        // ── 1. Update scalar fields (only when non-null in the request) ──────
        if (request.getName() != null)                challenge.setName(request.getName());
        if (request.getDescription() != null)         challenge.setDescription(request.getDescription());
        if (request.getDescriptionExpanded() != null) challenge.setDescriptionExpanded(request.getDescriptionExpanded());
        if (request.getCategory() != null)            challenge.setCategory(request.getCategory());
        if (request.getDifficulty() != null)          challenge.setDifficulty(request.getDifficulty());
        if (request.getTopic() != null)               challenge.setTopic(request.getTopic());
        if (request.getTimeDuration() != null)        challenge.setTimeDuration(request.getTimeDuration());
        if (request.getCoins() != null)               challenge.setCoins(request.getCoins());
        if (request.getCoinsForCorrectAnswer() != null) challenge.setCoinsForCorrectAnswer(request.getCoinsForCorrectAnswer());
        if (request.getTrophies() != null)            challenge.setTrophies(request.getTrophies());
        if (request.getSectionTitle() != null)        challenge.setSectionTitle(request.getSectionTitle());
        if (request.getStatus() != null)              challenge.setStatus(request.getStatus());
        if (request.getEnabled() != null)             challenge.setEnabled(request.getEnabled());
        if (request.getQuestionMode() != null)        challenge.setQuestionMode(request.getQuestionMode());
        if (request.getAgeGroups() != null && !request.getAgeGroups().isEmpty()) {
            challenge.setAgeGroups(request.getAgeGroupEnums());
        }
        challenge.setUpdatedByUserId(admin.getId());
        challenge.setUpdatedAt(now);

        // ── 2. Thumbnail — update only when a new file arrives ────────────────
        if (request.getThumbnail() != null && !request.getThumbnail().isEmpty()) {
            challengeFileStorageService.deleteThumbnail(extractFilename(challenge.getThumbnailImageUrl()));
            String newFilename = challengeFileStorageService.saveThumbnail(request.getThumbnail());
            challenge.setThumbnailImageUrl(newFilename);
        }
        // else: no thumbnail in payload → keep existing untouched

        // ── 3. Save challenge base (flush before touching attachments) ────────
        Challenge saved = challengeRepository.save(challenge);

        // ── 4. Remove explicitly deleted attachments ─────────────────────────
        if (request.getRemovedAttachmentIds() != null && !request.getRemovedAttachmentIds().isEmpty()) {
            for (Long attachmentId : request.getRemovedAttachmentIds()) {
                challengeAttachmentRepository.findById(attachmentId).ifPresent(att -> {
                    if (att.getChallenge().getId().equals(saved.getId())) {
                        challengeFileStorageService.deleteAttachment(extractFilename(att.getFileUrl()));
                        challengeAttachmentRepository.delete(att);
                    }
                });
            }
        }

        // ── 5. Add new attachment files ───────────────────────────────────────
        if (request.getAttachments() != null) {
            for (MultipartFile file : request.getAttachments()) {
                if (file == null || file.isEmpty()) continue;
                String filename = challengeFileStorageService.saveAttachment(file);
                ChallengeAttachment att = new ChallengeAttachment();
                att.setChallenge(saved);
                att.setFileName(file.getOriginalFilename());
                att.setFileUrl(filename);
                att.setFileSize(file.getSize());
                att.setFileType(resolveFileType(file.getOriginalFilename()));
                att.setCreatedAt(now);
                att.setUpdatedAt(now);
                challengeAttachmentRepository.save(att);
            }
        }

        // ── 6. Replace questions entirely ─────────────────────────────────────
        if (request.getQuestions() != null) {
            // Snapshot old question visuals and option images BEFORE deleting
            Map<Integer, String> oldVisualsByOrder = new java.util.LinkedHashMap<>();
            // key = "questionOrder-optionOrder" -> optionImageUrl
            Map<String, String> oldOptionImagesByKey = new java.util.LinkedHashMap<>();
            if (saved.getQuestions() != null) {
                for (Question q : saved.getQuestions()) {
                    if (q.getQuestionImageUrl() != null && !q.getQuestionImageUrl().isBlank()) {
                        oldVisualsByOrder.put(q.getQuestionOrder(), q.getQuestionImageUrl());
                    }
                    if (q.getOptions() != null) {
                        for (QuestionOption opt : q.getOptions()) {
                            if (opt.getOptionImageUrl() != null && !opt.getOptionImageUrl().isBlank()) {
                                oldOptionImagesByKey.put(q.getQuestionOrder() + "-" + opt.getOptionOrder(),
                                        opt.getOptionImageUrl());
                            }
                        }
                    }
                }
            }

            // Delete all existing options then questions (files handled per-question below)
            List<Question> existing = new ArrayList<>(saved.getQuestions() != null
                    ? saved.getQuestions() : List.of());
            for (Question q : existing) {
                if (q.getOptions() != null && !q.getOptions().isEmpty()) {
                    questionOptionRepository.deleteAll(q.getOptions());
                }
                questionRepository.delete(q);
            }
            saved.getQuestions().clear();
            challengeRepository.saveAndFlush(saved);

            // Insert fresh questions
            int order = 0;
            List<Question> newQuestions = new ArrayList<>();
            for (MultiQuestionChallengeRequestDTO.QuestionDTO qDto : request.getQuestions()) {
                int currentOrder = order++;
                Question q = new Question();
                q.setChallenge(saved);
                q.setQuestionOrder(currentOrder);
                q.setQuestionText(qDto.getQuestionText());
                q.setHint(qDto.getHint());
                q.setPositiveFeedback(qDto.getPositiveFeedback());
                q.setNegativeFeedback(qDto.getNegativeFeedback());
                q.setNegativeFeedbackTryAgain(qDto.getNegativeFeedbackTryAgain());
                q.setPoints(10);
                q.setTimeLimit(30);
                q.setAnswerType(resolveAnswerType(qDto.getAnswerType()));

                // Question visual: replace if new file sent, otherwise preserve old
                String oldVisual = oldVisualsByOrder.get(currentOrder);
                if (qDto.getQuestionVisual() != null && !qDto.getQuestionVisual().isEmpty()) {
                    if (oldVisual != null) {
                        challengeFileStorageService.deleteQuestionVisual(extractFilename(oldVisual));
                    }
                    q.setQuestionImageUrl(challengeFileStorageService.saveQuestionVisual(qDto.getQuestionVisual()));
                } else {
                    q.setQuestionImageUrl(oldVisual);
                }

                q.setCreatedAt(now);
                q.setUpdatedAt(now);

                Question savedQ = questionRepository.save(q);

                if (qDto.getOptions() != null) {
                    List<QuestionOption> opts = new ArrayList<>();
                    for (MultiQuestionChallengeRequestDTO.OptionDTO optDto : qDto.getOptions()) {
                        boolean hasText  = optDto.getOptionText()  != null && !optDto.getOptionText().trim().isEmpty();
                        boolean hasFile  = optDto.getOptionFile()  != null && !optDto.getOptionFile().isEmpty();
                        boolean hasOrder = optDto.getOptionOrder() != null;
                        // Skip only if there is truly nothing — not even an order slot to carry an old image into
                        if (!hasText && !hasFile && !hasOrder) continue;

                        QuestionOption opt = new QuestionOption();
                        opt.setQuestion(savedQ);
                        opt.setOptionText(hasText ? optDto.getOptionText() : null);
                        opt.setOptionOrder(optDto.getOptionOrder());
                        opt.setIsCorrect(Boolean.TRUE.equals(optDto.getIsCorrect()));

                        // Option image: replace if new file sent, otherwise preserve old
                        String optKey = currentOrder + "-" + optDto.getOptionOrder();
                        String oldOptImage = oldOptionImagesByKey.get(optKey);
                        if (hasFile) {
                            if (oldOptImage != null) {
                                challengeFileStorageService.deleteQuestionOption(extractFilename(oldOptImage));
                            }
                            opt.setOptionImageUrl(challengeFileStorageService.saveQuestionOption(optDto.getOptionFile()));
                        } else {
                            // No new file — carry forward existing image (null-safe: null if there was none)
                            opt.setOptionImageUrl(oldOptImage);
                        }

                        opt.setCreatedAt(now);
                        opt.setUpdatedAt(now);
                        opts.add(opt);
                    }
                    if (!opts.isEmpty()) {
                        questionOptionRepository.saveAll(opts);
                        savedQ.setOptions(opts);
                    }
                }
                newQuestions.add(savedQ);
            }
            saved.getQuestions().addAll(newQuestions);
        }

        // ── 7. Individual-mode questionVisual update ──────────────────────────
        // When questions list is absent (INDIVIDUAL mode), update the first question's visual
        if (request.getQuestions() == null
                && request.getQuestionVisual() != null
                && !request.getQuestionVisual().isEmpty()) {
            List<Question> qs = saved.getQuestions();
            if (qs != null && !qs.isEmpty()) {
                Question firstQ = qs.get(0);
                if (firstQ.getQuestionImageUrl() != null && !firstQ.getQuestionImageUrl().isBlank()) {
                    challengeFileStorageService.deleteQuestionVisual(extractFilename(firstQ.getQuestionImageUrl()));
                }
                firstQ.setQuestionImageUrl(challengeFileStorageService.saveQuestionVisual(request.getQuestionVisual()));
                questionRepository.save(firstQ);
            }
        }

        // ── 8. Reload fresh and return ────────────────────────────────────────
        Challenge finalChallenge = challengeRepository.findByIdWithQuestions(saved.getId())
                .orElse(saved);
        return convertToChallengeResponseDTO(finalChallenge);
    }

    private ChallengeAttachment.FileType resolveFileType(String filename) {
        if (filename == null) return ChallengeAttachment.FileType.PNG;
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf"))                          return ChallengeAttachment.FileType.PDF;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return ChallengeAttachment.FileType.JPG;
        return ChallengeAttachment.FileType.PNG;
    }

    private AnswerType resolveAnswerType(String raw) {
        if (raw == null) return AnswerType.MCQ;
        return switch (raw.toUpperCase()) {
            case "TRUE_FALSE", "TF" -> AnswerType.TRUE_FALSE;
            case "VISUALS"          -> AnswerType.VISUALS;
            default                 -> AnswerType.MCQ;
        };
    }

    // ─────────────────────────────────────────────────────────────────────────

    public ChallengeResponseDTO getChallengeById(Long id) {
        Challenge challenge = challengeRepository.findByIdWithQuestions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge not found with id: " + id));
//        return convertToResponseDTO(challenge);
        return convertToChallengeResponseDTO(challenge);
    }

//    public List<MultiQuestionChallengeResponseDTO> getAllChallenges() {
//        List<Challenge> challenges = challengeRepository.findAllWithQuestions();
//        return challenges.stream().map(this::convertToResponseDTO).collect(Collectors.toList());
//    }





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

//    private Challenge convertToEntity(MultiQuestionChallengeRequestDTO request) {
//        Challenge challenge = Challenge.builder()
//                .name(request.getName())
//                .description(request.getDescription())
//                .descriptionExpanded(request.getDescriptionExpanded())
//                .category(request.getCategory())
//                .difficulty(request.getDifficulty())
//                .topic(request.getTopic())
//                .status(request.getStatus())
//                .coins(request.getCoins())
//                .coinsForCorrectAnswer(request.getCoinsForCorrectAnswer())
//                .trophies(request.getTrophies())
//                .timeDuration(request.getTimeDuration())
//                .sectionTitle(request.getSectionTitle())
//                .positiveFeedback(request.getPositiveFeedback())
//                .negativeFeedback(request.getNegativeFeedback())
//                .negativeFeedbackTryAgain(request.getNegativeFeedbackTryAgain())
//                .thumbnailImageUrl(request.getThumbnailImageUrl())
//                .innerImageUrl(request.getInnerImageUrl())
//                .ageGroups(request.getAgeGroups())
//                .enabled(request.isEnabled())
//                .build();
//
//        // -----------------------
//// Map ageGroups as List
//// -----------------------
//        if (request.getAgeGroups() != null && !request.getAgeGroups().isEmpty()) {
//            // DTO already contains List<AgeGroup>, just copy it
//            challenge.setAgeGroups(new ArrayList<>(request.getAgeGroups()));
//        }
//
//
//        // -----------------------
//        // Map questions
//        // -----------------------
//        if (request.getQuestions() != null) {
//            List<Question> questions = request.getQuestions().stream()
//                    .map(this::convertQuestionRequestToEntity)
//                    .collect(Collectors.toList());
//
//            // Set challenge reference on each question
//            for (Question q : questions) {
//                q.setChallenge(challenge);
//            }
//            challenge.setQuestions(questions);
//        }
//
//        // -----------------------
//        // Map attachments
//        // -----------------------
//        if (request.getAttachments() != null) {
//            List<ChallengeAttachment> attachments = request.getAttachments().stream()
//                    .map(this::convertAttachmentRequestToEntity)
//                    .collect(Collectors.toList());
//
//            // Set challenge reference on each attachment
//            for (ChallengeAttachment a : attachments) {
//                a.setChallenge(challenge);
//            }
//            challenge.setAttachments(attachments);
//        }
//
//        return challenge;
//    }


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

//    private MultiQuestionChallengeResponseDTO convertToResponseDTO(Challenge challenge) {
//        MultiQuestionChallengeResponseDTO resp = MultiQuestionChallengeResponseDTO.builder()
//                .challengeId(challenge.getId())
//                .name(challenge.getName())
//                .description(challenge.getDescription())
//                .descriptionExpanded(challenge.getDescriptionExpanded())
//                .category(challenge.getCategory())
//                .difficulty(challenge.getDifficulty())
//                .ageGroups(challenge.getAgeGroups())
//                .topic(challenge.getTopic())
//                .status(challenge.getStatus())
//                .coins(challenge.getCoins())
//                .coinsForCorrectAnswer(challenge.getCoinsForCorrectAnswer())
//                .trophies(challenge.getTrophies())
//                .timeDuration(challenge.getTimeDuration())
//                .sectionTitle(challenge.getSectionTitle())
//                .positiveFeedback(challenge.getPositiveFeedback())
//                .negativeFeedback(challenge.getNegativeFeedback())
//                .negativeFeedbackTryAgain(challenge.getNegativeFeedbackTryAgain())
//                .thumbnailImageUrl(challenge.getThumbnailImageUrl())
//                .innerImageUrl(challenge.getInnerImageUrl())
//                .enabled(challenge.getEnabled() != null ? challenge.getEnabled() : true)
//                .build();
//
//        if (challenge.getQuestions() != null) {
//            List<QuestionResponseDTO> qDtos = challenge.getQuestions().stream()
//                    .map(this::convertQuestionToResponseDTO)
//                    .collect(Collectors.toList());
//            resp.setQuestions(qDtos);
//            resp.setTotalQuestions(qDtos.size());
//            int totalPoints = qDtos.stream().mapToInt(q -> q.getPoints() != null ? q.getPoints() : 0).sum();
//            resp.setTotalPoints(totalPoints);
//        }
//
//        if (challenge.getAttachments() != null) {
//            List<ChallengeAttachmentResponseDTO> attDtos = challenge.getAttachments().stream()
//                    .map(this::convertAttachmentToResponseDTO)
//                    .collect(Collectors.toList());
//            resp.setAttachments(attDtos);
//        }
//
//        List<UserChallengeCompletion> completions = userChallengeCompletionRepository.findByChallenge_Id(challenge.getId());
//        resp.setCompletionCount((long) completions.size());
//        if (!completions.isEmpty()) {
//            double avgScore = completions.stream().mapToInt(UserChallengeCompletion::getScore).average().orElse(0.0);
//            resp.setAverageScore(avgScore);
//        }
//
//        return resp;
//    }

//    private QuestionResponseDTO convertQuestionToResponseDTO(Question q) {
//        QuestionResponseDTO dto = QuestionResponseDTO.builder()
//                .questionId(q.getId())
//                .questionOrder(q.getQuestionOrder())
//                .questionText(q.getQuestionText())
//                .questionImageUrl(q.getQuestionImageUrl())
//                .hint(q.getHint())
//                .answerType(q.getAnswerType())
//                .points(q.getPoints())
//                .timeLimit(q.getTimeLimit())
//                .attachmentUrl(q.getAttachmentUrl())
//                .attachmentType(q.getAttachmentType())
//                .createdAt(q.getCreatedAt())
//                .updatedAt(q.getUpdatedAt())
//                .build();
//
//        if (q.getOptions() != null) {
//            List<QuestionOptionResponseDTO> opts = q.getOptions().stream()
//                    .map(this::convertOptionToResponseDTO)
//                    .collect(Collectors.toList());
//            dto.setOptions(opts);
//        }
//        return dto;
//    }

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
                .fileUrl(buildAttachmentDownloadUrl(att.getFileUrl()))
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
        return getAllChallengeViews(null);
    }

    public List<AdminChallengeDTO> getAllChallengeViews(QuestionMode questionMode) {
        List<Challenge> challenges = (questionMode != null)
                ? challengeRepository.findAllWithQuestionsByQuestionMode(questionMode)
                : challengeRepository.findAllWithQuestions();
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

        dto.setThumbnailImageUrl(buildThumbnailDownloadUrl(challenge.getThumbnailImageUrl()));
        dto.setInnerImageUrl(buildThumbnailDownloadUrl(challenge.getInnerImageUrl()));

        dto.setHasAttachments(challenge.getAttachments() != null && !challenge.getAttachments().isEmpty());
        dto.setAttachments(challenge.getAttachments() != null ?
                challenge.getAttachments().stream().map(this::convertAttachmentToResponseDTO).collect(Collectors.toList()) :
                null);

        dto.setCreatedBy(String.valueOf(challenge.getCreatedByAdminId()));
        dto.setCreatedAt(challenge.getCreatedAt());
        dto.setUpdatedBy(String.valueOf(challenge.getUpdatedByUserId()));
        dto.setUpdatedAt(challenge.getUpdatedAt());

//        dto.setPositiveFeedback(challenge.getPositiveFeedback());
//        dto.setNegativeFeedback(challenge.getNegativeFeedback());
//        dto.setNegativeFeedbackTryAgain(challenge.getNegativeFeedbackTryAgain());

        dto.setStatus(challenge.getStatus() != null ? challenge.getStatus().name().toLowerCase() : null);
        dto.setStatusDisplay(challenge.getStatus() != null ? challenge.getStatus().getDisplayName() : null);

        dto.setEnabled(challenge.getEnabled() != null ? challenge.getEnabled() : true);

        // Question-based metrics
        int numberOfQuestions = challenge.getQuestions() != null ? challenge.getQuestions().size() : 0;
        dto.setNumberOfQuestions(numberOfQuestions);
        if (challenge.getQuestionMode() != null) {
            dto.setMultiQuestion(challenge.getQuestionMode() == QuestionMode.MULTI);
        } else {
            dto.setMultiQuestion(numberOfQuestions > 1);
        }

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
        long totalChallenges = challengeRepository.countNonDeleted();
        long totalQuestions = questionRepository.count();

        long published = challengeRepository.countByStatusAndNotDeleted(Status.PUBLISHED);
        long drafts = challengeRepository.countByStatusAndNotDeleted(Status.DRAFT);
        long underReview = challengeRepository.countByStatusAndNotDeleted(Status.VERIFICATION_PENDING);

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
        if (Boolean.TRUE.equals(challenge.getDeleted())) {
            throw new BusinessException("Challenge is already deleted");
        }
        challenge.setDeleted(true);
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






















    @Transactional
    public ChallengeResponseDTO  createChallenge(
            MultiQuestionChallengeRequestDTO request,
            Admin admin) {

        try {
            // 1. Upload thumbnail if present
            String thumbnailUrl = null;
            if (request.getThumbnail() != null && !request.getThumbnail().isEmpty()) {
                thumbnailUrl = challengeFileStorageService.saveThumbnail(request.getThumbnail());
            }

            // 2. Create and save Challenge entity
            Challenge challenge = new Challenge();
            challenge.setName(request.getName());
            challenge.setDescription(request.getDescription());
            challenge.setDescriptionExpanded(request.getDescriptionExpanded());
            challenge.setCategory(request.getCategory());
            challenge.setDifficulty(request.getDifficulty());
            challenge.setTopic(request.getTopic());
            challenge.setTimeDuration(request.getTimeDuration());
            challenge.setCoins(request.getCoins() != null ? request.getCoins() : 0);
            challenge.setCoinsForCorrectAnswer(request.getCoinsForCorrectAnswer() != null ? request.getCoinsForCorrectAnswer() : 0);
            challenge.setTrophies(request.getTrophies() != null ? request.getTrophies() : 0);
            challenge.setSectionTitle(request.getSectionTitle());
            challenge.setStatus(request.getStatus() != null ? request.getStatus() : Status.DRAFT);
            challenge.setQuestionMode(request.getQuestionMode());
            challenge.setThumbnailImageUrl(thumbnailUrl);
            challenge.setCreatedByAdminId(admin.getId());
            challenge.setEnabled(true);

            // Set age groups
//            if (request.getAgeGroups() != null) {
//                challenge.setAgeGroups(new ArrayList<>(request.getAgeGroups()));
//            } else {
//                challenge.setAgeGroups(new ArrayList<>());
//            }





            // With:
            if (request.getAgeGroups() != null && !request.getAgeGroups().isEmpty()) {
                List<AgeGroup> ageGroupEnums = request.getAgeGroups().stream()
                        .map(age -> {
                            try {
                                return AgeGroup.valueOf(age);
                            } catch (IllegalArgumentException e) {
                                System.err.println("Invalid age group: " + age);
                                return null;
                            }
                        })
                        .filter(age -> age != null)
                        .collect(Collectors.toList());
                challenge.setAgeGroups(ageGroupEnums);
            } else {
                challenge.setAgeGroups(new ArrayList<>());
            }



            // Set timestamps
            LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
            challenge.setCreatedAt(now);
            challenge.setUpdatedAt(now);

            // Save challenge first to get ID
            Challenge savedChallenge = challengeRepository.save(challenge);

            // 3. Upload attachments
            if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
                List<ChallengeAttachment> attachments = new ArrayList<>();
                for (MultipartFile file : request.getAttachments()) {
                    if (file != null && !file.isEmpty()) {
                        String fileUrl = challengeFileStorageService.saveAttachment(file);
                        ChallengeAttachment attachment = new ChallengeAttachment();
                        attachment.setChallenge(savedChallenge);
                        attachment.setFileName(file.getOriginalFilename());
                        attachment.setFileUrl(fileUrl);
                        attachment.setFileSize(file.getSize());

                        // Determine file type
                        String fileName = file.getOriginalFilename();
                        if (fileName != null && fileName.toLowerCase().endsWith(".pdf")) {
                            attachment.setFileType(ChallengeAttachment.FileType.PDF);
                        } else if (fileName != null && (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg"))) {
                            attachment.setFileType(ChallengeAttachment.FileType.JPG);
                        } else if (fileName != null && fileName.toLowerCase().endsWith(".png")) {
                            attachment.setFileType(ChallengeAttachment.FileType.PNG);
                        }
                        attachment.setCreatedAt(now);
                        attachment.setUpdatedAt(now);
                        attachments.add(attachment);
                    }
                }
                savedChallenge.setAttachments(attachments);
            }

            // 4. Process questions
            List<Question> questions = new ArrayList<>();

            boolean isIndividual = request.getQuestionMode() == QuestionMode.INDIVIDUAL
                    || (request.getQuestions() == null || request.getQuestions().isEmpty());

            if (!isIndividual && request.getQuestions() != null && !request.getQuestions().isEmpty()) {
                // MULTI mode: list of questions from request.getQuestions()
                int questionOrder = 0;
                for (MultiQuestionChallengeRequestDTO.QuestionDTO qDto : request.getQuestions()) {
                    Question question = buildQuestion(savedChallenge, qDto.getQuestionText(),
                            qDto.getAnswerType(), qDto.getHint(), qDto.getPositiveFeedback(),
                            qDto.getNegativeFeedback(), qDto.getNegativeFeedbackTryAgain(),
                            questionOrder++, now);
                    if (qDto.getQuestionVisual() != null && !qDto.getQuestionVisual().isEmpty()) {
                        question.setQuestionImageUrl(challengeFileStorageService.saveQuestionVisual(qDto.getQuestionVisual()));
                    }
                    Question savedQuestion = questionRepository.save(question);
                    saveOptions(savedQuestion, qDto.getOptions(), now);
                    questions.add(savedQuestion);
                }
            } else if (request.getQuestionText() != null && !request.getQuestionText().trim().isEmpty()) {
                // INDIVIDUAL mode: flat single-question fields
                Question question = buildQuestion(savedChallenge, request.getQuestionText(),
                        request.getAnswerType(), request.getHint(), request.getPositiveFeedback(),
                        request.getNegativeFeedback(), request.getNegativeFeedbackTryAgain(),
                        0, now);
                if (request.getQuestionVisual() != null && !request.getQuestionVisual().isEmpty()) {
                    question.setQuestionImageUrl(challengeFileStorageService.saveQuestionVisual(request.getQuestionVisual()));
                }
                Question savedQuestion = questionRepository.save(question);
                saveOptions(savedQuestion, request.getOptions(), now);
                questions.add(savedQuestion);
            }

            if (!questions.isEmpty()) {
                savedChallenge.setQuestions(questions);
            }

            // Save everything again with relationships
            Challenge finalSaved = challengeRepository.save(savedChallenge);

            // 5. Convert to response DTO
            return convertToChallengeResponseDTO(finalSaved);

        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException("Failed to create challenge: " + e.getMessage());
        }
    }

    private Question buildQuestion(Challenge challenge, String questionText, String answerType,
                                   String hint, String positiveFeedback, String negativeFeedback,
                                   String negativeFeedbackTryAgain, int order, LocalDateTime now) {
        Question question = new Question();
        question.setChallenge(challenge);
        question.setQuestionOrder(order);
        question.setQuestionText(questionText);
        question.setHint(hint);
        question.setPositiveFeedback(positiveFeedback);
        question.setNegativeFeedback(negativeFeedback);
        question.setNegativeFeedbackTryAgain(negativeFeedbackTryAgain);
        question.setPoints(10);
        question.setTimeLimit(30);
        question.setCreatedAt(now);
        question.setUpdatedAt(now);
        if ("MCQ".equalsIgnoreCase(answerType)) {
            question.setAnswerType(AnswerType.MCQ);
        } else if ("TRUE_FALSE".equalsIgnoreCase(answerType) || "tf".equalsIgnoreCase(answerType)) {
            question.setAnswerType(AnswerType.TRUE_FALSE);
        } else if ("VISUALS".equalsIgnoreCase(answerType)) {
            question.setAnswerType(AnswerType.VISUALS);
        }
        return question;
    }

    private void saveOptions(Question savedQuestion,
                             List<MultiQuestionChallengeRequestDTO.OptionDTO> optDtos,
                             LocalDateTime now) {
        if (optDtos == null || optDtos.isEmpty()) return;
        List<QuestionOption> options = new ArrayList<>();
        for (MultiQuestionChallengeRequestDTO.OptionDTO optDto : optDtos) {
            boolean hasText = optDto.getOptionText() != null && !optDto.getOptionText().trim().isEmpty();
            boolean hasFile = optDto.getOptionFile() != null && !optDto.getOptionFile().isEmpty();
            if (!hasText && !hasFile) continue;

            QuestionOption option = new QuestionOption();
            option.setQuestion(savedQuestion);
            option.setOptionText(hasText ? optDto.getOptionText() : null);
            option.setOptionOrder(optDto.getOptionOrder());
            option.setIsCorrect(Boolean.TRUE.equals(optDto.getIsCorrect()));
            if (hasFile) {
                option.setOptionImageUrl(challengeFileStorageService.saveQuestionOption(optDto.getOptionFile()));
            }
            option.setCreatedAt(now);
            option.setUpdatedAt(now);
            options.add(option);
        }
        if (!options.isEmpty()) {
            savedQuestion.setOptions(options);
            questionOptionRepository.saveAll(options);
        }
    }






//    private String saveFile(MultipartFile file, String subDirectory) throws IOException {
//        Path uploadPath = Paths.get(uploadDirectory, subDirectory);
//        if (!Files.exists(uploadPath)) {
//            Files.createDirectories(uploadPath);
//        }
//
//        String originalFileName = file.getOriginalFilename();
//        String fileExtension = "";
//        if (originalFileName != null && originalFileName.contains(".")) {
//            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
//        }
//        String fileName = UUID.randomUUID().toString() + fileExtension;
//
//        Path filePath = uploadPath.resolve(fileName);
//        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
//
//        return "/uploads/" + subDirectory + "/" + fileName;
//    }

    private MultiQuestionChallengeResponseDTO convertToResponseDTO(Challenge challenge) {
        MultiQuestionChallengeResponseDTO dto = new MultiQuestionChallengeResponseDTO();
        dto.setChallengeId(challenge.getId());
        dto.setName(challenge.getName());
        dto.setDescription(challenge.getDescription());
        dto.setCategory(challenge.getCategory());
        dto.setDifficulty(challenge.getDifficulty());
        dto.setStatus(challenge.getStatus());
        dto.setThumbnailImageUrl(challenge.getThumbnailImageUrl());
        dto.setTotalQuestions(challenge.getTotalQuestions());
        return dto;
    }








































//    private ChallengeResponseDTO convertToChallengeResponseDTO(Challenge challenge) {
//        ChallengeResponseDTO dto = new ChallengeResponseDTO();
//
//        // Basic information
//        dto.setChallengeId(challenge.getId());
//        dto.setName(challenge.getName());
//        dto.setDescription(challenge.getDescription());
//        dto.setDescriptionExpanded(challenge.getDescriptionExpanded());
//        dto.setCategory(challenge.getCategory());
//        dto.setDifficulty(challenge.getDifficulty());
//        dto.setAgeGroups(challenge.getAgeGroups());
//        dto.setTopic(challenge.getTopic());
//        dto.setStatus(challenge.getStatus());
//        dto.setEnabled(challenge.getEnabled());
//
//        // Challenge settings
//        dto.setCoins(challenge.getCoins());
//        dto.setCoinsForCorrectAnswer(challenge.getCoinsForCorrectAnswer());
//        dto.setTrophies(challenge.getTrophies());
//        dto.setTimeDuration(challenge.getTimeDuration());
//        dto.setSectionTitle(challenge.getSectionTitle());
//
//        // Feedback
//        dto.setPositiveFeedback(challenge.getPositiveFeedback());
//        dto.setNegativeFeedback(challenge.getNegativeFeedback());
//        dto.setNegativeFeedbackTryAgain(challenge.getNegativeFeedbackTryAgain());
//
//        // Media URLs
//        dto.setThumbnailImageUrl(challenge.getThumbnailImageUrl());
//        dto.setInnerImageUrl(challenge.getInnerImageUrl());
//
//        // Questions and attachments
//        dto.setQuestions(convertToQuestionResponseDTOList(challenge.getQuestions()));
//        dto.setAttachments(convertToAttachmentResponseDTOList(challenge.getAttachments()));
//
//        // Statistics
//        dto.setTotalQuestions(challenge.getTotalQuestions());
//        dto.setTotalPoints(challenge.getTotalPoints());
//
//        // Display fields
//        dto.setDifficultyDisplay(challenge.getDifficulty() != null ? challenge.getDifficulty().getDisplayName() : null);
//        dto.setStatusDisplay(challenge.getStatus() != null ? challenge.getStatus().getDisplayName() : null);
//        dto.setPublished(challenge.isPublished());
//        dto.setAgeGroupsDisplay(challenge.getAgeGroupsDisplay());
//        dto.setCategoryDisplay(challenge.getCategory() != null ? challenge.getCategory().getDisplayName() : null);
//
//        return dto;
//    }

    private List<QuestionResponseDTO> convertToQuestionResponseDTOList(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            return new ArrayList<>();
        }

        return questions.stream()
                .sorted(Comparator.comparing(Question::getQuestionOrder))
                .map(this::convertToQuestionResponseDTO)
                .collect(Collectors.toList());
    }

//    private QuestionResponseDTO convertToQuestionResponseDTO(Question question) {
//        QuestionResponseDTO dto = new QuestionResponseDTO();
//        dto.setQuestionId(question.getId();
//        dto.setQuestionText(question.getQuestionText());
//        dto.setAnswerType(question.getAnswerType() != null ? question.getAnswerType().name() : null);
//        dto.setHint(question.getHint());
//        dto.setPositiveFeedback(question.getPositiveFeedback());
//        dto.setNegativeFeedback(question.getNegativeFeedback());
//        dto.setNegativeFeedbackTryAgain(question.getNegativeFeedbackTryAgain());
//        dto.setOptions(convertToOptionResponseDTOList(question.getOptions()));
//        return dto;
//    }



    private QuestionResponseDTO convertToQuestionResponseDTO(Question question) {
        QuestionResponseDTO dto = new QuestionResponseDTO();
        dto.setId(question.getId());
        dto.setQuestionText(question.getQuestionText());
        dto.setQuestionImageUrl(buildQuestionVisualDownloadUrl(question.getQuestionImageUrl()));
        dto.setAnswerType(question.getAnswerType() != null ? question.getAnswerType().name() : null);
        dto.setHint(question.getHint());
        dto.setPositiveFeedback(question.getPositiveFeedback());
        dto.setNegativeFeedback(question.getNegativeFeedback());
        dto.setNegativeFeedbackTryAgain(question.getNegativeFeedbackTryAgain());
        dto.setOptions(convertToOptionResponseDTOList(question.getOptions()));
        return dto;
    }



    private List<OptionResponseDTO> convertToOptionResponseDTOList(List<QuestionOption> options) {
        if (options == null || options.isEmpty()) {
            return new ArrayList<>();
        }

        return options.stream()
                .sorted(Comparator.comparing(QuestionOption::getOptionOrder))
                .map(this::convertToOptionResponseDTO)
                .collect(Collectors.toList());
    }

    private OptionResponseDTO convertToOptionResponseDTO(QuestionOption option) {
        OptionResponseDTO dto = new OptionResponseDTO();
        dto.setId(option.getId());
        dto.setOptionText(option.getOptionText());
        dto.setOptionImageUrl(buildQuestionOptionDownloadUrl(option.getOptionImageUrl()));
        dto.setOptionOrder(option.getOptionOrder());
        dto.setIsCorrect(option.getIsCorrect());
        return dto;
    }

    private List<AttachmentResponseDTO> convertToAttachmentResponseDTOList(List<ChallengeAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return new ArrayList<>();
        }

        return attachments.stream()
                .map(this::convertToAttachmentResponseDTO)
                .collect(Collectors.toList());
    }

    private AttachmentResponseDTO convertToAttachmentResponseDTO(ChallengeAttachment attachment) {
        AttachmentResponseDTO dto = new AttachmentResponseDTO();
        dto.setId(attachment.getId());
        dto.setFileName(attachment.getFileName());
        dto.setFileUrl(buildAttachmentDownloadUrl(attachment.getFileUrl()));
        dto.setFileType(attachment.getFileType() != null ? attachment.getFileType().name() : null);
        dto.setFileSize(attachment.getFileSize());
        return dto;
    }





























     public ChallengeApiResponse getAllChallenges(int page, int size, String id,
                                                  String title, String type, String status, String searchTerm) {

        // Build the specification for filtering
        Specification<Challenge> spec = buildSpecification(id, title, type, status, searchTerm);

        // Get total count before pagination (for filtered count)
//        long totalFilteredCount = challengeRepository.count(spec);
         long totalFilteredCount = challengeRepository.count(Specification.where(spec));
        // Get total count from database (unfiltered)
        long totalCount = challengeRepository.count();

        // Apply pagination
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Challenge> challengePage = challengeRepository.findAll(spec, pageable);

        // Convert to DTO using your existing method
        List<ChallengeResponseDTO> challenges = challengePage.getContent().stream()
                .map(this::convertToChallengeResponseDTO)
                .collect(Collectors.toList());

        // Build statistics
        ChallengeStatistics statistics = buildStatistics();

        // Build response matching Angular front-end expectations
        ChallengeApiResponse response = new ChallengeApiResponse();
        response.setSuccess(true);
        response.setChallenges(challenges);
        response.setStatistics(statistics);
        response.setTotalCount((int) totalCount);
        response.setFilteredCount((int) totalFilteredCount);
        response.setHasMore(challengePage.hasNext());
        response.setFiltered(searchTerm != null || type != null || status != null);
        response.setFilteredOutCount((int) (totalCount - totalFilteredCount));
        response.setMessage(null);

        // Set filter criteria
        FilterCriteria filterCriteria = new FilterCriteria();
        filterCriteria.setSearchTerm(searchTerm);
        filterCriteria.setType(type);
        filterCriteria.setStatus(status);
        filterCriteria.setDifficulty(null);
        filterCriteria.setAgeGroup(null);
        response.setFilterCriteria(filterCriteria);

        return response;
    }

    private Specification<Challenge> buildSpecification(String id, String title,
                                                        String type, String status, String searchTerm) {

        Specification<Challenge> spec = Specification.where(null);

        // Filter by ID (if provided and numeric)
        if (id != null && !id.trim().isEmpty()) {
            try {
                Long challengeId = Long.parseLong(id);
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("id"), challengeId));
            } catch (NumberFormatException e) {
                // Not a valid ID, ignore
            }
        }

        // Filter by title (if provided)
        if (title != null && !title.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + title.toLowerCase() + "%"));
        }

        // Global search term (searches in name and description)
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String searchPattern = "%" + searchTerm.toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("name")), searchPattern),
                            cb.like(cb.lower(root.get("description")), searchPattern)
                    ));
        }

        // Filter by type/category (maps to Angular's selectedCategory)
        if (type != null && !type.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("category")), type.toLowerCase()));
        }

        // Filter by status (maps APPROVED, DRAFT, PENDING to match Angular)
        if (status != null && !status.trim().isEmpty()) {
            try {
                // Angular sends: 'APPROVED', 'DRAFT', 'PENDING'
                Status challengeStatus = Status.valueOf(status.toUpperCase());
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("status"), challengeStatus));
            } catch (IllegalArgumentException e) {
                // Invalid status value, ignore
            }
        }

        return spec;
    }

    private ChallengeStatistics buildStatistics() {
        ChallengeStatistics stats = new ChallengeStatistics();
        stats.setTotalChallenges(challengeRepository.count());
        stats.setTotalQuiz(challengeRepository.countByCategory(Category.QUIZ));
        stats.setTotalPuzzles(challengeRepository.countByCategory(Category.PUZZLE));
        stats.setTotalArticles(challengeRepository.countByCategory(Category.ARTICLE));
        return stats;
    }

    // Your existing convertToChallengeResponseDTO method remains exactly as is
    private ChallengeResponseDTO convertToChallengeResponseDTO(Challenge challenge) {
        ChallengeResponseDTO dto = new ChallengeResponseDTO();

        // Basic information
        dto.setChallengeId(challenge.getId());
        dto.setName(challenge.getName());
        dto.setDescription(challenge.getDescription());
        dto.setDescriptionExpanded(challenge.getDescriptionExpanded());
        dto.setCategory(challenge.getCategory());
        dto.setDifficulty(challenge.getDifficulty());
        dto.setAgeGroups(challenge.getAgeGroups());
        dto.setTopic(challenge.getTopic());
        dto.setStatus(challenge.getStatus());
        dto.setEnabled(challenge.getEnabled());

        // Challenge settings
        dto.setCoins(challenge.getCoins());
        dto.setCoinsForCorrectAnswer(challenge.getCoinsForCorrectAnswer());
        dto.setTrophies(challenge.getTrophies());
        dto.setTimeDuration(challenge.getTimeDuration());
        dto.setSectionTitle(challenge.getSectionTitle());

        // Feedback
//        dto.setPositiveFeedback(challenge.getPositiveFeedback());
//        dto.setNegativeFeedback(challenge.getNegativeFeedback());
//        dto.setNegativeFeedbackTryAgain(challenge.getNegativeFeedbackTryAgain());

        // Media URLs — converted to proper download endpoints
        dto.setThumbnailImageUrl(buildThumbnailDownloadUrl(challenge.getThumbnailImageUrl()));
        dto.setInnerImageUrl(buildThumbnailDownloadUrl(challenge.getInnerImageUrl()));

        // Questions and attachments
        dto.setQuestions(convertToQuestionResponseDTOList(challenge.getQuestions()));
        dto.setAttachments(convertToAttachmentResponseDTOList(challenge.getAttachments()));

        // Statistics
        dto.setTotalQuestions(challenge.getTotalQuestions());
        dto.setTotalPoints(challenge.getTotalPoints());

        // Display fields
        dto.setDifficultyDisplay(challenge.getDifficulty() != null ? challenge.getDifficulty().getDisplayName() : null);
        dto.setStatusDisplay(challenge.getStatus() != null ? challenge.getStatus().getDisplayName() : null);
        dto.setPublished(challenge.isPublished());
        dto.setAgeGroupsDisplay(challenge.getAgeGroupsDisplay());
        dto.setCategoryDisplay(challenge.getCategory() != null ? challenge.getCategory().getDisplayName() : null);

        return dto;
    }


















}