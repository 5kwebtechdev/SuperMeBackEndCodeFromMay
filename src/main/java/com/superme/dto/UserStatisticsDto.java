package com.superme.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatisticsDto {

    private Long totalUsers;
    private Long totalActiveUsers;
    private Long totalInactiveUsers;
    private Long newRegistrationsThisMonth;
    private Long newRegistrationsToday;
    private Long newRegistrationsThisWeek;
    private Double activityRate; // Can be pre-calculated in service

}

