package com.aicoursegenerator.common.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@RestController
@RequestMapping("/api/debug")
public class DebugOmniRouteController {

    private static final Logger logger = LoggerFactory.getLogger(DebugOmniRouteController.class);
    private final HttpClient httpClient;
    private final String baseUrl;

    public DebugOmniRouteController(@org.springframework.beans.factory.annotation.Value("${ai.base-url:}") String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    @GetMapping("/omniroute")
    public ResponseEntity<String> testOmniRoute() {
        try {
            String jsonBody = "{\n" +
                    "  \"model\": \"course-generator\",\n" +
                    "  \"stream\": false,\n" +
                    "  \"messages\": [\n" +
                    "    {\n" +
                    "      \"role\": \"user\",\n" +
                    "      \"content\": \"Say hello.\"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";

            logger.info("Sending debug request to OmniRoute: {}", jsonBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(this.baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long duration = System.currentTimeMillis() - startTime;

            logger.info("Debug Response Status: {}", response.statusCode());
            logger.info("Debug Response Time: {} ms", duration);
            logger.info("Debug Response Body: {}", response.body());

            return ResponseEntity.status(response.statusCode()).body(response.body());

        } catch (Exception e) {
            logger.error("Debug endpoint failed", e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}
