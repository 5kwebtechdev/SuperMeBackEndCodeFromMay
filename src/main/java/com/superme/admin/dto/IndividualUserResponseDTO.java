package com.superme.admin.dto;

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
public class IndividualUserResponseDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;
    private Integer age;
    private String ageGroup;
    private String relationship;
    private String role;
    private Boolean enabled;
    private Boolean emailVerified;
    private LocalDateTime createdDateTime;
    private Integer coins;
    private Integer currentStreak;
    private Integer highestStreak;
    private String avatarImageName;
    private String petName;
    private Boolean deleted;
    
    // For edit form - password is NOT included for security
    private String fullName;  // Same as name, for form compatibility
    
    public String getFullName() {
        return this.name;
    }
    
    public String getDob() {
        return this.dateOfBirth != null ? this.dateOfBirth.toString() : null;
    }
}