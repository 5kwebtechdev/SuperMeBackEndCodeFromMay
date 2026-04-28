package com.superme.service;

import com.superme.exception.BusinessException;
import com.superme.exception.ResourceNotFoundException;
import com.superme.model.Avatar;
import com.superme.model.User;
import com.superme.repository.AvatarRepository;
import com.superme.repository.UserRepository;
import com.superme.util.AvatarNameGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AvatarService {

    private final AvatarRepository avatarRepository;

    private final UserRepository userRepository;
    private static final Random RANDOM = new Random();

    /* =================================================
       1. CREATE AVATAR (Registration)
       ================================================= */
    public Avatar createAvatarForUser(String userName, String gender, String url) {

        String avatarName = generateUniqueAvatarName(userName);

        Avatar avatar = new Avatar();
        avatar.setAvatarName(avatarName);
        avatar.setGender(gender);
        avatar.setUrl(url);

        return avatarRepository.save(avatar);
    }

    /* =================================================
       2. RENAME AVATAR (ONLY ONCE)
       ================================================= */
    public Avatar renameAvatar(Long userId, String newName, String avatarImageName) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Avatar avatar = user.getAvatar();

        if (avatar == null) {
            throw new BusinessException("Avatar not assigned");
        }

        if (avatar.isRenamedByUser() && !avatar.getAvatarName().equalsIgnoreCase(newName)) {
            throw new BusinessException("Avatar name can be changed only once");
        }

        String validated = validateUserAvatarName(newName,userId);

        avatar.setAvatarName(validated);
        avatar.setAvatarImageName(avatarImageName);
        avatar.setRenamedByUser(true);

        return avatarRepository.save(avatar);
    }

    /* =================================================
       3. SUGGEST NAMES WHILE TYPING
       ================================================= */
    public List<String> suggestAvatarNames(String input) {

        String base = normalize(input);

        if (base.length() < 3) {
            return List.of();
        }

        Set<String> suggestions = new LinkedHashSet<>();

        while (suggestions.size() < 5) {

            String candidate = generateSuggestion(base);

            if (isValid(candidate)
                    && !avatarRepository.existsByAvatarNameIgnoreCase(candidate)) {
                suggestions.add(candidate);
            }
        }

        return new ArrayList<>(suggestions);
    }

    /* =================================================
       HELPERS
       ================================================= */

    public String generateUniqueAvatarName(String userName) {

        String name;
        int attempts = 0;

        do {
            name = AvatarNameGenerator.generate(userName);
            attempts++;
        } while (avatarRepository.existsByAvatarNameIgnoreCase(name) && attempts < 15);

        if (attempts == 15) {
            throw new BusinessException("Unable to generate unique avatar name");
        }

        return name;
    }

    // In service - no repository changes needed
    private String validateUserAvatarName(String name, Long currentUserId) {
        String cleaned = normalize(name);

        if (cleaned.length() < 5 || cleaned.length() > 12) {
            throw new BusinessException("Avatar name must be 5–12 characters");
        }

        // 1. If avatar name doesn't exist → ALLOW (new name)
        if (!avatarRepository.existsByAvatarNameIgnoreCase(cleaned)) {
            return cleaned;
        }

        // 2. Avatar exists → check if it's current user's current avatar
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException("User not found"));

        // 3. If current user's avatar matches → ALLOW (keeping same name)
        if (currentUser.getAvatar().getAvatarName().equalsIgnoreCase(cleaned)) {
            return cleaned;
        }

        // 4. Different name that already exists → BLOCK
        throw new BusinessException("Avatar name already taken by another user");
    }




    private String normalize(String input) {
        return input.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9_]", "");
    }

    private boolean isValid(String name) {
        return name.length() >= 5 && name.length() <= 12;
    }

    private String generateSuggestion(String base) {

        int maxDigits = 12 - base.length();
        int digits = Math.max(1, Math.min(3, maxDigits));

        String number = RANDOM.ints(digits, 0, 10)
                .collect(StringBuilder::new,
                        StringBuilder::append,
                        StringBuilder::append)
                .toString();

        return base + number;
    }
}

