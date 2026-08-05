package com.aicoursegenerator.ai.service;

import org.springframework.stereotype.Component;

@Component
public class AiProviderFactory {

    private final AiProvider geminiProvider;

    public AiProviderFactory(AiProvider geminiProvider) {
        this.geminiProvider = geminiProvider;
    }

    public AiProvider getProvider() {
        return geminiProvider;
    }
}
