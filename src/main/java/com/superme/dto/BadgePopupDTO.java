package com.superme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@Builder
public class BadgePopupDTO {

    private Long badgeId;
    private String badgeName;
    private int level;
    private String title;           // "Level 1 - Badge Unlocked!"
    private String subtitle;        // "Coin Collector"
    private String message;         // "You collected 500 coins!"
    private String icon;            // badge icon
    private boolean showPopup;      // 👈 frontend trigger

}

