package com.superme.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BadgeRealtimeEvent {

    private Long badgeId;
    private String title;
    private String badgeName;
    private Integer level;
    private String message;
    private String icon;
}

