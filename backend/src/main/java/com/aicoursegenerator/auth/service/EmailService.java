package com.aicoursegenerator.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String toEmail, String otpCode, String otpType) {
        String subject = "Curricula.AI - OTP Verification";
        String body = "Dear Student,\n\n" +
                      "Your verification code is: " + otpCode + "\n" +
                      "Type of request: " + otpType + "\n" +
                      "This code will expire in 5 minutes.\n\n" +
                      "Best regards,\n" +
                      "Curricula.AI Team";

        logger.info("**************************************************");
        logger.info("OTP FOR {}: {}", toEmail, otpCode);
        logger.info("**************************************************");

        if (mailSender == null) {
            logger.warn("JavaMailSender is not configured. Falling back to log output.");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            logger.info("OTP email successfully sent to {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send email to {} via JavaMailSender: {}", toEmail, e.getMessage());
        }
    }
}
