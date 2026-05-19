package com.superme.service;

public interface ForgotPasswordService {

    void sendOtp(String email);

    void verifyOtp(String email, String otp);

    void resetPassword(String email, String newPassword);
}