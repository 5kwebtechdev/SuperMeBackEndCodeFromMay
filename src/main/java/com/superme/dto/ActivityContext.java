package com.superme.dto;

import com.superme.enums.ActivityType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ActivityContext {

    private Long userId;
    private ActivityType activityType;
    private int coinsEarned;
    private String feature;
    private LocalDate activityDate;
}
