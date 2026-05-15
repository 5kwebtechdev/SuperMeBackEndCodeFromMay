package com.superme.admin.service;

//import com.superme.dto.request.ParentUserRequestDTO;
//import com.superme.dto.response.ParentUserResponseDTO;
import com.superme.admin.Exception.DuplicateResourceException;
import com.superme.admin.dto.ParentUserMapper;
import com.superme.admin.dto.ParentUserRequestDTO;
import com.superme.admin.dto.ParentUserResponseDTO;
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
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
}