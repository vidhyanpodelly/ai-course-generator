package com.aicoursegenerator.auth.service;

import com.aicoursegenerator.auth.entity.OtpVerification;
import com.aicoursegenerator.auth.repository.OtpVerificationRepository;
import com.aicoursegenerator.common.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class OtpService {

    private final OtpVerificationRepository otpRepository;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpVerificationRepository otpRepository, EmailService emailService) {
        this.otpRepository = otpRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void sendOtp(String email, String otpType) {
        // Rate limiting check: verify if an OTP was sent in the last 60 seconds
        ZonedDateTime now = ZonedDateTime.now();
        
        // Clean expired OTPs first
        otpRepository.deleteByExpiresAtBefore(now);

        // Fetch user's active OTPs
        // If there's an unexpired OTP created less than 60 seconds ago, reject
        // Since we don't have createdAt on model directly (it's database populated),
        // we can check if the expiresAt is greater than (now + 4 minutes)
        // because expiresAt is set to now + 5 minutes.
        // If expiresAt > now + 4 minutes, it was sent less than 60 seconds ago.
        Optional<OtpVerification> activeOtp = otpRepository.findByEmail(email);
        if (activeOtp.isPresent() && activeOtp.get().getExpiresAt().isAfter(now.plusMinutes(14))) {
            throw new BadRequestException("Please wait at least 60 seconds before requesting another OTP.");
        }


        // Generate 6-digit code
        String otpCode = String.format("%06d", random.nextInt(1000000));

        // Delete any existing codes for this email
        otpRepository.deleteByEmail(email);

        OtpVerification otpVerification = new OtpVerification(
                UUID.randomUUID(),
                email,
                otpCode,
                otpType,
                now.plusMinutes(15)
        );

        otpRepository.save(otpVerification);
        emailService.sendOtpEmail(email, otpCode, otpType);
    }

    @Transactional
    public boolean verifyOtp(String email, String otpCode, String otpType) {
        ZonedDateTime now = ZonedDateTime.now();
        Optional<OtpVerification> opt = otpRepository.findByEmailAndOtpCodeAndOtpType(email, otpCode, otpType);
        
        if (opt.isEmpty()) {
            return false;
        }

        OtpVerification verification = opt.get();
        if (verification.getExpiresAt().isBefore(now)) {
            otpRepository.delete(verification);
            return false;
        }

        // OTP verified successfully, clean up
        otpRepository.delete(verification);
        return true;
    }
}
