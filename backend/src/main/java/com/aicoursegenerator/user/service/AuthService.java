package com.aicoursegenerator.user.service;

import com.aicoursegenerator.auth.dto.*;
import com.aicoursegenerator.auth.service.OtpService;
import com.aicoursegenerator.common.exception.BadRequestException;
import com.aicoursegenerator.security.JwtTokenProvider;
import com.aicoursegenerator.user.entity.User;
import com.aicoursegenerator.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final OtpService otpService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       OtpService otpService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.otpService = otpService;
    }

    @Transactional
    public void initiateSignUp(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email address is already in use");
        }
        otpService.sendOtp(email, "SIGNUP");
    }

    @Transactional
    public AuthResponse registerWithOtp(RegisterOtpRequest request) {
        boolean verified = otpService.verifyOtp(request.email(), request.otpCode(), "SIGNUP");
        if (!verified) {
            throw new BadRequestException("Invalid or expired OTP verification code");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email address is already in use");
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .build();

        user.setLearningStreak(1);
        user.setLastActiveAt(ZonedDateTime.now());

        User savedUser = userRepository.save(user);
        String token = tokenProvider.generateToken(savedUser.getEmail());

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName()
        );
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email address is already in use");
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .build();

        user.setLearningStreak(1);
        user.setLastActiveAt(ZonedDateTime.now());

        User savedUser = userRepository.save(user);
        String token = tokenProvider.generateToken(savedUser.getEmail());

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName()
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadRequestException("User record not found after authentication"));

        updateStreakAndActivity(user);

        String token = tokenProvider.generateToken(user.getEmail());

        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName()
        );
    }

    @Transactional
    public void updateStreakAndActivity(User user) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime lastActive = user.getLastActiveAt();

        if (lastActive == null) {
            user.setLearningStreak(1);
        } else {
            long diffDays = ChronoUnit.DAYS.between(lastActive.toLocalDate(), now.toLocalDate());
            if (diffDays == 1) {
                user.setLearningStreak(user.getLearningStreak() + 1);
            } else if (diffDays > 1) {
                user.setLearningStreak(1);
            }
        }
        user.setLastActiveAt(now);
        userRepository.save(user);
    }

    @Transactional
    public void initiateForgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found with email: " + email));
        otpService.sendOtp(user.getEmail(), "FORGOT_PASSWORD");
    }

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        boolean verified = otpService.verifyOtp(request.email(), request.otpCode(), "FORGOT_PASSWORD");
        if (!verified) {
            throw new BadRequestException("Invalid or expired OTP verification code");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadRequestException("User not found with email: " + request.email()));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}
