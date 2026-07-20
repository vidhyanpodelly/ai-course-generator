package com.aicoursegenerator.pdf.repository;

import com.aicoursegenerator.pdf.entity.PDFMetadata;
import com.aicoursegenerator.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PDFMetadataRepository extends JpaRepository<PDFMetadata, UUID> {
    List<PDFMetadata> findByUser(User user);
    Optional<PDFMetadata> findByIdAndUser(UUID id, User user);
}
