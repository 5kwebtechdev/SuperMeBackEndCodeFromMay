package com.superme.dto;

import com.superme.enums.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdResponse {
    private Long id;
    private String adName;
    private String adDescription;
    private String bannerImageUrl;
    private String bannerImageS3Key;
    private AdType adType;
    private TargetScreen targetScreen;
    private AgeGroup targetAudience;
    private Gender gender;
    private ClickActionType clickActionType;
    private String externalUrl;
    private String inAppScreen;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private Boolean noEndDate;
    private Priority priority;
    private AdStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}