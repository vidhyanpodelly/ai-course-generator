package com.aicoursegenerator.course.dto;

import java.util.List;
import java.util.UUID;

public record ChapterResponse(
    UUID id,
    UUID courseId,
    String title,
    String summary,
    int sequenceNumber,
    List<LessonResponse> lessons,
    boolean hasQuiz
) {}
