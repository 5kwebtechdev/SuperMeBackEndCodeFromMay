//package com.superme.SocialLoginService;
//
////import com.codingshuttle.youtube.hospitalManagement.dto.*;
////import com.codingshuttle.youtube.hospitalManagement.entity.User;
////import com.codingshuttle.youtube.hospitalManagement.entity.type.AuthProviderType;
////import com.codingshuttle.youtube.hospitalManagement.entity.type.RoleType;
////import com.codingshuttle.youtube.hospitalManagement.repository.UserRepository;
////import com.codingshuttle.youtube.hospitalManagement.security.AuthUtil;
//import com.superme.SocialLoginDto.LoginRequestDto;
//import com.superme.SocialLoginDto.LoginResponseDto;
//import com.superme.repository.UserRepository;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.BadCredentialsException;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.oauth2.core.user.OAuth2User;
//import org.springframework.stereotype.Service;
//
//import java.util.Set;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class AuthService {
//
//    private final AuthenticationManager authenticationManager;
//    private final AuthUtil authUtil;
//    private final UserRepository userRepository;
//    private final PasswordEncoder passwordEncoder;
//
//    public LoginResponseDto login(LoginRequestDto loginRequestDto) {
//
//        Authentication authentication = authenticationManager.authenticate(
//                new UsernamePasswordAuthenticationToken(loginRequestDto.getUsername(), loginRequestDto.getPassword())
//        );
//
//        User user = (User) authentication.getPrincipal();
//
//        String token = authUtil.generateAccessToken(user);
//        UserDto userDto = new UserDto(
//                user.getId(),
//                user.getEmail(),
//                user.getUsername(),
//                user.getProviderId(),
//                user.getProviderType(),
//                user.getRoles(),
////                user.getEnabled(),
////                user.getEmailVerified(),
//                user.getImageUrl()
//        );
//        return new LoginResponseDto(token, user.getId(),userDto);
//    }
//
//    public User signUpInternal(SignUpRequestDto signupRequestDto, AuthProviderType authProviderType, String providerId) {
//        User user = userRepository.findByEmail(signupRequestDto.getUsername()).orElse(null);
//
//        if(user != null) throw new IllegalArgumentException("User already exists");
//
//        user = User.builder()
//                .email(signupRequestDto.getUsername())  // Set email
//                .username(signupRequestDto.getName())       // Set name
//                .providerId(providerId)
//                .providerType(authProviderType)
//                .roles(signupRequestDto.getRoles())
//                .build();
//
//        if(authProviderType == AuthProviderType.EMAIL) {
//            user.setPassword(passwordEncoder.encode(signupRequestDto.getPassword()));
//        }
//
//        log.info("Saving user: email={}, name={}, provider={}", user.getEmail(), user.getUsername(), user.getProviderType());
//        user = userRepository.save(user);
//        return user;
//    }
//
//    public SignupResponseDto signup(SignUpRequestDto signupRequestDto) {
//        User user = signUpInternal(signupRequestDto, AuthProviderType.EMAIL, null);
//        return new SignupResponseDto(user.getId(), user.getEmail());
//    }
//
//    @Transactional
//    public ResponseEntity<LoginResponseDto> handleOAuth2LoginRequest(OAuth2User oAuth2User, String registrationId) {
//        log.info("Handling OAuth2 login for provider: {}", registrationId);
//        log.info("OAuth2User attributes: {}", oAuth2User.getAttributes());
//
//        AuthProviderType providerType = authUtil.getProviderTypeFromRegistrationId(registrationId);
//        String providerId = authUtil.determineProviderIdFromOAuth2User(oAuth2User, registrationId);
//
//        // Extract email - try multiple possible attribute names
//        String email = oAuth2User.getAttribute("email");
//        if (email == null || email.isBlank()) {
//            email = oAuth2User.getAttribute("sub");  // Google uses 'sub' as unique identifier
//        }
//        if (email == null || email.isBlank()) {
//            email = providerId + "@" + registrationId.toLowerCase() + ".user";  // Fallback
//        }
//
//        // Extract name
//        String name = oAuth2User.getAttribute("name");
//        if (name == null || name.isBlank()) {
//            name = oAuth2User.getAttribute("given_name");
//        }
//        if (name == null || name.isBlank()) {
//            name = email.split("@")[0];
//        }
//
//        log.info("Extracted email: {}, name: {}, providerId: {}", email, name, providerId);
//
//        // Check if user exists by providerId and providerType
//        User user = userRepository.findByProviderIdAndProviderType(providerId, providerType).orElse(null);
//        log.info("Existing user found: {}", user != null ? "YES (ID: " + user.getId() + providerId+" = providerId" : "NO"+providerId+"providerId");
//        // Check if user exists by email
//        User emailUser = userRepository.findByEmail(email).orElse(null);
//
//        if (user == null && emailUser == null) {
//            // New user - create account
//            log.info("Creating new user for OAuth2 login");
//            SignUpRequestDto signUpRequest = new SignUpRequestDto();
//            signUpRequest.setUsername(email);
//            signUpRequest.setName(name);
//            signUpRequest.setRoles(Set.of(RoleType.USER));
//
//            user = signUpInternal(signUpRequest, providerType, providerId);
//
//        } else if (user != null) {
//            // Existing user by provider - update email if needed
//            log.info("Existing user found by provider");
//            if (email != null && !email.isBlank() && !email.equals(user.getEmail())) {
//                user.setEmail(email);
//                user = userRepository.save(user);
//            }
//        } else if (emailUser != null) {
//            // Email exists but with different provider
//            log.error("Email {} already registered with provider: {}", email, emailUser.getProviderType());
//            throw new BadCredentialsException("This email is already registered with " + emailUser.getProviderType());
//        }
//
////        LoginResponseDto loginResponseDto = new LoginResponseDto(authUtil.generateAccessToken(user), user.getId());
//
//        UserDto userDto = new UserDto(
//                user.getId(),
//                user.getEmail(),
//                user.getUsername(),
//                user.getProviderId(),
//                user.getProviderType(),
//                user.getRoles(),
////                user.getEnabled(),
////                user.getEmailVerified(),
//                user.getImageUrl()
//        );
//
//        LoginResponseDto loginResponseDto = new LoginResponseDto(
//                authUtil.generateAccessToken(user),
//                user.getId(),
//                userDto
//        );
//
//
//
//        return ResponseEntity.ok(loginResponseDto);
//    }
//}