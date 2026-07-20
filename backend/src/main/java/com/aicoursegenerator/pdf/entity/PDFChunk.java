package com.aicoursegenerator.pdf.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "pdf_chunks")
public class PDFChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pdf_metadata_id", nullable = false)
    private PDFMetadata pdfMetadata;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    public PDFChunk() {
    }

    public PDFChunk(UUID id, PDFMetadata pdfMetadata, Integer chunkIndex, String content) {
        this.id = id;
        this.pdfMetadata = pdfMetadata;
        this.chunkIndex = chunkIndex;
        this.content = content;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public PDFMetadata getPdfMetadata() {
        return pdfMetadata;
    }

    public void setPdfMetadata(PDFMetadata pdfMetadata) {
        this.pdfMetadata = pdfMetadata;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
