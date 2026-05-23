package com.superme.admin.dto;

import com.superme.enums.Role;
import com.superme.model.User;
import com.superme.enums.Relationship;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class ParentUserMapper {
    
    public User toEntity(ParentUserRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        
        return User.builder()
            .id(dto.getId())
            .name(dto.getName())
            .email(dto.getEmail())
            .phone(dto.getMobile())
            .gender(dto.getGender())
            .dateOfBirth(dto.getDob())
            .role(Role.USER)
            .relationship(Relationship.PARENT)
            .enabled(true)
            .emailVerified(false)
            .isLoggedIn(false)
            .createdDateTime(LocalDateTime.now())
            .build();
    }
    
    public void updateEntity(ParentUserRequestDTO dto, User entity) {
        if (dto.getName() != null) {
            entity.setName(dto.getName());
        }
        if (dto.getEmail() != null) {
            entity.setEmail(dto.getEmail());
        }
        if (dto.getMobile() != null) {
            entity.setPhone(dto.getMobile());
        }
        if (dto.getGender() != null) {
            entity.setGender(dto.getGender());
        }
        if (dto.getDob() != null) {
            entity.setDateOfBirth(dto.getDob());
        }
//        if (dto.getRole() != null) {
            entity.setRole(Role.USER);
//        }
    }
    
    public ParentUserResponseDTO toResponseDTO(User user) {
        if (user == null) {
            return null;
        }
        
        ParentUserResponseDTO dto = new ParentUserResponseDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setMobile(user.getPhone());
        dto.setGender(user.getGender());
        dto.setDateOfBirth(user.getDateOfBirth());
        dto.setAge(user.getAge());
        dto.setAgeGroup(user.getAgeGroup());
        dto.setRole(user.getRole());
        dto.setRelationship(user.getRelationship());
        dto.setEnabled(user.isEnabled());
        dto.setEmailVerified(user.isEmailVerified());
        dto.setLastLoginDate(user.getLastLoginDate());
        dto.setCreatedDateTime(user.getCreatedDateTime());
        
        // Set status based on enabled and last login
        if (!user.isEnabled()) {
            dto.setStatus("inactive");
        } else if (user.getLastLoginDate() != null && 
                   user.getLastLoginDate().isAfter(LocalDateTime.now().minusDays(30))) {
            dto.setStatus("active");
        } else {
            dto.setStatus("inactive");
        }
        
        // Set family information
        if (user.getFamily() != null) {
            dto.setFamilyId(user.getFamily().getId());
            dto.setFamilyName(user.getFamily().getFamilyName());
            dto.setFamilyCode(user.getFamily().getFamilyCode());
        }

        // isCoParent: PARENT who joined a family they did not create
        boolean isCoParent = user.getFamily() != null
                && user.getFamily().getCreatedBy() != null
                && !user.getFamily().getCreatedBy().equals(user.getId());
        dto.setIsCoParent(isCoParent);

        return dto;
    }
}