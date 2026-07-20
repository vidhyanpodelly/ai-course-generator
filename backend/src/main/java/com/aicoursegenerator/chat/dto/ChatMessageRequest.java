package com.aicoursegenerator.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatMessageRequest(
    @NotBlank(message = "Message content cannot be blank")
    String message
) {}
