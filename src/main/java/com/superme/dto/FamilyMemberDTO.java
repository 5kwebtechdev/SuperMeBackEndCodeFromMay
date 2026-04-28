package com.superme.dto;


import com.superme.enums.Relationship;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FamilyMemberDTO {
    private Long id;
    private Long userId;
    private String name;
    private Relationship relationship;
    private LocalDate dateOfBirth;
    private String avatarImageName;
}
