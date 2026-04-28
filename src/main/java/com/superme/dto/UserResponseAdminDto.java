package com.superme.dto;

import com.superme.enums.AgeGroup;
import com.superme.enums.Relationship;
import com.superme.enums.Role;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseAdminDto {

    private Long id;
    private String name;
    private String email;
    private String phone;

    private Role role;
    private Boolean isLoggedIn;
    private Boolean emailVerified;
    private Boolean enabled;

    private String gender;
    private LocalDate dateOfBirth;

    private Integer age;
    private AgeGroup ageGroup;

    private Integer coins;
    private Integer currentStreak;
    private Integer highestStreak;

    private LocalDateTime lastLoginDate;
    private LocalDateTime createdDateTime;

    // RELATIONSHIPS (only returning IDs to keep DTO lightweight)
    private Long familyId;
    private Long avatarId;
    private Long petId;

    private Relationship relationship;
}
