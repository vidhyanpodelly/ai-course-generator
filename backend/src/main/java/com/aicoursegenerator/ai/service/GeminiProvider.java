package com.aicoursegenerator.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class GeminiProvider implements AiProvider {

    private static final Logger logger = LoggerFactory.getLogger(GeminiProvider.class);
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String modelName;
    private final String baseUrl;
    private final Duration timeout;
    private final int maxRetries;

    public GeminiProvider(
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-3.5-flash}") String modelName,
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @Value("${gemini.timeout:120s}") Duration timeout,
            @Value("${gemini.max-retries:3}") int maxRetries,
            ObjectMapper objectMapper) {
        
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl + "v1beta/models/" : baseUrl + "/v1beta/models/";
        this.timeout = timeout;
        this.maxRetries = maxRetries;
        
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        
        if (this.apiKey == null || this.apiKey.trim().isEmpty()) {
            throw new IllegalStateException("GEMINI_API_KEY environment variable is missing.");
        }
        logger.info("Initialized GeminiProvider with model {}", this.modelName);
    }

    @Override
    public String generateText(String systemPrompt, String userPrompt) {
        return executeRequest(systemPrompt, userPrompt, false);
    }

    @Override
    public <T> T generateStructuredJson(String systemPrompt, String userPrompt, Class<T> responseClass) {
        String jsonResponse = executeRequest(systemPrompt, userPrompt, true);
        
        // Clean markdown blocks if Gemini happened to include them despite the mime-type
        jsonResponse = jsonResponse.trim();
        if (jsonResponse.startsWith("```json")) {
            jsonResponse = jsonResponse.substring(7);
        } else if (jsonResponse.startsWith("```")) {
            jsonResponse = jsonResponse.substring(3);
        }
        if (jsonResponse.endsWith("```")) {
            jsonResponse = jsonResponse.substring(0, jsonResponse.length() - 3);
        }
        jsonResponse = jsonResponse.trim();

        try {
            return objectMapper.readValue(jsonResponse, responseClass);
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse JSON response into class {}: {}", responseClass.getSimpleName(), jsonResponse);
            throw new RuntimeException("JSON parsing failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void streamText(String systemPrompt, String userPrompt, SseEmitter emitter, Consumer<String> onComplete) {
        throw new UnsupportedOperationException("Streaming is not yet implemented for direct Gemini.");
    }

    private String executeRequest(String systemPrompt, String userPrompt, boolean requireJson) {
        long startTime = System.currentTimeMillis();
        int attempt = 0;
        
        while (attempt < this.maxRetries) {
            attempt++;
            try {
                Map<String, Object> payload = buildPayload(systemPrompt, userPrompt, requireJson);
                String jsonBody = objectMapper.writeValueAsString(payload);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(this.baseUrl + this.modelName + ":generateContent?key=" + apiKey))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .timeout(this.timeout)
                        .build();

                logger.debug("Sending request to Gemini (Attempt {}/{})", attempt, this.maxRetries);
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                long duration = System.currentTimeMillis() - startTime;
                
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    JsonNode root = objectMapper.readTree(response.body());
                    logUsage(root);
                    logger.info("Gemini request successful. Status: {}, Duration: {}ms", response.statusCode(), duration);
                    return extractText(root);
                } else if (response.statusCode() == 429 || response.statusCode() == 503 || response.statusCode() == 500) {
                    logger.warn("Transient error from Gemini (Status: {}). Retrying... (Attempt {}/{})", response.statusCode(), attempt, this.maxRetries);
                    Thread.sleep((long) Math.pow(2, attempt) * 1000); // Exponential backoff
                } else {
                    logger.error("Gemini request failed. Status: {}, Body: {}", response.statusCode(), response.body());
                    throw new RuntimeException("AI provider failed with status " + response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                logger.error("Error communicating with Gemini (Attempt {}/{}): {}", attempt, this.maxRetries, e.getMessage());
                if (attempt == this.maxRetries || e instanceof InterruptedException) {
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    throw new RuntimeException("Failed to communicate with AI provider", e);
                }
            }
        }
        throw new RuntimeException("AI provider failed after " + this.maxRetries + " attempts");
    }

    private Map<String, Object> buildPayload(String systemPrompt, String userPrompt, boolean requireJson) {
        Map<String, Object> payload = new HashMap<>();
        
        // System instruction
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            Map<String, Object> sysInstruction = new HashMap<>();
            List<Map<String, String>> sysParts = new ArrayList<>();
            sysParts.add(Map.of("text", systemPrompt));
            sysInstruction.put("parts", sysParts);
            payload.put("systemInstruction", sysInstruction);
        }

        // Contents (User prompt)
        Map<String, Object> content = new HashMap<>();
        content.put("role", "user");
        List<Map<String, String>> userParts = new ArrayList<>();
        userParts.add(Map.of("text", userPrompt));
        content.put("parts", userParts);
        
        List<Map<String, Object>> contents = new ArrayList<>();
        contents.add(content);
        payload.put("contents", contents);

        // Config for JSON
        if (requireJson) {
            Map<String, Object> config = new HashMap<>();
            config.put("responseMimeType", "application/json");
            payload.put("generationConfig", config);
        }

        return payload;
    }

    private String extractText(JsonNode root) {
        try {
            JsonNode textNode = root.at("/candidates/0/content/parts/0/text");
            if (textNode.isMissingNode()) {
                throw new RuntimeException("No text found in response");
            }
            return textNode.asText();
        } catch (Exception e) {
            logger.error("Failed to extract text from Gemini response. Response body might be malformed or blocked due to safety settings.");
            throw e;
        }
    }
    
    private void logUsage(JsonNode root) {
        JsonNode usage = root.get("usageMetadata");
        if (usage != null) {
            int promptTokens = usage.path("promptTokenCount").asInt(0);
            int candidatesTokens = usage.path("candidatesTokenCount").asInt(0);
            int totalTokens = usage.path("totalTokenCount").asInt(0);
            logger.info("Gemini Token usage - Prompt: {}, Response: {}, Total: {}", promptTokens, candidatesTokens, totalTokens);
        }
    }
}
