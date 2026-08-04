package com.aicoursegenerator.course.dto;

import java.util.List;

public record SequenceResponse(
    List<String> actors,
    List<SequenceMessage> messages
) {
    public record SequenceMessage(
        String from,
        String to,
        String message
    ) {}
}
