package com.superme.controller;

import com.superme.dto.OAuth2MobileLoginRequest;
import com.superme.dto.LoginResponse;
import com.superme.dto.UserDTO;
import com.superme.enums.Role;
import com.superme.exception.BusinessException;
import com.superme.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
@RequiredArgsConstructor
public class OAuth2Controller {

    /**
     * Simple ping endpoint
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        log.info("✅ Test ping endpoint called");
        return ResponseEntity.ok("pong - Server is running!");
    }

    /**
     * TEST OAUTH2 LOGIN - Completely separate URL
     * URL: /api/public/test/oauth2-login
     * This bypasses all JWT filters
     */
//    @PostMapping("/oauth2-login")
//    public ResponseEntity<LoginResponse> testOAuth2Login(
//            @RequestBody OAuth2MobileLoginRequest request) {
//
//        log.info("========================================");
//        log.info("🔧 TEST OAUTH2 ENDPOINT (NO AUTH REQUIRED)");
//        log.info("URL: /api/public/test/oauth2-login");
//        log.info("Provider: {}", request.getProvider());
//        log.info("Email: {}", request.getEmail());
//        log.info("Name: {}", request.getName());
//        log.info("ProviderId: {}", request.getProviderId());
//        log.info("========================================");
//
//        // Create response
//        UserDTO userDTO = new UserDTO();
//        userDTO.setId(System.currentTimeMillis()); // Temporary ID
//        userDTO.setEmail(request.getEmail());
//        userDTO.setName(request.getName() != null ? request.getName() : request.getEmail().split("@")[0]);
//        userDTO.setRole(Role.USER);
//        userDTO.setGender("Not specified");
//
//        String token = "test-jwt-token-for-" + request.getEmail() + "-" + System.currentTimeMillis();
//
//        LoginResponse response = new LoginResponse(token, userDTO);
//
//        log.info("✅ Test response created. Token: {}", token);
//
//        return ResponseEntity.status(HttpStatus.OK).body(response);
//    }









    private final UserService userService;

    /**
     * WORKING OAuth2 Mobile Login Endpoint
     * URL: POST /v1/auth/oauth2/mobile-login
     */
    @PostMapping("/oauth2-login")
    public ResponseEntity<LoginResponse> mobileOAuth2Login(
            @Valid @RequestBody OAuth2MobileLoginRequest request) {

        log.info("========================================");
        log.info("📱 OAuth2 Mobile Login Request");
        log.info("Provider: {}", request.getProvider());
        log.info("Email: {}", request.getEmail());
        log.info("Name: {}", request.getName());
        log.info("========================================");

        // Validate provider
        if (!isValidProvider(request.getProvider())) {
            log.error("Invalid provider: {}", request.getProvider());
            throw new BusinessException("Invalid provider. Supported: google, github, facebook");
        }

        // Validate email
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            log.error("Email is required");
            throw new BusinessException("Email is required");
        }

        try {
            // Check if user exists BEFORE processing
            boolean isNewUser = !userService.existsByEmail(request.getEmail());
            log.info("Is new user: {}", isNewUser);

            // Process login (creates user if doesn't exist)
            LoginResponse response = userService.processMobileOAuth2Login(request);

            log.info("✅ OAuth2 login successful. User ID: {}, New User: {}",
                    response.getUser().getId(), isNewUser);

            // Return 201 for new user, 200 for existing
            HttpStatus status = isNewUser ? HttpStatus.CREATED : HttpStatus.OK;

            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            log.error("❌ OAuth2 login failed: {}", e.getMessage(), e);
            throw new BusinessException("OAuth2 login failed: " + e.getMessage());
        }
    }

    private boolean isValidProvider(String provider) {
        return provider != null &&
                (provider.equalsIgnoreCase("google") ||
                        provider.equalsIgnoreCase("github") ||
                        provider.equalsIgnoreCase("facebook"));
    }






    @GetMapping("/check-mail-already-exists")
    public ResponseEntity<Map<String, Boolean>> checkEmailExists(@RequestParam String email) {
        Map<String, Boolean> response = new HashMap<>();
        boolean exists = userService.checkEmailExists(email);
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }


    // Inner class for simulation response
    @lombok.Data
    public static class TestLoginResponse {
        private boolean success;
        private String email;
        private String name;
        private String token;
        private String message;
    }
}