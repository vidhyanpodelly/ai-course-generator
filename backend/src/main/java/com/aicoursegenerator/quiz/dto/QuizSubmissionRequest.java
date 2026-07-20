package com.aicoursegenerator.quiz.dto;

import java.util.Map;

public record QuizSubmissionRequest(
    Map<String, String> answers
) {}
