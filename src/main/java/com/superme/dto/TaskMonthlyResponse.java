package com.superme.dto;

import com.superme.model.TaskCompletion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskMonthlyResponse {

    private Long taskId;
    private String taskName;
    private List<DayStatus> monthlyCompletion;

    @Data
    @AllArgsConstructor
    public static class DayStatus {
        private LocalDate date;
        private TaskCompletion.CompletionStatus status;
    }
}

