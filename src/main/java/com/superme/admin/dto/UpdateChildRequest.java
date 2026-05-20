package com.superme.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdateChildRequest {

    private String name;

    @Email(message = "Invalid email format")
    private String email;

    // Accept both "mobile" and "phone"
    @JsonProperty("mobile")
    @JsonAlias("phone")
    private String mobile;

    private String gender;

    @Past(message = "Date of birth must be in the past")
    @JsonProperty("dob")
    @JsonAlias({"dob", "dateOfBirth"})
    private LocalDate dob;

    // Can be a numeric family ID ("8") or a family code ("FAM9KQ2T")
    private String familyId;

    // Avatar image filename (e.g. "avatars-3.png")
    private String avatarId;

    // Pet image filename (e.g. "Image-Panda-Happy.png")
    private String pet;

    private String relationship;

    // Optional — password is only updated when this field is non-blank
    private String password;

    private Integer age;
}