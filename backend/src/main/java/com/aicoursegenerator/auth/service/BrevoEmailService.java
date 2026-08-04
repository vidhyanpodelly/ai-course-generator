package com.aicoursegenerator.auth.service;

import com.aicoursegenerator.auth.dto.BrevoEmailRequest;
import com.aicoursegenerator.auth.exception.EmailDeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Service
public class BrevoEmailService implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(BrevoEmailService.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final String apiKey;
    private final String fromEmail;
    private final String fromName;
    private final RestClient restClient;

    public BrevoEmailService(
            @Value("${brevo.api.key:}") String apiKey,
            @Value("${brevo.from.email:}") String fromEmail,
            @Value("${brevo.from.name:CurriculaAI}") String fromName,
            RestClient.Builder restClientBuilder) {
        
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);

        this.restClient = restClientBuilder
                .baseUrl(BREVO_API_URL)
                .requestFactory(factory)
                .defaultHeader("api-key", apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public void sendOtpEmail(String toEmail, String otpCode, String otpType) {
        String subject = "Curricula.AI - OTP Verification";
        
        // Professional HTML Email with branding and expiry
        String htmlBody = "<div style=\"font-family: Arial, sans-serif; padding: 20px; color: #333;\">" +
                "<h2 style=\"color: #0056b3;\">Curricula.AI Verification</h2>" +
                "<p>Dear Student,</p>" +
                "<p>Your verification code for <strong>" + otpType + "</strong> is:</p>" +
                "<div style=\"font-size: 24px; font-weight: bold; padding: 10px; margin: 20px 0; background-color: #f4f4f4; border-radius: 5px; text-align: center; letter-spacing: 5px;\">" +
                otpCode + "</div>" +
                "<p>This code will expire in 5 minutes. If you didn't request this, you can safely ignore this email.</p>" +
                "<br>" +
                "<p>Best regards,<br><strong>Curricula.AI Team</strong></p>" +
                "</div>";

        logger.info("**************************************************");
        logger.info("OTP FOR {}: {}", toEmail, otpCode);
        logger.info("**************************************************");

        if (apiKey == null || apiKey.isBlank() || apiKey.contains("BREVO_API_KEY")) {
            logger.warn("Brevo API Key is not configured. Falling back to log output. Email NOT sent.");
            return;
        }

        BrevoEmailRequest.Sender sender = new BrevoEmailRequest.Sender(fromName, fromEmail);
        BrevoEmailRequest.Recipient recipient = new BrevoEmailRequest.Recipient(toEmail, "");
        
        BrevoEmailRequest request = new BrevoEmailRequest(sender, List.of(recipient), subject, htmlBody);

        try {
            ResponseEntity<String> response = restClient.post()
                    .body(request)
                    .retrieve()
                    .toEntity(String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("OTP email successfully sent to {}", toEmail);
            } else {
                logger.error("Failed to send email to {}. Status code: {}", toEmail, response.getStatusCode());
                throw new EmailDeliveryException("Failed to send email. Status: " + response.getStatusCode());
            }
        } catch (RestClientResponseException e) {
            logger.error("Error response from Brevo API for {}: {} - {}", toEmail, e.getStatusCode(), e.getResponseBodyAsString());
            throw new EmailDeliveryException("Failed to send email via Brevo API", e);
        } catch (Exception e) {
            logger.error("Error sending email to {}: {}", toEmail, e.getMessage());
            throw new EmailDeliveryException("Failed to send email via Brevo API", e);
        }
    }
}
