package com.aicoursegenerator.ai.service;

import org.springframework.stereotype.Component;

@Component
public class AiProviderFactory {

    private final AiProvider openAICompatibleProvider;

    public AiProviderFactory(AiProvider openAICompatibleProvider) {
        this.openAICompatibleProvider = openAICompatibleProvider;
    }

    public AiProvider getProvider() {
        return openAICompatibleProvider;
    }
}
