package com.superme.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserFamilyDetailsResponse {

    private String mode; // FAMILY | SELF

    // FAMILY MODE
    private FamilyDetailsDTO family;

    // SELF MODE
    private SelfModeDTO selfMode;
}
