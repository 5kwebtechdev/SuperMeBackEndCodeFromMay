package com.superme.service;

import com.superme.dto.OAuth2LoginResponse;
import com.superme.dto.OAuth2UserInfo;
import com.superme.model.User;
import com.superme.repository.UserRepository;
import com.superme.util.UserJwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2Service {

    private final UserRepository userRepository;

    @Transactional
    public OAuth2LoginResponse processOAuth2Login(OAuth2UserInfo userInfo) {
        log.info("Processing OAuth2 login for: {}", userInfo.getEmail());
        
        // Check if user exists by email
        User existingUser = userRepository.findByEmail(userInfo.getEmail()).orElse(null);
        
        boolean isNewUser = false;
        User user;
        
        if (existingUser != null) {
            user = existingUser;
            // Update OAuth2 info if not already set
            if (user.getOauth2ProviderId() == null) {
                user.setOauth2ProviderId(userInfo.getProviderId());
                user.setOauth2ProviderType(userInfo.getProviderType());
                user.setOauth2AvatarUrl(userInfo.getAvatarUrl());
                user = userRepository.save(user);
            }
            log.info("Existing user logged in via OAuth2: {}", user.getId());
        } else {
            // Create new user with OAuth2 info
            user = new User();
            user.setName(userInfo.getName() != null ? userInfo.getName() : userInfo.getEmail().split("@")[0]);
            user.setEmail(userInfo.getEmail());
            user.setOauth2ProviderId(userInfo.getProviderId());
            user.setOauth2ProviderType(userInfo.getProviderType());
            user.setOauth2AvatarUrl(userInfo.getAvatarUrl());
            user.setEnabled(true);
            user.setEmailVerified(true); // OAuth2 emails are verified
            user.setCreatedDateTime(java.time.LocalDateTime.now());
            
            // Required fields with defaults
            user.setGender("Not specified");
            user.setDateOfBirth(java.time.LocalDate.now().minusYears(18));
            user.setRelationship(com.superme.enums.Relationship.SELF);
            user.setRole(com.superme.enums.Role.USER);
            
            user = userRepository.save(user);
            isNewUser = true;
            log.info("New user created via OAuth2: {}", user.getId());
        }
        
        // Generate JWT token using existing UserJwtUtil
        String token = UserJwtUtil.generateToken(user);
        
        return new OAuth2LoginResponse(
            token,
            user.getId(),
            user.getEmail(),
            user.getName(),
            isNewUser
        );
    }
}