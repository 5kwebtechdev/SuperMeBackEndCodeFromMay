package com.superme.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserProfileUpdateDto {

    private String name;
    private String gender;
    private LocalDate dateOfBirth;
    private String phone;
    private String email;
    private Long avatarId; // send avatar id from frontend
    private String avatarName;
    private String avatarImageName; // optional
}