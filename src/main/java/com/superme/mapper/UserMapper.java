package com.superme.mapper;

import com.superme.dto.FamilyDTO;
import com.superme.dto.FamilyMemberDTO;
import com.superme.dto.UserDTO;
import com.superme.model.Family;
import com.superme.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserDTO toDto(User user) {
        if (user == null) return null;

        Family family = user.getFamily();
        FamilyDTO familyDTO = null;
        List<FamilyMemberDTO> memberDTOs = new ArrayList<>();

        if (family != null) {
            familyDTO = new FamilyDTO(family.getId(), family.getFamilyCode(), family.getFamilyName());

            if (family.getMembers() != null) {
                memberDTOs = family.getMembers().stream()
                        .map(member -> new FamilyMemberDTO(
                                member.getId(),
                                member.getId(),
                                member.getName(),
                                member.getRelationship(),
                                member.getDateOfBirth(),
                                member.getAvatar() != null ? member.getAvatar().getAvatarImageName() : null
                        ))
                        .collect(Collectors.toList());
            }
        }

        return new UserDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getGender(),
                user.getDateOfBirth(),
                user.getRelationship(),
                user.getRole(),
                user.getReferralCode(),
                familyDTO,
                memberDTOs,
                user.getAvatar(),
                user.getPet(),
                user.getLastDailyBonusDate()
        );
    }
}

