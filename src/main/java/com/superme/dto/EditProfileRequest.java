package com.superme.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class EditProfileRequest {

    private String name;
    private String email;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;
    private String avatarName;
    private String avatarImageName;
}
