package com.superme.dto;

import com.superme.enums.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdRequest {

    @NotBlank(message = "Ad name is required")
    private String adName;

    private String adDescription;

    private String bannerImageUrl;
    private String bannerImageS3Key;

    @NotNull(message = "Ad type is required")
    private AdType adType;

    @NotNull(message = "Target screen is required")
    private TargetScreen targetScreen;

    @NotNull(message = "Target audience is required")
    private AgeGroup targetAudience;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotNull(message = "Click action type is required")
    private ClickActionType clickActionType;

    private String externalUrl;

    private String inAppScreen;

    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    private Boolean noEndDate;

    @NotNull(message = "Priority is required")
    private Priority priority;

    @NotNull(message = "Status is required")
    private AdStatus status;
}
