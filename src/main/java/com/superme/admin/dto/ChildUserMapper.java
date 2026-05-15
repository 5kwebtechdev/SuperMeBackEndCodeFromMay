package com.superme.admin.dto;


import com.superme.model.Avatar;
import com.superme.model.Family;
import com.superme.model.Pet;
import com.superme.model.User;
import com.superme.enums.Relationship;
import com.superme.enums.Role;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class ChildUserMapper {
    
    public User toEntity(ChildUserRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        
        User user = User.builder()
            .id(dto.getId())
            .name(dto.getName())
            .email(dto.getEmail())
            .phone(dto.getMobile())
            .gender(dto.getGender())
            .dateOfBirth(dto.getDateOfBirth())
            .role(dto.getRole() != null ? dto.getRole() : Role.USER)
            .relationship(dto.getRelationship() != null ? dto.getRelationship() : Relationship.CHILD)
            .enabled(true)
            .emailVerified(false)
            .isLoggedIn(false)
            .createdDateTime(LocalDateTime.now())
            .coins(0)
            .currentStreak(0)
            .highestStreak(0)
            .build();
        
        // Set username (if provided, otherwise generate from name)
        if (dto.getUsername() != null && !dto.getUsername().isEmpty()) {
            user.setName(dto.getUsername());
        }
        
        return user;
    }
    
    public void updateEntity(ChildUserRequestDTO dto, User entity) {
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
        if (dto.getDateOfBirth() != null) {
            entity.setDateOfBirth(dto.getDateOfBirth());
        }
        if (dto.getRole() != null) {
            entity.setRole(dto.getRole());
        }
        if (dto.getRelationship() != null) {
            entity.setRelationship(dto.getRelationship());
        }
        if (dto.getUsername() != null) {
            entity.setName(dto.getUsername());
        }
    }
    
    public ChildUserResponseDTO toResponseDTO(User user) {
        if (user == null) {
            return null;
        }
        
        ChildUserResponseDTO dto = new ChildUserResponseDTO();
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
        dto.setCoins(user.getCoins());
        dto.setCurrentStreak(user.getCurrentStreak());
        dto.setHighestStreak(user.getHighestStreak());
        
        // Set username (using name field)
        dto.setUsername(user.getName());
        
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
        
        // Set avatar information
        if (user.getAvatar() != null) {
            dto.setAvatarId(String.valueOf(user.getAvatar().getId()));
            // Set avatar URL if needed
        }
        
        // Set pet information
        if (user.getPet() != null) {
            dto.setPet(user.getPet().getPetName());
        }
        
        return dto;
    }
}