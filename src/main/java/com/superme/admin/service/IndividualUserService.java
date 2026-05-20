package com.superme.admin.service;

import com.superme.admin.dto.IndividualUserRegisterDTO;
import com.superme.admin.dto.IndividualUserResponseDTO;
import com.superme.enums.AgeGroup;
import com.superme.enums.Relationship;
import com.superme.enums.Role;
import com.superme.model.*;
import com.superme.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IndividualUserService {

    private final UserRepository userRepository;
    private final AvatarRepository avatarRepository;
    private final PetRepository petRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserPasswordRepository userPasswordRepository;

    @Transactional
    public User registerIndividualUser(IndividualUserRegisterDTO dto) {

        // 1️⃣ Validate email uniqueness
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered: " + dto.getEmail());
        }

        // 2️⃣ Validate phone uniqueness
        if (userRepository.findByPhone(dto.getPhone()).isPresent()) {
            throw new RuntimeException("Phone number already registered: " + dto.getPhone());
        }

        // 3️⃣ Calculate age from DOB
        int age = Period.between(dto.getDob(), LocalDate.now()).getYears();
        if (age < 5) {
            throw new RuntimeException("User must be at least 5 years old");
        }

        // 4️⃣ Find or create Avatar (by avatarImageName from frontend)
        Avatar avatar = getOrCreateAvatar(dto.getAvatar(), dto.getGender());

        // 5️⃣ Find or create Pet (by petName from frontend)
        Pet pet = getOrCreatePet(dto.getPet());

        // 6️⃣ Create User entity
        User user = User.builder()
                .name(dto.getFullName() != null ? dto.getFullName() : dto.getName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .gender(dto.getGender())
                .dateOfBirth(dto.getDob())
                .relationship(Relationship.SELF)
                .role(Role.USER)
                .enabled(true)
                .isLoggedIn(false)
                .emailVerified(false)
                .emailVerificationToken(null)  // ✅ Set to null
                .createdDateTime(LocalDateTime.now())
                .referralCode(null)             // ✅ Set to null
                .coins(0)
                .currentStreak(0)
                .highestStreak(0)
                .age(age)
                .ageGroup(AgeGroup.fromAge(age))
                .family(null)
                .avatar(avatar)
                .pet(pet)
                .dailyActivityStreak(0)
                .learningActivityStreak(0)
                .totalActiveDays(0)
                .learningStreak(0)
                .activityStreak(0)
                .deleted(false)
                .build();

        // 7️⃣ Save user first
        User savedUser = userRepository.save(user);

        // 8️⃣ Save password in UserPassword table
        UserPassword userPassword = UserPassword.builder()
                .user(savedUser)
                .password(passwordEncoder.encode(dto.getPassword()))
                .build();

        userPasswordRepository.save(userPassword);

        return savedUser;
    }

    /**
     * Always creates a fresh Avatar row for this user.
     * avatarImageName records the selected image (e.g. "avatars-3.png").
     * Each user gets their own Avatar entity so the OneToOne constraint is satisfied.
     */
    private Avatar getOrCreateAvatar(String avatarImageName, String gender) {
        if (avatarImageName == null || avatarImageName.isEmpty()) {
            avatarImageName = gender != null && gender.equalsIgnoreCase("MALE")
                    ? "avatars-1.png"
                    : "avatar_girl_6.png";
        }
        Avatar newAvatar = Avatar.builder()
                .avatarImageName(avatarImageName)
                .avatarName(generateUniqueAvatarName())
                .gender(gender)
                .renamedByUser(false)
                .url(avatarImageName)
                .build();
        return avatarRepository.save(newAvatar);
    }

    /**
     * Find or create Pet based on the petName from frontend
     * Save petName as it is coming, set URL as null
     */
    private Pet getOrCreatePet(String petName) {
        if (petName == null || petName.isEmpty()) {
            petName = "Panda"; // Default pet
        }

        // Try to find existing pet by petName
        String finalPetName = petName;
        return petRepository.findByPetName(petName)
                .orElseGet(() -> {
                    // Create new pet if not exists
                    Pet newPet = Pet.builder()
                            .petName(finalPetName)           // ✅ Save petName as it is coming
                            .gender("MALE")             // Default gender
                            .url(null)                  // ✅ Set URL as null
                            .build();
                    return petRepository.save(newPet);
                });
    }

    /**
     * Generate unique avatar name (for internal identification)
     */
    private String generateUniqueAvatarName() {
        return "avatar_" +  UUID.randomUUID().toString().substring(0, 4);
    }














    /**
     * Update an existing user from the admin panel (PUT /admin/users/{id}).
     * All fields are optional except the path ID. Password is only updated when provided.
     */
    @Transactional
    public IndividualUserResponseDTO updateIndividualUser(Long id, IndividualUserRegisterDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        // Name
        String name = (dto.getFullName() != null && !dto.getFullName().isBlank())
                ? dto.getFullName().trim()
                : (dto.getName() != null && !dto.getName().isBlank() ? dto.getName().trim() : null);
        if (name != null) {
            user.setName(name);
        }

        // Email — uniqueness check excluding self
        if (dto.getEmail() != null && !dto.getEmail().isBlank()
                && !dto.getEmail().equals(user.getEmail())) {
            if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
                throw new RuntimeException("Email already registered: " + dto.getEmail());
            }
            user.setEmail(dto.getEmail());
        }

        // Phone — prefer "phone", fall back to "mobile"; uniqueness check excluding self
        String phone = (dto.getPhone() != null && !dto.getPhone().isBlank()) ? dto.getPhone()
                : (dto.getMobile() != null && !dto.getMobile().isBlank() ? dto.getMobile() : null);
        if (phone != null && !phone.equals(user.getPhone())) {
            if (userRepository.findByPhone(phone).isPresent()) {
                throw new RuntimeException("Phone number already registered: " + phone);
            }
            user.setPhone(phone);
        }

        if (dto.getGender() != null && !dto.getGender().isBlank()) {
            user.setGender(dto.getGender());
        }

        // DOB — recalculate age and ageGroup
        if (dto.getDob() != null) {
            user.setDateOfBirth(dto.getDob());
            int age = Period.between(dto.getDob(), LocalDate.now()).getYears();
            user.setAge(age);
            user.setAgeGroup(AgeGroup.fromAge(age));
        }

        // Avatar — update existing record in-place to keep OneToOne intact
        if (dto.getAvatar() != null && !dto.getAvatar().isBlank()) {
            if (user.getAvatar() != null) {
                Avatar existing = user.getAvatar();
                existing.setAvatarImageName(dto.getAvatar());
                existing.setUrl(dto.getAvatar());
                avatarRepository.save(existing);
            } else {
                Avatar newAvatar = Avatar.builder()
                        .avatarImageName(dto.getAvatar())
                        .avatarName(generateUniqueAvatarName())
                        .gender(user.getGender())
                        .renamedByUser(false)
                        .url(dto.getAvatar())
                        .build();
                user.setAvatar(avatarRepository.save(newAvatar));
            }
        }

        // Pet — find by name, then url, or create a new catalog entry
        if (dto.getPet() != null && !dto.getPet().isBlank()) {
            Pet pet = petRepository.findByPetName(dto.getPet())
                    .or(() -> petRepository.findByUrl(dto.getPet()))
                    .orElseGet(() -> petRepository.save(Pet.builder()
                            .petName(dto.getPet())
                            .url(dto.getPet())
                            .build()));
            user.setPet(pet);
        }

        // Password — optional; only update when explicitly provided
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            userPasswordRepository.findByUserId(id).ifPresentOrElse(
                    existing -> {
                        existing.setPassword(passwordEncoder.encode(dto.getPassword()));
                        userPasswordRepository.save(existing);
                    },
                    () -> userPasswordRepository.save(UserPassword.builder()
                            .user(user)
                            .password(passwordEncoder.encode(dto.getPassword()))
                            .build())
            );
        }

        return toResponseDTO(userRepository.save(user));
    }

    public IndividualUserResponseDTO getIndividualUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Individual user not found with id: " + id));

        if (user.getRole() != Role.USER) {
            throw new RuntimeException("User with id " + id + " is not an individual user");
        }

        return toResponseDTO(user);
    }

    private IndividualUserResponseDTO toResponseDTO(User user) {
        return IndividualUserResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .fullName(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .age(user.getAge())
                .ageGroup(user.getAgeGroup() != null ? user.getAgeGroup().getDisplayName() : null)
                .relationship(user.getRelationship() != null ? user.getRelationship().name() : null)
                .role(user.getRole() != null ? user.getRole().name() : null)
                .enabled(user.isEnabled())
                .emailVerified(user.isEmailVerified())
                .createdDateTime(user.getCreatedDateTime())
                .coins(user.getCoins())
                .currentStreak(user.getCurrentStreak())
                .highestStreak(user.getHighestStreak())
                .avatarImageName(user.getAvatar() != null ? user.getAvatar().getAvatarImageName() : null)
                .petName(user.getPet() != null ? user.getPet().getPetName() : null)
                .deleted(user.getDeleted())
                .build();
    }
}