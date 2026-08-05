package com.aicoursegenerator.ai.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Stack;

public class JsonRepairUtil {

    private static final Logger logger = LoggerFactory.getLogger(JsonRepairUtil.class);

    public static String repair(String json) {
        if (json == null || json.trim().isEmpty()) {
            return "{}";
        }

        String cleaned = json.trim();
        // Remove markdown formatting if present
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        cleaned = cleaned.trim();

        // Basic truncation repair
        Stack<Character> brackets = new Stack<>();
        boolean inString = false;
        boolean escapeNext = false;

        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            
            if (escapeNext) {
                escapeNext = false;
                continue;
            }

            if (c == '\\') {
                escapeNext = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == '{' || c == '[') {
                    brackets.push(c);
                } else if (c == '}') {
                    if (!brackets.isEmpty() && brackets.peek() == '{') {
                        brackets.pop();
                    }
                } else if (c == ']') {
                    if (!brackets.isEmpty() && brackets.peek() == '[') {
                        brackets.pop();
                    }
                }
            }
        }

        StringBuilder repaired = new StringBuilder(cleaned);
        
        // Close string if open
        if (inString) {
            repaired.append("\"");
        }

        // Add closing brackets
        while (!brackets.isEmpty()) {
            char open = brackets.pop();
            if (open == '{') {
                repaired.append("}");
            } else if (open == '[') {
                repaired.append("]");
            }
        }

        String result = repaired.toString();
        if (!result.equals(cleaned)) {
            logger.warn("JSON Repair applied. Original length: {}, Repaired length: {}", cleaned.length(), result.length());
        }

        // Strip leading garbage before first { or [
        int firstBrace = result.indexOf('{');
        int firstBracket = result.indexOf('[');
        int start = -1;
        if (firstBrace != -1 && firstBracket != -1) {
            start = Math.min(firstBrace, firstBracket);
        } else if (firstBrace != -1) {
            start = firstBrace;
        } else if (firstBracket != -1) {
            start = firstBracket;
        }
        if (start > 0) {
            result = result.substring(start);
        }

        return result.trim();
    }
}
