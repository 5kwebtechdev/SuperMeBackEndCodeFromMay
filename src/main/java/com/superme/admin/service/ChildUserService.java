package com.superme.admin.service;


import com.superme.admin.Exception.DuplicateResourceException;
import com.superme.admin.dto.ChildUserMapper;
import com.superme.admin.dto.ChildUserRequestDTO;
import com.superme.admin.dto.ChildUserResponseDTO;
//import com.superme.admin.repository.ChildUserRepository;
 import com.superme.exception.ResourceNotFoundException;
import com.superme.model.Family;
import com.superme.model.User;
import com.superme.model.UserPassword;
import com.superme.enums.Relationship;
import com.superme.enums.Role;
import com.superme.repository.ChildUserRepository;
import com.superme.repository.FamilyRepository;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class ChildUserService {
    
    private final ChildUserRepository childUserRepository;
    private final FamilyRepository familyRepository;
    private final UserPasswordRepository userPasswordRepository;
    private final ChildUserMapper childUserMapper;
    private final PasswordEncoder passwordEncoder;
    
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