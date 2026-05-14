package com.superme.controller;

 import com.superme.admin.dto.AdminLoginResponse;
import com.superme.admin.repository.AdminRepository;
import com.superme.admin.service.AuthService;
import com.superme.dto.AuthenticationRequest;
import com.superme.dto.LoginResponse;
import com.superme.dto.UserDTO;
import com.superme.exception.BusinessException;
import com.superme.exception.UnauthorizedActionException;
import com.superme.mapper.UserMapper;
import com.superme.model.User;
import com.superme.repository.UserRepository;
import com.superme.service.UserService;
import com.superme.util.UserJwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class LoginController {
    public static final Logger log = LoggerFactory.getLogger(LoginController.class);
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

        log.info("Login request received");
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            log.warn("Login failed: Password is empty");
            throw new BusinessException("Password cannot be empty");
        }

        String email = trimToNull(request.getEmail());
        String phone = trimToNull(request.getPhone());

        log.info("Login attempt with email: {} phone: {}", email, phone);
        boolean adminMatch = false;
        boolean userMatch = false;

        if (email != null) {
            adminMatch = adminAuthService.existsByEmail(email);
            userMatch = userService.existsByEmail(email);
            log.debug("Email match -> admin: {}, user: {}", adminMatch, userMatch);
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
            log.info("Routing login to ADMIN flow");

            AdminLoginResponse response = (email != null)
                    ? adminAuthService.login(email, request.getPassword())
                    : adminAuthService.loginByPhone(phone, request.getPassword());

            // 🔥 Convert ADMIN → USER-like structure
            UserDTO userDTO = new UserDTO();
            userDTO.setId(response.getAdmin().getId());
            userDTO.setName(response.getAdmin().getFullName());
            userDTO.setEmail(response.getAdmin().getEmail());
            userDTO.setPhone(response.getAdmin().getPhone());
            userDTO.setRole(com.superme.enums.Role.valueOf(response.getAdmin().getRole()));

            // Optional mappings
            userDTO.setGender(null);
            userDTO.setDateOfBirth(null);
            userDTO.setRelationship(null);
            userDTO.setReferralCode(null);
            userDTO.setFamily(null);
            userDTO.setFamilyMembers(null);
            userDTO.setAvatar(null);
            userDTO.setPet(null);

            // ✅ Return SAME structure as normal user login
            return ResponseEntity.ok(new LoginResponse(
                    response.getToken(),
                    userDTO
            ));
        }


        log.info("Routing login to USER flow");
        User user = userService.login(email, phone, request.getPassword())
                .orElseThrow(() -> new UnauthorizedActionException("Invalid credentials."));
        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);
        String token = UserJwtUtil.generateToken(user);
        log.info("User login successful. userId: {}", user.getId());
        return ResponseEntity.ok(new LoginResponse(token, UserMapper.toDto(user)));
    }



    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }







































}
