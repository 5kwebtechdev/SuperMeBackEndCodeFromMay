package com.superme.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSearchResponse {

    private List<TaskResponse> tasks; // List of all matched tasks

    private Long totalTasks;
    private Long totalTasksCompleted;
    private Double taskCompletionRate;

}
