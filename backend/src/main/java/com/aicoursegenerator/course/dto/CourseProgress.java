package com.aicoursegenerator.course.dto;

import java.util.List;
import java.util.UUID;

public record CourseProgress(
    int completedLessonsCount,
    int totalLessonsCount,
    double percentage,
    List<UUID> completedLessonIds
) {}
