package com.aicoursegenerator.course.controller;

import com.aicoursegenerator.course.service.CourseExportService;
import com.aicoursegenerator.security.CustomUserDetails;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
public class CourseExportController {

    private final CourseExportService exportService;

    public CourseExportController(CourseExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/{courseId}/export")
    public ResponseEntity<byte[]> exportCourse(
            @PathVariable("courseId") UUID courseId,
            @RequestParam(value = "format", defaultValue = "MD") String format,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        String content;
        String filenameExtension;
        MediaType mediaType;

        if ("HTML".equalsIgnoreCase(format)) {
            content = exportService.exportHtml(courseId, userDetails.getUser());
            filenameExtension = "html";
            mediaType = MediaType.TEXT_HTML;
        } else {
            content = exportService.exportMarkdown(courseId, userDetails.getUser());
            filenameExtension = "md";
            mediaType = MediaType.TEXT_MARKDOWN;
        }

        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        String filename = "course_export_" + courseId + "." + filenameExtension;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(mediaType)
                .contentLength(bytes.length)
                .body(bytes);
    }
}
