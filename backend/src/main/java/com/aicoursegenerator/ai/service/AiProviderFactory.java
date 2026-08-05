package com.aicoursegenerator.ai.service;

import org.springframework.stereotype.Component;

@Component
public class AiProviderFactory {

    private final AiProvider nvidiaProvider;

    public AiProviderFactory(AiProvider nvidiaProvider) {
        this.nvidiaProvider = nvidiaProvider;
    }

    public AiProvider getProvider() {
        return nvidiaProvider;
    }
}
