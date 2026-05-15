package com.superme.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.superme.enums.AgeGroup;
import com.superme.enums.Relationship;
import com.superme.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ChildUserRequestDTO {
    
    private Long id;
    
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;
    
    @Email(message = "Invalid email format")
    private String email;
    
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
    private String mobile;
    
    @NotBlank(message = "Gender is required")
    private String gender;
    
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @JsonProperty("dob")
    @JsonAlias({"dateOfBirth", "dob"})
    private LocalDate dateOfBirth;
    
    private Role role;
    private Relationship relationship;
    
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    
    // Family related fields
    private String familyCode;
    private String familyId;
    
    // Avatar and Pet
    private String avatarId;
    private String pet;
    
    // Username (optional, can be auto-generated)
    private String username;
    
    // Computed fields
    private Integer age;
    private AgeGroup ageGroup;
    
    // Parent/Guardian info (optional)
    private Long parentId;
    private String parentName;
}