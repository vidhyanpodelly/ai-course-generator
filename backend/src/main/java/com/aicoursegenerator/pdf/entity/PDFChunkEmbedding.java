package com.aicoursegenerator.pdf.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "pdf_chunk_embeddings")
public class PDFChunkEmbedding {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pdf_chunk_id", nullable = false)
    private PDFChunk pdfChunk;

    @Column(name = "embedding", nullable = false, columnDefinition = "float8[]")
    private double[] embedding;

    public PDFChunkEmbedding() {}

    public PDFChunkEmbedding(UUID id, PDFChunk pdfChunk, double[] embedding) {
        this.id = id;
        this.pdfChunk = pdfChunk;
        this.embedding = embedding;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public PDFChunk getPdfChunk() {
        return pdfChunk;
    }

    public void setPdfChunk(PDFChunk pdfChunk) {
        this.pdfChunk = pdfChunk;
    }

    public double[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(double[] embedding) {
        this.embedding = embedding;
    }
}
