package com.aicoursegenerator.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class AIHealthCheckService {

    private static final Logger logger = LoggerFactory.getLogger(AIHealthCheckService.class);

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private volatile boolean isAiAvailable = false;
    private volatile String lastFailureReason = "Not checked yet";
    private volatile long lastLatency = -1;
    private volatile List<String> availableModels = new ArrayList<>();

    public AIHealthCheckService(
            @Value("${ai.base-url:}") String baseUrl,
            @Value("${ai.api-key:}") String apiKey,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        logger.info("Performing startup AI health check against OmniRoute at {}", baseUrl);
        checkAIHealth();
    }

    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void checkAIHealth() {
        if (baseUrl == null || baseUrl.isEmpty() || baseUrl.equals("${AI_BASE_URL}")) {
            isAiAvailable = false;
            lastFailureReason = "AI_BASE_URL is not configured";
            logger.error("Health Check Failed: {}", lastFailureReason);
            return;
        }

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/models"))
                    .timeout(Duration.ofSeconds(15))
                    .GET();

            if (apiKey != null && !apiKey.trim().isEmpty()) {
                requestBuilder.header("Authorization", "Bearer " + apiKey);
            }

            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            long duration = System.currentTimeMillis() - startTime;

            this.lastLatency = duration;

            if (response.statusCode() == 200) {
                this.isAiAvailable = true;
                this.lastFailureReason = null;
                parseModels(response.body());
                logger.info("AI Health Check Passed. Latency: {}ms, Available models: {}", duration, availableModels.size());
            } else {
                this.isAiAvailable = false;
                this.lastFailureReason = "HTTP " + response.statusCode() + " - " + response.body();
                logger.error("AI Health Check Failed: {}", lastFailureReason);
            }
        } catch (Exception e) {
            this.isAiAvailable = false;
            this.lastLatency = -1;
            this.lastFailureReason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            logger.error("AI Health Check Failed (Network/IO): {}", this.lastFailureReason);
        }
    }

    private void parseModels(String jsonBody) {
        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            JsonNode data = root.path("data");
            List<String> models = new ArrayList<>();
            if (data.isArray()) {
                for (JsonNode modelNode : data) {
                    models.add(modelNode.path("id").asText());
                }
            }
            this.availableModels = models;
        } catch (Exception e) {
            logger.warn("Failed to parse models from health check response", e);
        }
    }

    public boolean isAiAvailable() {
        return isAiAvailable;
    }

    public String getLastFailureReason() {
        return lastFailureReason;
    }

    public long getLastLatency() {
        return lastLatency;
    }

    public List<String> getAvailableModels() {
        return availableModels;
    }
}
