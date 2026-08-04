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
public class OpenRouterProvider implements AiProvider {

    private static final Logger logger = LoggerFactory.getLogger(OpenRouterProvider.class);

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenRouterProvider(
            @Value("${ai.openrouter.key:}") String apiKey,
            @Value("${ai.openrouter.model:nvidia/nemotron-3-ultra-550b-a55b:free}") String model,
            @Value("${ai.openrouter.url:https://openrouter.ai/api/v1/chat/completions}") String baseUrl,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
                
        logger.info("Active AI Provider: OpenRouter (Model: {}, Key length: {})", 
                    model, apiKey != null ? apiKey.length() : 0);
    }

    @Override
    public String generateText(String systemPrompt, String userPrompt) {
        return executeWithRetry(systemPrompt, userPrompt, false);
    }

    @Override
    public <T> T generateStructuredJson(String systemPrompt, String userPrompt, Class<T> responseClass) {
        String jsonResponse = executeWithRetry(systemPrompt, userPrompt, true);
        jsonResponse = cleanJsonResponse(jsonResponse);
        try {
            return objectMapper.readValue(jsonResponse, responseClass);
        } catch (Exception e) {
            logger.error("Failed to parse JSON response into {}: {}", responseClass.getSimpleName(), e.getMessage());
            throw new RuntimeException("JSON parsing failed", e);
        }
    }

    private String executeWithRetry(String systemPrompt, String userPrompt, boolean requireJson) {
        int maxRetries = 3;
        int delayMs = 2000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String prompt = userPrompt;
                if (requireJson && attempt > 1) {
                    prompt += "\n\nIMPORTANT: You must return ONLY a valid JSON object matching the requested schema. Do not enclose it in markdown blocks. Output raw JSON only.";
                }
                
                return callOpenRouterApi(systemPrompt, prompt);
                
            } catch (Exception e) {
                boolean isRetryable = isRetryableError(e);
                logger.error("Attempt {} failed for OpenRouter API: {}", attempt, e.getMessage());
                
                if (attempt == maxRetries || !isRetryable) {
                    logger.error("Exhausted retries or non-retryable error. Propagating failure.");
                    throw new RuntimeException(e.getMessage(), e);
                }
                
                try {
                    logger.info("Waiting {}ms before next attempt...", delayMs);
                    Thread.sleep(delayMs);
                    delayMs *= 2; // Exponential backoff: 2s, 4s, 8s
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted during backoff", ie);
                }
            }
        }
        throw new RuntimeException("OpenRouter API failed after " + maxRetries + " attempts");
    }

    private boolean isRetryableError(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return true;
        // Check for common retryable conditions
        return e instanceof HttpTimeoutException ||
               msg.contains("timeout") ||
               msg.contains("status 429") ||
               msg.contains("status 500") ||
               msg.contains("status 502") ||
               msg.contains("status 503") ||
               msg.contains("status 504") ||
               msg.contains("status 404") ||
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

    private String callOpenRouterApi(String systemPrompt, String userPrompt) throws Exception {
        Map<String, Object> requestBody = Map.of(
                "model", this.model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );
        
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        
        logger.info("--- AI Request ---");
        logger.info("Provider: OpenRouter");
        logger.info("Model: {}", this.model);
        logger.info("Endpoint: {}", this.baseUrl);
        logger.info("Prompt length: {}", jsonBody.length());
        
        long startTime = System.currentTimeMillis();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("HTTP-Referer", "http://localhost:3000") // Required by OpenRouter
                .header("X-Title", "AI Course Generator") // Optional but good for OpenRouter
                .timeout(Duration.ofSeconds(90)) // Added 90s timeout explicitly
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        long duration = System.currentTimeMillis() - startTime;
        
        logger.info("--- AI Response ---");
        logger.info("HTTP Status: {}", response.statusCode());
        logger.info("Response Time: {} ms", duration);

        if (response.statusCode() != 200) {
            logger.error("OpenRouter Error Response Body: {}", response.body());
            throw new RuntimeException("OpenRouter returned status " + response.statusCode() + ": " + response.body());
        }
        
        JsonNode rootNode = objectMapper.readTree(response.body());
        
        // Try to log token usage
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
        
        return rootNode.path("choices").get(0).path("message").path("content").asText();
    }

    @Override
    public void streamText(String systemPrompt, String userPrompt, SseEmitter emitter, Consumer<String> onComplete) {
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
                logger.info("Provider: OpenRouter");
                logger.info("Model: {}", this.model);
                logger.info("Endpoint: {}", this.baseUrl);
                logger.info("Prompt length: {}", jsonBody.length());
                
                long startTime = System.currentTimeMillis();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .header("HTTP-Referer", "http://localhost:3000")
                        .header("X-Title", "AI Course Generator")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<Stream<String>> response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
                long duration = System.currentTimeMillis() - startTime;

                logger.info("--- AI Streaming Connect ---");
                logger.info("HTTP Status: {}", response.statusCode());
                logger.info("Connection Time: {} ms", duration);

                if (response.statusCode() != 200) {
                    emitter.send(SseEmitter.event().data("Error: OpenRouter returned status " + response.statusCode()));
                    emitter.complete();
                    return;
                }

                StringBuilder fullContent = new StringBuilder();

                response.body().forEach(line -> {
                    if (line.startsWith("data: ") && !line.equals("data: [DONE]")) {
                        String data = line.substring(6);
                        try {
                            JsonNode rootNode = objectMapper.readTree(data);
                            JsonNode choices = rootNode.path("choices");
                            if (choices.isArray() && choices.size() > 0) {
                                JsonNode delta = choices.get(0).path("delta");
                                if (delta.has("content")) {
                                    String text = delta.get("content").asText();
                                    fullContent.append(text);
                                    
                                    Map<String, String> chunkMap = Map.of("text", text);
                                    String chunkJson = objectMapper.writeValueAsString(chunkMap);
                                    emitter.send(SseEmitter.event().data(chunkJson));
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
                logger.error("Error in OpenRouter streaming: {}", e.getMessage(), e);
                emitter.completeWithError(e);
            }
        }).start();
    }
}
