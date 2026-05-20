package com.superme.admin.controller;

import com.superme.admin.dto.IndividualUserRegisterDTO;
import com.superme.admin.dto.IndividualUserResponseDTO;
import com.superme.admin.service.IndividualUserService;
import com.superme.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles individual (SELF) user registration initiated from the admin panel.
 * Base path: /admin/register  →  with WebConfig prefix → /v1/admin/register
 */
@Slf4j
@RestController
@RequestMapping("/admin/register")
@RequiredArgsConstructor
public class AdminRegisterController {

    private final IndividualUserService individualUserService;

    /**
     * POST /v1/admin/register/individualuser
     *
     * Registers a new SELF user from the admin panel.
     * Creates the user record, a dedicated Avatar, an optional Pet,
     * and saves the BCrypt-hashed password.
     *
     * Required fields: fullName (or name), email, dob, gender, password
     * Optional fields: phone/mobile, avatar, pet, country
     */
    @PostMapping("/individualuser")
    public ResponseEntity<?> registerIndividualUser(
            @Valid @RequestBody IndividualUserRegisterDTO request) {
        log.info("Admin panel: registering individual user with email={}", request.getEmail());
        try {
            User saved = individualUserService.registerIndividualUser(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User registered successfully");
            response.put("userId", saved.getId());
            response.put("name", saved.getName());
            response.put("email", saved.getEmail());
            response.put("relationship", saved.getRelationship());
            response.put("role", saved.getRole());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            log.error("Individual user registration failed: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("success", "false");
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * GET /v1/admin/register/individualuser/{id}
     * Fetch a previously registered individual user by ID.
     */
    @GetMapping("/individualuser/{id}")
    public ResponseEntity<?> getIndividualUserById(@PathVariable Long id) {
        try {
            IndividualUserResponseDTO user = individualUserService.getIndividualUserById(id);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("success", "false");
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
}