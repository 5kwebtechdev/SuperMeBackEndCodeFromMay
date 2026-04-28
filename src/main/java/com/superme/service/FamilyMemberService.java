package com.superme.service;

import com.superme.dto.AddFamilyMember;
import com.superme.dto.AddFamilyMemberRequest;
import com.superme.enums.Relationship;
import com.superme.enums.Role;
import com.superme.exception.ResourceNotFoundException;
import com.superme.model.Family;
import com.superme.model.FamilyMember;
import com.superme.model.User;
import com.superme.repository.FamilyMemberRepository;
import com.superme.repository.FamilyRepository;
import com.superme.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FamilyMemberService {

    @Autowired
    private FamilyMemberRepository familyMemberRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FamilyRepository familyRepository;

    public List<FamilyMember> getFamilyMembers(Family family) {
        return familyMemberRepository.findByFamily(family);
    }

    // Updated: count children using User's relationship
    public long countChildren(Family family) {
        return familyMemberRepository.countChildren(family);
    }

    // Updated: count parents using User's relationship
    public long countParent(Family family) {
        return familyMemberRepository.countParents(family);
    }

    // Partner relationship is not used anymore, return 0 for compatibility
    public long countPartner(Family family) {
        return 0L;
    }

    public FamilyMember save(FamilyMember member) {
        return familyMemberRepository.save(member);
    }

    public Optional<FamilyMember> getByUser(User user) {
        return familyMemberRepository.findByUser(user);
    }

    /**
     * Remove a user from their family and set relationship to SELF.
     */
    public void removeUserFromFamilyAndSetSelf(User user) {
        user.setFamily(null);
        user.setRelationship(Relationship.SELF);
        userRepository.save(user);
        familyMemberRepository.findByUser(user).ifPresent(familyMemberRepository::delete);
    }

    /**
     * Parent removes all family members except themselves, and sets their relationship to SELF.
     * Returns the number of members removed.
     */
    public int removeAllFamilyMembersAndConvertParentToSelf(User parent) {
        if (parent.getFamily() == null) return 0;
        List<FamilyMember> members = familyMemberRepository.findByFamily(parent.getFamily());
        int removed = 0;
        for (FamilyMember fm : members) {
            User memberUser = fm.getUser();
            if (!memberUser.getId().equals(parent.getId())) {
                memberUser.setFamily(null);
                memberUser.setRelationship(Relationship.SELF);
                userRepository.save(memberUser);
                familyMemberRepository.delete(fm);
                removed++;
            }
        }
        // Now convert parent to self and remove family
        parent.setFamily(null);
        parent.setRelationship(Relationship.SELF);
        userRepository.save(parent);
        familyMemberRepository.findByUser(parent).ifPresent(familyMemberRepository::delete);
        familyRepository.delete(parent.getFamily());
        return removed;
    }

//    public void addFamilyMember(Long mainUserId, Map<String, Object> req) {
//        User mainUser = userRepository.findById(mainUserId)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
//
//        Family family = mainUser.getFamily();
//        if (family == null) {
//            throw new ResourceNotFoundException("Family is not associated with the user");
//        }
//
//        String name = (String) req.get("name");
//        String gender = (String) req.get("gender");
//        LocalDate dob = (LocalDate) req.get("dateOfBirth");
//
//        if (name == null || name.isBlank() || gender == null || gender.isBlank() || dob == null) {
//            throw new IllegalArgumentException("All fields (name, gender, dateOfBirth) are required");
//        }
//
////        Long dobMillis = Long.valueOf(dobObj.toString());
////        LocalDate dobLocalDate = Instant.ofEpochMilli(dobMillis).atZone(ZoneId.systemDefault()).toLocalDate();
//
//        // Create and save new family member user
//        User member = new User();
//        member.setName(name);
//        member.setGender(gender);
//        member.setDateOfBirth(dob);
//        member.setRole(Role.USER);
//        member.setFamily(family);
//        member.setEnabled(true);
//        userRepository.save(member);
//
//        // Link in FamilyMember
//        FamilyMember fm = new FamilyMember();
//        fm.setFamily(family);
//        fm.setUser(member);
//        fm.setDateOfBirth(dob);
//        familyMemberRepository.save(fm);
//    }



public void addFamilyMember(AddFamilyMember req) {

    User mainUser = userRepository.findById(req.getMainUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    Family family = mainUser.getFamily();
    if (family == null) {
        throw new ResourceNotFoundException("Family is not associated with the user");
    }

    // ✅ Convert DOB
    LocalDate dob;
    try {
        dob = LocalDate.parse(req.getDateOfBirth());
    } catch (Exception e) {
        throw new IllegalArgumentException("Invalid date format. Use yyyy-MM-dd");
    }

    // ✅ Validate
    if (req.getName() == null || req.getName().isBlank()
            || req.getGender() == null || req.getGender().isBlank()
            || req.getRelationship() == null || req.getRelationship().isBlank()) {
        throw new IllegalArgumentException("All fields are required");
    }

    // ✅ Convert relationship String → Enum
    Relationship relationship;
    try {
        relationship = Relationship.valueOf(req.getRelationship().toUpperCase());
    } catch (Exception e) {
        throw new IllegalArgumentException("Invalid relationship. Use CHILD or PARENT");
    }

    // 🚫 Optional: Prevent SELF
    if (relationship == Relationship.SELF) {
        throw new IllegalArgumentException("Cannot add member as SELF");
    }

    // ✅ Create user
    User member = new User();
    member.setName(req.getName());
    member.setGender(req.getGender());
    member.setDateOfBirth(dob);
    member.setRole(Role.USER);
    member.setFamily(family);
    member.setRelationship(relationship); // ✅ FIX HERE
    member.setEnabled(true);

    userRepository.save(member);

    // ✅ FamilyMember mapping
    FamilyMember fm = new FamilyMember();
    fm.setFamily(family);
    fm.setUser(member);
    fm.setDateOfBirth(dob);

    familyMemberRepository.save(fm);
}

    public Map<String, Object> switchToFamilyMember(Long currentUserId, Long memberId) {
        // Fetch current user
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if requester is part of a family
        familyMemberRepository.findByUser(currentUser)
                .orElseThrow(() -> new AccessDeniedException("Not a family member"));

        // Validate relationship
        String relationship = currentUser.getRelationship() != null
                ? currentUser.getRelationship().name().toLowerCase()
                : "";

        if (!relationship.equals("parent") && !relationship.equals("self")) {
            throw new AccessDeniedException("Only parents can monitor/switch accounts");
        }

        // Fetch target member
        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Family member not found"));

        // 🔐 Replace with your JWT generation logic
        String token = "GENERATED_JWT_FOR_" + member.getId();

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("memberId", member.getId());
        response.put("name", member.getName());
        response.put("relationship",
                member.getRelationship() != null ? member.getRelationship().name().toLowerCase() : null);

        return response;
    }

    public List<Map<String, Object>> getFamilyMembersByUser(Long userId) {
        // 1️⃣ Fetch the user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 2️⃣ Check if user belongs to a family
        Family family = user.getFamily();
        if (family == null) {
            throw new ResourceNotFoundException("Family is not associated with the user");
        }

        // 3️⃣ Get members of the family
        List<FamilyMember> members = familyMemberRepository.findByFamily(family);
        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }

        // 4️⃣ Build response
        return members.stream().map(fm -> {
            User member = fm.getUser();
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", member.getId());
            map.put("memberId", fm.getId());
            map.put("name", member.getName());
            map.put("relationship",
                    member.getRelationship() != null ? member.getRelationship().name().toLowerCase() : null);
            map.put("dateOfBirth",
                    member.getDateOfBirth() != null ? member.getDateOfBirth().toString() : null);
            return map;
        }).collect(Collectors.toList());
    }


    public Optional<FamilyMember> getByUserAndFamilyId(User child, Long id) {
        return familyMemberRepository.findByUserAndFamilyId(child, id);
    }
}
