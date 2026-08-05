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
        
        String filenameExtension;
        MediaType mediaType;
        byte[] bytes;

        if ("HTML".equalsIgnoreCase(format)) {
            String content = exportService.exportHtml(courseId, userDetails.getUser());
            bytes = content.getBytes(StandardCharsets.UTF_8);
            filenameExtension = "html";
            mediaType = MediaType.TEXT_HTML;
        } else if ("PDF".equalsIgnoreCase(format)) {
            bytes = exportService.exportPdf(courseId, userDetails.getUser());
            filenameExtension = "pdf";
            mediaType = MediaType.APPLICATION_PDF;
        } else if ("ZIP".equalsIgnoreCase(format)) {
            bytes = exportService.exportZip(courseId, userDetails.getUser());
            filenameExtension = "zip";
            mediaType = MediaType.parseMediaType("application/zip");
        } else {
            String content = exportService.exportMarkdown(courseId, userDetails.getUser());
            bytes = content.getBytes(StandardCharsets.UTF_8);
            filenameExtension = "md";
            mediaType = MediaType.TEXT_MARKDOWN;
        }

        String filename = "course_export_" + courseId + "." + filenameExtension;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(mediaType)
                .contentLength(bytes.length)
                .body(bytes);
    }
}
