package com.superme.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.superme.enums.AgeGroup;
import com.superme.enums.Relationship;
import com.superme.enums.Role;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ChildUserResponseDTO {
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
    
    // Avatar and Pet
    private String avatarId;
    private String avatarUrl;
    private String pet;
    private String petUrl;
    
    // Username
    private String username;
    
    // Parent/Guardian info
    private Long parentId;
    private String parentName;
    
    // Gamification fields
    private Integer coins;
    private Integer currentStreak;
    private Integer highestStreak;
    
    // Status
    private String status;
}