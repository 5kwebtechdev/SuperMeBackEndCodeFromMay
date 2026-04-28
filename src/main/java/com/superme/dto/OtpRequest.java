package com.superme.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpRequest {

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid mobile number. Must be 10 digits starting with 6–9.")
    private String phone;

    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Either mobile or email must be provided")
    private String identifierType; // "MOBILE" or "EMAIL"
}
