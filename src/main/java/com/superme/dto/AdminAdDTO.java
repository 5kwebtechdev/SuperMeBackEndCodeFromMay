package com.superme.dto;

import com.superme.enums.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminAdDTO {
    private Long id;
    private String adName;
    private String adDescription;
    private AdType adType;
    private String bannerImageS3Key;
    private String bannerImageUrl;
    private ClickActionType clickActionType;
    private TargetScreen targetScreen;
    private AgeGroup targetAudience;
    private Gender gender;
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