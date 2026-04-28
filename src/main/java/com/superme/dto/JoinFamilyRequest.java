package com.superme.dto;

import com.superme.enums.Relationship;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JoinFamilyRequest {

    @NotNull(message = "Family code must not be null")
    private String familyCode;

    @NotNull(message = "Relationship must not be null")
    private String relationship;
}
