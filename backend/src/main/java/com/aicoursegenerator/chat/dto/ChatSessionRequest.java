package com.aicoursegenerator.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatSessionRequest(
    @NotBlank(message = "Title cannot be blank")
    String title
) {}
