package com.aicoursegenerator.course.controller;

import com.aicoursegenerator.common.dto.ApiResponse;
import com.aicoursegenerator.course.service.CertificateService;
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
public class CertificateController {

    private final CertificateService certificateService;

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @GetMapping("/{courseId}/certificate")
    public ResponseEntity<ApiResponse<CertificateService.CertificateData>> getCertificate(
            @PathVariable("courseId") UUID courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CertificateService.CertificateData data = certificateService.getCertificate(courseId, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("Certificate compiled successfully", data));
    }
}
