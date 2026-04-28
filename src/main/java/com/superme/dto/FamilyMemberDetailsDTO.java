package com.superme.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FamilyMemberDetailsDTO {
    private Long userId;
    private String name;
    private String avatarName;
    private String avatarImageName;
    private String relationship;
    private int age;
    private boolean isYou;
}
