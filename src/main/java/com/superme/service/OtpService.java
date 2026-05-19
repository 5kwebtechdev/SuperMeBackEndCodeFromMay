package com.superme.service;

public interface OtpService {

    void sendOtp(String email);

    void verifyOtp(String email, String otp);
}