package com.superme.admin.service;

//import com.superme.dto.request.ParentUserRequestDTO;
//import com.superme.dto.response.ParentUserResponseDTO;
import com.superme.admin.Exception.DuplicateResourceException;
import com.superme.admin.dto.ChildSummaryDTO;
import com.superme.admin.dto.ParentUserMapper;
import com.superme.admin.dto.ParentUserRequestDTO;
import com.superme.admin.dto.ParentUserResponseDTO;
import com.superme.admin.dto.ParentWithChildrenResponseDTO;
import com.superme.exception.ResourceNotFoundException;
//import com.superme.exception.DuplicateResourceException;
//import com.superme.mapper.ParentUserMapper;
import com.superme.model.Family;
import com.superme.model.User;
import com.superme.model.UserPassword;
import com.superme.enums.Relationship;
import com.superme.enums.Role;
import com.superme.repository.FamilyRepository;
import com.superme.repository.UserRepository;
import com.superme.repository.UserPasswordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParentUserService {

    private final UserRepository userRepository;
    private final FamilyRepository familyRepository;
    private final UserPasswordRepository userPasswordRepository;
    private final ParentUserMapper parentUserMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create a new parent user
     */
    @Transactional
    public ParentUserResponseDTO createParentUser(ParentUserRequestDTO request) {
        log.info("Creating new parent user with email: {}", request.getEmail());

        // Check for duplicate email
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("User", "email", request.getEmail());
            }
        }

        // Check for duplicate phone
        if (request.getMobile() != null && !request.getMobile().isEmpty()) {
            if (userRepository.existsByPhone(request.getMobile())) {
                throw new DuplicateResourceException("User", "phone number", request.getMobile());
            }
        }

        // Create user entity
        User user = parentUserMapper.toEntity(request);
        user.setRelationship(Relationship.PARENT);
        user.setRole(Role.USER);

        // Save user first
        User savedUser = userRepository.save(user);
        log.info("User saved with ID: {}", savedUser.getId());

        // Create and save password separately
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            UserPassword userPassword = UserPassword.builder()
                    .user(savedUser)
                    .password(passwordEncoder.encode(request.getPassword()))
                    .build();
            userPasswordRepository.save(userPassword);
            log.info("Password saved for user ID: {}", savedUser.getId());
        }

        // Handle family creation or joining
        handleFamilyAssociation(request, savedUser);

        // Update user with family
        User updatedUser = userRepository.save(savedUser);
        log.info("Parent user created successfully with ID: {}", updatedUser.getId());

        return parentUserMapper.toResponseDTO(updatedUser);
    }

    /**
     * Update an existing parent user
     */
    @Transactional
    public ParentUserResponseDTO updateParentUser(Long id, ParentUserRequestDTO request) {
        log.info("Updating parent user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parent user not found with ID: " + id));

        // Check for duplicate email (excluding current user)
        if (request.getEmail() != null && !request.getEmail().isEmpty() &&
                !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("User", "email", request.getEmail());
            }
        }

        // Check for duplicate phone (excluding current user)
        if (request.getMobile() != null && !request.getMobile().isEmpty() &&
                !request.getMobile().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.getMobile())) {
                throw new DuplicateResourceException("User", "phone number", request.getMobile());
            }
        }

        // Update user fields
        parentUserMapper.updateEntity(request, user);

        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            UserPassword existingPassword = userPasswordRepository.findByUserId(id)
                    .orElse(null);

            if (existingPassword != null) {
                // Update existing password
                existingPassword.setPassword(passwordEncoder.encode(request.getPassword()));
                userPasswordRepository.save(existingPassword);
                log.info("Password updated for user ID: {}", id);
            } else {
                // Create new password entry if doesn't exist
                UserPassword newPassword = UserPassword.builder()
                        .user(user)
                        .password(passwordEncoder.encode(request.getPassword()))
                        .build();
                userPasswordRepository.save(newPassword);
                log.info("New password created for user ID: {}", id);
            }
        }

        // Handle family update if needed
        if (request.getIsFamily() != null && request.getCreateFamily() != null) {
            handleFamilyAssociation(request, user);
        }

        User updatedUser = userRepository.save(user);
        log.info("Parent user updated successfully with ID: {}", updatedUser.getId());

        return parentUserMapper.toResponseDTO(updatedUser);
    }

    /**
     * Get parent user by ID
     */
    @Transactional(readOnly = true)
    public ParentUserResponseDTO getParentUserById(Long id) {
        log.info("Fetching parent user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parent user not found with ID: " + id));

        // Verify it's a parent user
        if (user.getRelationship() != Relationship.PARENT) {
            throw new ResourceNotFoundException("User with ID " + id + " is not a parent");
        }

        return parentUserMapper.toResponseDTO(user);
    }



    /**
     * Get all parent users (no pagination)
     */
    @Transactional(readOnly = true)
    public List<ParentUserResponseDTO> getAllParentUsersList() {
        log.info("Fetching all parent users (list)");

        List<User> users = userRepository.findByRole(Role.USER);

        return users.stream()
                .filter(user -> user.getRelationship() == Relationship.PARENT)
                .map(parentUserMapper::toResponseDTO)
                .toList();
    }

    /**
     * Delete parent user (soft delete)
     */
    @Transactional
    public void deleteParentUser(Long id) {
        log.info("Soft deleting parent user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parent user not found with ID: " + id));

        user.setEnabled(false);
        user.setDeleted(true);
        userRepository.save(user);

        log.info("Parent user soft deleted successfully with ID: {}", id);
    }

    /**
     * Enable/Disable parent user
     */
    @Transactional
    public ParentUserResponseDTO setUserStatus(Long id, boolean enabled) {
        log.info("Setting status for user ID: {} to enabled: {}", id, enabled);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parent user not found with ID: " + id));

        user.setEnabled(enabled);
        User savedUser = userRepository.save(user);

        return parentUserMapper.toResponseDTO(savedUser);
    }

    /**
     * Check if family code exists
     */
    public boolean checkFamilyCodeExists(String familyCode) {
        log.info("Checking if family code exists: {}", familyCode);
        return familyRepository.findByFamilyCode(familyCode).isPresent();
    }

    /**
     * Get parent user with their children list
     */
    @Transactional(readOnly = true)
    public ParentWithChildrenResponseDTO getParentWithChildren(Long id) {
        log.info("Fetching parent with children for ID: {}", id);

        User parent = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parent user not found with ID: " + id));

        if (parent.getRelationship() != Relationship.PARENT) {
            throw new ResourceNotFoundException("User with ID " + id + " is not a parent");
        }

        List<User> children = parent.getFamily() != null
                ? userRepository.findByFamilyAndRelationship(parent.getFamily(), Relationship.CHILD)
                : List.of();

        List<ChildSummaryDTO> childDTOs = children.stream()
                .map(child -> ChildSummaryDTO.builder()
                        .id(child.getId())
                        .name(child.getName())
                        .age(child.getAge())
                        .lastLoginDate(child.getLastLoginDate())
                        .build())
                .toList();

        Family family = parent.getFamily();

        boolean isCoParent = family != null
                && family.getCreatedBy() != null
                && !family.getCreatedBy().equals(parent.getId());

        return ParentWithChildrenResponseDTO.builder()
                .id(parent.getId())
                .name(parent.getName())
                .email(parent.getEmail())
                .phone(parent.getPhone())
                .gender(parent.getGender())
                .age(parent.getAge())
                .dateOfBirth(parent.getDateOfBirth())
                .enabled(parent.isEnabled())
                .relationship(parent.getRelationship().name().toLowerCase())
                .familyId(family != null ? family.getId() : null)
                .familyName(family != null ? family.getFamilyName() : null)
                .familyCode(family != null ? family.getFamilyCode() : null)
                .linkedKids(childDTOs.size())
                .createdDateTime(parent.getCreatedDateTime())
                .lastLoginDate(parent.getLastLoginDate())
                .isCoParent(isCoParent)
                .children(childDTOs)
                .build();
    }

    /**
     * Handle family creation or joining
     */
    private void handleFamilyAssociation(ParentUserRequestDTO request, User user) {
        if (Boolean.TRUE.equals(request.getCreateFamily())) {
            // Create new family
            Family family = Family.builder()
                    .familyCode(generateFamilyCode())
                    .familyName(request.getFamilyName() != null ?
                            request.getFamilyName() :
                            Family.generateFamilyName(user.getName()))
                    .createdBy(user.getId())
                    .build();

            Family savedFamily = familyRepository.save(family);
            user.setFamily(savedFamily);
            log.info("Created new family: {} with code: {}", savedFamily.getFamilyName(), savedFamily.getFamilyCode());

        } else if (request.getFamilyCode() != null && !request.getFamilyCode().isEmpty()) {
            // Join existing family
            Family family = familyRepository.findByFamilyCode(request.getFamilyCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Family not found with code: " + request.getFamilyCode()));

            user.setFamily(family);
            log.info("User joined existing family: {} with code: {}", family.getFamilyName(), family.getFamilyCode());
        }
    }

    /**
     * Generate unique family code
     */
    private String generateFamilyCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (familyRepository.findByFamilyCode(code).isPresent());
        return code;
    }

    /**
     * Generate a filtered Excel file of parent users.
     *
     * Filter params (all optional):
     *   q             – keyword matched against name, email, phone
     *   gender        – MALE | FEMALE
     *   enabled       – true = active, false = inactive
     *   ageGroup      – enum name e.g. AGE_18_PLUS
     *   minAge        – lower bound (inclusive)
     *   maxAge        – upper bound (inclusive)
     *   emailVerified – true | false
     *   familyCode    – filter by a specific family code
     *   hasChildren   – true = only parents who have ≥1 child, false = no children
     *   createdFrom   – yyyy-MM-dd (inclusive)
     *   createdTo     – yyyy-MM-dd (inclusive)
     */
    public byte[] generateParentUsersExcel(
            String q,
            String gender,
            Boolean enabled,
            String ageGroup,
            Integer minAge,
            Integer maxAge,
            Boolean emailVerified,
            String familyCode,
            Boolean hasChildren,
            LocalDate createdFrom,
            LocalDate createdTo) throws IOException {

        Stream<User> stream = userRepository.findByRelationship(Relationship.PARENT).stream();

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
        if (familyCode != null && !familyCode.isBlank()) {
            stream = stream.filter(u -> u.getFamily() != null &&
                    familyCode.trim().equalsIgnoreCase(u.getFamily().getFamilyCode()));
        }
        if (createdFrom != null) {
            stream = stream.filter(u -> u.getCreatedDateTime() != null &&
                    !u.getCreatedDateTime().toLocalDate().isBefore(createdFrom));
        }
        if (createdTo != null) {
            stream = stream.filter(u -> u.getCreatedDateTime() != null &&
                    !u.getCreatedDateTime().toLocalDate().isAfter(createdTo));
        }

        // hasChildren filter requires child count — resolve after stream materializes
        List<User> users = stream.toList();

        if (hasChildren != null) {
            users = users.stream()
                    .filter(u -> {
                        int childCount = u.getFamily() != null
                                ? userRepository.findByFamilyAndRelationship(u.getFamily(), Relationship.CHILD).size()
                                : 0;
                        return hasChildren ? childCount > 0 : childCount == 0;
                    })
                    .toList();
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Parent Users");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            String[] cols = {
                "ID", "Name", "Email", "Phone", "Gender",
                "Date of Birth", "Age", "Age Group",
                "Role", "Relationship", "Enabled", "Email Verified",
                "Coins", "Current Streak", "Highest Streak",
                "Avatar", "Pet",
                "Family ID", "Family Name", "Family Code", "Family Created By",
                "Children Count",
                "Created Date", "Last Login", "Deleted"
            };

            Row header = sheet.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 22 * 256);
            }
            sheet.createFreezePane(0, 1);

            int rowIdx = 1;
            for (User u : users) {
                Family fam = u.getFamily();
                int childCount = fam != null
                        ? userRepository.findByFamilyAndRelationship(fam, Relationship.CHILD).size()
                        : 0;

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
                row.createCell(17).setCellValue(fam != null ? fam.getId() : 0L);
                row.createCell(18).setCellValue(fam != null ? nvl(fam.getFamilyName()) : "");
                row.createCell(19).setCellValue(fam != null ? nvl(fam.getFamilyCode()) : "");
                row.createCell(20).setCellValue(fam != null && fam.getCreatedBy() != null ? fam.getCreatedBy() : 0L);
                row.createCell(21).setCellValue(childCount);
                row.createCell(22).setCellValue(u.getCreatedDateTime() != null ? u.getCreatedDateTime().toString() : "");
                row.createCell(23).setCellValue(u.getLastLoginDate() != null ? u.getLastLoginDate().toString() : "");
                row.createCell(24).setCellValue(u.getDeleted() != null && u.getDeleted() ? "Yes" : "No");
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