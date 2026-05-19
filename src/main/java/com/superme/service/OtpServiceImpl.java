package com.superme.service;

import com.superme.exception.BusinessException;
import com.superme.model.OtpData;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final EmailService emailService;

    private static final int OTP_EXPIRY_MINUTES = 5;

    // Thread-safe in-memory OTP storage (no database required)
    private final ConcurrentHashMap<String, OtpData> otpStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ScheduledFuture<?>> cleanupFutures = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public void sendOtp(String email) {
        // Cancel any previous cleanup task so resend resets the timer cleanly
        cancelExistingCleanup(email);

        String otp = generateOtp();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        otpStore.put(email, new OtpData(otp, expiryTime));
        log.info("OTP stored for: {}", email);

        // Auto-remove from memory after expiry — no stale data lingers
        ScheduledFuture<?> future = scheduler.schedule(
                () -> autoRemoveOtp(email),
                OTP_EXPIRY_MINUTES,
                TimeUnit.MINUTES
        );
        cleanupFutures.put(email, future);

        String htmlBody = buildOtpEmailHtml(otp);
        emailService.sendHtmlMail(email, "Your OTP Verification Code - SuperMe", htmlBody);
        log.info("OTP email dispatched to: {}", email);
    }

    @Override
    public void verifyOtp(String email, String otp) {
        OtpData otpData = otpStore.get(email);

        if (otpData == null) {
            log.warn("No OTP entry found for: {}", email);
            throw new BusinessException("No OTP found for this email. Please request a new OTP.");
        }

        if (otpData.isExpired()) {
            cleanupOtp(email);
            log.warn("OTP expired for: {}", email);
            throw new BusinessException("OTP has expired. Please request a new OTP.");
        }

        if (!otpData.getOtp().equals(otp)) {
            log.warn("Incorrect OTP attempt for: {}", email);
            throw new BusinessException("Invalid OTP. Please check and try again.");
        }

        // Valid — remove immediately so it cannot be reused
        cleanupOtp(email);
        log.info("OTP verified successfully for: {}", email);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private void autoRemoveOtp(String email) {
        otpStore.remove(email);
        cleanupFutures.remove(email);
        log.info("OTP auto-expired and removed for: {}", email);
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
        // SecureRandom guarantees cryptographically strong random values; 4-digit range 1000–9999
        int otp = 1000 + secureRandom.nextInt(9000);
        return String.valueOf(otp);
    }

    // ─── HTML email template ──────────────────────────────────────────────────

    private String buildOtpEmailHtml(String otp) {
        return "<!DOCTYPE html>"
                + "<html lang=\"en\">"
                + "<head>"
                + "<meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\">"
                + "<title>OTP Verification</title>"
                + "</head>"
                + "<body style=\"margin:0;padding:0;background:#f0f2f5;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;\">"
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">"
                + "<tr><td align=\"center\" style=\"padding:40px 16px;\">"

                // ── Card ──
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\""
                + " style=\"max-width:500px;background:#ffffff;border-radius:16px;"
                + "overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);\">"

                // ── Header ──
                + "<tr><td style=\"background:linear-gradient(135deg,#4f46e5 0%,#7c3aed 100%);"
                + "padding:36px 40px;text-align:center;\">"
                + "<h1 style=\"color:#fff;margin:0 0 4px;font-size:26px;font-weight:700;"
                + "letter-spacing:-0.5px;\">SuperMe</h1>"
                + "<p style=\"color:rgba(255,255,255,0.75);margin:0;font-size:12px;"
                + "letter-spacing:2px;text-transform:uppercase;\">Email Verification</p>"
                + "</td></tr>"

                // ── Body ──
                + "<tr><td style=\"padding:36px 40px 28px;\">"
                + "<h2 style=\"color:#111827;font-size:20px;font-weight:600;margin:0 0 10px;\">"
                + "Verify your email</h2>"
                + "<p style=\"color:#6b7280;font-size:14px;line-height:1.7;margin:0 0 28px;\">"
                + "Enter the one-time password below to complete your verification. "
                + "This code expires in <strong style=\"color:#111827;\">" + OTP_EXPIRY_MINUTES + " minutes</strong>."
                + "</p>"

                // ── OTP Box ──
                + "<div style=\"background:#f5f3ff;border:2px dashed #a5b4fc;border-radius:12px;"
                + "padding:28px 20px;text-align:center;margin-bottom:28px;\">"
                + "<p style=\"color:#7c3aed;font-size:11px;font-weight:700;letter-spacing:3px;"
                + "text-transform:uppercase;margin:0 0 14px;\">One-Time Password</p>"
                + "<div style=\"font-size:44px;font-weight:800;letter-spacing:14px;color:#4f46e5;"
                + "font-family:'Courier New',Courier,monospace;\">" + otp + "</div>"
                + "<p style=\"color:#9ca3af;font-size:12px;margin:14px 0 0;\">"
                + "&#9201;&nbsp;Expires in " + OTP_EXPIRY_MINUTES + " minutes</p>"
                + "</div>"

                // ── Security notice ──
                + "<div style=\"background:#fffbeb;border-left:3px solid #f59e0b;"
                + "border-radius:4px;padding:14px 16px;\">"
                + "<p style=\"color:#92400e;font-size:13px;margin:0;line-height:1.55;\">"
                + "<strong>&#9888; Security Notice:</strong>&nbsp;Never share this OTP with anyone. "
                + "SuperMe will never ask for your OTP via phone or email."
                + "</p>"
                + "</div>"
                + "</td></tr>"

                // ── Footer ──
                + "<tr><td style=\"background:#f9fafb;border-top:1px solid #f3f4f6;"
                + "padding:20px 40px;text-align:center;\">"
                + "<p style=\"color:#9ca3af;font-size:12px;line-height:1.6;margin:0 0 6px;\">"
                + "If you didn't request this OTP, you can safely ignore this email.</p>"
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
        log.info("Shutting down OTP scheduler...");
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