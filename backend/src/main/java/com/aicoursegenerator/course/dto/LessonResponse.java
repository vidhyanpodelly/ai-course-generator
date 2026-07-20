package com.aicoursegenerator.course.dto;

import java.util.List;
import java.util.UUID;

public record LessonResponse(
    UUID id,
    UUID chapterId,
    String title,
    String explanation,
    List<String> keyTakeaways,
    List<String> importantNotes,
    List<String> realWorldExamples,
    int sequenceNumber,
    boolean completed
) {}
