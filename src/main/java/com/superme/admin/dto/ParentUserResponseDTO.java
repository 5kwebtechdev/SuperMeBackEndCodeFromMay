package com.superme.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.superme.enums.AgeGroup;
import com.superme.enums.Relationship;
import com.superme.enums.Role;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ParentUserResponseDTO {
    private Long id;
    private String name;
    private String email;
    private String mobile;
    private String gender;
    @JsonProperty("dob")
    private LocalDate dateOfBirth;
    private Integer age;
    private AgeGroup ageGroup;
    private Role role;
    private Relationship relationship;
    private Boolean enabled;
    private Boolean emailVerified;
    private LocalDateTime lastLoginDate;
    private LocalDateTime createdDateTime;
    
    // Family information
    private Long familyId;
    private String familyName;
    private String familyCode;
    
    // Status
    private String status; // active/inactive based on enabled and lastLoginDate
}