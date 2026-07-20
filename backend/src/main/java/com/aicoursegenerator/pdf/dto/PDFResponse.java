package com.aicoursegenerator.pdf.dto;

import java.time.ZonedDateTime;
import java.util.UUID;

public record PDFResponse(
    UUID id,
    String filename,
    Long fileSize,
    Integer totalPages,
    String status,
    String failureReason,
    ZonedDateTime createdAt
) {}
