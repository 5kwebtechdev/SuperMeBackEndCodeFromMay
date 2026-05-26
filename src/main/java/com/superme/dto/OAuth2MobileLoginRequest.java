package com.superme.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2MobileLoginRequest {

    @NotBlank(message = "Provider is required")
    private String provider;       // google, github, facebook

    @NotBlank(message = "Provider ID is required")
    private String providerId;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    private String name;
    private String avatarUrl;

    // New fields for new-user setup
    private String gender;          // "male" / "female" / "other"
    private LocalDate dob;          // date of birth
    private String avatarImageName; // e.g. "Image-Owl-Happy.png"
    private String petName;         // e.g. "Dragon", "Llama"

    // Family fields — only used when role = "PARENT"
    private String role;            // "PARENT" triggers family logic
    private String familyName;      // create new family with this name (if no familyCode)
    private String familyCode;      // join existing family by this code
}