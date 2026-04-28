package com.superme.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class FamilyDetailsDTO {
    private Long familyId;
    private String familyName;
    private String familyCode;
    private String status; // ACTIVE
    private LocalDate createdOn;
    private FamilyMemberDetailsDTO creator;
    private List<FamilyMemberDetailsDTO> members;
}
