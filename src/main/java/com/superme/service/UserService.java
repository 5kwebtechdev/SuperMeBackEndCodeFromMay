// Java
package com.superme.service;

import com.superme.admin.model.Admin;
import com.superme.admin.repository.AdminRepository;
import com.superme.dto.*;
import com.superme.enums.AgeGroup;
import com.superme.enums.Relationship;
import com.superme.enums.Role;
import com.superme.exception.BusinessException;
import com.superme.exception.InternalServerErrorException;
import com.superme.exception.ResourceNotFoundException;
import com.superme.exception.UnauthorizedActionException;
import com.superme.model.*;
import com.superme.repository.*;
import com.superme.util.OtpUtil;
import com.superme.util.ReferralCodeGenerator;
import com.superme.util.UserJwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private AvatarRepository avatarRepository;

    @Autowired
    private UserPasswordRepository userPasswordRepository;

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private FamilyMemberRepository familyMemberRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FamilyService familyService;

    @Autowired
    private CoinTransactionRepository coinTransactionRepository;

    @Autowired
    private CoinService coinService;

    @Autowired
    private AvatarService avatarService;

    private final Map<String, OtpEntry> otpStorage = new ConcurrentHashMap<>();
    private static final int OTP_EXPIRY_MINUTES = 5;



    public Optional<User> login(String email, String phone, String password) {

        Optional<User> user = Optional.empty();

        // ✳ Validate email
        if (email != null && !email.isBlank()) {
            if (!isValidEmail(email)) {
                throw new BusinessException("Invalid email format.");
            }
            user = userRepository.findByEmail(email);

            // ✳ Validate phone
        } else if (phone != null && !phone.isBlank()) {
            if (!isValidPhoneForLogin(phone)) {
                throw new BusinessException("Invalid phone format.");
            }
            user = findByAnyPhoneCandidate(phone);

        } else {
            throw new BusinessException("Email or phone is required.");
        }


        // ✳ User not found → 404
        User loggedInUser = user.orElseThrow(() ->
                new ResourceNotFoundException("User not found."));

        // ✳ Password fetch failure → 404
        UserPassword storedPassword = userPasswordRepository.findByUser(loggedInUser)
                .orElseThrow(() -> new ResourceNotFoundException("User credentials not found."));

        // ✳ Incorrect password → 401
        if (!passwordEncoder.matches(password, storedPassword.getPassword())) {
            throw new UnauthorizedActionException("Invalid credentials.");
        }

        loggedInUser.setLastLoginDate(LocalDateTime.now());
        userRepository.save(loggedInUser);

        return Optional.of(loggedInUser);
    }















    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) return false;
        return userRepository.existsByEmail(email.trim());
    }

    public boolean existsByPhone(String phone) {
        if (phone == null || phone.isBlank()) return false;
        for (String candidate : phoneLookupCandidates(phone)) {
            if (userRepository.existsByPhone(candidate)) return true;
        }
        return false;
    }


    public User register(User user, String password) {

        if ((user.getEmail() == null || user.getEmail().isBlank()) &&
                (user.getPhone() == null || user.getPhone().isBlank())) {
            throw new BusinessException("Either email or phone is required.");
        }
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            if (!isValidEmail(user.getEmail())) {
                throw new BusinessException("Invalid email format.");
            }
            if (userRepository.findByEmail(user.getEmail()).isPresent()) {
                throw new BusinessException("Email already used.");
            }
        }
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            if (!isValidPhoneWithCountryCode(user.getPhone())) {
                throw new BusinessException(
                        "Invalid phone format. Must include country code, e.g. +91 9876543210");
            }
            if (userRepository.findByPhone(user.getPhone()).isPresent()) {
                throw new BusinessException("Phone already used.");
            }
        }
        if (user.getRelationship() == null) {
            throw new BusinessException("Relationship is required (CHILD, PARENT, SELF).");
        }
        // Required fields
        user.setRole(Role.USER);
        user.setEmailVerified(false);
        user.setCoins(50);
        user.setCurrentStreak(0);
        user.setHighestStreak(0);
        user.setEnabled(true);
        // All other fields are set to null or default if not provided
        user.setEmailVerificationToken(null);
        user.setFamily(null);
        user.setLastLoginDate(null);
        if (user.getAvatar() == null) {
            Avatar defaultAvatar = avatarRepository.findByAvatarName("default-avatar.png").orElse(null);
            user.setAvatar(defaultAvatar);
        }
        if (user.getPet() == null) {
            Pet defaultPet = petRepository.findByPetName("default-pet").orElse(null);
            user.setPet(defaultPet);
        }
        user.setCreatedDateTime(java.time.LocalDateTime.now());
        User savedUser = userRepository.save(user);
        UserPassword userPassword = new UserPassword();
        userPassword.setUser(savedUser);
        userPassword.setPassword(passwordEncoder.encode(password));
        userPasswordRepository.save(userPassword);
        return savedUser;
    }

    // Phone must start with + and country code, e.g. +91 9876543210
    private boolean isValidPhoneWithCountryCode(String phone) {
        return phone != null && phone.matches("^\\+\\d{1,4}\\s?\\d{6,15}$");
    }

    private Optional<User> findByAnyPhoneCandidate(String phone) {
        for (String candidate : phoneLookupCandidates(phone)) {
            Optional<User> u = userRepository.findByPhone(candidate);
            if (u.isPresent()) return u;
        }
        return Optional.empty();
    }

    /**
     * Login accepts a wider set of phone inputs than registration:
     * - "+91 9876543210", "+919876543210"
     * - "9876543210" (tries "+91" and "+" prefixed variants as lookup candidates)
     */
    private boolean isValidPhoneForLogin(String phone) {
        List<String> candidates = phoneLookupCandidates(phone);
        if (candidates.isEmpty()) return false;

        for (String c : candidates) {
            if (c.startsWith("+")) {
                if (c.matches("^\\+\\d{7,15}$")) return true;
            } else {
                if (c.matches("^\\d{6,15}$")) return true;
            }
        }
        return false;
    }

    private static List<String> phoneLookupCandidates(String phone) {
        if (phone == null) return List.of();
        String raw = phone.trim();
        if (raw.isEmpty()) return List.of();

        // Remove common separators but keep leading '+' if present.
        String stripped = raw.replaceAll("[\\s\\-()]", "");

        // Support inputs like 0091... -> +91...
        if (stripped.startsWith("00")) {
            stripped = "+" + stripped.substring(2);
        }

        List<String> out = new ArrayList<>();
        addUnique(out, raw);
        addUnique(out, stripped);

        if (stripped.startsWith("+")) {
            String digitsOnly = stripped.substring(1).replaceAll("\\D", "");
            addUnique(out, "+" + digitsOnly);

            // Also try "+CC <number>" variant for DBs that store a space.
            if (digitsOnly.startsWith("91") && digitsOnly.length() > 2) {
                addUnique(out, "+91 " + digitsOnly.substring(2));
            }
        } else {
            String digitsOnly = stripped.replaceAll("\\D", "");
            addUnique(out, digitsOnly);

            // Try E.164-like variant.
            if (!digitsOnly.isEmpty()) {
                addUnique(out, "+" + digitsOnly);
            }

            // If the client sends country-code-prefixed digits without '+', also try "+CC <number>".
            if (digitsOnly.startsWith("91") && digitsOnly.length() > 2) {
                String rest = digitsOnly.substring(2);
                addUnique(out, "+91" + rest);
                addUnique(out, "+91 " + rest);
            }

            // India default (common in this project). This is only a lookup candidate.
            if (digitsOnly.length() == 10) {
                addUnique(out, "+91" + digitsOnly);
                addUnique(out, "+91 " + digitsOnly);
            }
        }

        return out;
    }

    private static void addUnique(List<String> list, String value) {
        if (value == null) return;
        String v = value.trim();
        if (v.isEmpty()) return;
        if (!list.contains(v)) list.add(v);
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    public String forgotPassword(String identifier, String newPassword) {
        Optional<User> user;
        if (identifier.contains("@")) {
            user = userRepository.findByEmail(identifier);
        } else {
            user = userRepository.findByPhone(identifier);
        }
        if (user.isPresent()) {
            User existingUser = user.get();
            Optional<UserPassword> userPasswordOpt = userPasswordRepository.findByUser(existingUser);
            if (userPasswordOpt.isPresent()) {
                UserPassword userPassword = userPasswordOpt.get();
                userPassword.setPassword(passwordEncoder.encode(newPassword));
                userPasswordRepository.save(userPassword);
                return "Password reset successful!";
            }
        }
        return "User not found!";
    }

    private void updateStreaks(User user) {
        LocalDate today = LocalDate.now();
        LocalDate lastLogin = null;
        if (user.getLastLoginDate() != null) {
            lastLogin = user.getLastLoginDate().toLocalDate();
        }
        if (lastLogin == null || !lastLogin.equals(today)) {
            if (lastLogin != null && lastLogin.plusDays(1).equals(today)) {
                user.setCurrentStreak(user.getCurrentStreak() + 1);
            } else {
                user.setCurrentStreak(1);
            }
            if (user.getCurrentStreak() > user.getHighestStreak()) {
                user.setHighestStreak(user.getCurrentStreak());
            }
            user.setLastLoginDate(java.time.LocalDateTime.now());
        }
    }

    private void initializeUserFields(User user) {
        if (user.getRole() == Role.USER) {
            if (user.getAvatar() == null) {
                Avatar defaultAvatar = avatarRepository.findByAvatarName("default-avatar.png").orElse(null);
                user.setAvatar(defaultAvatar);
            }
            if (user.getPet() == null) {
                Pet defaultPet = petRepository.findByPetName("default-pet").orElse(null);
                user.setPet(defaultPet);
            }
            user.setCoins(0);
            user.setCurrentStreak(0);
            user.setHighestStreak(0);
            user.setLastLoginDate(null);
        } else if (user.getRole() == Role.ADMIN) {
            user.setAvatar(null);
            user.setPet(null);
            user.setCoins(0);
            user.setCurrentStreak(0);
            user.setHighestStreak(0);
            user.setLastLoginDate(null);
        } else {
            throw new BusinessException("Invalid role specified. Role must be either 'USER' or 'ADMIN'.");
        }
    }

    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    public boolean verifyEmail(String token) {
        Optional<User> userOptional = userRepository.findByEmailVerificationToken(token);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setEmailVerified(true);
            user.setEmailVerificationToken(null);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    @Transactional
    public void restoreStreak(User user) {

        ZoneId zone = ZoneId.of("Asia/Kolkata");
        LocalDate today = LocalDate.now(zone);

        LocalDate missedDate = user.getLastMissedStreakDate();
        Integer previousStreak = user.getLastStreakCount();

        if (missedDate == null || previousStreak == null) {
            throw new BusinessException("No streak available to restore");
        }

        // allow restore only if exactly ONE day was missed
        if (!missedDate.plusDays(2).equals(today)) {
            throw new BusinessException("Restore window expired");
        }

        final int RESTORE_COST = 5;

        int coins = user.getCoins();
        if (coins < RESTORE_COST) {
            throw new BusinessException("Not enough coins to restore streak");
        }

        // 💰 deduct coins
        user.setCoins(coins - RESTORE_COST);

        // 🔥 FULL RESTORE
        user.setCurrentStreak(previousStreak + 1);
        user.setLastStreakDate(today);

        // cleanup restore state
        user.setLastMissedStreakDate(null);
        user.setLastStreakCount(null);

        if (user.getCurrentStreak() > user.getHighestStreak()) {
            user.setHighestStreak(user.getCurrentStreak());
        }

        userRepository.save(user);

        // 🧾 coin transaction
        CoinTransaction tx = new CoinTransaction();
        tx.setUser(user);
        tx.setAmount(-RESTORE_COST);
        tx.setType("STREAK_RESTORE");
        tx.setDescription("Streak restored using coins");
        tx.setTimestamp(LocalDateTime.now(zone));

        coinTransactionRepository.save(tx);
    }




    @Transactional(readOnly = true)
    public LeaderboardResponse getTopUsersByStreak(Long currentUserId) {

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (currentUser.getRelationship() != Relationship.CHILD) {
            return new LeaderboardResponse(null, List.of());
        }

        List<User> leaderboardUsers =
                userRepository.findLeaderboardUsersByAgeGroup(
                        currentUser.getAgeGroup()
                );

        leaderboardUsers.sort(
                Comparator
                        .comparingInt(User::getCurrentStreak).reversed()
                        .thenComparing(User::getCreatedDateTime)
        );

        Map<Long, Integer> rankMap =
                computeSequentialRanks(leaderboardUsers);

        CurrentUserRankDTO currentUserDto =
                buildCurrentUser(currentUser, rankMap);

        List<UserStreakDTO> topUsers =
                buildTopUsers(leaderboardUsers, rankMap);

        return new LeaderboardResponse(currentUserDto, topUsers);
    }

    // =======================
    // RANKING
    // =======================

    private Map<Long, Integer> computeSequentialRanks(List<User> users) {

        Map<Long, Integer> rankMap = new HashMap<>();
        int rank = 1;

        for (User user : users) {
            rankMap.put(user.getId(), rank++);
        }

        return rankMap;
    }

    // =======================
    // DTO BUILDERS
    // =======================

    private CurrentUserRankDTO buildCurrentUser(
            User currentUser,
            Map<Long, Integer> rankMap
    ) {

        String avatarImageName =
                currentUser.getAvatar() != null
                        ? currentUser.getAvatar().getAvatarImageName()
                        : null;

        int rank = rankMap.getOrDefault(
                currentUser.getId(),
                Integer.MAX_VALUE
        );

        int currentStreak = currentUser.getCurrentStreak();
        int highestStreak = currentUser.getHighestStreak(); // 🔥 USED HERE
        Integer totalActiveDays = currentUser.getTotalActiveDays();

        return new CurrentUserRankDTO(
                avatarImageName,
                rank,
                currentStreak,
                highestStreak,
                totalActiveDays,
                currentStreak > 0,
                generatePetMessage(rank, currentStreak),
                currentUser.getName(),
                currentUser.getAvatar() != null ? currentUser.getAvatar().getAvatarName() : null
        );
    }

    private List<UserStreakDTO> buildTopUsers(
            List<User> users,
            Map<Long, Integer> rankMap
    ) {

        List<UserStreakDTO> result = new ArrayList<>();

        for (User user : users) {

            String avatarImageName =
                    user.getAvatar() != null
                            ? user.getAvatar().getAvatarImageName()
                            : null;

            result.add(
                    new UserStreakDTO(
                            rankMap.get(user.getId()),
                            user.getName(),
                            user.getCurrentStreak(),
                            avatarImageName,
                            user.getAvatar() != null ? user.getAvatar().getAvatarName() : null
                    )
            );

            if (result.size() == 10) break;
        }

        return result;
    }


    private String generatePetMessage(int rank, int streakDays) {
        if (streakDays == 0) {
            return "No spot on the board...yet!\nBuild your streak and show them how it's done!";
        }

        if (rank <= 10) {
            return String.format("You're ranked #%d, keep going champ!", rank);
        } else if (rank <= 100) {
            return String.format("You're ranked #%d. Keep your streak alive!", rank);
        } else {
            return String.format("You're ranked #%d. Every day makes you stronger!", rank);
        }
    }
    public List<User> getAllUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    public List<User> getUsersWhoLoggedInToday() {
        LocalDate today = LocalDate.now();
        return userRepository.findAll().stream()
                .filter(u -> u.getLastLoginDate() != null && u.getLastLoginDate().toLocalDate().equals(today))
                .toList();
    }

    public long getTotalUserCount() {
        return userRepository.count();
    }

    public String deleteAccount(Long userId, String principalName) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new ResourceNotFoundException("User not found");
        }
        User userToDelete = userOptional.get();
        // Only allow if principalName matches user email/phone/id (as string)
        if (!principalName.equals(String.valueOf(userToDelete.getId())) &&
                (userToDelete.getEmail() == null || !principalName.equals(userToDelete.getEmail())) &&
                (userToDelete.getPhone() == null || !principalName.equals(userToDelete.getPhone()))) {
            throw new UnauthorizedActionException("You are not authorized to delete this account.");
        }
        userPasswordRepository.findByUser(userToDelete)
                .ifPresent(userPasswordRepository::delete);
        userRepository.delete(userToDelete);
        return "Account deleted successfully!";
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public User updateUser(Long id, User user) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        existing.setName(user.getName());
        existing.setEmail(user.getEmail());
        existing.setPhone(user.getPhone());
        if (user.getRole() != null) {
            existing.setRole(user.getRole());
        }
        if (user.getRelationship() != null) {
            existing.setRelationship(user.getRelationship());
        }
        // Update dateOfBirth if provided (this will automatically update age and
        // ageGroup)
        if (user.getDateOfBirth() != null) {
            existing.setDateOfBirth(user.getDateOfBirth());
        }
        return userRepository.save(existing);
    }

    public void setUserStatus(Long id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setEnabled(enabled);
        userRepository.save(user);
    }

//    public void logout(Long userId, String principalName) {
//        Optional<User> userOptional = userRepository.findById(userId);
//        if (userOptional.isEmpty()) {
//            throw new ResourceNotFoundException("User not found");
//        }
//        User user = userOptional.get();
//        // Only allow if principalName matches user email/phone/id (as string)
//        if (!principalName.equals(String.valueOf(user.getId())) &&
//                (user.getEmail() == null || !principalName.equals(user.getEmail())) &&
//                (user.getPhone() == null || !principalName.equals(user.getPhone()))) {
//            throw new UnauthorizedActionException("You are not authorized to logout this account.");
//        }
//        user.setLoggedIn(false);
//         userRepository.save(user);
//    }


//    public ResponseEntity<?> logout(Long userId, String token) {
//        try {
//            String jwt = token.replace("Bearer ", "");
//
//            // 🔐 Extract ID from token
//            Long tokenUserId = UserJwtUtil.getUserIdFromToken(jwt);
//
//            // ✅ Security check
//            if (!tokenUserId.equals(userId)) {
//                throw new UnauthorizedActionException("Invalid userId for this token");
//            }
//
//            // 🔴 Check ADMIN first
//            Optional<Admin> adminOpt = adminRepository.findById(userId);
//
//            if (adminOpt.isPresent()) {
////                Admin admin = adminOpt.get();
//
//                // ❌ No record maintenance
////                adminAuthService.logout(admin.getId());
//
//                return ResponseEntity.ok("Admin logged out successfully");
//            }
//
//            // 🟢 Otherwise USER
//            User user = userRepository.findById(userId)
//                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
//        user.setLoggedIn(false);
//         userRepository.save(user);
//            // ✅ Maintain record ONLY for user
//
//            return ResponseEntity.ok("User logged out successfully");
//
//        } catch (UnauthorizedActionException e) {
//            throw e;
//
//        } catch (ResourceNotFoundException e) {
//            throw new ResourceNotFoundException("User/Admin not found");
//
//        } catch (Exception e) {
//            throw new InternalServerErrorException("An error occurred while logout the account.");
//        }
//    }


    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User getUserByPhone(String phone) {
        return userRepository.findByPhone(phone).orElse(null);
    }

    /**
     * Add a family member (parent or child) to a family.
     * Enforces max 2 parents and 2 children per family.
     * Relationship as enum required.
     */
    @Transactional
    public User addFamilyMember(Long mainUserId, String name, String gender, java.time.LocalDate dateOfBirth,
                                Relationship relationship) {
        User mainUser = userRepository.findById(mainUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Main user not found"));
        Family family = mainUser.getFamily();

        long childCount = familyMemberRepository.countByRelationship(family, Relationship.CHILD);
        long parentCount = familyMemberRepository.countByRelationship(family, Relationship.PARENT);
        if (relationship == Relationship.CHILD && childCount >= 2)
            throw new BusinessException("Maximum 2 children allowed");
        if (relationship == Relationship.PARENT && parentCount >= 2)
            throw new BusinessException("Maximum 2 parents allowed");

        User member = new User();
        member.setName(name);
        member.setGender(gender);
        member.setDateOfBirth(dateOfBirth);
        member.setRole(Role.USER);
        member.setFamily(family);
        member.setEnabled(true);
        member.setRelationship(relationship);
        userRepository.save(member);

        FamilyMember fm = new FamilyMember();
        fm.setFamily(family);
        fm.setUser(member);
        fm.setDateOfBirth(dateOfBirth);
        familyMemberRepository.save(fm);

        return member;
    }

    public String generateFamilyJoinToken(Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Family not found"));
        return "JOIN-" + family.getId() + "-" + Instant.now().getEpochSecond();
    }

    /**
     * Join a family using a QR code (family code).
     * Relationship as enum required.
     */
    @Transactional
    public String joinFamily(Long userId, String familyCode, Relationship relationship) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Optional<Family> familyOpt = familyRepository.findByFamilyCode(familyCode);
        Family family = familyOpt.orElseThrow(() -> new BusinessException("Invalid family code."));

        user.setFamily(family);
        user.setRelationship(relationship);
        userRepository.save(user);

        FamilyMember fm = new FamilyMember();
        fm.setFamily(family);
        fm.setUser(user);
        fm.setDateOfBirth(user.getDateOfBirth());
        familyMemberRepository.save(fm);

        return "Successfully joined the family!";
    }

    public Optional<User> getUserByUsername(String username) {
        return Optional.empty();
    }

    // ===== NEW AGE-RELATED METHODS =====

    /**
     * Get users by specific age
     */
    public List<User> getUsersByAge(int age) {
        return userRepository.findByAge(age);
    }

    /**
     * Get users by age group
     */
    public List<User> getUsersByAgeGroup(AgeGroup ageGroup) {
        return userRepository.findByAgeGroup(ageGroup);
    }

    /**
     * Get users within age range
     */
    public List<User> getUsersByAgeRange(int minAge, int maxAge) {
        return userRepository.findByAgeBetween(minAge, maxAge);
    }

    /**
     * Get users by age group and role
     */
    public List<User> getUsersByAgeGroupAndRole(AgeGroup ageGroup, Role role) {
        return userRepository.findByAgeGroupAndRole(ageGroup, role);
    }

    /**
     * Get users by age group within a family
     */
    public List<User> getUsersByAgeGroupInFamily(AgeGroup ageGroup, Long familyId) {
        return userRepository.findByAgeGroupAndFamilyId(ageGroup, familyId);
    }

    /**
     * Get users by age within a family
     */
    public List<User> getUsersByAgeInFamily(int age, Long familyId) {
        return userRepository.findByAgeAndFamilyId(age, familyId);
    }

    /**
     * Get users older than specific age
     */
    public List<User> getUsersOlderThan(int age) {
        return userRepository.findByAgeGreaterThan(age);
    }

    /**
     * Get users younger than specific age
     */
    public List<User> getUsersYoungerThan(int age) {
        return userRepository.findByAgeLessThan(age);
    }

    /**
     * Get top performers by age group (ordered by coins)
     */
    public List<User> getTopPerformersByAgeGroup(AgeGroup ageGroup) {
        return userRepository.findTopPerformersByAgeGroup(ageGroup);
    }

    /**
     * Get users by age group with minimum streak
     */
    public List<User> getUsersByAgeGroupWithMinimumStreak(AgeGroup ageGroup, int minStreak) {
        return userRepository.findByAgeGroupWithStreakGreaterThan(ageGroup, minStreak);
    }

    /**
     * Count users by age group
     */
    public long countUsersByAgeGroup(AgeGroup ageGroup) {
        return userRepository.countByAgeGroup(ageGroup);
    }

    /**
     * Get all available age groups in the system
     */
    public List<AgeGroup> getAvailableAgeGroups() {
        return userRepository.findDistinctAgeGroups();
    }

    /**
     * Get users by multiple age groups
     */
    public List<User> getUsersByMultipleAgeGroups(List<AgeGroup> ageGroups) {
        return userRepository.findByAgeGroupIn(ageGroups);
    }

    /**
     * Get leaderboard by age group (top 10 by coins)
     */
    public List<User> getLeaderboardByAgeGroupByCoins(AgeGroup ageGroup) {
        return userRepository.findTop10ByAgeGroupOrderByCoinsDesc(ageGroup);
    }

    /**
     * Get leaderboard by age group (top 10 by streak)
     */
    public List<User> getLeaderboardByAgeGroupByStreak(AgeGroup ageGroup) {
        return userRepository.findTop10ByAgeGroupOrderByHighestStreakDesc(ageGroup);
    }

    /**
     * Update user ages for all users (maintenance method)
     */
    @Transactional
    public void updateAllUserAges() {
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            if (user.getDateOfBirth() != null) {
                // The @PreUpdate lifecycle method will automatically update age and ageGroup
                userRepository.save(user);
            }
        }
    }

    public OtpResponse generateOtp(OtpRequest request) {
        String key;

        // Determine identifier
        if ("MOBILE".equalsIgnoreCase(request.getIdentifierType())) {
            if (request.getPhone() == null) {
                return new OtpResponse("Mobile number is required for MOBILE identifier", false);
            }

            // 🚫 Already registered check
            if (userRepository.findByPhone(request.getPhone()).isPresent()) {
                throw new IllegalArgumentException("Phone number is already registered.");
            }

            key = request.getPhone();

        } else if ("EMAIL".equalsIgnoreCase(request.getIdentifierType())) {
            if (request.getEmail() == null) {
                return new OtpResponse("Email is required for EMAIL identifier", false);
            }

            // Check if user already exists
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new BusinessException("Email is already registered.");
            }

            key = request.getEmail();

        } else {
            return new OtpResponse("Identifier type must be either MOBILE or EMAIL", false);
        }

        // ✅ Generate and store OTP
        String otp = OtpUtil.generateOtp(4);
        otpStorage.put(key, new OtpEntry(otp, LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES)));

        // TODO: integrate with SMS/email sending
        return new OtpResponse("OTP generated successfully for " + key + " : " + otp, true);
    }

    public OtpResponse verifyOtp(OtpVerifyRequest request) {
        String key = switch (request.getIdentifierType().toUpperCase()) {
            case "MOBILE" -> request.getMobile();
            case "EMAIL" -> request.getEmail();
            default -> null;
        };

        if (key == null) {
            return new OtpResponse("Invalid identifier type", false);
        }

        OtpEntry entry = otpStorage.get(key);
        if (entry == null) {
            return new OtpResponse("No OTP found or OTP expired", false);
        }

        if (entry.expiry.isBefore(LocalDateTime.now())) {
            otpStorage.remove(key);
            return new OtpResponse("OTP expired", false);
        }

        if (!entry.otp.equals(request.getOtp())) {
            return new OtpResponse("Invalid OTP", false);
        }

        // OTP verified successfully
        otpStorage.remove(key);
        return new OtpResponse("OTP verified successfully", true);
    }

    public String checkAlreadyRegisteredUser(String identifier) {
        Optional<User> user;
        if (identifier.contains("@")) {
            user = userRepository.findByEmail(identifier);
        } else {
            user = userRepository.findByPhone(identifier);
        }
        if (user.isPresent()) {
            return "User already registered!";
        }
        return "User not found!";
    }

    @Transactional
    public ResponseEntity<?> registerUser(RegisterRequest request) {
        try {
            // 3️⃣ --- Create and Populate User ---
            User user = new User();
            Family family = new Family();

            // ✅ Basic Validations
            if (request.getName() == null || request.getName().isEmpty()) {
                throw new BusinessException("Name is required.");
            }
            if ((request.getEmail() == null || request.getEmail().isBlank()) &&
                    (request.getPhone() == null || request.getPhone().isBlank())) {
                throw new BusinessException("Either email or phone must be provided.");
            }
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                throw new BusinessException("Password is required.");
            }
            if (request.getGender() == null || request.getGender().isEmpty()) {
                throw new BusinessException("Gender is required.");
            }
            if (request.getDateOfBirth() == null) {
                throw new BusinessException("Date of birth is required.");
            }
            if (request.getRelationship() == null || request.getRelationship().isEmpty()) {
                throw new BusinessException("Relationship is required (CHILD, PARENT, SELF).");
            }

            // ✅ Check for existing user by email OR phone
            boolean userExists = false;

            if (request.getEmail() != null && !request.getEmail().isBlank()) {
                userExists = userRepository.findByEmail(request.getEmail()).isPresent();
            }

            if (!userExists && request.getPhone() != null && !request.getPhone().isBlank()) {
                userExists = userRepository.findByPhone(request.getPhone()).isPresent();
            }

            if (userExists) {
                throw new BusinessException("User already exists with the provided email or phone.");
            }



            Relationship relationship = Relationship.valueOf(request.getRelationship().toUpperCase());

            // 2️⃣ --- Family Code Logic Based on Relationship ---
            String familyCode = request.getFamilyCode();

            user.setName(request.getName());
            user.setGender(request.getGender());
            user.setRelationship(relationship);
            user.setDateOfBirth(request.getDateOfBirth());
            user.setEmail(request.getEmail());
            user.setPhone(request.getPhone().isEmpty() ? null : request.getPhone());
            user.setCreatedDateTime(LocalDateTime.now());


            //Referral Code
            User referrer = null;
            if (request.getReferralCode() != null && !(request.getReferralCode().isBlank())) {
                Optional<User> referrerOpt = userRepository.findByReferralCode(request.getReferralCode().trim());
                if (referrerOpt.isPresent()) {
                    referrer = referrerOpt.get();
                    referrer.setCoins(referrer.getCoins() + 100);
                    user.setCoins(user.getCoins() + 100); // reward new user also
                    userRepository.save(referrer);

                    // Transaction for referrer
                    CoinTransaction referrerTx = CoinTransaction.builder()
                            .user(referrer)
                            .habit(null)
                            .task(null)
                            .amount(100)
                            .type("REFERRAL_BONUS")
                            .timestamp(java.time.LocalDateTime.now())
                            .description("Referral bonus: invited new user " + user.getName())
                            .build();
                    coinTransactionRepository.save(referrerTx);
                } else {
                    throw new BusinessException("Invalid referral code.");
                }
                // Do NOT set referral code on 'user' object
            }

            // Save new user
            User savedUser = userRepository.save(user);

            // Transaction for new user, after saved and id is available
            if (referrer != null) {
                CoinTransaction newUserTx = CoinTransaction.builder()
                        .user(savedUser)
                        .habit(null)
                        .task(null)
                        .amount(100)
                        .type("REFERRAL_BONUS")
                        .timestamp(java.time.LocalDateTime.now())
                        .description("Referral bonus: used referral code of " + referrer.getName())
                        .build();
                coinTransactionRepository.save(newUserTx);
            }


            // 4️⃣ --- Set Pet if Exists ---
            if (request.getPetName() != null) {
                Pet pet = petRepository.findByPetName(request.getPetName())
                        .orElseThrow(() -> new ResourceNotFoundException("Pet not found."));
                user.setPet(pet);
            }

            // 5️⃣ --- Set Avatar if Exists ---
            // ---------------- AVATAR AUTO-CREATION ----------------
            Avatar avatar = new Avatar();
            avatar.setAvatarName(avatarService.generateUniqueAvatarName(user.getName()));
            avatar.setAvatarImageName(request.getAvatarImageName());
            avatar.setGender(user.getGender());
            avatar = avatarRepository.save(avatar);

            user.setAvatar(avatar);

            if (relationship == Relationship.CHILD) {

                // Family code is mandatory for CHILD
                if (request.getFamilyCode() == null || request.getFamilyCode().isBlank()) {
                    throw new BusinessException("Family code is mandatory for child registration.");
                }

                Optional<Family> optionalFamily = familyRepository.findByFamilyCode(familyCode);

                if (optionalFamily.isPresent()) {

                    Family existingFamily = optionalFamily.get();

                    long childCount = familyMemberRepository
                            .countByRelationship(existingFamily, Relationship.CHILD);

                    if (childCount >= 2) {
                        throw new BusinessException(
                                "This family already has the maximum number of children (2)."
                        );
                    }

                    user.setFamily(existingFamily);

                } } else if (relationship == Relationship.PARENT) {
                // ✅ Since familyCode is always provided for parent
                if (request.getFamilyCode() == null || request.getFamilyCode().isBlank()) {
                    throw new BusinessException("Family code is required for parent registration.");
                }

                Optional<Family> optionalFamily = familyRepository.findByFamilyCode(familyCode);
                if (optionalFamily.isPresent()) {
                    // 🟩 Co-parent joining existing family
                    Family existingFamily = optionalFamily.get();

                    long parentCount = familyMemberRepository.countByRelationship(existingFamily, Relationship.PARENT);
                    if (parentCount >= 2) {
                        throw new BusinessException("This family already has the maximum number of parents (2).");
                    }

                    user.setFamily(existingFamily);

                } else {
                    // 🟦 Fresh parent creating new family using provided code
                    user = userRepository.save(user); // ensure ID available for createdBy

                    Family newFamily = new Family();
                    newFamily.setFamilyName(
                            request.getFamilyName() != null
                                    ? request.getFamilyName()
                                    : user.getName() + "'s Family"
                    );
                    newFamily.setFamilyCode(familyCode);
                    newFamily.setCreatedBy(user.getId());
                    newFamily = familyRepository.save(newFamily);

                    user.setFamily(newFamily);
                }
            } else if (relationship == Relationship.SELF) {
                user.setFamily(null);
            }else {
                throw new BusinessException("Invalid relationship type provided.");
            }



            savedUser = userRepository.save(user);

            UserPassword userPassword = new UserPassword();
            userPassword.setUser(savedUser);
            userPassword.setPassword(passwordEncoder.encode(request.getPassword()));
            userPasswordRepository.save(userPassword);

            // 🧩 Step 7: Add Family Member Record
            if (user.getFamily() != null) {
                FamilyMember member = new FamilyMember();
                member.setFamily(user.getFamily());
                member.setUser(savedUser);
                member.setDateOfBirth(user.getDateOfBirth());
                familyMemberRepository.save(member);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(mapUserToDto(savedUser));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getMessage());  }
    }



    public static UserResponseDto mapUserToDto(User user) {
        if (user == null) return null;

        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .coins(user.getCoins())
                .currentStreak(user.getCurrentStreak())
                .highestStreak(user.getHighestStreak())
                .phone(user.getPhone())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .age(user.getAge())
                .ageGroup(user.getAgeGroup() != null ? user.getAgeGroup() : null)
                .avatarName(user.getAvatar() != null ? user.getAvatar().getAvatarName() : null)
                .avatarImageName(user.getAvatar() != null ? user.getAvatar().getAvatarImageName() : null)
                .petName(user.getPet() != null ? user.getPet().getPetName() : null)
                .relationship(user.getRelationship() != null ? user.getRelationship() : null)
                .createdDateTime(user.getCreatedDateTime())
                .familyCode(user.getFamily() != null ? user.getFamily().getFamilyCode() : null)
                .familyName(user.getFamily() != null ? user.getFamily().getFamilyName() : null)
                .referralCode(user.getReferralCode())
                .build();
    }

    private FamilyResponseDTO mapFamilyToDTO(Family family) {
        return new FamilyResponseDTO(
                family.getId(),
                family.getFamilyCode(),
                family.getFamilyName(),
                family.getFamilyMembers().stream()
                        .map(fm -> new FamilyResponseDTO.MemberDTO(
                                fm.getId(),
                                fm.getUser().getName(),
                                fm.getUser().getRelationship() != null ? fm.getUser().getRelationship().name() : null,
                                fm.getDateOfBirth()))
                        .collect(Collectors.toList()));
    }

    //Referral Code
    public String generateReferralCode(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getReferralCode() != null) return user.getReferralCode();

        String code;
        do {
            code = ReferralCodeGenerator.randomAlphaNumeric(8);
        } while (userRepository.existsByReferralCode(code));
        user.setReferralCode(code);
        userRepository.save(user);
        return code;
    }


    public UserResponseDto updateProfile(Long userId, EditProfileRequest req) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // ────────────────────────────────────
        // 1️⃣ Check if email already exists
        // ────────────────────────────────────
        if (req.getEmail() != null && !req.getEmail().equals(user.getEmail())) {

            boolean emailExists = userRepository.existsByEmail(req.getEmail());

            if (emailExists) {
                throw new BusinessException("Email is already registered");
            }

            user.setEmail(req.getEmail());
        }

        // ────────────────────────────────────
        // 2️⃣ Check if phone already exists
        // ────────────────────────────────────
        if (req.getPhone() != null && !req.getPhone().equals(user.getPhone())) {

            boolean phoneExists = userRepository.existsByPhone(req.getPhone());

            if (phoneExists) {
                throw new BusinessException("Phone number is already registered");
            }

            user.setPhone(req.getPhone());
        }

        // ────────────────────────────────────
        // 3️⃣ Update basic details
        // ────────────────────────────────────
        if (req.getName() != null) user.setName(req.getName());
        if (req.getGender() != null) user.setGender(req.getGender());

        // ────────────────────────────────────
        // 4️⃣ Update DOB + Auto age & ageGroup
        // ────────────────────────────────────
        if (req.getDateOfBirth() != null) {
            user.setDateOfBirth(req.getDateOfBirth());

            int age = Period.between(req.getDateOfBirth(), LocalDate.now()).getYears();
            user.setAge(age);
            user.setAgeGroup(AgeGroup.fromAge(age));
        }

        // ────────────────────────────────────
        // 5️⃣ Update avatar if provided
        // ────────────────────────────────────
        if (req.getAvatarName() != null && req.getAvatarImageName()!=null) {
            Avatar avatar = avatarService.renameAvatar(user.getId(), req.getAvatarName(),req.getAvatarImageName());
            user.setAvatar(avatar);
        }

        return mapUserToDto(userRepository.save(user));
    }


    public void changePassword(Long userId, ChangePasswordRequest req) {

        // Fetch user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Fetch existing password record for this user
        UserPassword userPassword = userPasswordRepository
                .findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Password record not found for user"));

        // Validate current password
        if (!passwordEncoder.matches(req.getCurrentPassword(), userPassword.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }

        // Validate new + confirm password
        if (!req.getNewPassword().equals(req.getRetypeNewPassword())) {
            throw new BusinessException("New passwords do not match");
        }

        // Update password (same row)
        userPassword.setPassword(passwordEncoder.encode(req.getNewPassword()));

        userPasswordRepository.save(userPassword); // update existing row
    }

    @Transactional(readOnly = true)
    public UserFamilyDetailsResponse getFamilyDetails(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // SELF MODE
        if (user.getFamily() == null) {
            return UserFamilyDetailsResponse.builder()
                    .mode("SELF")
                    .selfMode(SelfModeDTO.builder()
                            .title("You're now in Self mode!")
                            .message("The family connection has been removed by the creator. Your account is still safe and all data is saved.")
                            .canContinueWithNewRole(true)
                            .build())
                    .build();
        }

        // FAMILY MODE
        Family family = user.getFamily();

        return UserFamilyDetailsResponse.builder()
                .mode("FAMILY")
                .family(mapToFamilyDTO(family, user))
                .build();
    }

    private FamilyDetailsDTO mapToFamilyDTO(Family family, User loggedInUser) {

        User creator = getUserById(family.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("Creator not found"));

        return FamilyDetailsDTO.builder()
                .familyId(family.getId())
                .familyName(family.getFamilyName())
                .familyCode(family.getFamilyCode())
                .status("ACTIVE")
                .createdOn(family.getCreatedAt())
                // Creator is always PARENT
                .creator(mapMember(creator, loggedInUser))
                .members(
                        family.getMembers().stream()
                                // ❌ Exclude creator from members list
                                .filter(member -> !member.getId().equals(creator.getId()))
                                // ✅ Convert extra parents to CO_PARENT
                                .map(member -> mapMemberWithCoParentRule(member, loggedInUser))
                                .toList()
                )
                .build();
    }

    private FamilyMemberDetailsDTO mapMemberWithCoParentRule(
            User member,
            User loggedInUser) {

        FamilyMemberDetailsDTO dto = mapMember(member, loggedInUser);

        // ✅ Any additional PARENT (not creator) becomes CO_PARENT
        if (dto.getRelationship() == "Parent") {
            dto.setRelationship("Co-Parent");
        }

        return dto;
    }


    private FamilyMemberDetailsDTO mapMember(User user, User loggedInUser) {

        return FamilyMemberDetailsDTO.builder()
                .userId(user.getId())
                .name(user.getName())
                .avatarName(
                        user.getAvatar() != null
                                ? user.getAvatar().getAvatarName()
                                : null
                )
                .avatarImageName(
                        user.getAvatar() != null
                                ? user.getAvatar().getAvatarImageName()
                                : null
                )
                .relationship(user.getRelationship().getDisplayName())
                .age(user.getAge())
                .isYou(user.getId().equals(loggedInUser.getId()))
                .build();
    }

    public void convertSelfToParent(Long userId,String familyName, String familyCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRelationship() != Relationship.SELF) {
            throw new BusinessException("Only users in SELF mode can convert to PARENT.");
        }

        // Create new family
        Family newFamily = new Family();
        newFamily.setFamilyName(familyName != null ? familyName : user.getName() + "'s Family");
        newFamily.setFamilyCode(familyCode);
        newFamily.setCreatedBy(user.getId());
        newFamily = familyRepository.save(newFamily);

        // Update user
        user.setFamily(newFamily);
        user.setRelationship(Relationship.PARENT);
        userRepository.save(user);

        // Add family member record
        FamilyMember fm = new FamilyMember();
        fm.setFamily(newFamily);
        fm.setUser(user);
        fm.setDateOfBirth(user.getDateOfBirth());
        familyMemberRepository.save(fm);
    }

    public boolean existsReferralCode(String referralCode) {
        if (referralCode == null || referralCode.trim().isEmpty()) {
            return false;
        }
        return userRepository.existsByReferralCodeIgnoreCase(referralCode.trim());
    }



    private record OtpEntry(String otp, LocalDateTime expiry) {}

    public User getUserFromPrincipal(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        try {
            // Principal name should be the user ID (as seen in logs: Principal: 63)
            Long userId = Long.valueOf(principal.getName());
            return userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with ID: " + userId));
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user ID format in principal");
        }
    }


    public StreakStatusDto getStreakStatus(User user) {

        ZoneId zone = ZoneId.of("Asia/Kolkata");
        LocalDate today = LocalDate.now(zone);

        boolean canRestore = false;
        LocalDate deadline = null;
        Integer restoreTo = null;

        LocalDate missedDate = user.getLastMissedStreakDate();
        Integer lastStreak = user.getLastStreakCount();

        // Restore allowed ONLY within one day
        if (missedDate != null && lastStreak != null) {

            LocalDate restoreDay = missedDate.plusDays(2);

            if (today.equals(restoreDay)) {
                canRestore = true;
                deadline = restoreDay;
                restoreTo = lastStreak + 1;
            }
        }

        return new StreakStatusDto(
                user.getCurrentStreak(),
                user.getHighestStreak(),
                canRestore,
                deadline,
                restoreTo,
                5 // restore cost
        );
    }

}
