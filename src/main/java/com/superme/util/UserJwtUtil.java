package com.superme.util;

import com.superme.exception.InternalServerErrorException;
import com.superme.exception.InvalidTokenException;
import com.superme.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.spec.KeySpec;
import java.util.Date;

public class UserJwtUtil {

    // ✅ FIXED: Use SAME configuration as JwtAuthFilter
    private static final String USER_PASSPHRASE = System.getenv().getOrDefault(
            "JWT_SECRET",
            "mindfull777mindfull777mindfull777!!");
    private static final String ADMIN_PASSPHRASE = System.getenv().getOrDefault(
            "JWT_ADMIN_SECRET",
            "MindfullPhase1SecretKeyForJWTTokenGenerationAndValidationProcess2024!@#$%");

    private static final int ITERATIONS = 150_000;
    private static final int KEY_LENGTH = 512;
    // ✅ FIXED: Use SAME salts as JwtAuthFilter
    private static final byte[] USER_SALT = "UserSaltForMindfull123!".getBytes(StandardCharsets.UTF_8);
    private static final byte[] ADMIN_SALT = "AdminSaltForMindfull123!".getBytes(StandardCharsets.UTF_8);

    // ✅ FIXED: Use SAME key derivation method as JwtAuthFilter
    private static byte[] deriveKey(char[] passphrase, byte[] salt, int iterations, int keyLength) throws Exception {
        KeySpec spec = new PBEKeySpec(passphrase, salt, iterations, keyLength);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return factory.generateSecret(spec).getEncoded();
    }

    // ✅ FIXED: Generate JWT token with SAME security as JwtAuthFilter
    public static String generateToken(User user) {
        try {
            // Use SAME key derivation as JwtAuthFilter
            byte[] keyBytes = deriveKey(USER_PASSPHRASE.toCharArray(), USER_SALT, ITERATIONS, KEY_LENGTH);
            Key secretKey = Keys.hmacShaKeyFor(keyBytes);

            long nowMillis = System.currentTimeMillis();
            long expMillis = nowMillis + 365L * 24 * 60 * 60 * 1000; // 1 year

            return Jwts.builder()
                    .setSubject(String.valueOf(user.getId()))
                    .claim("id", user.getId())
                    .claim("name", user.getName())
                    // ❌ REMOVED: .claim("username", usernameValue) - YOU DON'T USE USERNAME
                    .claim("email", user.getEmail())
                    .claim("phone", user.getPhone())
                    .claim("gender", user.getGender())
                    .claim("dateOfBirth", user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null)
                    .claim("age", user.getAge())
                    .claim("ageGroup", user.getAgeGroup() != null ? user.getAgeGroup().name() : null)
                    .claim("relationship", user.getRelationship() != null ? user.getRelationship().name() : null)
                    .claim("role", user.getRole() != null ? user.getRole().name() : "USER")
                    .claim("enabled", user.isEnabled())
                    .claim("coins", user.getCoins())
                    .setIssuedAt(new Date(nowMillis))
                    .setExpiration(new Date(expMillis))
                    .signWith(secretKey, SignatureAlgorithm.HS256)
                    .compact();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }

    // ✅ FIXED: Extract user ID from token
    public static Long getUserIdFromToken(String token) {
        System.out.println("1.1 Extracting user ID from token: " + token);
        try {
            byte[] keyBytes = deriveKey(USER_PASSPHRASE.toCharArray(), USER_SALT, ITERATIONS, KEY_LENGTH);
            Key secretKey = Keys.hmacShaKeyFor(keyBytes);

            String userId = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();

            return Long.parseLong(userId);
        }  catch (ExpiredJwtException ex) {
            throw new InvalidTokenException("TOKEN_EXPIRED");

        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("INVALID_TOKEN");
        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to extract user ID from token");
        }
    }

    // ✅ Extract role from token
    public static String getRoleFromToken(String token) {
        try {
            byte[] keyBytes = deriveKey(USER_PASSPHRASE.toCharArray(), USER_SALT, ITERATIONS, KEY_LENGTH);
            Key secretKey = Keys.hmacShaKeyFor(keyBytes);

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String role = claims.get("role", String.class);
            return role != null ? role : "USER";
        } catch (Exception e) {
            throw new RuntimeException("Invalid token for role extraction", e);
        }
    }






    public static Long getUserIdFromAdminToken(String token) {
        try {
            byte[] keyBytes = deriveKey(ADMIN_PASSPHRASE.toCharArray(), ADMIN_SALT, ITERATIONS, KEY_LENGTH);
            Key key = Keys.hmacShaKeyFor(keyBytes);

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return Long.parseLong(claims.getSubject());

        } catch (Exception e) {
            throw new InvalidTokenException("INVALID_ADMIN_TOKEN");
        }
    }

}
