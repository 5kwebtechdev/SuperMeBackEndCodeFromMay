package com.superme.controller;

import com.superme.dto.OtpResponse;
import com.superme.dto.SendOtpRequest;
import com.superme.dto.VerifyOtpRequest;
import com.superme.exception.BusinessException;
import com.superme.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;

    /**
     * POST /api/auth/send-otp
     * Generates a 6-digit OTP, stores it in memory (5-min TTL), and emails it.
     * Calling again for the same email resets the timer.
     */
    @PostMapping("/send-otp")
    public ResponseEntity<OtpResponse> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        log.info("Send-OTP request for: {}", request.getEmail());
        otpService.sendOtp(request.getEmail());
        return ResponseEntity.ok(OtpResponse.success("OTP sent successfully"));
    }

    /**
     * POST /api/auth/verify-otp
     * Validates the OTP. On success removes it so it cannot be reused.
     * Returns 400 with {success:false, message:...} for all failure cases.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<OtpResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        log.info("Verify-OTP request for: {}", request.getEmail());
        try {
            otpService.verifyOtp(request.getEmail(), request.getOtp());
            return ResponseEntity.ok(OtpResponse.success("OTP verified successfully"));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(OtpResponse.failure(e.getMessage()));
        }
    }
}