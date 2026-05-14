package com.superme.controller;

import com.superme.dto.OAuth2MobileLoginRequest;
import com.superme.dto.LoginResponse;
import com.superme.dto.UserDTO;
import com.superme.enums.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class TestController {

    /**
     * SIMPLE TEST ENDPOINT - No token required
     * This will help verify if the endpoint is reachable
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        log.info("✅ Test ping endpoint called");
        return ResponseEntity.ok("pong - Server is running!");
    }

    /**
     * TEST OAUTH2 ENDPOINT - No token required
     * This bypasses all security and just returns dummy data
     */
    @PostMapping("/oauth2-test")
    public ResponseEntity<LoginResponse> testOAuth2Login(
            @RequestBody OAuth2MobileLoginRequest request) {
        
        log.info("========================================");
        log.info("🔧 TEST OAUTH2 ENDPOINT CALLED");
        log.info("Provider: {}", request.getProvider());
        log.info("Email: {}", request.getEmail());
        log.info("Name: {}", request.getName());
        log.info("ProviderId: {}", request.getProviderId());
        log.info("========================================");
        
        // Create dummy response
        UserDTO dummyUser = new UserDTO();
        dummyUser.setId(999L);
        dummyUser.setEmail(request.getEmail());
        dummyUser.setName(request.getName() != null ? request.getName() : request.getEmail().split("@")[0]);
        dummyUser.setRole(Role.USER);
        dummyUser.setGender("Not specified");
        
        LoginResponse response = new LoginResponse(
            "test-jwt-token-for-" + request.getEmail(),
            dummyUser
        );
        
        log.info("✅ Test response created for: {}", request.getEmail());
        
        return ResponseEntity.ok(response);
    }

    /**
     * TEST ENDPOINT with path variable
     */
    @GetMapping("/echo/{message}")
    public ResponseEntity<String> echo(@PathVariable String message) {
        log.info("Echo: {}", message);
        return ResponseEntity.ok("You said: " + message);
    }
}