package com.superme.controller;

import com.superme.dto.ProfileRequestDto;
import com.superme.exception.BusinessException;
import com.superme.exception.ResourceNotFoundException;
import com.superme.model.Profile;
import com.superme.model.User;
import com.superme.service.ProfileService;
import com.superme.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Optional;

@RestController
@RequestMapping("/profile")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<?> createProfile(@RequestBody ProfileRequestDto profileRequestDto, Principal principal) {
        User user = userService.getUserById(Long.valueOf(principal.getName()))
                .orElseThrow(() -> new BusinessException("User not found."));

        // Only allow users with the 'USER' role to have profiles
        if (!"USER".equalsIgnoreCase(user.getRole().name())) {
            throw new BusinessException("Only users with the 'USER' role can have profiles.");
        }

        Profile createdProfile = profileService.createProfile(profileRequestDto, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProfile);
    }

    /**
     * Get the child profile.
     */
    @GetMapping
    public ResponseEntity<?> getProfile(Principal principal) {
        System.out.println(principal.getName()+"== principal.getName()");
        User user = userService.getUserById(Long.valueOf(principal.getName()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        Optional<Profile> profile = profileService.getProfile(user);
        return profile
                .<ResponseEntity<?>>map(value -> ResponseEntity.ok(value))
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found."));
    }

    /**
     * Update the child profile.
     */
    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody ProfileRequestDto profileRequestDto, Principal principal) {
        User user = userService.getUserById(Long.valueOf(principal.getName()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        Profile profile = profileService.updateProfile(profileRequestDto, user);
        return ResponseEntity.ok(profile);
    }
}