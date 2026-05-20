package com.superme.admin.service;


import com.superme.admin.Exception.DuplicateResourceException;
import com.superme.admin.dto.AddChildRequest;
import com.superme.admin.dto.ChildUserMapper;
import com.superme.admin.dto.ChildUserRequestDTO;
import com.superme.admin.dto.ChildUserResponseDTO;
import com.superme.admin.dto.UpdateChildRequest;
import com.superme.exception.ResourceNotFoundException;
import com.superme.model.Avatar;
import com.superme.model.Family;
import com.superme.model.FamilyMember;
import com.superme.model.Pet;
import com.superme.model.User;
import com.superme.model.UserPassword;
import com.superme.enums.Relationship;
import com.superme.enums.Role;
import com.superme.repository.AvatarRepository;
import com.superme.repository.ChildUserRepository;
import com.superme.repository.FamilyMemberRepository;
import com.superme.repository.FamilyRepository;
import com.superme.repository.PetRepository;
import com.superme.repository.UserPasswordRepository;
import com.superme.service.AvatarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChildUserService {

    private final ChildUserRepository childUserRepository;
    private final FamilyRepository familyRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final AvatarRepository avatarRepository;
    private final AvatarService avatarService;
    private final PetRepository petRepository;
    private final UserPasswordRepository userPasswordRepository;
    private final ChildUserMapper childUserMapper;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * Add a child user via admin panel (POST /admin/users/addChild).
     * Validates family code, enforces max-2-children limit, creates Avatar,
     * looks up Pet, persists User + FamilyMember + password.
     */
    @Transactional
    public ChildUserResponseDTO addChild(AddChildRequest request) {
        String name = (request.getFullName() != null && !request.getFullName().isBlank())
                ? request.getFullName().trim()
                : null;
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Child name is required");
        }

        String phone = (request.getMobile() != null && !request.getMobile().isBlank())
                ? request.getMobile().trim()
                : null;

        // Uniqueness checks
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (childUserRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("Child", "email", request.getEmail());
            }
        }
        if (phone != null) {
            if (childUserRepository.existsByPhone(phone)) {
                throw new DuplicateResourceException("Child", "phone number", phone);
            }
        }

        // Verify family exists by the code sent as "familyId"
        Family family = familyRepository.findByFamilyCode(request.getFamilyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Family not found with code: " + request.getFamilyId()));

        // Enforce max 2 children per family
        long childCount = childUserRepository.countByFamilyIdAndRelationship(
                family.getId(), Relationship.CHILD);
        if (childCount >= 2) {
            throw new IllegalStateException(
                    "Family already has the maximum of 2 children. Cannot add more.");
        }

        // Create a dedicated Avatar for this child
        Avatar avatar = null;
        if (request.getAvatarId() != null && !request.getAvatarId().isBlank()) {
            String uniqueAvatarName = avatarService.generateUniqueAvatarName(name);
            avatar = Avatar.builder()
                    .avatarName(uniqueAvatarName)
                    .gender(request.getGender())
                    .avatarImageName(request.getAvatarId())
                    .url(request.getAvatarId())
                    .renamedByUser(false)
                    .build();
            avatar = avatarRepository.save(avatar);
            log.info("Created avatar '{}' for child '{}'", uniqueAvatarName, name);
        }

        // Resolve pet: try petName, then url, then create a new catalog entry so pet_id is always set
        Pet pet = null;
        if (request.getPet() != null && !request.getPet().isBlank()) {
            String petValue = request.getPet().trim();
            pet = petRepository.findByPetName(petValue)
                    .or(() -> petRepository.findByUrl(petValue))
                    .orElseGet(() -> {
                        log.info("Pet '{}' not found in catalog — creating new entry.", petValue);
                        return petRepository.save(Pet.builder()
                                .petName(petValue)
                                .url(petValue)
                                .build());
                    });
        }

        // Build and save the child User
        User child = User.builder()
                .name(name)
                .email(request.getEmail())
                .phone(phone)
                .gender(request.getGender())
                .dateOfBirth(request.getDob())
                .relationship(Relationship.CHILD)
                .role(Role.USER)
                .enabled(true)
                .emailVerified(false)
                .isLoggedIn(false)
                .createdDateTime(LocalDateTime.now())
                .coins(0)
                .currentStreak(0)
                .highestStreak(0)
                .family(family)
                .avatar(avatar)
                .pet(pet)
                .build();

        User savedChild = childUserRepository.save(child);
        log.info("Child user saved with ID: {}", savedChild.getId());

        // Create FamilyMember record linking child to family
        FamilyMember familyMember = FamilyMember.builder()
                .family(family)
                .user(savedChild)
                .dateOfBirth(request.getDob())
                .build();
        familyMemberRepository.save(familyMember);

        // Save BCrypt-hashed password
        UserPassword userPassword = UserPassword.builder()
                .user(savedChild)
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        userPasswordRepository.save(userPassword);
        log.info("Child user '{}' created successfully with family code '{}'",
                name, request.getFamilyId());

        return childUserMapper.toResponseDTO(savedChild);
    }

    /**
     * Update an existing child user via admin panel (PUT /admin/users/updateChild/{id}).
     * Only fields present in the request are updated. Password is optional.
     */
    @Transactional
    public ChildUserResponseDTO updateChild(Long id, UpdateChildRequest request) {
        User child = childUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Child user not found with ID: " + id));

        if (child.getRelationship() != Relationship.CHILD) {
            throw new IllegalArgumentException("User with ID " + id + " is not a child user");
        }

        // Email uniqueness (excluding self)
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && !request.getEmail().equals(child.getEmail())) {
            if (childUserRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("Child", "email", request.getEmail());
            }
        }

        // Phone uniqueness (excluding self)
        String phone = (request.getMobile() != null && !request.getMobile().isBlank())
                ? request.getMobile().trim() : null;
        if (phone != null && !phone.equals(child.getPhone())) {
            if (childUserRepository.existsByPhone(phone)) {
                throw new DuplicateResourceException("Child", "phone number", phone);
            }
        }

        // Update basic fields when provided
        if (request.getName() != null && !request.getName().isBlank()) {
            child.setName(request.getName().trim());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            child.setEmail(request.getEmail());
        }
        if (phone != null) {
            child.setPhone(phone);
        }
        if (request.getGender() != null && !request.getGender().isBlank()) {
            child.setGender(request.getGender());
        }
        if (request.getDob() != null) {
            child.setDateOfBirth(request.getDob());
        }

        // Resolve family: numeric ID ("8") or family code ("FAM9KQ2T")
        if (request.getFamilyId() != null && !request.getFamilyId().isBlank()) {
            child.setFamily(resolveFamilyByIdOrCode(request.getFamilyId()));
        }

        // Avatar: update existing record in-place, or create a new one
        if (request.getAvatarId() != null && !request.getAvatarId().isBlank()) {
            if (child.getAvatar() != null) {
                Avatar existing = child.getAvatar();
                existing.setAvatarImageName(request.getAvatarId());
                existing.setUrl(request.getAvatarId());
                avatarRepository.save(existing);
            } else {
                String uniqueName = avatarService.generateUniqueAvatarName(child.getName());
                Avatar newAvatar = Avatar.builder()
                        .avatarName(uniqueName)
                        .gender(child.getGender())
                        .avatarImageName(request.getAvatarId())
                        .url(request.getAvatarId())
                        .renamedByUser(false)
                        .build();
                child.setAvatar(avatarRepository.save(newAvatar));
            }
        }

        // Pet: find by name, then by url, or create a new catalog entry
        if (request.getPet() != null && !request.getPet().isBlank()) {
            String petValue = request.getPet().trim();
            Pet pet = petRepository.findByPetName(petValue)
                    .or(() -> petRepository.findByUrl(petValue))
                    .orElseGet(() -> {
                        log.info("Pet '{}' not in catalog — creating new entry.", petValue);
                        return petRepository.save(Pet.builder()
                                .petName(petValue)
                                .url(petValue)
                                .build());
                    });
            child.setPet(pet);
        }

        // Password is optional — only update when explicitly provided
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            userPasswordRepository.findByUserId(id).ifPresentOrElse(
                    existing -> {
                        existing.setPassword(passwordEncoder.encode(request.getPassword()));
                        userPasswordRepository.save(existing);
                        log.info("Password updated for child ID: {}", id);
                    },
                    () -> {
                        userPasswordRepository.save(UserPassword.builder()
                                .user(child)
                                .password(passwordEncoder.encode(request.getPassword()))
                                .build());
                        log.info("New password created for child ID: {}", id);
                    }
            );
        }

        User updated = childUserRepository.save(child);
        log.info("Child user {} updated successfully", id);
        return childUserMapper.toResponseDTO(updated);
    }

    /** Resolves a Family from a string that is either a numeric ID or a family code. */
    private Family resolveFamilyByIdOrCode(String familyId) {
        try {
            Long numericId = Long.parseLong(familyId);
            return familyRepository.findById(numericId)
                    .orElseThrow(() -> new ResourceNotFoundException("Family not found with ID: " + familyId));
        } catch (NumberFormatException e) {
            return familyRepository.findByFamilyCode(familyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Family not found with code: " + familyId));
        }
    }

    /**
     * Create a new child user
     */
    @Transactional
    public ChildUserResponseDTO createChildUser(ChildUserRequestDTO request) {
        log.info("Creating new child user with email: {}", request.getEmail());
        
        // Check for duplicate email
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (childUserRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("Child", "email", request.getEmail());
            }
        }
        
        // Check for duplicate phone
        if (request.getMobile() != null && !request.getMobile().isEmpty()) {
            if (childUserRepository.existsByPhone(request.getMobile())) {
                throw new DuplicateResourceException("Child", "phone number", request.getMobile());
            }
        }
        
        // Create user entity
        User child = childUserMapper.toEntity(request);
        child.setRelationship(Relationship.CHILD);
        child.setRole(Role.USER);
        
        // Handle family association
        if (request.getFamilyCode() != null && !request.getFamilyCode().isEmpty()) {
            Family family = familyRepository.findByFamilyCode(request.getFamilyCode())
                .orElseThrow(() -> new ResourceNotFoundException("Family not found with code: " + request.getFamilyCode()));
            child.setFamily(family);
        } else if (request.getFamilyId() != null) {
            Family family = familyRepository.findByFamilyCode(request.getFamilyId())
                .orElseThrow(() -> new ResourceNotFoundException("Family not found with Code: " + request.getFamilyId()));
            child.setFamily(family);
        }
        
        // Save child user
        User savedChild = childUserRepository.save(child);
        log.info("Child user saved with ID: {}", savedChild.getId());
        
        // Create and save password
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            UserPassword userPassword = UserPassword.builder()
                .user(savedChild)
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
            userPasswordRepository.save(userPassword);
            log.info("Password saved for child user ID: {}", savedChild.getId());
        }
        
        log.info("Child user created successfully with ID: {}", savedChild.getId());
        return childUserMapper.toResponseDTO(savedChild);
    }
    
    /**
     * Update an existing child user
     */
    @Transactional
    public ChildUserResponseDTO updateChildUser(Long id, ChildUserRequestDTO request) {
        log.info("Updating child user with ID: {}", id);
        
        User child = childUserRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Child user not found with ID: " + id));
        
        // Check for duplicate email (excluding current user)
        if (request.getEmail() != null && !request.getEmail().isEmpty() && 
            !request.getEmail().equals(child.getEmail())) {
            if (childUserRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("Child", "email", request.getEmail());
            }
        }
        
        // Check for duplicate phone (excluding current user)
        if (request.getMobile() != null && !request.getMobile().isEmpty() && 
            !request.getMobile().equals(child.getPhone())) {
            if (childUserRepository.existsByPhone(request.getMobile())) {
                throw new DuplicateResourceException("Child", "phone number", request.getMobile());
            }
        }
        
        // Update child fields
        childUserMapper.updateEntity(request, child);
        
        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            UserPassword existingPassword = userPasswordRepository.findByUserId(id)
                .orElse(null);
            
            if (existingPassword != null) {
                existingPassword.setPassword(passwordEncoder.encode(request.getPassword()));
                userPasswordRepository.save(existingPassword);
                log.info("Password updated for child ID: {}", id);
            } else {
                UserPassword newPassword = UserPassword.builder()
                    .user(child)
                    .password(passwordEncoder.encode(request.getPassword()))
                    .build();
                userPasswordRepository.save(newPassword);
                log.info("New password created for child ID: {}", id);
            }
        }
        
        // Update family if changed
        if (request.getFamilyCode() != null && !request.getFamilyCode().isEmpty()) {
            Family family = familyRepository.findByFamilyCode(request.getFamilyCode())
                .orElseThrow(() -> new ResourceNotFoundException("Family not found with code: " + request.getFamilyCode()));
            child.setFamily(family);
        }
        
        User updatedChild = childUserRepository.save(child);
        log.info("Child user updated successfully with ID: {}", updatedChild.getId());
        
        return childUserMapper.toResponseDTO(updatedChild);
    }
    
    /**
     * Get child user by ID
     */
    @Transactional(readOnly = true)
    public ChildUserResponseDTO getChildUserById(Long id) {
        log.info("Fetching child user with ID: {}", id);
        
        User child = childUserRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Child user not found with ID: " + id));
        
        // Verify it's a child user
        if (child.getRelationship() != Relationship.CHILD) {
            throw new ResourceNotFoundException("User with ID " + id + " is not a child");
        }
        
        return childUserMapper.toResponseDTO(child);
    }
    
    /**
     * Get all child users with pagination
     */
    @Transactional(readOnly = true)
    public Page<ChildUserResponseDTO> getAllChildUsers(Pageable pageable, String search) {
        log.info("Fetching all child users - page: {}, size: {}, search: {}", 
                 pageable.getPageNumber(), pageable.getPageSize(), search);
        
        Page<User> childrenPage;
        
        if (search != null && !search.isEmpty()) {
            childrenPage = childUserRepository.searchByRelationshipAndKeyword(Relationship.CHILD, search, pageable);
        } else {
            childrenPage = childUserRepository.findByRelationship(Relationship.CHILD, pageable);
        }
        
        return childrenPage.map(childUserMapper::toResponseDTO);
    }
    
    /**
     * Get all child users (no pagination)
     */
    @Transactional(readOnly = true)
    public List<ChildUserResponseDTO> getAllChildUsersList() {
        log.info("Fetching all child users (list)");
        
        List<User> children = childUserRepository.findByRelationship(Relationship.CHILD);
        
        return children.stream()
            .map(childUserMapper::toResponseDTO)
            .toList();
    }
    
    /**
     * Get children by family ID
     */
    @Transactional(readOnly = true)
    public List<ChildUserResponseDTO> getChildrenByFamilyId(Long familyId) {
        log.info("Fetching children for family ID: {}", familyId);
        
        List<User> children = childUserRepository.findChildrenByFamilyId(familyId, Relationship.CHILD);
        
        return children.stream()
            .map(childUserMapper::toResponseDTO)
            .toList();
    }
    
    /**
     * Delete child user (soft delete)
     */
    @Transactional
    public void deleteChildUser(Long id) {
        log.info("Soft deleting child user with ID: {}", id);
        
        User child = childUserRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Child user not found with ID: " + id));
        
        child.setEnabled(false);
        child.setDeleted(true);
        childUserRepository.save(child);
        
        log.info("Child user soft deleted successfully with ID: {}", id);
    }
    
    /**
     * Enable/Disable child user
     */
    @Transactional
    public ChildUserResponseDTO setChildStatus(Long id, boolean enabled) {
        log.info("Setting status for child ID: {} to enabled: {}", id, enabled);
        
        User child = childUserRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Child user not found with ID: " + id));
        
        child.setEnabled(enabled);
        User savedChild = childUserRepository.save(child);
        
        return childUserMapper.toResponseDTO(savedChild);
    }
    
    /**
     * Get children count by family
     */
    @Transactional(readOnly = true)
    public long getChildrenCountByFamily(Long familyId) {
        return childUserRepository.countByFamilyIdAndRelationship(familyId, Relationship.CHILD);
    }
}