package com.superme.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SelfModeDTO {
    private String title;
    private String message;
    private boolean canContinueWithNewRole;
}
