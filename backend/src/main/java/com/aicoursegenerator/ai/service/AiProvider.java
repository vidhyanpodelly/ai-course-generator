package com.aicoursegenerator.ai.service;

public interface AiProvider {
    String generateText(String systemPrompt, String userPrompt);
    <T> T generateStructuredJson(String systemPrompt, String userPrompt, Class<T> responseClass);
    void streamText(String systemPrompt, String userPrompt, org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter, java.util.function.Consumer<String> onComplete);
}
