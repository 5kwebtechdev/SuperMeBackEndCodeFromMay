package com.superme.controller;

import com.superme.dto.*;
import com.superme.enums.*;
import com.superme.exception.BusinessException;
import com.superme.exception.InternalServerErrorException;
import com.superme.exception.ResourceNotFoundException;
import com.superme.model.User;
import com.superme.service.ChallengeService;
import com.superme.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/challenges")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class ChallengeController {

    @Autowired
    private ChallengeService challengeService;

    @Autowired
    private UserRepository userRepository;

    // Helper method to get user from Principal - FIXED
    private User getUserFromPrincipal(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        try {
            // Principal name should be the user ID (as seen in logs: Principal: 63)
            Long userId = Long.valueOf(principal.getName());
            return userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with ID: " + userId));
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user ID format in principal");
        }
    }

    // ✅ NEW: Search challenges with multiple criteria
    @GetMapping("/search")
    public ResponseEntity<?> searchChallenges(
            Principal principal,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) List<AgeGroup> ageGroups,
            @RequestParam(required = false) Integer minCoins,
            @RequestParam(required = false) Integer maxCoins,
            @RequestParam(required = false) Integer coinsForCorrectAnswer,
            @RequestParam(required = false) Integer minTrophies,
            @RequestParam(required = false) Integer maxTrophies,
            @RequestParam(required = false) String timeDuration,
            @RequestParam(required = false) SectionTitle sectionTitle,
            @RequestParam(required = false) AnswerType answerType,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdBefore,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate updatedAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate updatedBefore,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir) {

        try {
            User user = getUserFromPrincipal(principal);

            List<MultiQuestionChallengeResponseDTO> challenges = challengeService.searchChallenges(
                    user,
                    name, description, category, topic, difficulty, status,
                    ageGroups, minCoins, maxCoins, coinsForCorrectAnswer,
                    minTrophies, maxTrophies, timeDuration, sectionTitle,
                    answerType, enabled, createdAfter, createdBefore,
                    updatedAfter, updatedBefore, sortBy, sortDir);

            if (challenges.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "status", HttpStatus.NOT_FOUND.value(),
                                "message", "No challenges found matching the search criteria"
                        ));
            }

            // Calculate statistics
            long totalChallenges = challenges.size();
            long completedChallenges = challenges.stream()
                    .filter(challenge -> challenge.getCompletionCount() != null && challenge.getCompletionCount() > 0)
                    .count();
            double completionRate = totalChallenges > 0 ? (completedChallenges * 100.0) / totalChallenges : 0.0;
            long totalCoins = challenges.stream().mapToLong(MultiQuestionChallengeResponseDTO::getCoins).sum();
            long totalTrophies = challenges.stream().mapToLong(MultiQuestionChallengeResponseDTO::getTrophies).sum();

            ChallengeSearchResponse response = new ChallengeSearchResponse(
                    null, // We'll need to update ChallengeSearchResponse to handle MultiQuestionChallengeResponseDTO
                    totalChallenges, completedChallenges,
                    completionRate, totalCoins, totalTrophies
            );

            // For now, return the challenges directly
            return ResponseEntity.ok(challenges);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "message", "Error searching challenges"
                    ));
        }
    }

    // Get all challenges for the logged-in user
    @GetMapping
    public ResponseEntity<?> getAllChallenges(Principal principal) {
        try {
            User user = getUserFromPrincipal(principal);
            List<MultiQuestionChallengeResponseDTO> challenges = challengeService.getAllChallengesForUser(user);

            if (challenges.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "status", HttpStatus.NOT_FOUND.value(),
                                "message", "No challenges found"
                        ));
            }

            return ResponseEntity.ok(challenges);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "message", "Error retrieving challenges"
                    ));
        }
    }

    // Get challenges suitable for user's age
    @GetMapping("/age-based")
    public ResponseEntity<?> getChallengesForUserAge(
            Principal principal,
            @RequestParam(required = false) Integer age) {
        try {
            User user = getUserFromPrincipal(principal);

            // Use user's age if not provided
            int userAge = (age != null) ? age : user.getAge();

            List<MultiQuestionChallengeResponseDTO> challenges = challengeService.getChallengesForUserAge(user, userAge);

            if (challenges.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "status", HttpStatus.NOT_FOUND.value(),
                                "message", "No age-appropriate challenges found"
                        ));
            }

            return ResponseEntity.ok(challenges);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "message", "Error retrieving age-based challenges"
                    ));
        }
    }

    // Get challenges by specific age group
    @GetMapping("/age-group/{ageGroup}")
    public ResponseEntity<?> getChallengesByAgeGroup(
            Principal principal,
            @PathVariable AgeGroup ageGroup) {
        try {
            User user = getUserFromPrincipal(principal);
            List<MultiQuestionChallengeResponseDTO> challenges = challengeService.getChallengesByAgeGroup(user, ageGroup);

            if (challenges.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "status", HttpStatus.NOT_FOUND.value(),
                                "message", "No challenges found for age group: " + ageGroup.getDisplayName()
                        ));
            }

            return ResponseEntity.ok(challenges);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "message", "Error retrieving challenges for age group"
                    ));
        }
    }

    // Get single challenge by id
    @GetMapping("/{challengeId}")
    public ResponseEntity<?> getChallengeById(
            Principal principal,
            @PathVariable Long challengeId) {
            User user = getUserFromPrincipal(principal);
            MultiQuestionChallengeResponseDTO challengeDTO = challengeService.getChallengeByIdForUser(user, challengeId);

            if (challengeDTO == null) {
                throw new ResourceNotFoundException("Challenge not found with ID: " + challengeId);
            }
            return ResponseEntity.ok(challengeDTO);
        }



    // Get all questions/options for a challenge
    @GetMapping("/{challengeId}/questions")
    public ResponseEntity<?> getChallengeQuestions(
            Principal principal,
            @PathVariable Long challengeId) {
        try {
            User user = getUserFromPrincipal(principal);
            List<?> questions = challengeService.getChallengeQuestionsForUser(user, challengeId);

            if (questions == null || questions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "status", HttpStatus.NOT_FOUND.value(),
                                "message", "No questions found for this challenge"
                        ));
            }

            return ResponseEntity.ok(questions);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "message", "Error retrieving questions"
                    ));
        }
    }

    // Get challenge result for a user
    @GetMapping("/{challengeId}/result")
    public ResponseEntity<?> getChallengeResult(
            Principal principal,
            @PathVariable Long challengeId) {
        try {
            User user = getUserFromPrincipal(principal);
            ChallengeResultDTO result = challengeService.getChallengeResultForUser(user, challengeId);

            if (result == null) {
                throw new BusinessException("No result found for this challenge");
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            throw new BusinessException("Error retrieving challenge result: " + e.getMessage());
        }
    }

    // Submit a challenge answer - returns detailed ChallengeResultDTO
    // Single question submission (existing method - KEEP THIS)
    @PostMapping("/{challengeId}/submit")
    public ResponseEntity<?> submitChallenge(
            Principal principal,
            @PathVariable Long challengeId,
            @RequestParam String selectedOption) {
        try {
            User user = getUserFromPrincipal(principal);
            ChallengeResultDTO result = challengeService.submitChallenge(user, challengeId, selectedOption);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            throw new BusinessException("Error submitting challenge: " + e.getMessage());
        }
    }

    // NEW: Batch submission endpoint for submitting all questions at once
    @PostMapping("/{challengeId}/submit-all")
    public ResponseEntity<?> submitChallengeBatch(
            Principal principal,
            @PathVariable Long challengeId,
            @RequestBody ChallengeBatchSubmissionDTO submission) {
        try {
            User user = getUserFromPrincipal(principal);
            ChallengeResultDTO result = challengeService.submitChallengeBatch(user, challengeId, submission);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            throw new BusinessException("Error submitting challenge batch: " + e.getMessage());
        }
    }

    // Use hint for a challenge
    @GetMapping("/hint/{questionId}/use")
    public HintDto useHint(
            Principal principal,
            @PathVariable Long questionId) {

        User user = getUserFromPrincipal(principal);
        HintDto hintDto = challengeService.useHint(user, questionId);

        if (hintDto == null) {
            throw new ResourceNotFoundException("Hint not found for question ID: " + questionId);
        }

        return hintDto;
    }
}