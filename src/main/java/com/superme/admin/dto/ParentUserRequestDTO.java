package com.superme.admin.dto;

import com.superme.enums.Relationship;
import com.superme.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ParentUserRequestDTO {
    
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
    private LocalDate dob;
    
    @NotNull(message = "relationship is required")
    private Relationship relationship;
    
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password; // Optional for edit
    
    // Family related fields
    private Boolean isFamily; // true = join family, false = create family
    private Boolean createFamily;
    private String familyName;
    private String familyCode;
    
    // Computed field
    private Integer age;
}