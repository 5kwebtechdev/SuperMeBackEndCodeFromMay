package com.superme.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminDashboardKpiResponse {
    private long totalActiveUsers;
    private long learningAdded;
    private long notesCreatedToday;
}