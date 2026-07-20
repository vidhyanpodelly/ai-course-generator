package com.aicoursegenerator.course.dto;

import java.util.List;

public record LessonExplanationResponse(
    String explanation,
    List<String> keyTakeaways,
    List<String> importantNotes,
    List<String> realWorldExamples
) {}
