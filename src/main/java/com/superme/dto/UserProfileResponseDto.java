package com.superme.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UserProfileResponseDto {

    private String name;
    private String gender;
    private LocalDate dateOfBirth;
    private String phone;
    private String email;
    private String avatarName;
    private Long avatarId;
    private String avatarImageName; // ✅ NEW
}