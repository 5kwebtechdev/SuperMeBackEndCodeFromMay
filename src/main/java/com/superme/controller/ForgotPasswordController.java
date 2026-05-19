package com.superme.controller;

import com.superme.dto.ForgotPasswordRequest;
import com.superme.dto.OtpResponse;
import com.superme.dto.ResetPasswordRequest;
import com.superme.dto.VerifyOtpRequest;
import com.superme.exception.BusinessException;
import com.superme.exception.ResourceNotFoundException;
import com.superme.service.ForgotPasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth/forgot-password")
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final ForgotPasswordService forgotPasswordService;

    /**
     * POST /auth/forgot-password
     * Validates the email exists, then sends a 4-digit OTP to the inbox.
     */
    @PostMapping
    public ResponseEntity<OtpResponse> sendOtp(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Forgot-password OTP request for: {}", request.getEmail());
        try {
            forgotPasswordService.sendOtp(request.getEmail());
            return ResponseEntity.ok(OtpResponse.success("OTP sent successfully to your registered email"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(OtpResponse.failure(e.getMessage()));
        }
    }

    /**
     * POST /auth/forgot-password/verify-otp
     * Verifies the 4-digit OTP. On success opens a 10-minute password-reset window.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<OtpResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        log.info("Forgot-password OTP verify for: {}", request.getEmail());
        try {
            forgotPasswordService.verifyOtp(request.getEmail(), request.getOtp());
            return ResponseEntity.ok(OtpResponse.success("OTP verified successfully. You may now reset your password."));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(OtpResponse.failure(e.getMessage()));
        }
    }

    /**
     * POST /auth/forgot-password/reset-password
     * Resets the password. Requires prior OTP verification within the last 10 minutes.
     * Password is BCrypt-hashed before storage.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<OtpResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Password reset attempt for: {}", request.getEmail());
        try {
            forgotPasswordService.resetPassword(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(OtpResponse.success("Password reset successfully. Please login with your new password."));
        } catch (BusinessException | ResourceNotFoundException e) {
            return ResponseEntity.badRequest().body(OtpResponse.failure(e.getMessage()));
        }
    }
}