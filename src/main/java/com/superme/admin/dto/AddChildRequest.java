package com.superme.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Data;
import java.time.LocalDate;

@Data
public class AddChildRequest {

    // Accept both "fullName" and "name" from the frontend payload
    @JsonProperty("fullName")
    @JsonAlias("name")
    private String fullName;

    @Email(message = "Invalid email format")
    private String email;

    // Accept both "mobile" and "phone" from the frontend payload
    @JsonProperty("mobile")
    @JsonAlias("phone")
    private String mobile;

    private String gender;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @JsonProperty("dob")
    @JsonAlias({"dob", "dateOfBirth"})
    private LocalDate dob;

    @NotBlank(message = "Password is required")
    private String password;

    // familyId in the frontend payload holds the family code (e.g. "FAM9KQ2T")
    @NotBlank(message = "Family code is required")
    private String familyId;

    // Avatar image filename selected by the child (e.g. "avatars-3.png")
    private String avatarId;

    // Pet image filename selected by the child (e.g. "Image-Owl-Happy.png")
    private String pet;

    private String username;

    // "CHILD" string from frontend — ignored for actual Role (always USER); maps to Relationship.CHILD
    private String role;

    private Integer age;
}