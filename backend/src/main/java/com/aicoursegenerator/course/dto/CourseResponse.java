package com.aicoursegenerator.course.dto;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public record CourseResponse(
    UUID id,
    UUID pdfMetadataId,
    String title,
    String description,
    String estimatedDuration,
    String difficultyLevel,
    List<String> prerequisites,
    List<String> learningObjectives,
    String status,
    String failureReason,
    ZonedDateTime createdAt
) {}
