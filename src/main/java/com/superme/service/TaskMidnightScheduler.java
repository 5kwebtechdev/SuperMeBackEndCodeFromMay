package com.superme.service;

import com.superme.model.TaskCompletion;
import com.superme.repository.TaskCompletionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskMidnightScheduler {

    private final TaskCompletionRepository taskCompletionRepository;

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    /**
     * Runs every day at 12:00 AM IST
     *
     * Rule:
     * - Yesterday's ONGOING -> SKIPPED
     * - Today's PENDING stays PENDING
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void moveOngoingTasksToSkippedAfterMidnight() {

        LocalDate yesterday = LocalDate.now(ZONE).minusDays(1);

        List<TaskCompletion> ongoingTasks =
                taskCompletionRepository.findByCompletionDateAndStatus(
                        yesterday,
                        TaskCompletion.CompletionStatus.ONGOING
                );

        for (TaskCompletion tc : ongoingTasks) {
            tc.setStatus(TaskCompletion.CompletionStatus.SKIPPED);
            tc.setCompletionTime(null); // optional cleanup
            log.info("🌙 TASK ONGOING → SKIPPED: {}", tc.getTask().getTitle());
        }

        taskCompletionRepository.saveAll(ongoingTasks);
    }
}
