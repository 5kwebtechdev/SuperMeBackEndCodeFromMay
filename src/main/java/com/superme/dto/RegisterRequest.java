
package com.superme.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO for user registration. Only required fields are included.
 * Validation: At least one of email or phone must be provided.
 */
@Data
public class RegisterRequest {

    // Required fields only for registration
    private String name;
    private String gender;
    private String relationship;
    private LocalDate dateOfBirth;
    private String password;

    private String petName;

    private String avatarName;

    private String avatarImageName;

    @Pattern(regexp = "^[A-Za-z0-9]{8}$",
            message = "Family code must be exactly 8 alphanumeric characters")
    private String familyCode;

    private String familyName;

    private String referralCode;

    /**
     * Ensure at least one of email or phone is provided.
     */
    @AssertTrue(message = "Either email or phone must be provided")
    public boolean isEmailOrPhoneProvided() {
        return (email != null && !email.isEmpty()) || (phone != null && !phone.isEmpty());
    }

    // Email - single field (original)
    @Email(message = "Must be valid email format")
    private String email;

    // Phone split: country code and national number
    @Pattern(regexp = "^\\+[1-9]\\d{1,4}$",
            message = "Country code must be +91, +1, +44 etc.")
    private String countryCode;

    @Pattern(regexp = "^[0-9]{7,15}$",
            message = "Phone must be 7-15 digits")
    private String phone;
}