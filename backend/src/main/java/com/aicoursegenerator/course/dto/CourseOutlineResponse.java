package com.aicoursegenerator.course.dto;

import java.util.List;

public record CourseOutlineResponse(
    String title,
    String description,
    String estimatedDuration,
    String difficultyLevel,
    List<String> prerequisites,
    List<String> learningObjectives,
    List<ChapterOutline> chapters
) {
    public record ChapterOutline(
        String title,
        String summary,
        int sequenceNumber,
        List<LessonOutline> lessons
    ) {}

    public record LessonOutline(
        String title,
        int sequenceNumber
    ) {}
}
