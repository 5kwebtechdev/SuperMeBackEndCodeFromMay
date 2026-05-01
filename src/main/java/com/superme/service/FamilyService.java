package com.superme.service;

import com.superme.enums.Relationship;
import com.superme.exception.BusinessException;
import com.superme.exception.ResourceNotFoundException;
import com.superme.model.Family;
import com.superme.model.FamilyMember;
import com.superme.model.User;
import com.superme.repository.FamilyRepository;
import com.superme.repository.UserRepository;
import com.superme.util.FamilyCodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class FamilyService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FamilyRepository familyRepository;

    private final FamilyMemberService familyMemberService;

    public FamilyService(UserRepository userRepository, FamilyRepository familyRepository, FamilyMemberService familyMemberService) {
        this.userRepository = userRepository;
        this.familyRepository = familyRepository;
        this.familyMemberService = familyMemberService;
    }


    public Map<String, Object> getFamilyCodeByUserId(Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found for ID: " + userId));

            Family family = user.getFamily();
            if (family == null) {
                throw new BusinessException("Family not associated with user ID: " + userId);
            }

            // 7️⃣ Build response
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("familyId", family.getId());
            response.put("familyCode", family.getFamilyCode());
            response.put("familyName", family.getFamilyName());

            return response;

        } catch(Exception e) {
            // ✅ Wrap unexpected exceptions in a generic business-level exception
            throw new BusinessException("Error fetching family code: " + e.getMessage());
        }
    }

    public String generateJoinToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Family family = user.getFamily();
        if (family == null) {
            throw new BusinessException("Family is not associated with the user.");
        }

        return createToken(family.getFamilyCode());
    }

    private String createToken(String familyCode) {
        return familyCode + "-" + Instant.now().toEpochMilli();
    }

    @Transactional
    public void joinFamily(Long userId, String familyCode, String relationship) {

        if (familyCode == null || familyCode.isBlank()) {
            throw new BusinessException("familyCode is required");
        }

        Relationship rel;
        try {
            rel = Relationship.valueOf(relationship.toUpperCase());
        } catch (Exception e) {
            throw new BusinessException("Invalid relationship type");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Family family = familyRepository.findByFamilyCode(familyCode)
                .orElseThrow(() -> new ResourceNotFoundException("Family not found"));

        // 🔍 Count existing parents & children
        long parentCount = family.getMembers().stream()
                .filter(u -> u.getRelationship() == Relationship.PARENT)
                .count();

        long childCount = family.getMembers().stream()
                .filter(u -> u.getRelationship() == Relationship.CHILD)
                .count();

        // ❌ Validation
        if (rel == Relationship.PARENT && parentCount >= 2) {
            throw new BusinessException("Only two parents are allowed in a family");
        }

        if (rel == Relationship.CHILD && childCount >= 2) {
            throw new BusinessException("Only two children are allowed in a family");
        }

        // ❌ User already in family
        if (user.getFamily() != null) {
            throw new BusinessException("User already belongs to a family");
        }

        // ✅ Assign family
        user.setFamily(family);
        user.setRelationship(rel);
        userRepository.save(user);

        // ✅ Ensure FamilyMember entry exists
        familyMemberService.getByUser(user).orElseGet(() -> {
            FamilyMember fm = new FamilyMember();
            fm.setFamily(family);
            fm.setUser(user);
            fm.setDateOfBirth(user.getDateOfBirth());
            return familyMemberService.save(fm);
        });
    }


    private String extractFamilyCode(String token) {
        String[] parts = token.split("-");
        if (parts.length == 0 || parts[0].isBlank()) {
            throw new BusinessException("Invalid token");
        }
        return parts[0];
    }

    public Map<String, Object> generateFamilyCode(String familyName) {


        // 3️⃣ Generate full family name
        String fName = familyName.trim() + "'s Family";

        // 4️⃣ Generate unique family code (8 chars)
        String familyCode = generateUniqueFamilyCode(familyName);


        // 7️⃣ Build response
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("familyCode", familyCode);
        response.put("familyName", fName);
        Family family = new Family();
        family.setFamilyCode(familyCode);
        family.setFamilyName(fName);
//        family.setCreatedBy(userId);
        family.setCreatedAt(java.time.LocalDate.now());
        familyRepository.save(family);
        return response;
    }

    public String generateUniqueFamilyCode(String familyName) {
        String code;
        do {
            code = FamilyCodeGenerator.generateFamilyCode(familyName);
        } while (familyRepository.existsByFamilyCode(code)); // ensure uniqueness
        return code;
    }
    public boolean updateUserForFamily(String familyCode, Long userId) {
        Optional<Family> optionalFamily = familyRepository.findByFamilyCode(familyCode);

        if (optionalFamily.isPresent()) {
            Family family = optionalFamily.get();
            family.setCreatedBy(userId); // or whatever field you want to update
            familyRepository.save(family);
            return true;
        }

        return false;
    }

    public Family findByFamilyCode(String familyCode) {
        return familyRepository.findByFamilyCode(familyCode)
                .orElseThrow(() -> new ResourceNotFoundException("Family not found with code: " + familyCode));
    }
}
