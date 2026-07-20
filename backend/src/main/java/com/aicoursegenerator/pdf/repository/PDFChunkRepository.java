package com.aicoursegenerator.pdf.repository;

import com.aicoursegenerator.pdf.entity.PDFChunk;
import com.aicoursegenerator.pdf.entity.PDFMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PDFChunkRepository extends JpaRepository<PDFChunk, UUID> {
    List<PDFChunk> findByPdfMetadataOrderByChunkIndexAsc(PDFMetadata pdfMetadata);

    @Query("SELECT c FROM PDFChunk c WHERE c.pdfMetadata.id = :pdfMetadataId AND c.chunkIndex = :chunkIndex")
    java.util.Optional<PDFChunk> findByPdfMetadataIdAndChunkIndex(
            @Param("pdfMetadataId") java.util.UUID pdfMetadataId,
            @Param("chunkIndex") int chunkIndex
    );


    @Query(value = "SELECT * FROM pdf_chunks WHERE pdf_metadata_id = :pdfMetadataId " +
                   "AND to_tsvector('english', content) @@ plainto_tsquery('english', :query) " +
                   "LIMIT :limit", nativeQuery = true)
    List<PDFChunk> searchChunks(
            @Param("pdfMetadataId") UUID pdfMetadataId,
            @Param("query") String query,
            @Param("limit") int limit
    );
}
