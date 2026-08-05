package com.aicoursegenerator.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Service
public class OpenAICompatibleProvider implements AiProvider {

    private static final Logger logger = LoggerFactory.getLogger(OpenAICompatibleProvider.class);

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAICompatibleProvider(
            @Value("${ai.api-key:}") String apiKey,
            @Value("${ai.model:course-generator}") String model,
            @Value("${ai.base-url:http://localhost:20128/v1}") String baseUrl,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        // Ensure trailing slash for consistent endpoint building
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
                
        logger.info("Active AI Provider: Generic OpenAI Compatible API");
        logger.info("Base URL: {}", this.baseUrl);
        logger.info("Model combo string: {}", this.model);
    }

    @Override
    public String generateText(String systemPrompt, String userPrompt) {
        return executeWithRetry(systemPrompt, userPrompt, false);
    }

    @Override
    public <T> T generateStructuredJson(String systemPrompt, String userPrompt, Class<T> responseClass) {
        String jsonResponse = executeWithRetry(systemPrompt, userPrompt, true);
        String cleanedResponse = cleanJsonResponse(jsonResponse);
        try {
            return objectMapper.readValue(cleanedResponse, responseClass);
        } catch (Exception e) {
            logger.warn("JSON schema validation failed, attempting repair. Error: {}", e.getMessage());
            String repairedResponse = com.aicoursegenerator.ai.util.JsonRepairUtil.repair(jsonResponse);
            try {
                return objectMapper.readValue(repairedResponse, responseClass);
            } catch (Exception ex) {
                logger.error("Failed to parse JSON response even after repair into {}: {}", responseClass.getSimpleName(), ex.getMessage());
                throw new com.aicoursegenerator.ai.exception.AIResponseParsingException("JSON parsing failed", ex);
            }
        }
    }

    private String executeWithRetry(String systemPrompt, String userPrompt, boolean requireJson) {
        int maxRetries = 3;
        int delayMs = 1500;

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                String prompt = userPrompt;
                if (requireJson && attempt > 0) {
                    prompt += "\n\nIMPORTANT: You must return ONLY a valid JSON object matching the requested schema. Do not enclose it in markdown blocks. Output raw JSON only.";
                }
                
                return callApi(systemPrompt, prompt, attempt + 1);
                
            } catch (Exception e) {
                boolean isRetryable = isRetryableError(e);
                logger.error("Attempt {} failed: {}", attempt + 1, e.getMessage());
                
                if (attempt == maxRetries - 1 || !isRetryable) {
                    logger.error("Exhausted all retries or non-retryable error. Propagating failure.");
                    throw new RuntimeException(e.getMessage(), e);
                }
                
                try {
                    logger.info("Retrying API call. Waiting {}ms...", delayMs);
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted during retry", ie);
                }
            }
        }
        throw new RuntimeException("AI API failed across all retries");
    }

    private boolean isRetryableError(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return true;
        return e instanceof HttpTimeoutException ||
               msg.contains("timeout") ||
               msg.contains("status 429") ||
               msg.contains("status 500") ||
               msg.contains("status 502") ||
               msg.contains("status 503") ||
               msg.contains("status 504") ||
               msg.contains("model unavailable");
    }

    private String cleanJsonResponse(String response) {
        if (response == null) return "";
        response = response.trim();
        if (response.startsWith("```json")) {
            response = response.substring(7);
        } else if (response.startsWith("```")) {
            response = response.substring(3);
        }
        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }
        
        int firstBrace = response.indexOf('{');
        int firstBracket = response.indexOf('[');
        int startIndex = -1;
        
        if (firstBrace != -1 && firstBracket != -1) {
            startIndex = Math.min(firstBrace, firstBracket);
        } else if (firstBrace != -1) {
            startIndex = firstBrace;
        } else if (firstBracket != -1) {
            startIndex = firstBracket;
        }
        
        if (startIndex != -1) {
            response = response.substring(startIndex);
        }
        
        int lastBrace = response.lastIndexOf('}');
        int lastBracket = response.lastIndexOf(']');
        int endIndex = -1;
        
        if (lastBrace != -1 && lastBracket != -1) {
            endIndex = Math.max(lastBrace, lastBracket);
        } else if (lastBrace != -1) {
            endIndex = lastBrace;
        } else if (lastBracket != -1) {
            endIndex = lastBracket;
        }
        
        if (endIndex != -1) {
            response = response.substring(0, endIndex + 1);
        }
        
        return response.trim();
    }

    private String callApi(String systemPrompt, String userPrompt, int attemptCount) throws Exception {
        Map<String, Object> requestBody = Map.of(
                "model", this.model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );
        
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        
        logger.info("--- AI Request ---");
        logger.info("Provider: OmniRoute");
        logger.info("Combo: {}", this.model);
        logger.info("Base URL: {}", this.baseUrl);
        logger.info("Request Length: {} chars", jsonBody.length());
        
        long startTime = System.currentTimeMillis();
        
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(90))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
                
        if (this.apiKey != null && !this.apiKey.trim().isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + apiKey);
        }

        HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        long duration = System.currentTimeMillis() - startTime;
        
        logger.info("--- AI Response ---");
        logger.info("HTTP Status: {}", response.statusCode());
        logger.info("Response Time: {} ms", duration);

        if (response.statusCode() != 200) {
            logger.error("API Error Response Body: {}", response.body());
            throw new RuntimeException("API returned status " + response.statusCode() + ": " + response.body());
        }
        
        logger.debug("Raw Response: {}", response.body());
        JsonNode rootNode = objectMapper.readTree(response.body());
        
        if (rootNode.has("error")) {
            String errorMsg = rootNode.path("error").path("message").asText("Unknown AI Error");
            logger.error("API Error: {}", errorMsg);
            throw new com.aicoursegenerator.ai.exception.AIResponseParsingException("API Error: " + errorMsg);
        }

        if (!rootNode.has("choices") || !rootNode.get("choices").isArray() || rootNode.get("choices").size() == 0) {
            throw new com.aicoursegenerator.ai.exception.AIResponseParsingException("Malformed Response: Missing or empty 'choices' array");
        }
        
        // Log actually resolved model from OmniRoute and token usage
        if (rootNode.has("model")) {
            logger.info("Model actually resolved: {}", rootNode.path("model").asText());
        }
        
        if (rootNode.has("usage")) {
            JsonNode usage = rootNode.get("usage");
            logger.info("Token Usage - Prompt: {}, Completion: {}, Total: {}", 
                usage.path("prompt_tokens").asInt(0),
                usage.path("completion_tokens").asInt(0),
                usage.path("total_tokens").asInt(0)
            );
        } else {
            logger.info("Token Usage: Not provided by API");
        }
        
        logger.info("Retry Count: {}", attemptCount - 1);
        
        JsonNode firstChoice = rootNode.get("choices").get(0);
        if (firstChoice == null || firstChoice.isMissingNode() || firstChoice.isNull()) {
            throw new com.aicoursegenerator.ai.exception.AIResponseParsingException("Malformed Response: First choice is null");
        }
        
        return firstChoice.path("message").path("content").asText();
    }

    @Override
    public void streamText(String systemPrompt, String userPrompt, SseEmitter emitter, Consumer<String> onComplete) {
        streamTextWithRetry(systemPrompt, userPrompt, emitter, onComplete, 0);
    }

    private void streamTextWithRetry(String systemPrompt, String userPrompt, SseEmitter emitter, Consumer<String> onComplete, int attemptIndex) {
        int maxRetries = 3;
        if (attemptIndex >= maxRetries) {
            emitter.completeWithError(new RuntimeException("API Streaming failed after " + maxRetries + " attempts"));
            return;
        }
        
        new Thread(() -> {
            try {
                Map<String, Object> requestBody = Map.of(
                        "model", this.model,
                        "messages", List.of(
                                Map.of("role", "system", "content", systemPrompt),
                                Map.of("role", "user", "content", userPrompt)
                        ),
                        "stream", true
                );

                String jsonBody = objectMapper.writeValueAsString(requestBody);

                logger.info("--- AI Streaming Request ---");
                logger.info("Provider: OmniRoute");
                logger.info("Combo: {}", this.model);
                logger.info("Endpoint: {}", this.baseUrl);
                
                long startTime = System.currentTimeMillis();

                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/chat/completions"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
                        
                if (this.apiKey != null && !this.apiKey.trim().isEmpty()) {
                    requestBuilder.header("Authorization", "Bearer " + apiKey);
                }

                HttpResponse<Stream<String>> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofLines());
                long duration = System.currentTimeMillis() - startTime;

                logger.info("--- AI Streaming Connect ---");
                logger.info("HTTP Status: {}", response.statusCode());
                logger.info("Connection Time: {} ms", duration);

                if (response.statusCode() != 200) {
                    emitter.send(SseEmitter.event().data("Error: API returned status " + response.statusCode()));
                    emitter.complete();
                    return;
                }

                StringBuilder fullContent = new StringBuilder();

                response.body().forEach(line -> {
                    if (line.startsWith("data: ") && !line.equals("data: [DONE]")) {
                        String data = line.substring(6);
                        try {
                            JsonNode rootNode = objectMapper.readTree(data);
                            
                            if (rootNode.has("error")) {
                                String errorMsg = rootNode.path("error").path("message").asText("Unknown AI Error");
                                logger.error("API Error in stream: {}", errorMsg);
                                throw new com.aicoursegenerator.ai.exception.AIResponseParsingException("API Error in stream: " + errorMsg);
                            }
                            
                            JsonNode choices = rootNode.path("choices");
                            if (choices.isArray() && choices.size() > 0) {
                                JsonNode firstChoice = choices.get(0);
                                if (firstChoice != null && !firstChoice.isMissingNode() && !firstChoice.isNull()) {
                                    JsonNode delta = firstChoice.path("delta");
                                    if (delta.has("content")) {
                                        String text = delta.get("content").asText();
                                        fullContent.append(text);
                                        
                                        Map<String, String> chunkMap = Map.of("text", text);
                                        String chunkJson = objectMapper.writeValueAsString(chunkMap);
                                        emitter.send(SseEmitter.event().data(chunkJson));
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to parse SSE line: {}", line, e);
                        }
                    }
                });

                if (onComplete != null) {
                    onComplete.accept(fullContent.toString());
                }
                emitter.complete();
            } catch (Exception e) {
                logger.error("Streaming failed on attempt {}: {}", attemptIndex + 1, e.getMessage());
                if (isRetryableError(e)) {
                    logger.info("Attempting retry for streaming...");
                    streamTextWithRetry(systemPrompt, userPrompt, emitter, onComplete, attemptIndex + 1);
                } else {
                    logger.error("Non-retryable streaming error.", e);
                    emitter.completeWithError(e);
                }
            }
        }).start();
    }
}
