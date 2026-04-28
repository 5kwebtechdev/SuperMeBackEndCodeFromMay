package com.superme.controller;

import com.superme.admin.dto.AdminLoginResponse;
import com.superme.admin.model.Admin;
import com.superme.admin.repository.AdminRepository;
import com.superme.admin.service.AuthService;
import com.superme.dto.AuthenticationRequest;
import com.superme.dto.LoginResponse;
import com.superme.exception.BusinessException;
import com.superme.exception.InternalServerErrorException;
import com.superme.exception.ResourceNotFoundException;
import com.superme.exception.UnauthorizedActionException;
import com.superme.mapper.UserMapper;
import com.superme.model.User;
import com.superme.repository.UserRepository;
import com.superme.service.UserService;
import com.superme.util.UserJwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class LoginController {

    private final UserService userService;
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final AuthService adminAuthService;

    public LoginController(UserService userService, AuthService adminAuthService,UserRepository userRepository,AdminRepository adminRepository) {
        this.userService = userService;
        this.adminAuthService = adminAuthService;
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
    }

    /**
     * Unified login endpoint that routes to the correct DB (admin vs user) based on the identifier.
     *
     * If the identifier exists in both DBs, we fail fast and ask the client to call the explicit endpoint:
     * - /admin/auth/login (admin)
     * - /auth/user/login  (user)
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthenticationRequest request) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException("Password cannot be empty");
        }

        String email = trimToNull(request.getEmail());
        String phone = trimToNull(request.getPhone());

        boolean adminMatch = false;
        boolean userMatch = false;

        if (email != null) {
            adminMatch = adminAuthService.existsByEmail(email);
            userMatch = userService.existsByEmail(email);
        }

        if (phone != null) {
            adminMatch = adminMatch || adminAuthService.existsByPhone(phone);
            userMatch = userMatch || userService.existsByPhone(phone);
        }

        if (adminMatch && userMatch) {
            throw new BusinessException(
                    "Identifier matches both Admin and User. Use /admin/auth/login for admin or /auth/user/login for user."
            );
        }



        if (adminMatch) {
            AdminLoginResponse response = (email != null)
                    ? adminAuthService.login(email, request.getPassword())
                    : adminAuthService.loginByPhone(phone, request.getPassword());
            return ResponseEntity.ok(response);
        }

        User user = userService.login(email, phone, request.getPassword())
                .orElseThrow(() -> new UnauthorizedActionException("Invalid credentials."));
        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);
        String token = UserJwtUtil.generateToken(user);
        return ResponseEntity.ok(new LoginResponse(token, UserMapper.toDto(user)));
    }



















//@PostMapping("/logout")
//public ResponseEntity<String> logout(
//        @RequestHeader(value = "Authorization", required = false) String token,
//        @RequestParam Long userId
//) {
//    try {
//
//        // ✅ 1. Check ADMIN first
//        Optional<Admin> adminOpt = adminRepository.findById(userId);
//
//        if (adminOpt.isPresent()) {
//            return ResponseEntity.ok("Admin logged out successfully");
//        }
//
//        // ✅ 2. Otherwise USER
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new ResourceNotFoundException("User/Admin not found"));
//
//        // ✅ 3. Maintain logout record ONLY for user
//        userService.logout(user.getId(), String.valueOf(user.getId()));
//
//        return ResponseEntity.ok("User logged out successfully");
//
//    } catch (Exception e) {
//        throw new InternalServerErrorException("Logout failed: " + e.getMessage());
//    }
//}




    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
