package com.aicoursegenerator.quiz.dto;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

public record QuizAttemptResponse(
    UUID id,
    UUID chapterId,
    int score,
    int totalQuestions,
    Map<String, String> answers,
    ZonedDateTime createdAt
) {}
