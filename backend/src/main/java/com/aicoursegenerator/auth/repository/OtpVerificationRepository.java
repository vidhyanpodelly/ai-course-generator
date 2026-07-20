package com.aicoursegenerator.auth.repository;

import com.aicoursegenerator.auth.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, UUID> {
    Optional<OtpVerification> findByEmailAndOtpCodeAndOtpType(String email, String otpCode, String otpType);
    Optional<OtpVerification> findByEmail(String email);

    void deleteByEmail(String email);
    void deleteByExpiresAtBefore(ZonedDateTime now);
}
