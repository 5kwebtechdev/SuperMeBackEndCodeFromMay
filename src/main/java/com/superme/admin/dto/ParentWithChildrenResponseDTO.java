package com.superme.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ParentWithChildrenResponseDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String gender;
    private Integer age;
    @JsonProperty("dob")
    private LocalDate dateOfBirth;
    private Boolean enabled;
    private String relationship;
    private Long familyId;
    private String familyName;
    private String familyCode;
    private Integer linkedKids;
    private LocalDateTime createdDateTime;
    private LocalDateTime lastLoginDate;
    private List<ChildSummaryDTO> children;
}