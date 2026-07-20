package com.aicoursegenerator.chat.dto;

import java.time.ZonedDateTime;
import java.util.UUID;

public record ChatMessageResponse(
    UUID id,
    UUID sessionId,
    String sender,
    String messageContent,
    ZonedDateTime createdAt
) {}
