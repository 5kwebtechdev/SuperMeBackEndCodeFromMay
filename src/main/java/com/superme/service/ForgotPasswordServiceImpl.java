package com.superme.service;

import com.superme.exception.BusinessException;
import com.superme.exception.ResourceNotFoundException;
import com.superme.model.OtpData;
import com.superme.model.User;
import com.superme.model.UserPassword;
import com.superme.repository.UserPasswordRepository;
import com.superme.repository.UserRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForgotPasswordServiceImpl implements ForgotPasswordService {

    private final UserRepository userRepository;
    private final UserPasswordRepository userPasswordRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private static final int OTP_EXPIRY_MINUTES = 5;
    // Window in which reset-password is allowed after OTP verification
    private static final int VERIFIED_WINDOW_MINUTES = 10;

    // Independent OTP store — separate from the general send-otp module
    private final ConcurrentHashMap<String, OtpData> otpStore = new ConcurrentHashMap<>();
    // Tracks emails whose OTP was verified; value = expiry of the reset window
    private final ConcurrentHashMap<String, LocalDateTime> verifiedEmails = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ScheduledFuture<?>> cleanupFutures = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final SecureRandom secureRandom = new SecureRandom();

    // ─── API 1: Send OTP ─────────────────────────────────────────────────────

    @Override
    public void sendOtp(String email) {
        // Validate the email exists before doing anything
        if (!userRepository.existsByEmail(email)) {
            throw new ResourceNotFoundException("No account registered with this email address.");
        }

        // Cancel any previous pending cleanup and clear any stale verified state
        cancelExistingCleanup(email);
        verifiedEmails.remove(email);

        String otp = generateOtp();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        otpStore.put(email, new OtpData(otp, expiryTime));
        log.info("Forgot-password OTP generated for: {}", email);

        // Auto-remove from memory after 5 minutes
        ScheduledFuture<?> future = scheduler.schedule(
                () -> autoRemoveOtp(email),
                OTP_EXPIRY_MINUTES,
                TimeUnit.MINUTES
        );
        cleanupFutures.put(email, future);

        emailService.sendHtmlMail(email, "Reset Your Password - SuperMe", buildResetEmail(otp));
        log.info("Forgot-password OTP email dispatched to: {}", email);
    }

    // ─── API 2: Verify OTP ───────────────────────────────────────────────────

    @Override
    public void verifyOtp(String email, String otp) {
        OtpData otpData = otpStore.get(email);

        if (otpData == null) {
            log.warn("Forgot-password: no OTP found for {}", email);
            throw new BusinessException("No OTP found for this email. Please request a new OTP.");
        }
        if (otpData.isExpired()) {
            cleanupOtp(email);
            log.warn("Forgot-password: OTP expired for {}", email);
            throw new BusinessException("OTP has expired. Please request a new OTP.");
        }
        if (!otpData.getOtp().equals(otp)) {
            log.warn("Forgot-password: incorrect OTP for {}", email);
            throw new BusinessException("Invalid OTP. Please check and try again.");
        }

        // Valid — remove OTP and open the 10-minute reset window
        cleanupOtp(email);
        verifiedEmails.put(email, LocalDateTime.now().plusMinutes(VERIFIED_WINDOW_MINUTES));

        // Auto-close reset window after 10 minutes
        scheduler.schedule(
                () -> verifiedEmails.remove(email),
                VERIFIED_WINDOW_MINUTES,
                TimeUnit.MINUTES
        );

        log.info("Forgot-password OTP verified for: {}", email);
    }

    // ─── API 3: Reset Password ───────────────────────────────────────────────

    @Override
    public void resetPassword(String email, String newPassword) {
        // Ensure OTP was verified recently within the reset window
        LocalDateTime windowExpiry = verifiedEmails.get(email);
        if (windowExpiry == null || LocalDateTime.now().isAfter(windowExpiry)) {
            verifiedEmails.remove(email);
            throw new BusinessException("Password reset session expired. Please verify your OTP again.");
        }

        validatePasswordStrength(newPassword);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account registered with this email address."));

        UserPassword userPassword = userPasswordRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("User credentials not found."));

        userPassword.setPassword(passwordEncoder.encode(newPassword));
        userPasswordRepository.save(userPassword);

        // One-time use — close the reset window immediately after success
        verifiedEmails.remove(email);
        log.info("Password reset successfully for: {}", email);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private void validatePasswordStrength(String password) {
        if (password.length() < 6) {
            throw new BusinessException("Password must be at least 6 characters.");
        }
        if (password.length() > 20) {
            throw new BusinessException("Password must not exceed 20 characters.");
        }
        Set<String> banned = Set.of("123456", "12345678", "password", "qwerty", "111111", "000000", "abc123");
        if (banned.contains(password.toLowerCase())) {
            throw new BusinessException("This password is too common. Please choose a stronger password.");
        }
    }

    private void autoRemoveOtp(String email) {
        otpStore.remove(email);
        cleanupFutures.remove(email);
        log.info("Forgot-password OTP auto-expired for: {}", email);
    }

    private void cleanupOtp(String email) {
        otpStore.remove(email);
        cancelExistingCleanup(email);
    }

    private void cancelExistingCleanup(String email) {
        ScheduledFuture<?> existing = cleanupFutures.remove(email);
        if (existing != null) {
            existing.cancel(false);
        }
    }

    private String generateOtp() {
        // 4-digit: 1000–9999
        int otp = 1000 + secureRandom.nextInt(9000);
        return String.valueOf(otp);
    }

    // ─── HTML email template ─────────────────────────────────────────────────

    private String buildResetEmail(String otp) {
        return "<!DOCTYPE html>"
                + "<html lang=\"en\">"
                + "<head>"
                + "<meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\">"
                + "<title>Reset Password</title>"
                + "</head>"
                + "<body style=\"margin:0;padding:0;background:#f0f2f5;"
                + "font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;\">"
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">"
                + "<tr><td align=\"center\" style=\"padding:40px 16px;\">"

                // Card
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\""
                + " style=\"max-width:500px;background:#fff;border-radius:16px;"
                + "overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);\">"

                // Header — red/orange palette signals "action required"
                + "<tr><td style=\"background:linear-gradient(135deg,#f97316 0%,#ef4444 100%);"
                + "padding:36px 40px;text-align:center;\">"
                + "<h1 style=\"color:#fff;margin:0 0 4px;font-size:26px;font-weight:700;\">SuperMe</h1>"
                + "<p style=\"color:rgba(255,255,255,0.80);margin:0;font-size:12px;"
                + "letter-spacing:2px;text-transform:uppercase;\">Password Reset</p>"
                + "</td></tr>"

                // Body
                + "<tr><td style=\"padding:36px 40px 28px;\">"
                + "<h2 style=\"color:#111827;font-size:20px;font-weight:600;margin:0 0 10px;\">"
                + "Reset your password</h2>"
                + "<p style=\"color:#6b7280;font-size:14px;line-height:1.7;margin:0 0 28px;\">"
                + "We received a request to reset your password. Use the OTP below to proceed. "
                + "This code expires in <strong style=\"color:#111827;\">" + OTP_EXPIRY_MINUTES + " minutes</strong>."
                + "</p>"

                // OTP Box
                + "<div style=\"background:#fff7ed;border:2px dashed #fb923c;border-radius:12px;"
                + "padding:28px 20px;text-align:center;margin-bottom:28px;\">"
                + "<p style=\"color:#c2410c;font-size:11px;font-weight:700;letter-spacing:3px;"
                + "text-transform:uppercase;margin:0 0 14px;\">Your Reset OTP</p>"
                + "<div style=\"font-size:44px;font-weight:800;letter-spacing:14px;color:#ea580c;"
                + "font-family:'Courier New',Courier,monospace;\">" + otp + "</div>"
                + "<p style=\"color:#9ca3af;font-size:12px;margin:14px 0 0;\">"
                + "&#9201;&nbsp;Expires in " + OTP_EXPIRY_MINUTES + " minutes</p>"
                + "</div>"

                // Warning box
                + "<div style=\"background:#fef2f2;border-left:3px solid #ef4444;"
                + "border-radius:4px;padding:14px 16px;\">"
                + "<p style=\"color:#b91c1c;font-size:13px;margin:0;line-height:1.55;\">"
                + "<strong>&#9888; Important:</strong>&nbsp;If you did not request a password reset, "
                + "your account may be at risk. Please secure your account immediately.</p>"
                + "</div>"
                + "</td></tr>"

                // Footer
                + "<tr><td style=\"background:#f9fafb;border-top:1px solid #f3f4f6;"
                + "padding:20px 40px;text-align:center;\">"
                + "<p style=\"color:#9ca3af;font-size:12px;line-height:1.6;margin:0 0 6px;\">"
                + "This link expires in " + OTP_EXPIRY_MINUTES + " minutes. Do not share this OTP with anyone.</p>"
                + "<p style=\"color:#d1d5db;font-size:11px;margin:0;\">"
                + "&copy; 2025 SuperMe App &bull; All rights reserved</p>"
                + "</td></tr>"

                + "</table>"
                + "</td></tr>"
                + "</table>"
                + "</body>"
                + "</html>";
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down ForgotPassword scheduler...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}