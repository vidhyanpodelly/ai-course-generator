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
            @Value("${ai.openrouter.model:google/gemini-flash-1.5}") String model,
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
        return callOpenRouterApi(systemPrompt, userPrompt, false);
    }

    @Override
    public <T> T generateStructuredJson(String systemPrompt, String userPrompt, Class<T> responseClass) {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String prompt = userPrompt;
                if (attempt > 1) {
                    prompt += "\n\nIMPORTANT: You must return ONLY a valid JSON object matching the requested schema. Do not enclose it in markdown blocks. Output raw JSON only.";
                }
                String jsonResponse = callOpenRouterApi(systemPrompt, prompt, true);
                jsonResponse = cleanJsonResponse(jsonResponse);
                
                T result = objectMapper.readValue(jsonResponse, responseClass);
                return result;
            } catch (Exception e) {
                logger.error("Attempt {} failed for OpenRouter JSON generation: {}", attempt, e.getMessage());
                if (attempt == maxRetries) {
                    throw new RuntimeException("OpenRouter JSON generation failed after " + maxRetries + " attempts", e);
                }
            }
        }
        return null;
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

    private String callOpenRouterApi(String systemPrompt, String userPrompt, boolean requireJson) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", this.model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    )
            );
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("HTTP-Referer", "http://localhost:3000") // Required by OpenRouter
                    .header("X-Title", "AI Course Generator") // Optional but good for OpenRouter
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("OpenRouter returned status " + response.statusCode() + ": " + response.body());
            }
            
            JsonNode rootNode = objectMapper.readTree(response.body());
            return rootNode.path("choices").get(0).path("message").path("content").asText();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to call OpenRouter API: " + e.getMessage(), e);
        }
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

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .header("HTTP-Referer", "http://localhost:3000")
                        .header("X-Title", "AI Course Generator")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<Stream<String>> response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines());

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
