package com.superme.dto;

import com.superme.enums.AgeGroup;
import com.superme.enums.Relationship;
import com.superme.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserSearchRequestDto {

    // Exact / single fields
    private Long id;
    private List<Long> ids;

    // String searches (contains / LIKE)
    private String name;
    private String email;
    private String phone;

    // Lists for categorical filters (as requested)
    private List<String> genders;                 // String list
    private List<Role> roles;                     // Role enum list
    private List<Relationship> relationships;     // Relationship enum list
    private List<AgeGroup> ageGroups;             // AgeGroup enum list

    // Date of birth (range)
    private LocalDate dobFrom;
    private LocalDate dobTo;

    // Age (range)
    private Integer minAge;
    private Integer maxAge;

    // Numeric ranges
    private Integer minCoins;
    private Integer maxCoins;

    private Integer minCurrentStreak;
    private Integer maxCurrentStreak;

    private Integer minHighestStreak;
    private Integer maxHighestStreak;

    // Booleans
    private Boolean isLoggedIn;
    private Boolean emailVerified;
    private Boolean enabled;

    // Last login and created date ranges
    private LocalDateTime lastLoginFrom;
    private LocalDateTime lastLoginTo;

    private LocalDateTime createdFrom;
    private LocalDateTime createdTo;

    // ManyToOne relation filters by id(s)
    private Long familyId;
    private List<Long> familyIds;

    private Long avatarId;
    private List<Long> avatarIds;

    private Long petId;
    private List<Long> petIds;

    // OneToMany existence flags (no field-level filtering)
    private Boolean hasCalendarEvents;
    private Boolean hasCoinHistory;
    private Boolean hasRewards;
    private Boolean hasBadges;
    private Boolean hasNotes;

    // Sorting / pagination could be added separately (page, size, sort)
}

