package com.superme.controller;

import com.superme.admin.model.Admin;
import com.superme.admin.repository.AdminRepository;
import com.superme.admin.service.AuthService;
import com.superme.dto.*;
import com.superme.enums.Relationship;
import com.superme.enums.Role;
import com.superme.exception.BusinessException;
import com.superme.exception.InternalServerErrorException;
import com.superme.exception.ResourceNotFoundException;
import com.superme.exception.UnauthorizedActionException;
import com.superme.mapper.UserMapper;
import com.superme.model.*;
import com.superme.repository.AvatarRepository;
import com.superme.repository.PetRepository;
import com.superme.service.AvatarService;
import com.superme.service.FeedbackService;
import com.superme.util.UserJwtUtil;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Path;
import jakarta.validation.Valid;
import com.superme.service.UserService;
import com.superme.service.FamilyMemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class UserController {

    @Autowired
    private AuthService authService;

    @PostConstruct
    public void init() {
        System.out.println("UserController Loaded ✅");
    }




    @Autowired
    private PetRepository petRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private AvatarRepository avatarRepository;

    @Autowired
    private AvatarService avatarService;

    @Autowired
    private FeedbackService feedbackService;



    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private FamilyMemberService familyMemberService;

    // ✅ API to check if a user is already registered
    @GetMapping("/check/alreadyRegistered")
    public ResponseEntity<Map<String, Object>> checkAlreadyRegisteredUser(
            @RequestParam("identifier") String identifier) {

        String message = userService.checkAlreadyRegisteredUser(identifier);

        Map<String, Object> response = new HashMap<>();
        response.put("identifier", identifier);
        response.put("message", message);
        response.put("status", message.equals("User already registered!") ? true : false);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/generate-otp")
    public OtpResponse generateOtp(@Valid @RequestBody OtpRequest request) {
        return userService.generateOtp(request);
    }

    @PostMapping("/register/verify-otp")
    public OtpResponse verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return userService.verifyOtp(request);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        ResponseEntity<?> register = userService.registerUser(request);
        return register;
    }


//    @PostMapping("/user/login")
//    public ResponseEntity<?> login(@Valid @RequestBody AuthenticationRequest request) {
//
//        if (request.getPassword() == null || request.getPassword().isBlank()) {
//            throw new BusinessException("Password cannot be empty");
//        }
//
//        String loginId = (request.getEmail() != null && !request.getEmail().isBlank())
//                ? request.getEmail()
//                : request.getPhone();
//
//        logger.info("Login attempt for: {}", loginId);
//
//        User user = userService.login(request.getEmail(), request.getPhone(), request.getPassword())
//                .orElseThrow(() -> new UnauthorizedActionException("Invalid credentials."));
//
//        String token = UserJwtUtil.generateToken(user);
//
//        logger.info("Login successful for: {}", loginId);
//
//        return ResponseEntity.ok(new LoginResponse(token, UserMapper.toDto(user)));
//    }

    // ─────────────────────────────
    // UPDATE USER PROFILE
    // ─────────────────────────────
    @PutMapping("/edit-profile")
    public ResponseEntity<?> updateProfile(@RequestHeader("Authorization") String token,
                                           @RequestBody EditProfileRequest req) {

        Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));

        UserResponseDto updatedUser = userService.updateProfile(userId, req);
        return ResponseEntity.ok(updatedUser);
    }

    // ─────────────────────────────
    // CHANGE PASSWORD
    // ─────────────────────────────
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestHeader("Authorization") String token,
                                            @RequestBody ChangePasswordRequest req) {
        Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));

        userService.changePassword(userId, req);
        return ResponseEntity.ok("Password updated successfully");
    }



    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        boolean isVerified = userService.verifyEmail(token);
        if (isVerified) {
            return ResponseEntity.ok("Email verified successfully!");
        } else {
            throw new ResourceNotFoundException("Invalid or expired token!");
        }
    }

//    @GetMapping("/leaderboard/streaks")
//    public ResponseEntity<LeaderboardResponse> getStreakLeaderboard(
//            @RequestHeader("Authorization") String token) {
//
//        Long userId = UserJwtUtil.getUserIdFromToken(
//                token.replace("Bearer ", "")
//        );
//
//        LeaderboardResponse leaderboardData =
//                userService.getTopUsersByStreak(userId);
//
//        // Optional safety check
//        if (leaderboardData == null ||
//                leaderboardData.getTopUsers() == null ||
//                leaderboardData.getTopUsers().isEmpty()) {
//
//            throw new ResourceNotFoundException("No leaderboard data available.");
//        }
//
//        return ResponseEntity.ok(leaderboardData);
//    }


    @GetMapping("/leaderboard/streaks")
    public ResponseEntity<LeaderboardResponse> getStreakLeaderboard(
            @RequestHeader("Authorization") String token) {

        Long userId = UserJwtUtil.getUserIdFromToken(
                token.replace("Bearer ", "")
        );

        LeaderboardResponse leaderboardData =
                userService.getTopUsersByStreak(userId);

        // ✅ Handle null safely
        if (leaderboardData == null) {
            leaderboardData = new LeaderboardResponse(
                    null,
                    new ArrayList<>(),
                    "No leaderboard data available"
            );
        }

        // ✅ Handle null list
        if (leaderboardData.getTopUsers() == null) {
            leaderboardData = new LeaderboardResponse(
                    leaderboardData.getCurrentUser(),
                    new ArrayList<>(),
                    null
            );
        }

        return ResponseEntity.ok(leaderboardData);
    }

//    @GetMapping("/users-by-role")
//    public ResponseEntity<List<?>> getUsersByRole(@RequestParam String role) {
//        Role roleEnum;
//        try {
//            roleEnum = Role.valueOf(role.toUpperCase());
//        } catch (Exception e) {
//            throw new BusinessException("Role must be either 'USER' or 'ADMIN'.");
//        }
//         if(role.equals("USER")) {
//             List<User> users = userService.getAllUsersByRole(roleEnum);
//
//             if (users == null || users.isEmpty()) {
//                 throw new ResourceNotFoundException("No users found for role: " + role);
//             }
//
//             return ResponseEntity.ok(users);
//         }else if(role.equals("ADMIN")) {
//             List<Admin> users = authService.getAllUsersByRole(roleEnum);
//
//             if (users == null || users.isEmpty()) {
//                 throw new ResourceNotFoundException("No users found for role: " + role);
//             }
//
//             return ResponseEntity.ok(users);
//
//          }else{
//                throw new BusinessException("Role must be either 'USER' or 'ADMIN'.");
//         }
//    }

    @GetMapping("/users-by-role")
    public ResponseEntity<?> getUsersByRole(@RequestParam String role) {

        Role roleEnum;
        try {
            roleEnum = Role.valueOf(role.toUpperCase());
        } catch (Exception e) {
            throw new BusinessException("Invalid role.");
        }

        if (roleEnum == Role.USER) {
            return ResponseEntity.ok(userService.getAllUsersByRole(roleEnum));
        }
        System.out.println(roleEnum);
        return ResponseEntity.ok(adminRepository.findByRole(roleEnum));
    }




    @GetMapping("/users-logged-in-today")
    public ResponseEntity<List<User>> getUsersWhoLoggedInToday() {
        List<User> users = userService.getUsersWhoLoggedInToday();
        if (users == null || users.isEmpty()) {
            throw new ResourceNotFoundException("No users logged in today.");
        }
        return ResponseEntity.ok(users);
    }

//    @DeleteMapping("/delete-user/{userId}")
//    public ResponseEntity<String> deleteAccount(@PathVariable Long userId, Principal principal) {
//        try {
//            String message = userService.deleteAccount(userId, principal.getName());
//            return ResponseEntity.ok(message);
//        } catch (UnauthorizedActionException e) {
//           throw new UnauthorizedActionException(e.getMessage());
//        } catch (ResourceNotFoundException e) {
//            throw new ResourceNotFoundException("User not found.");
//        } catch (Exception e) {
//            throw new InternalServerErrorException("An error occurred while deleting the account.");
//        }
//    }

//    @PostMapping("/logout")
//    public ResponseEntity<String> logout(@RequestParam Long userId, Principal principal) {
//        try {
//            userService.logout(userId, principal.getName());
//            return ResponseEntity.ok("User logged out successfully.");
//        } catch (UnauthorizedActionException e) {
//            throw new UnauthorizedActionException(e.getMessage());
//        } catch (ResourceNotFoundException e) {
//            throw new ResourceNotFoundException("User not found.");
//        } catch (Exception e) {
//            throw new InternalServerErrorException("An error occurred while logout the account.");
//        }
//    }

//    @PostMapping("/add-family-member")
//    public ResponseEntity<?> addFamilyMember(Principal principal, @RequestBody AddFamilyMemberRequest req) {
//        Long mainUserId = Long.parseLong(principal.getName());
//        String name = req.getName();
//        String gender = req.getGender();
//        java.time.LocalDate dateOfBirth = req.getDateOfBirth();
//        Relationship relationship;
//        try {
//            relationship = Relationship.valueOf(req.getRelationship().toUpperCase());
//        } catch (Exception e) {
//           throw new BusinessException("Relationship must be CHILD, PARENT, or SELF.");
//        }
//
//        User user = userService.getUserById(mainUserId)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
//
//        try {
//            User member = userService.addFamilyMember(mainUserId, name, gender, dateOfBirth, relationship);
//            return ResponseEntity.status(HttpStatus.CREATED).body(member);
//        } catch (Exception e) {
//            throw new InternalServerErrorException("Failed to add family member: " + e.getMessage());
//        }
//    }

    // ✅ GET API: Fetch user details by ID
    @GetMapping("/user-details/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id, HttpServletRequest httpServletRequest) {
        // 1️⃣ Extract and validate token
        String authHeader = httpServletRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
        }

        Long userId = UserJwtUtil.getUserIdFromToken(authHeader.substring(7));

        // 2️⃣ Prevent users from fetching other users' data
        if (!userId.equals(id)) {
            throw new UnauthorizedActionException("Access denied");
        }

        // 3️⃣ Fetch user (Optional → direct entity)
        User user = userService.getUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        return ResponseEntity.ok(user);
    }

    // NOTE: Login response mapping moved to com.superme.mapper.UserMapper so it can be reused by the unified router.


    //Referal Code Generation API
    @PostMapping("/users/generate-referral")
    public ResponseEntity<?> generateReferralCode(@RequestHeader("Authorization") String token) {
        Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        String code = userService.generateReferralCode(userId);
        return ResponseEntity.ok(Map.of("referralCode", code));
    }

    @GetMapping("/check-referral")
    public ResponseEntity<Boolean> checkReferralCode(@RequestParam String referralCode) {
        boolean exists = userService.existsReferralCode(referralCode);
        return ResponseEntity.ok(exists);
    }

    @PostMapping("/restore-streak")
    public ResponseEntity<String> restoreStreak(
            @RequestHeader("Authorization") String token
    ) {
        Long userId = UserJwtUtil.getUserIdFromToken(
                token.replace("Bearer ", "")
        );

        User user = userService.getUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userService.restoreStreak(user);

        return ResponseEntity.ok("🔥 Streak restored! Coins have been deducted.");
    }


    // Get streak status using JWT user id from Authorization header
    @GetMapping("/streak-status")
    public ResponseEntity<StreakStatusDto> getStreakStatus(@RequestHeader("Authorization") String token) {
        Long userId = UserJwtUtil.getUserIdFromToken(
                token.replace("Bearer ", "")
        );

        User user = userService.getUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return ResponseEntity.ok(userService.getStreakStatus(user));
    }


    @GetMapping("/family-details")
    public UserFamilyDetailsResponse getFamilyDetails(
            @RequestHeader("Authorization") String token) {
        Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return userService.getFamilyDetails(userId);
    }


    /* ===============================
      RENAME AVATAR (Edit Profile)
      =============================== */
    @PutMapping("/rename-avatar")
    public Avatar renameAvatar(
            @RequestHeader("Authorization") String token,
            @RequestParam String avatarName,
            @RequestParam String avatarImageName) {
        Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return avatarService.renameAvatar(userId, avatarName,avatarImageName);
    }


    /* ===============================
       SUGGEST AVATAR NAMES
       =============================== */
    @GetMapping("/avatar-suggestions")
    public List<String> suggestAvatarNames(
            @RequestParam String input) {

        return avatarService.suggestAvatarNames(input);
    }

    @PostMapping("/submit-feedback")
    public ResponseEntity<?> submitFeedback(@RequestHeader("Authorization") String token,@Valid @RequestBody FeedbackRequestDto request) {

        Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));

        feedbackService.submitFeedback(userId, request);

        return ResponseEntity.ok(Map.of("message", "Thank you for your feedback!"));
    }

    @PostMapping("/self-to-parent")
    public ResponseEntity<?> selfToParent(@RequestHeader("Authorization") String token,@RequestParam String familyName,@RequestParam String familyCode) {

        Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));

        userService.convertSelfToParent(userId,familyName,familyCode);

        return ResponseEntity.ok(Map.of("message", "User converted from SELF to PARENT successfully!"));
    }

}
