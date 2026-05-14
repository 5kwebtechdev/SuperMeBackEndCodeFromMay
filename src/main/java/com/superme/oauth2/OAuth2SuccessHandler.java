package com.superme.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superme.dto.OAuth2LoginResponse;
import com.superme.dto.OAuth2UserInfo;
import com.superme.service.OAuth2Service;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2Service oauth2Service;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        
        log.info("OAuth2SuccessHandler - Processing successful authentication");
        
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        
        // Extract user info
        OAuth2UserInfo userInfo = extractUserInfo(oAuth2User, registrationId);
        
        // Process login
        OAuth2LoginResponse loginResponse = oauth2Service.processOAuth2Login(userInfo);
        
        // Return JSON response
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(), loginResponse);
    }
    
    private OAuth2UserInfo extractUserInfo(OAuth2User oAuth2User, String registrationId) {
        String providerType = registrationId.toUpperCase();
        String providerId = null;
        String email = null;
        String name = null;
        String avatarUrl = null;
        
        if ("google".equalsIgnoreCase(registrationId)) {
            providerId = oAuth2User.getAttribute("sub");
            email = oAuth2User.getAttribute("email");
            name = oAuth2User.getAttribute("name");
            avatarUrl = oAuth2User.getAttribute("picture");
        } else if ("github".equalsIgnoreCase(registrationId)) {
            providerId = String.valueOf(oAuth2User.getAttribute("id"));
            email = oAuth2User.getAttribute("email");
            name = oAuth2User.getAttribute("name");
            if (name == null) name = oAuth2User.getAttribute("login");
            avatarUrl = oAuth2User.getAttribute("avatar_url");
        }
        
        if (email == null) {
            email = providerId + "@" + registrationId.toLowerCase() + ".user";
        }
        
        if (name == null) {
            name = email.split("@")[0];
        }
        
        return OAuth2UserInfo.builder()
                .providerId(providerId)
                .providerType(providerType)
                .email(email)
                .name(name)
                .avatarUrl(avatarUrl)
                .build();
    }
}