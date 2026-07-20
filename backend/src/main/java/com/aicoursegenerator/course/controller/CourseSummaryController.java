package com.aicoursegenerator.course.controller;

import com.aicoursegenerator.common.dto.ApiResponse;
import com.aicoursegenerator.course.service.CourseSummaryService;
import com.aicoursegenerator.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
public class CourseSummaryController {

    private final CourseSummaryService summaryService;

    public CourseSummaryController(CourseSummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping("/{courseId}/summary")
    public ResponseEntity<ApiResponse<CourseSummaryService.PDFSummaryResponse>> getOrGenerateSummary(
            @PathVariable("courseId") UUID courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CourseSummaryService.PDFSummaryResponse summary = summaryService.getOrGenerateSummary(courseId, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("Syllabus summaries retrieved successfully", summary));
    }
}
