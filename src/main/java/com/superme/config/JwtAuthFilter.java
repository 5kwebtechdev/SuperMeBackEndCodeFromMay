package com.superme.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superme.controller.LoginController;
import com.superme.exception.ErrorResponse;
import com.superme.service.LogOutService;
import com.superme.service.UserService;
import com.superme.util.UserJwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    // ✅ UPDATED: Enhanced JWT configuration with PBKDF2 key derivation
    private static final String USER_PASSPHRASE = System.getenv().getOrDefault(
            "JWT_SECRET",
            "mindfull777mindfull777mindfull777!!");
    private static final String ADMIN_PASSPHRASE = System.getenv().getOrDefault(
            "JWT_ADMIN_SECRET",
            "MindfullPhase1SecretKeyForJWTTokenGenerationAndValidationProcess2024!@#$%");

    private static final int ITERATIONS = 150_000;
    private static final int KEY_LENGTH = 512;
    private static final byte[] USER_SALT = "UserSaltForMindfull123!".getBytes(StandardCharsets.UTF_8);
    private static final byte[] ADMIN_SALT = "AdminSaltForMindfull123!".getBytes(StandardCharsets.UTF_8);
    private final HttpServletRequest httpServletRequest;
    private final LogOutService logOutService;

    public JwtAuthFilter(HttpServletRequest httpServletRequest,LogOutService logOutService) {
        this.httpServletRequest = httpServletRequest;
        this.logOutService = logOutService;
    }

    // ✅ UPDATED: Key derivation method using PBKDF2
    private static byte[] deriveKey(char[] passphrase, byte[] salt, int iterations, int keyLength) throws Exception {
        KeySpec spec = new PBEKeySpec(passphrase, salt, iterations, keyLength);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return factory.generateSecret(spec).getEncoded();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Skip filter for ALL OPTIONS requests (CORS preflight) so they are always
        // handled by Spring's CORS config
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();

        List<String> publicEndpoints = Arrays.asList(

                "/v1/api/public/oauth2-login",
                "/v1/api/public/oauth2-login/**",


                "/oauth2/**",           // ADD THIS LINE
                "/login/oauth2/**",     // ADD THIS LINE
                "/v1/auth/login",
                "/v1/journal/download/image/",
                "/v1/auth/oauth2/mobile-login",
                "/v1/auth/oauth2/mobile-login/",
                "/v1/auth/oauth2/mobile-login/**",
                "/v1/challenges/download/",
                "/v1/articles/download/",
                "/v1/admin/articles/thumbnail/",
                "/v1/admin/articles/content/",
                "/v1/admin/articles/attachment/",
                "/v1/tutors/download/",
                "/v1/courses/download/",
                "/v1/delete-account",
                "/v1/delete-request",
                  "/v1/auth/check",
                "/v1/auth/register",
                "/v1/auth/forgot-password",
                "/v1/auth/verify-email",
                "/v1/admin/auth/login",
                "/v1/v1/admin/auth/login",
                "/v1/auth/leaderboard/streaks",
                "/v1/auth/users-by-role",
                "/v1/auth/users-logged-in-today",
                "/v1/admin/auth/register",
                // "/v1/auth/logout",
                // "/v1/auth/delete-account",
                "/v1/family/create",
                "/v1/family/join",
                "/v1/family/add-member",
                "/v1/family/check-family-code",
                // "/v1/family/members",
                "/error",
                "/favicon.ico",
                "/v1/auth/register/verify-otp",
                "v1/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/v1/faq",
                "/v1/ws/**",
                "/**",
                "/*.html");

        return publicEndpoints.stream().anyMatch(uri::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        // ✅ 1️⃣ Bypass Swagger & Public Endpoints
        if (uri.startsWith("/v1/ws")
                        ||uri.startsWith("/swagger-ui")
                || uri.startsWith("/v1/v3/api-docs")
                || uri.startsWith("/swagger-resources")
                || uri.startsWith("/webjars")
                || uri.equals("/swagger-ui.html")) {

            // Allow Swagger and public routes without JWT
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Authorization token is missing or malformed");
            return;
        }
        String token = header.substring(7);

        try {
            Claims claims = null;
            String userRole = "USER";
            String userId = null;
            String email = null;
//            boolean isAdminEndpoint = request.getRequestURI().startsWith("/v1/admin/");
            boolean isAdminEndpoint =
                    uri.startsWith("/v1/admin/");

            boolean isCommonEndpoint = COMMON_ENDPOINTS.stream()
                    .anyMatch(uri::startsWith);
            if (httpServletRequest.getRequestURI().endsWith("/logout")) {

                System.out.println("Logout API called: " + httpServletRequest.getRequestURI());

                  token = httpServletRequest.getHeader("Authorization");

                if (token != null && token.startsWith("Bearer ")) {

                    String jwt = token.replace("Bearer ", "");
                    Long userId2 = null;

                     try {
                        userId2 = UserJwtUtil.getUserIdFromToken(jwt);

                    } catch (Exception userEx) {


                         try {
                            userId2 = UserJwtUtil.getUserIdFromAdminToken(jwt);

                        } catch (Exception adminEx) {


                            sendErrorResponse(request, response,
                                    HttpServletResponse.SC_UNAUTHORIZED,
                                    "Invalid token");

                            return;
                        }
                    }

                     logOutService.logout(userId2, token);

                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"Logged out successfully\"}");

                    return; // 🔥 STOP CHAIN
                }
            }
            else if (isAdminEndpoint ) {
                // ✅ UPDATED: Admin endpoint - parse with admin secret using PBKDF2
                try {
                    byte[] adminKeyBytes = deriveKey(ADMIN_PASSPHRASE.toCharArray(), ADMIN_SALT, ITERATIONS,
                            KEY_LENGTH);
                    SecretKey adminKey = Keys.hmacShaKeyFor(adminKeyBytes);

                    claims = Jwts.parserBuilder()
                            .setSigningKey(adminKey)
                            .build()
                            .parseClaimsJws(token)
                            .getBody();

                    email = claims.getSubject();
                    userRole = claims.get("role", String.class);

                    if (userRole != null) {
                        userRole = userRole.toUpperCase();
                    } else {
                        userRole = "ADMIN";
                    }

                    userId = email;

                    System.out.println("=== Admin JWT Token Parsed (PBKDF2) ===");
                    System.out.println("Email: " + email);
                    System.out.println("Role: " + userRole);

                } catch (ExpiredJwtException e) {
                    sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Admin token expired");
                    return;
                } catch (Exception e) {
                    sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid admin token");
                    return;
                }
            } else if (isCommonEndpoint) {

                // 🔥 TRY ADMIN TOKEN FIRST
                try {
                    byte[] adminKeyBytes = deriveKey(ADMIN_PASSPHRASE.toCharArray(), ADMIN_SALT, ITERATIONS, KEY_LENGTH);
                    SecretKey adminKey = Keys.hmacShaKeyFor(adminKeyBytes);

                    claims = Jwts.parserBuilder()
                            .setSigningKey(adminKey)
                            .build()
                            .parseClaimsJws(token)
                            .getBody();

                    userId = claims.getSubject();
                    userRole = claims.get("role", String.class);

                    userRole = userRole != null ? userRole.toUpperCase() : "ADMIN";

                    System.out.println("✅ Authenticated as ADMIN (common endpoint)");

                } catch (Exception adminEx) {

                    // 🔥 FALLBACK → TRY USER TOKEN
                    try {
                        byte[] userKeyBytes = deriveKey(USER_PASSPHRASE.toCharArray(), USER_SALT, ITERATIONS, KEY_LENGTH);
                        SecretKey userKey = Keys.hmacShaKeyFor(userKeyBytes);

                        claims = Jwts.parserBuilder()
                                .setSigningKey(userKey)
                                .build()
                                .parseClaimsJws(token)
                                .getBody();

                        userId = claims.getSubject();
                        userRole = claims.get("role", String.class);

                        userRole = userRole != null ? userRole.toUpperCase() : "USER";

                        System.out.println("✅ Authenticated as USER (common endpoint)");

                    } catch (Exception userEx) {
                        sendErrorResponse(request, response,
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "Invalid token (neither admin nor user)");
                        return;
                    }
                }
            }
            else {
                // ✅ UPDATED: User endpoint - parse with user secret using PBKDF2
                try {
                    byte[] userKeyBytes = deriveKey(USER_PASSPHRASE.toCharArray(), USER_SALT, ITERATIONS,
                            KEY_LENGTH);
                    SecretKey userKey = Keys.hmacShaKeyFor(userKeyBytes);

                    claims = Jwts.parserBuilder()
                            .setSigningKey(userKey)
                            .build()
                            .parseClaimsJws(token)
                            .getBody();

                    userId = claims.getSubject();
                    String name = claims.get("name", String.class);
                    String gender = claims.get("gender", String.class);
                    Object dateOfBirthObj = claims.get("dateOfBirth");
                    String dateOfBirth = dateOfBirthObj != null ? dateOfBirthObj.toString() : null;

                    // ✅ EXTRACT: Only the claims you actually use (NO USERNAME)
                    email = claims.get("email", String.class);
                    String phone = claims.get("phone", String.class);
                    String role = claims.get("role", String.class);
                    Boolean enabled = claims.get("enabled", Boolean.class);
                    Integer coins = claims.get("coins", Integer.class);
                    String relationship = claims.get("relationship", String.class);

                    userRole = role != null ? role.toUpperCase() : "USER";

                    System.out.println("=== User JWT Token Parsed (PBKDF2) ===");
                    System.out.println("User ID: " + userId);
                    System.out.println("Name: " + name);
                    System.out.println("Email: " + email);
                    System.out.println("Phone: " + phone);
                    System.out.println("Gender: " + gender);
                    System.out.println("Date of Birth: " + dateOfBirth);
                    System.out.println("Relationship: " + relationship);
                    System.out.println("Role: " + userRole);
                    System.out.println("Enabled: " + enabled);
                    System.out.println("Coins: " + coins);

                } catch (ExpiredJwtException e) {
                    sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, "User token expired");
                    return;
                }

                catch (Exception e) {
                    System.out.println("when the client is calling the logout api with the admin token "+httpServletRequest.getRequestURI());
                    sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid user token");
                    return;
                }
            }

            // ✅ UPDATED: Set authentication (only requires userId)
            if (userId != null) {
                String roleWithPrefix = userRole.startsWith("ROLE_") ? userRole : "ROLE_" + userRole;
                List<SimpleGrantedAuthority> authorities = Collections
                        .singletonList(new SimpleGrantedAuthority(roleWithPrefix));

                Authentication auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);

                System.out.println("=== Authentication Set ===");
                System.out.println("Principal: " + userId);
                System.out.println("Authorities: " + authorities);
                System.out.println("Request URI: " + request.getRequestURI());
                System.out.println("Is Admin Endpoint: " + isAdminEndpoint);
            }

        } catch (Exception e) {
            sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Token processing failed: " + e.getMessage());
            return;
        }
        filterChain.doFilter(request, response);
    }


















    private void sendErrorResponse(HttpServletRequest request, HttpServletResponse response,
                                   int status, String message) throws IOException {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(status)
                .error("Unauthorized")
                .message(message)
                .path(request.getRequestURI())
                .build();

        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
    }








    private static final List<String> COMMON_ENDPOINTS = Arrays.asList(
            "/v1/mood/vibes/",
            "/v1/challenges",
            "/v1/calendar/user-calendar",
            "/v1/articles",
            "/v1/journal/entries"
    );

}
