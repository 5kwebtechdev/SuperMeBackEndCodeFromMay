package com.superme.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdStatistics {
    private long totalAds;
    private long activeAds;
    private long inactiveAds;
}