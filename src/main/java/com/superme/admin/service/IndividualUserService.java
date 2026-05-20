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

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

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

    /**
     * Generate a filtered Excel file of individual (SELF) users.
     *
     * Filter params (all optional):
     *   q           – keyword matched against name, email, phone
     *   gender      – e.g. "MALE" or "FEMALE"
     *   enabled     – true = active, false = inactive
     *   ageGroup    – enum name, e.g. "BELOW_11", "AGE_11_TO_13"
     *   minAge      – lower bound (inclusive)
     *   maxAge      – upper bound (inclusive)
     *   emailVerified – true / false
     *   createdFrom – earliest createdDate (LocalDate, inclusive)
     *   createdTo   – latest  createdDate (LocalDate, inclusive)
     */
    public byte[] generateIndividualUsersExcel(
            String q,
            String gender,
            Boolean enabled,
            String ageGroup,
            Integer minAge,
            Integer maxAge,
            Boolean emailVerified,
            LocalDate createdFrom,
            LocalDate createdTo) throws IOException {

        Stream<User> stream = userRepository.findByRelationship(Relationship.SELF).stream();

        if (q != null && !q.isBlank()) {
            String term = q.toLowerCase().trim();
            stream = stream.filter(u ->
                    (u.getName()  != null && u.getName().toLowerCase().contains(term))  ||
                    (u.getEmail() != null && u.getEmail().toLowerCase().contains(term)) ||
                    (u.getPhone() != null && u.getPhone().toLowerCase().contains(term)) ||
                    (u.getId()    != null && u.getId().toString().contains(term)));
        }
        if (gender != null && !gender.isBlank()) {
            stream = stream.filter(u -> gender.trim().equalsIgnoreCase(u.getGender()));
        }
        if (enabled != null) {
            stream = stream.filter(u -> u.isEnabled() == enabled);
        }
        if (ageGroup != null && !ageGroup.isBlank()) {
            stream = stream.filter(u -> u.getAgeGroup() != null &&
                    ageGroup.trim().equalsIgnoreCase(u.getAgeGroup().name()));
        }
        if (minAge != null) {
            stream = stream.filter(u -> u.getAge() >= minAge);
        }
        if (maxAge != null) {
            stream = stream.filter(u -> u.getAge() <= maxAge);
        }
        if (emailVerified != null) {
            stream = stream.filter(u -> u.isEmailVerified() == emailVerified);
        }
        if (createdFrom != null) {
            stream = stream.filter(u -> u.getCreatedDateTime() != null &&
                    !u.getCreatedDateTime().toLocalDate().isBefore(createdFrom));
        }
        if (createdTo != null) {
            stream = stream.filter(u -> u.getCreatedDateTime() != null &&
                    !u.getCreatedDateTime().toLocalDate().isAfter(createdTo));
        }

        List<User> users = stream.toList();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Individual Users");

            // Bold header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            String[] cols = {
                "ID", "Name", "Email", "Phone", "Gender",
                "Date of Birth", "Age", "Age Group",
                "Role", "Relationship", "Enabled", "Email Verified",
                "Coins", "Current Streak", "Highest Streak",
                "Avatar", "Pet", "Created Date", "Last Login", "Deleted"
            };

            Row header = sheet.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 22 * 256);
            }
            sheet.createFreezePane(0, 1); // freeze header row

            int rowIdx = 1;
            for (User u : users) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(u.getId() != null ? u.getId() : 0L);
                row.createCell(1).setCellValue(nvl(u.getName()));
                row.createCell(2).setCellValue(nvl(u.getEmail()));
                row.createCell(3).setCellValue(nvl(u.getPhone()));
                row.createCell(4).setCellValue(nvl(u.getGender()));
                row.createCell(5).setCellValue(u.getDateOfBirth() != null ? u.getDateOfBirth().toString() : "");
                row.createCell(6).setCellValue(u.getAge());
                row.createCell(7).setCellValue(u.getAgeGroup() != null ? u.getAgeGroup().getDisplayName() : "");
                row.createCell(8).setCellValue(u.getRole() != null ? u.getRole().name() : "");
                row.createCell(9).setCellValue(u.getRelationship() != null ? u.getRelationship().name().toLowerCase() : "");
                row.createCell(10).setCellValue(u.isEnabled() ? "Active" : "Inactive");
                row.createCell(11).setCellValue(u.isEmailVerified() ? "Yes" : "No");
                row.createCell(12).setCellValue(u.getCoins());
                row.createCell(13).setCellValue(u.getCurrentStreak());
                row.createCell(14).setCellValue(u.getHighestStreak());
                row.createCell(15).setCellValue(u.getAvatar() != null ? nvl(u.getAvatar().getAvatarImageName()) : "");
                row.createCell(16).setCellValue(u.getPet() != null ? nvl(u.getPet().getPetName()) : "");
                row.createCell(17).setCellValue(u.getCreatedDateTime() != null ? u.getCreatedDateTime().toString() : "");
                row.createCell(18).setCellValue(u.getLastLoginDate() != null ? u.getLastLoginDate().toString() : "");
                row.createCell(19).setCellValue(u.getDeleted() != null && u.getDeleted() ? "Yes" : "No");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private String nvl(String value) {
        return value != null ? value : "";
    }
}