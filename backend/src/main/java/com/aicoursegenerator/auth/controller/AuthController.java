package com.aicoursegenerator.auth.controller;

import com.aicoursegenerator.auth.dto.*;
import com.aicoursegenerator.common.dto.ApiResponse;
import com.aicoursegenerator.security.CustomUserDetails;
import com.aicoursegenerator.user.dto.UserResponse;
import com.aicoursegenerator.user.entity.User;
import com.aicoursegenerator.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final com.aicoursegenerator.course.service.CertificateService certificateService;

    public AuthController(
            AuthService authService,
            com.aicoursegenerator.course.service.CertificateService certificateService) {
        this.authService = authService;
        this.certificateService = certificateService;
    }

    @GetMapping("/verify/{certId}")
    public ResponseEntity<ApiResponse<com.aicoursegenerator.course.service.CertificateService.CertificateData>> verifyCertificate(
            @PathVariable("certId") String certId) {
        com.aicoursegenerator.course.service.CertificateService.CertificateData data = 
                certificateService.verifyCertificatePublic(UUID.fromString(certId));
        return ResponseEntity.ok(ApiResponse.success("Certificate verified successfully", data));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/register/initiate")
    public ResponseEntity<ApiResponse<Void>> initiateRegister(@RequestParam String email) {
        authService.initiateSignUp(email);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("OTP verification code sent to your email", null));
    }

    @PostMapping("/register/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> registerVerify(@Valid @RequestBody RegisterOtpRequest request) {
        AuthResponse response = authService.registerWithOtp(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User verified and registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/otp/send")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody OtpSendRequest request) {
        if ("SIGNUP".equalsIgnoreCase(request.type())) {
            authService.initiateSignUp(request.email());
        } else if ("FORGOT_PASSWORD".equalsIgnoreCase(request.type())) {
            authService.initiateForgotPassword(request.email());
        } else {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Invalid OTP type: must be SIGNUP or FORGOT_PASSWORD"));
        }
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("OTP verification code sent successfully", null));
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestParam String email) {
        authService.initiateForgotPassword(email);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("Password reset OTP sent to your email", null));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("Password has been reset successfully", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }
        User user = userDetails.getUser();
        UserResponse response = new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName()
        );
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("Authenticated user details retrieved", response));
    }
}
