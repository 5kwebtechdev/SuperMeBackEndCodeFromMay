package com.superme.dto;

import com.superme.enums.*;
import lombok.Data;

@Data
public class AdFilterRequest {
    private TargetScreen targetScreen;
    private AgeGroup targetAudience;
    private Gender gender;
    private AdStatus status;
    private Priority priority;
}
