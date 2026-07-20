package com.aicoursegenerator.pdf.controller;

import com.aicoursegenerator.common.dto.ApiResponse;
import com.aicoursegenerator.pdf.dto.PDFResponse;
import com.aicoursegenerator.pdf.entity.PDFMetadata;
import com.aicoursegenerator.pdf.service.PDFService;
import com.aicoursegenerator.security.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pdf")
public class PDFController {

    private final PDFService pdfService;

    public PDFController(PDFService pdfService) {
        this.pdfService = pdfService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<PDFResponse>> uploadPDF(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PDFMetadata metadata = pdfService.handleUpload(file, userDetails.getUser());
        PDFResponse response = mapToResponse(metadata);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("PDF uploaded and processed successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PDFResponse>>> getAllPDFs(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<PDFMetadata> metadataList = pdfService.getAllPDFsForUser(userDetails.getUser());
        List<PDFResponse> responseList = metadataList.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("Successfully retrieved PDFs", responseList));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PDFResponse>> getPDFDetails(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PDFMetadata metadata = pdfService.getMetadata(id, userDetails.getUser());
        PDFResponse response = mapToResponse(metadata);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("Successfully retrieved PDF metadata", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePDF(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        pdfService.deletePDF(id, userDetails.getUser());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("PDF deleted successfully", null));
    }

    private PDFResponse mapToResponse(PDFMetadata m) {
        return new PDFResponse(
                m.getId(),
                m.getFilename(),
                m.getFileSize(),
                m.getTotalPages(),
                m.getStatus(),
                m.getFailureReason(),
                m.getCreatedAt()
        );
    }
}
