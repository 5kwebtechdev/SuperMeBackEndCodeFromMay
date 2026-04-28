package com.superme.service;

import com.superme.dto.AdminCreateUserDTO;
import com.superme.exception.BusinessException;
import com.superme.model.Avatar;
import com.superme.model.Pet;
import com.superme.model.User;
import com.superme.repository.AvatarRepository;
import com.superme.repository.PetRepository;
import com.superme.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AdminUserCrudService {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private AvatarRepository avatarRepository;

  @Autowired
  private PetRepository petRepository;

  public User createUser(AdminCreateUserDTO dto) {
      User user = new User();
      // Removed username field
      user.setName(dto.getName());
      user.setGender(dto.getGender());
      user.setEmail(dto.getEmail());
      user.setPhone(dto.getPhone());

      // Set dateOfBirth
      if (dto.getDateOfBirth() != null) {
        user.setDateOfBirth(dto.getDateOfBirth());
      }

      if (dto.getAvatarName() != null) {
        Avatar avatar = avatarRepository.findByAvatarName(dto.getAvatarName())
                .orElseThrow(() -> new RuntimeException("Avatar not found"));
        user.setAvatar(avatar);
      }

      if (dto.getPetName() != null) {
        Pet pet = petRepository.findByPetName(dto.getPetName())
                .orElseThrow(() -> new RuntimeException("Pet not found"));
        user.setPet(pet);
      }
      // Set relationship from DTO (enum)
      if (dto.getRelationship() != null) {
        user.setRelationship(dto.getRelationship());
      } else {
        throw new IllegalArgumentException("Relationship is required (CHILD, PARENT, SELF).");
      }
   return userRepository.save(user);
  }

  public Optional<User> getUserById(Long id) {
    return userRepository.findById(id);
  }

  public User updateUser(Long id, User updatedUser) {
    return userRepository.findById(id)
        .map(user -> {
          // Removed username field
          user.setName(updatedUser.getName());
          user.setGender(updatedUser.getGender());
          user.setEmail(updatedUser.getEmail());
          user.setPhone(updatedUser.getPhone());
          user.setLastLoginDate(updatedUser.getLastLoginDate());
          user.setRelationship(updatedUser.getRelationship());
          // Set other fields as needed
          return userRepository.save(user);
        })
        .orElseThrow(() -> new RuntimeException("User not found"));
  }

    public void deleteUser(Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("User not found with id: " + id));

            user.setEnabled(false);
            userRepository.save(user);
        } catch (BusinessException ex) {
            throw ex; // handled by GlobalExceptionHandler
        } catch (Exception ex) {
            throw new BusinessException("An unexpected error occurred while deleting the user.");
        }
    }

}
