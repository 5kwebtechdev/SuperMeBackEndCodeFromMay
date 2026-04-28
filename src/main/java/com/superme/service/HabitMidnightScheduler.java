package com.superme.service;

import com.superme.model.HabitCompletion;
import com.superme.repository.HabitCompletionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HabitMidnightScheduler {

    private final HabitCompletionRepository habitCompletionRepository;

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    /**
     * Runs every day at 12:00 AM IST
     * Rule:
     * - Yesterday's ONGOING -> SKIPPED
     * - Today's PENDING stays PENDING (no touch)
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void moveOngoingToSkippedAfterMidnight() {

        LocalDate yesterday = LocalDate.now(ZONE).minusDays(1);

        List<HabitCompletion> ongoingHabits =
                habitCompletionRepository.findByCompletionDateAndStatus(
                        yesterday,
                        HabitCompletion.CompletionStatus.ONGOING
                );

        for (HabitCompletion hc : ongoingHabits) {
            hc.setStatus(HabitCompletion.CompletionStatus.SKIPPED);
            hc.setCompletionTime(null); // optional cleanup
            log.info("🌙 ONGOING → SKIPPED: {}", hc.getHabit().getTitle());
        }

        habitCompletionRepository.saveAll(ongoingHabits);
    }
}


