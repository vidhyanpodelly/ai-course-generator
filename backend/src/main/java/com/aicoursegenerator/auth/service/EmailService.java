package com.aicoursegenerator.auth.service;


public interface EmailService {
    void sendOtpEmail(String toEmail, String otpCode, String otpType);
}
