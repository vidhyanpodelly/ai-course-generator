package com.aicoursegenerator.pdf.repository;

import com.aicoursegenerator.pdf.entity.PDFChunk;
import com.aicoursegenerator.pdf.entity.PDFChunkEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PDFChunkEmbeddingRepository extends JpaRepository<PDFChunkEmbedding, UUID> {
    Optional<PDFChunkEmbedding> findByPdfChunk(PDFChunk pdfChunk);

    @Modifying
    @Query("DELETE FROM PDFChunkEmbedding e WHERE e.pdfChunk.pdfMetadata.id = :pdfMetadataId")
    void deleteByPdfMetadataId(@Param("pdfMetadataId") UUID pdfMetadataId);

    @Query("SELECT e FROM PDFChunkEmbedding e WHERE e.pdfChunk.pdfMetadata.id = :pdfMetadataId")
    List<PDFChunkEmbedding> findByPdfMetadataId(@Param("pdfMetadataId") UUID pdfMetadataId);
}
