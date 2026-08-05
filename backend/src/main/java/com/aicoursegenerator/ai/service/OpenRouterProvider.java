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
public class OpenRouterProvider implements AiProvider {

    private static final Logger logger = LoggerFactory.getLogger(OpenRouterProvider.class);
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String modelName;
    private final String baseUrl;
    private final Duration timeout;
    private final int maxRetries;

    public OpenRouterProvider(
            @Value("${openrouter.api-key:}") String apiKey,
            @Value("${openrouter.model:google/gemma-4-31b-it:free}") String modelName,
            @Value("${openrouter.base-url:https://openrouter.ai/api/v1}") String baseUrl,
            @Value("${openrouter.timeout:120s}") Duration timeout,
            @Value("${openrouter.max-retries:3}") int maxRetries,
            ObjectMapper objectMapper) {

        this.apiKey = apiKey;
        this.modelName = modelName;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.timeout = timeout;
        this.maxRetries = maxRetries;

        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        if (this.apiKey == null || this.apiKey.trim().isEmpty()) {
            throw new IllegalStateException("OPENROUTER_API_KEY environment variable is missing.");
        }
        logger.info("Initialized OpenRouterProvider with model {}", this.modelName);
    }

    @Override
    public String generateText(String systemPrompt, String userPrompt) {
        return executeRequest(systemPrompt, userPrompt, false);
    }

    @Override
    public <T> T generateStructuredJson(String systemPrompt, String userPrompt, Class<T> responseClass) {
        String jsonResponse = executeRequest(systemPrompt, userPrompt, true);

        // Clean markdown blocks if the model happened to include them
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
        throw new UnsupportedOperationException("Streaming is not yet implemented for direct OpenRouter.");
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
                        .uri(URI.create(this.baseUrl + "/chat/completions"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + this.apiKey)
                        .header("HTTP-Referer", "https://aicoursegenerator.com")
                        .header("X-Title", "AI Course Generator")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .timeout(this.timeout)
                        .build();

                logger.debug("Sending request to OpenRouter (Attempt {}/{})", attempt, this.maxRetries);

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                long duration = System.currentTimeMillis() - startTime;

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    JsonNode root = objectMapper.readTree(response.body());
                    logUsage(root);
                    logger.info("OpenRouter request successful. Status: {}, Duration: {}ms", response.statusCode(), duration);
                    return extractText(root);
                } else if (response.statusCode() == 429 || response.statusCode() == 503 || response.statusCode() == 500 || response.statusCode() == 502) {
                    logger.warn("Transient error from OpenRouter (Status: {}). Body: {}. Retrying... (Attempt {}/{})", response.statusCode(), response.body(), attempt, this.maxRetries);
                    Thread.sleep((long) Math.pow(2, attempt + 1) * 1000); // Exponential backoff (4s, 8s, 16s, 32s, etc)
                } else if (response.statusCode() == 401) {
                    logger.error("OpenRouter request failed with 401 Unauthorized. Check your OPENROUTER_API_KEY.");
                    throw new RuntimeException("AI provider authentication failed (401).");
                } else {
                    logger.error("OpenRouter request failed. Status: {}, Body: {}", response.statusCode(), response.body());
                    throw new RuntimeException("AI provider failed with status " + response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                logger.error("Error communicating with OpenRouter (Attempt {}/{}): {}", attempt, this.maxRetries, e.getMessage());
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

        payload.put("model", this.modelName);

        List<Map<String, String>> messages = new ArrayList<>();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            Map<String, String> sysMsg = new HashMap<>();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            messages.add(sysMsg);
        }

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        messages.add(userMsg);

        payload.put("messages", messages);
        payload.put("stream", false);

        // OpenRouter specific json mode
        if (requireJson) {
            Map<String, Object> responseFormat = new HashMap<>();
            responseFormat.put("type", "json_object");
            payload.put("response_format", responseFormat);
        }

        return payload;
    }

    private String extractText(JsonNode root) {
        try {
            JsonNode textNode = root.at("/choices/0/message/content");
            if (textNode.isMissingNode()) {
                throw new RuntimeException("No text found in response");
            }
            return textNode.asText();
        } catch (Exception e) {
            logger.error("Failed to extract text from OpenRouter response.");
            throw e;
        }
    }

    private void logUsage(JsonNode root) {
        JsonNode usage = root.get("usage");
        if (usage != null) {
            int promptTokens = usage.path("prompt_tokens").asInt(0);
            int completionTokens = usage.path("completion_tokens").asInt(0);
            int totalTokens = usage.path("total_tokens").asInt(0);
            logger.info("OpenRouter Token usage - Prompt: {}, Response: {}, Total: {}", promptTokens, completionTokens, totalTokens);
        }
    }
}
