package com.superme.admin.security;

import com.superme.exception.BusinessException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.Date;

@Component
public class AdminJwtUtil {

  private static final String jwtSecret = "MindfullPhase1SecretKeyForJWTTokenGenerationAndValidationProcess2024!@#$%";
  private static final byte[] ADMIN_SALT = "AdminSaltForMindfull123!" .getBytes(StandardCharsets.UTF_8);
  private static final int ITERATIONS = 150_000;
  private static final int KEY_LENGTH = 512;
  private static final long jwtExpirationMs = 365L * 24 * 60 * 60 * 1000; // 1 day

  // ✅ PBKDF2 key derivation
  private static SecretKey derivePBKDF2Key(String passphrase) {
    try {
      KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), ADMIN_SALT, ITERATIONS, KEY_LENGTH);
      SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
      byte[] keyBytes = factory.generateSecret(spec).getEncoded();
      return Keys.hmacShaKeyFor(keyBytes);
    } catch (Exception e) {
      throw new RuntimeException("Failed to derive PBKDF2 key for JWT", e);
    }
  }

  private static SecretKey getSigningKey() {
    return derivePBKDF2Key(jwtSecret);
  }

  public String getEmailFromToken(String token) {
    return getClaims(token).getSubject();
  }

  public static String extractRole(String token) {
    String role = getClaims(token).get("role", String.class);
    return role != null ? role.toUpperCase() : "USER";
  }

  private static Claims getClaims(String token) {
    return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
  }

  public boolean validateToken(String token) {
    try {
      getClaims(token);
      return true;
    } catch (JwtException e) {
      System.out.println("JWT validation failed: " + e.getMessage());
      return false;
    }
  }

  public static Long getAdminIdFromToken(String token) {
    try {
      SecretKey secretKey = derivePBKDF2Key(jwtSecret);
      String id = Jwts.parserBuilder()
              .setSigningKey(secretKey)
              .build()
              .parseClaimsJws(token)
              .getBody()
              .getSubject();

      return Long.parseLong(id);
    } catch (Exception e) {
      throw new BusinessException("Invalid token");
    }
  }

  public String generateToken(Long id, String fullName, String email, String role, String department, String designation) {
    String upperRole = role.toUpperCase();

    return Jwts.builder()
            .setSubject(String.valueOf(id))
            .claim("role", upperRole)
            .claim("id", id)
            .claim("fullName", fullName)
            .claim("department", department)
            .claim("designation", designation)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
            .signWith(getSigningKey())
            .compact();
  }
}
