package com.aicoursegenerator.chat.dto;

import java.time.ZonedDateTime;
import java.util.UUID;

public record ChatSessionResponse(
    UUID id,
    UUID courseId,
    String title,
    ZonedDateTime createdAt,
    ZonedDateTime updatedAt
) {}
