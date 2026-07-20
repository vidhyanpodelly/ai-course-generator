package com.aicoursegenerator.course.controller;

import com.aicoursegenerator.common.dto.ApiResponse;
import com.aicoursegenerator.course.service.DiagramService;
import com.aicoursegenerator.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
public class DiagramController {

    private final DiagramService diagramService;

    public DiagramController(DiagramService diagramService) {
        this.diagramService = diagramService;
    }

    @GetMapping("/chapters/{chapterId}/mindmap")
    public ResponseEntity<ApiResponse<Map<String, String>>> getOrGenerateMindMap(
            @PathVariable("chapterId") UUID chapterId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String mermaidData = diagramService.getOrGenerateMindMap(chapterId, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("Mind map loaded successfully", Map.of("mermaidData", mermaidData)));
    }

    @GetMapping("/chapters/{chapterId}/diagram")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateDiagram(
            @PathVariable("chapterId") UUID chapterId,
            @RequestParam("type") String type,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String mermaidData = diagramService.generateDiagram(chapterId, type, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("Diagram generated successfully", Map.of("mermaidData", mermaidData)));
    }
}
