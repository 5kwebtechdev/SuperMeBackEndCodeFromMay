package com.superme.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BadgeText {
    private CelebrationDTO celebrationCard;
    private List<BadgeResponseDTO> badges;
    private Integer totalBadgesEarned; // Total count of earned badges only
}