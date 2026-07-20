package com.aicoursegenerator.ai.service;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class PromptLoader {

    public String loadPrompt(String filename) {
        try (InputStream is = getClass().getResourceAsStream("/prompts/" + filename)) {
            if (is == null) {
                throw new RuntimeException("Prompt template not found: /prompts/" + filename);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read prompt template: " + filename, e);
        }
    }

    public String interpolate(String template, Map<String, String> variables) {
        if (template == null) {
            return "";
        }
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }
}
