package com.superme.controller;

import com.superme.dto.UserProfileResponseDto;
import com.superme.dto.UserProfileUpdateDto;
import com.superme.exception.ResourceNotFoundException;
import com.superme.model.User;
import com.superme.service.UserService;
import com.superme.util.UserJwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/user/profile")
@CrossOrigin(origins = "*")
public class UserProfileController {

    @Autowired
    private UserService userService;

    // ✅ GET PROFILE
    @GetMapping
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String token) {

        Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UserProfileResponseDto dto = userService.mapToDto(user);
        return ResponseEntity.ok(dto);
    }

    // ✅ UPDATE PROFILE
    @PutMapping
    public ResponseEntity<?> updateProfile(
            @RequestBody UserProfileUpdateDto dto,
            @RequestHeader("Authorization") String token) {
        Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));

        User updatedUser = userService.updateUserProfile(
                userId,
                dto
        );

        return ResponseEntity.ok(updatedUser);
    }
}