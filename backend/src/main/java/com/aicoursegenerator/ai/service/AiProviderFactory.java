package com.aicoursegenerator.ai.service;

import org.springframework.stereotype.Component;

@Component
public class AiProviderFactory {

    private final AiProvider openRouterProvider;

    public AiProviderFactory(AiProvider openRouterProvider) {
        this.openRouterProvider = openRouterProvider;
    }

    public AiProvider getProvider() {
        return openRouterProvider;
    }
}
