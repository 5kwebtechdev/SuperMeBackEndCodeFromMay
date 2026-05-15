package com.superme.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.superme.enums.AgeGroup;
import com.superme.enums.Relationship;
import com.superme.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseUserResponseDTO {
    
    // Basic Information
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String gender;
    
    @JsonProperty("dob")
    private LocalDate dateOfBirth;
    
    private Integer age;
    private AgeGroup ageGroup;
    private Relationship relationship;
    private Role role;
    
    // Account Status
    private Boolean enabled;
    private Boolean emailVerified;
    private String status; // active, inactive, pending
    
    // Timestamps
    private LocalDateTime lastLoginDate;
    private LocalDateTime createdDateTime;
    
    // Family Information
    private Long familyId;
    private String familyName;
    private String familyCode;
    
    // Avatar & Pet (Optional)
    private String avatarId;
    private String avatarImageName;
    private String avatarName;
    private String petImageName;

    // Username (for child users)
    private String username;
    
    // Parent/Guardian info (for child users)
    private Long parentId;
    private String parentName;
    
    // Gamification (Optional)
    private Integer coins;
    private Integer currentStreak;
    private Integer highestStreak;
    
    // Helper method to set status based on enabled and last login
    public void calculateStatus() {
        if (this.enabled == null) {
            this.status = "unknown";
        } else if (!this.enabled) {
            this.status = "inactive";
        } else if (this.lastLoginDate != null && 
                   this.lastLoginDate.isAfter(LocalDateTime.now().minusDays(30))) {
            this.status = "active";
        } else {
            this.status = "inactive";
        }
    }
}