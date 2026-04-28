package com.superme.dto;

import com.superme.enums.AgeGroup;
import com.superme.enums.Relationship;
import com.superme.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class UserResponseDto {

    private Long id;
    private String name;
    private String email;
    private String phone;

    private Role role;
    private Relationship relationship;

    private boolean isLoggedIn;
    private boolean emailVerified;
    private boolean enabled;

    private String gender;
    private LocalDate dateOfBirth;

    private int age;
    private AgeGroup ageGroup;

    private int coins;
    private int currentStreak;
    private int highestStreak;

    private LocalDateTime lastLoginDate;
    private LocalDateTime createdDateTime;

    private String avatarName;
    private String avatarImageName;
    private String petName;
    private String familyCode;
    private String familyName;
    private String referralCode;
}

