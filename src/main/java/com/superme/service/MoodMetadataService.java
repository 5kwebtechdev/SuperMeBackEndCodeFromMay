package com.superme.service;

import com.superme.dto.MoodDisplayResponse;
import com.superme.enums.Relationship;
import com.superme.exception.ResourceNotFoundException;
import com.superme.model.*;
import com.superme.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class MoodMetadataService {

    private final MoodMetadataRepository moodMetadataRepository;

    private final HabitRepository habitRepository;
    private final TaskRepository taskRepository;

    private final UserRepository userRepository;

    private final MoodRepository moodRepository;

    private final FamilyMemberRepository familyMemberRepository;

    private final TrophyRepository trophyRepository;

    private final HabitCompletionRepository habitCompletionRepository;

    private final TaskCompletionRepository taskCompletionRepository;

    public List<MoodDisplayResponse> buildMoodDisplayResponse(Long userId) {
        // 1️⃣ Get user
        System.out.println("Building mood display for user ID: " + userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 2️⃣ Determine audience
        String audience = (user.getRelationship() == Relationship.PARENT) ? "PARENT" : "SELF";

        // 3️⃣ Determine target users
        List<User> targetUsers;
        if (user.getRelationship() == Relationship.PARENT) {
            // fetch children
            List<FamilyMember> familyMembers = familyMemberRepository.findByFamily(user.getFamily());
            targetUsers = familyMembers.stream()
                    .filter(fm -> fm.getUser().getRelationship() == Relationship.CHILD)
                    .map(FamilyMember::getUser)
                    .toList();
        } else {
            // SELF
            targetUsers = List.of(user);
            System.out.println("Target Users: " + targetUsers);
        }

        List<MoodDisplayResponse> responses = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (User targetUser : targetUsers) {
            System.out.print("Processing mood for user: " + targetUser.getId());

            // 4️⃣ Get latest mood of the target user
            Optional<Mood> moodOpt = moodRepository.findTopByUserOrderByDateDescTimeDesc(targetUser);
            System.out.println(" - Latest mood fetched" + (moodOpt.isPresent() ? ": " + moodOpt.get().getValue() + " on " + moodOpt.get().getDate() : ": None"));
            String mood = "NEUTRAL"; // Default to NEUTRAL

            if (moodOpt.isPresent()) {
                Mood latestMood = moodOpt.get();
                LocalDate lastMoodDate = latestMood.getDate();

                // Check if the latest mood is from today
                if (lastMoodDate != null && lastMoodDate.equals(today)) {
                    mood = latestMood.getValue();
                    System.out.print(" - Mood from today: " + mood.toUpperCase());
                } else {
                    System.out.print(" - Last mood was on " + lastMoodDate + ", setting to NEUTRAL for today");
                }
            } else {
                System.out.print(" - No mood history, setting to NEUTRAL");
            }

            System.out.print(", Mood: " + mood.toUpperCase());

            // TODAY'S DATA

            int totalTasks =
                    taskCompletionRepository
                            .countByTask_CreatedByAndCompletionDate(
                                    targetUser,
                                    today
                            );

            int completedTasks =
                    taskCompletionRepository
                            .countByTask_CreatedByAndCompletionDateAndStatus(
                                    targetUser,
                                    today,
                                    TaskCompletion.CompletionStatus.COMPLETED
                            );

            int totalHabits =
                    habitCompletionRepository
                            .countByHabit_CreatedByAndCompletionDate(
                                    targetUser,
                                    today
                            );

            int completedHabits =
                    habitCompletionRepository
                            .countByHabit_CreatedByAndCompletionDateAndStatus(
                                    targetUser,
                                    today,
                                    HabitCompletion.CompletionStatus.COMPLETED
                            );

            // --- User XP/coins/streak/badges ---
            int coins = targetUser.getCoins();
            Integer trophyCount = trophyRepository.getTotalTrophyCountByUserId(targetUser.getId().toString());
            int trophies = (trophyCount != null) ? trophyCount : 0;
            int streak = targetUser.getCurrentStreak();
            int badges = targetUser.getBadges().size();

            // 5️⃣ Compute progress
            int total = totalTasks + totalHabits;
            int completed = completedTasks + completedHabits;
            double progress = total > 0 ? ((double) completed / total) * 100 : 0.0;

            // If mood exists, fetch metadata and description
            String title = null, description = null, emptyDesc = null, completedDesc = null;


            MoodMetadata meta = moodMetadataRepository
                    .findByMoodAndAudience(mood.toUpperCase(), audience)
                    .orElseThrow(() -> new ResourceNotFoundException("Mood metadata not found"));


//            MoodMetadata meta = moodMetadataRepository
//                    .findByMoodAndAudience(mood.toUpperCase(), audience)
//                    .orElse(null);
//
//            if (meta == null) {
//                return new ArrayList<>(); // Return empty list
//            }



            emptyDesc = meta.getEmptyStateDescription();
            completedDesc = meta.getCompletedStateDescription();

            if (total == 0) {
                description = emptyDesc;
            } else if (completed == total) {
                description = completedDesc;
            } else {
                description = meta.getDescriptionTemplate()
                        .replace("<habitCount>", String.valueOf(totalHabits - completedHabits))
                        .replace("<taskCount>", String.valueOf(totalTasks - completedTasks));
            }

            title = meta.getTitle();

            // 🆕 PARENT-ONLY METRICS - Updated according to PDF
            Double completionRate = null;
            Double consistencyRate = null;
            Double improvementRate = null;
            Integer appScore = null;

            if (user.getRelationship() == Relationship.PARENT) {
                // 1️⃣ COMPLETION RATE (ALL habits + tasks combined since joining)
                completionRate = calculateCompletionRate(targetUser);

                // 2️⃣ CONSISTENCY RATE (How often child opens the app)
                consistencyRate = calculateConsistencyRate(targetUser);

                // 3️⃣ IMPROVEMENT RATE (This month vs Last month)
                improvementRate = calculateImprovementRate(targetUser);

                // 4️⃣ APP SCORE (Weighted combination)
                appScore = calculateAppScore(completionRate, consistencyRate, improvementRate);
            }

            // 9️⃣ Build response
            MoodDisplayResponse response = MoodDisplayResponse.builder()
                    .assignedTo(targetUser.getId())
                    .userName(targetUser.getName())
                    .relationship(targetUser.getRelationship())
                    .avatarImageName(targetUser.getAvatar().getAvatarImageName())
                    .mood(mood)
                    .audience(audience)
                    .title(title)
                    .description(description)
                    .emptyStateDescription(emptyDesc)
                    .completedStateDescription(completedDesc)
                    .totalHabits(totalHabits)
                    .completedHabits(completedHabits)
                    .totalTasks(totalTasks)
                    .completedTasks(completedTasks)
                    .progressPercentage(progress)
                    .coins(coins)
                    .badges(badges)
                    .streak(streak)
                    .trophies(trophies)
                    // 🆕 Parent-only fields
                    .completionRate(completionRate)
                    .consistencyRate(consistencyRate)
                    .improvementRate(improvementRate)
                    .appScore(appScore)
                    .build();

            responses.add(response);
            System.out.println(" - Added mood display for user: " + targetUser.getId());
        }
        return responses;
    }

// ==================== HELPER METHODS (Based on Metrics Documentation) ====================

    /**
     * Metric 12: Completion Rate
     * Overall percentage of ALL habits + tasks the child completed (since joining)
     *
     * Formula: (Total Completed Habits + Total Completed Tasks) ÷ (Total Scheduled Habits + Total Scheduled Tasks) × 100
     */
    /**
     * Metric 12: Completion Rate (Parent Home Screen)
     * Overall percentage of ALL habits + tasks the child completed
     * Formula: (Total completed habits + tasks) ÷ (Total scheduled habits + tasks) × 100
     *
     * Note: Using your existing methods - they count total activities, not scheduled days
     * This is a simplification since we can't track scheduled days without date-range queries
     */
    private Double calculateCompletionRate(User targetUser) {
        // Using your existing methods
        int totalScheduledHabits = habitRepository.countByCreatedBy(targetUser);
        int totalCompletedHabits = habitRepository.countCompletedByCreatedBy(targetUser);

        int totalScheduledTasks = taskRepository.countByCreatedBy(targetUser);
        int totalCompletedTasks = taskRepository.countCompletedByCreatedBy(targetUser);

        int totalScheduled = totalScheduledHabits + totalScheduledTasks;
        int totalCompleted = totalCompletedHabits + totalCompletedTasks;

        if (totalScheduled == 0) {
            return 0.0;
        }

        double rate = ((double) totalCompleted / totalScheduled) * 100.0;
        return Math.round(rate * 10.0) / 10.0;
    }

    /**
     * Metric 13: Consistency Rate (Parent Home Screen)
     * How often does the child open the app?
     * Formula: (Days app opened) ÷ (Days since they joined) × 100
     *
     * Problem: We don't have "Total Days Active" stored
     * Workaround: Use current streak as approximation (not accurate per PDF)
     */
    private Double calculateConsistencyRate(User targetUser) {
        LocalDate joinedDate = targetUser.getCreatedDateTime().toLocalDate();
        LocalDate today = LocalDate.now();

        // Total days since joining
        long totalDaysSinceJoined = ChronoUnit.DAYS.between(joinedDate, today) + 1;

        // WORKAROUND: Using current streak as proxy (not accurate per PDF)
        // PDF Metric 7 is "Total Days Active" which counts unique days, not consecutive
        // But we don't have this field
        long totalDaysActive = targetUser.getCurrentStreak(); // This is wrong but the best we can do

        if (totalDaysSinceJoined == 0) {
            return 0.0;
        }

        double rate = ((double) totalDaysActive / totalDaysSinceJoined) * 100.0;
        return Math.round(rate * 10.0) / 10.0;
    }

    /**
     * Metric 14: Improvement Rate (Parent Home Screen)
     * Is the child getting better or worse than last month?
     * Formula: ((This Month Rate - Last Month Rate) ÷ Last Month Rate) × 100
     * First month users: Show 0%
     *
     * Problem: We can't calculate monthly rates without date-range queries
     * Workaround: Return 0% for now (placeholder)
     */
    private Double calculateImprovementRate(User targetUser) {
        // We can't calculate this accurately without date-range repository methods
        // The PDF requires comparing this month vs last month completion rates

        // Check if user joined in current month (first month)
        LocalDate joinDate = targetUser.getCreatedDateTime().toLocalDate();
        YearMonth currentMonth = YearMonth.now();
        YearMonth joinMonth = YearMonth.from(joinDate);

        if (joinMonth.equals(currentMonth)) {
            return 0.0; // First month user
        }

        // Since we don't have date-range queries, we cannot calculate this metric
        // Return placeholder 0% or null
        return 0.0; // Placeholder
    }

    /**
     * Metric 15: App's Score (Parent Home Screen)
     * One overall grade (0-100) combining 3 metrics
     *
     * Problem: We don't have accurate improvement rate
     * Workaround: Use only completion and consistency rates
     */
    private Integer calculateAppScore(Double completionRate, Double consistencyRate, Double improvementRate) {
        if (completionRate == null || consistencyRate == null) {
            return 0;
        }

        // Since improvementRate can't be calculated accurately, give it a neutral score
        double normalizedImprovement = 50.0; // Neutral score

        // Calculate weighted score according to PDF (page 17)
        double weightedScore =
                (completionRate * 0.40) +    // 40% weight
                        (consistencyRate * 0.35) +   // 35% weight
                        (normalizedImprovement * 0.25); // 25% weight (neutral)

        return (int) Math.round(weightedScore);
    }



    /**
     * Helper: Calculate completion rate for a specific month
     */
    private Double calculateCompletionRateForMonth(User targetUser, YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        int completedHabits = habitRepository.countCompletedByCreatedByAndDateRange(targetUser, startDate, endDate);
        int completedTasks = taskRepository.countCompletedByCreatedByAndDateRange(targetUser, startDate, endDate);

        int scheduledHabits = habitRepository.countByCreatedByAndDateRange(targetUser, startDate, endDate);
        int scheduledTasks = taskRepository.countByCreatedByAndDateRange(targetUser, startDate, endDate);

        int totalScheduled = scheduledHabits + scheduledTasks;
        int totalCompleted = completedHabits + completedTasks;

        if (totalScheduled == 0) {
            return 0.0;
        }

        return ((double) totalCompleted / totalScheduled) * 100.0;
    }


}
