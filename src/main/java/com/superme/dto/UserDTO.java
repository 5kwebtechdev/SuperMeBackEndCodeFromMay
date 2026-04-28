package com.superme.dto;

import com.superme.enums.Relationship;
import com.superme.enums.Role;
import com.superme.model.Avatar;
import com.superme.model.Pet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;
    private Relationship relationship;
    private Role role;
    private String referralCode;
    private FamilyDTO family;
    private List<FamilyMemberDTO> familyMembers;
    private Avatar avatar;
    private Pet pet;
    private LocalDate lastDailyBonusDate;
}
