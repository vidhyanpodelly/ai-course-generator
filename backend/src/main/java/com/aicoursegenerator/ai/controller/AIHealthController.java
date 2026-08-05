package com.aicoursegenerator.ai.controller;

import com.aicoursegenerator.ai.service.AIHealthCheckService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class AIHealthController {

    private final AIHealthCheckService healthCheckService;

    public AIHealthController(AIHealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }

    @GetMapping("/ai")
    public ResponseEntity<Map<String, Object>> getAiHealth() {
        if (healthCheckService.isAiAvailable()) {
            return ResponseEntity.ok(Map.of(
                    "status", "UP",
                    "provider", "OpenRouter",
                    "models", healthCheckService.getAvailableModels(),
                    "latency", healthCheckService.getLastLatency()
            ));
        } else {
            return ResponseEntity.status(503).body(Map.of(
                    "status", "DOWN",
                    "reason", healthCheckService.getLastFailureReason()
            ));
        }
    }
}
