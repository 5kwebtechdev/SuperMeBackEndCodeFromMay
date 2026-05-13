package com.superme.admin.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Data
public class IndividualUserRegisterDTO {
    
    @Size(min = 3, max = 32, message = "Username must be between 3 and 32 characters")
    private String username;
    
    @NotNull(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
    
    @NotNull(message = "Full name is required")
    @Size(min = 2, message = "Full name must be at least 2 characters")
    private String fullName;
    
    @NotNull(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotNull(message = "Date of birth is required")
    private LocalDate dob;
    
    @NotNull(message = "Gender is required")
    private String gender;
    
    @NotNull(message = "Country is required")
    private String country;
    
    @NotNull(message = "Phone is required")
    private String phone;
    
    private String avatar;
    
    private String pet;
    
    private String mobile;
    
    private String name;
    
    private Integer age;
}