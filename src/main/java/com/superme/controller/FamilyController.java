package com.superme.controller;

import com.superme.dto.AddFamilyMember;
import com.superme.dto.AddFamilyMemberRequest;
import com.superme.dto.JoinFamilyRequest;
import com.superme.dto.JoinTokenResponse;
import com.superme.enums.Relationship;
import com.superme.exception.BusinessException;
import com.superme.exception.ResourceNotFoundException;
import com.superme.exception.UnauthorizedActionException;
import com.superme.model.User;
import com.superme.repository.FamilyRepository;
import com.superme.repository.UserRepository;
import com.superme.service.FamilyMemberService;
import com.superme.service.FamilyService;
import com.superme.util.UserJwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/family")
public class FamilyController {

    private final UserRepository userRepository;
    private final FamilyRepository familyRepository;
    private final FamilyMemberService familyMemberService;

    private final FamilyService familyService;

    @Autowired
    public FamilyController(UserRepository userRepository, FamilyRepository familyRepository,
                            FamilyMemberService familyMemberService, FamilyService familyService) {
        this.userRepository = userRepository;
        this.familyRepository = familyRepository;
        this.familyMemberService = familyMemberService;
        this.familyService = familyService;
    }

    // Get the family code for QR/invite
    @GetMapping("/code")
    public ResponseEntity<Map<String, Object>> getFamilyCode(@RequestParam Long userId) {
        Map<String, Object> response = familyService.getFamilyCodeByUserId(userId);
        return ResponseEntity.ok(response);
    }

    // Generate a join token (for QR code) for the family
    @GetMapping("/generate-join-token")
    public ResponseEntity<JoinTokenResponse> generateJoinToken(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        String token = familyService.generateJoinToken(userId);
        return ResponseEntity.ok(new JoinTokenResponse(token));
    }

    // Join a family using a scanned token (from QR code)
    @PostMapping("/join")
    public ResponseEntity<String> joinFamily(@RequestBody JoinFamilyRequest request, @RequestHeader("Authorization") String token) {
        Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        familyService.joinFamily(userId, request.getFamilyCode(), request.getRelationship());
        return ResponseEntity.ok("Joined family successfully!");
    }




    // Add a family member (parent or child) by main user
    @PostMapping("/add-member")
    public ResponseEntity<?> addFamilyMember(@RequestBody AddFamilyMember request) {
        familyMemberService.addFamilyMember(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Family member added successfully"));
    }





    // List all family members with relationship from User table only
    @GetMapping("/members")
    public ResponseEntity<List<Map<String, Object>>> getFamilyMembers(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        List<Map<String, Object>> members = familyMemberService.getFamilyMembersByUser(userId);

        if (members.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(members);
    }

    // Switch/monitor endpoint: only "parent" or "self" can switch/monitor
    @GetMapping("/switch/{memberId}")
    public ResponseEntity<?> switchToFamilyMember(Principal principal, @PathVariable Long memberId) {
        Long currentUserId = Long.parseLong(principal.getName());
        Map<String, Object> response = familyMemberService.switchToFamilyMember(currentUserId, memberId);
        return ResponseEntity.ok(response);
    }

    // Create a new family
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> generateFamilyCode(
            @RequestParam String familyName,
            @RequestParam Long userId
    ) {

        Map<String, Object> response = familyService.generateFamilyCode(familyName,userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Endpoint for a member to leave the family and become self
    @PostMapping("/leave")
    public ResponseEntity<?> leaveFamily(Principal principal) {
        User user = userRepository.findById(Long.parseLong(principal.getName()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getFamily() == null) {
            throw new BusinessException("User does not belong to a family.");
        }
        familyMemberService.removeUserFromFamilyAndSetSelf(user);
        return ResponseEntity.ok("You have left the family and are now self.");
    }

    // Endpoint for parent to remove all family members (except themselves)
    @PostMapping("/remove-all")
    public ResponseEntity<?> removeAllFamilyMembers(Principal principal) {
        User parent = userRepository.findById(Long.parseLong(principal.getName()))
                .orElseThrow(() -> new BusinessException("User not found"));
        if (parent.getRelationship() != Relationship.PARENT) {
            throw new UnauthorizedActionException("Only parent can remove all family members.");
        }
        int removed = familyMemberService.removeAllFamilyMembersAndConvertParentToSelf(parent);
        return ResponseEntity.ok("Removed " + removed + " family members and converted parent to self.");
    }

    // Endpoint for parent to remove a specific member from the family
    @PostMapping("/remove")
    public ResponseEntity<?> removeFamilyMember(
            Principal principal,
            @RequestBody Map<String, Object> req) {
        User parent = userRepository.findById(Long.parseLong(principal.getName()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (parent.getRelationship() != Relationship.PARENT) {
            throw new UnauthorizedActionException("Only parent can remove family members.");
        }
        Long memberId = Long.valueOf(req.get("memberId").toString());
        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Family member not found"));
        if (member.getFamily() == null || !member.getFamily().getId().equals(parent.getFamily().getId())) {
            throw new BusinessException("Member does not belong to your family.");
        }
        familyMemberService.removeUserFromFamilyAndSetSelf(member);
        return ResponseEntity.ok("Family member removed and set to self.");
    }

// Check if a family code exists
    @GetMapping("/check-family-code")
    public ResponseEntity<Map<String, Boolean>> checkFamilyCodeExists(@RequestParam String familyCode) {
    boolean exists = familyRepository.existsByFamilyCode(familyCode);
    return ResponseEntity.ok(Map.of("exists", exists));
        }
}