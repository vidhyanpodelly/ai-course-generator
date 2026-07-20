package com.aicoursegenerator.pdf.service;

import com.aicoursegenerator.ai.service.VectorEmbeddingService;
import com.aicoursegenerator.common.exception.BadRequestException;
import com.aicoursegenerator.common.exception.ResourceNotFoundException;
import com.aicoursegenerator.pdf.entity.PDFChunk;
import com.aicoursegenerator.pdf.entity.PDFMetadata;
import com.aicoursegenerator.pdf.repository.PDFChunkRepository;
import com.aicoursegenerator.pdf.repository.PDFMetadataRepository;
import com.aicoursegenerator.user.entity.User;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PDFService {

    private static final Logger logger = LoggerFactory.getLogger(PDFService.class);

    private final PDFMetadataRepository metadataRepository;
    private final PDFChunkRepository chunkRepository;
    private final VectorEmbeddingService vectorEmbeddingService;
    private final Path uploadDir;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public PDFService(
            PDFMetadataRepository metadataRepository,
            PDFChunkRepository chunkRepository,
            VectorEmbeddingService vectorEmbeddingService,
            @Value("${app.upload.dir}") String uploadDirStr) {
        this.metadataRepository = metadataRepository;
        this.chunkRepository = chunkRepository;
        this.vectorEmbeddingService = vectorEmbeddingService;
        this.uploadDir = Paths.get(uploadDirStr).toAbsolutePath().normalize();
        
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            logger.error("Failed to create upload directory: {}", uploadDirStr, e);
            throw new RuntimeException("Could not create directory for PDF uploads", e);
        }
    }

    @Transactional
    public PDFMetadata handleUpload(MultipartFile file, User user) {
        if (file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new BadRequestException("Only PDF files are supported");
        }

        logger.info("Stage: PDF Upload - file: {}, size: {} bytes", originalFilename, file.getSize());

        // Generate unique filename to avoid duplicates
        String savedFilename = UUID.randomUUID().toString() + "_" + originalFilename;
        Path targetLocation = this.uploadDir.resolve(savedFilename);

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            logger.info("PDF saved locally at: {}", targetLocation);
        } catch (IOException e) {
            logger.error("Error storing file: {}", originalFilename, e);
            throw new RuntimeException("Failed to save uploaded PDF file", e);
        }

        PDFMetadata metadata = PDFMetadata.builder()
                .user(user)
                .filename(originalFilename)
                .filePath(targetLocation.toString())
                .fileSize(file.getSize())
                .status("PENDING")
                .createdAt(ZonedDateTime.now())
                .build();

        PDFMetadata savedMetadata = metadataRepository.save(metadata);

        // Process PDF and extract text chunks in sync/background
        try {
            processAndChunkPDF(savedMetadata);
        } catch (Exception e) {
            logger.error("Failed to process and chunk PDF ID: {}", savedMetadata.getId(), e);
            savedMetadata.setStatus("FAILED");
            savedMetadata.setFailureReason(e.getMessage());
            metadataRepository.save(savedMetadata);
        }

        return savedMetadata;
    }

    private void processAndChunkPDF(PDFMetadata metadata) throws IOException {
        File pdfFile = new File(metadata.getFilePath());
        
        logger.info("Stage: PDF Parsing - started for file: {}", metadata.getFilePath());
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            int totalPages = document.getNumberOfPages();
            metadata.setTotalPages(totalPages);
            logger.info("Stage: PDF Parsing - completed. Total pages: {}", totalPages);
            
            PDFTextStripper stripper = new PDFTextStripper();
            String fullText = stripper.getText(document);

            if (fullText == null || fullText.trim().isEmpty()) {
                throw new BadRequestException("PDF document text content is empty or unparsable");
            }

            // Chunking algorithm
            List<String> chunks = splitTextIntoChunks(fullText, 2000, 200);
            logger.info("Stage: Extracted Text - splitting text of length {} into chunks. Total chunks: {}", fullText.length(), chunks.size());
            
            List<PDFChunk> pdfChunks = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                PDFChunk chunk = new PDFChunk(null, metadata, i, chunks.get(i));
                pdfChunks.add(chunk);
            }

            chunkRepository.saveAll(pdfChunks);
            metadata.setStatus("PARSED");
            metadataRepository.save(metadata);
            logger.info("Successfully extracted and saved {} text chunks from PDF ID: {}", pdfChunks.size(), metadata.getId());

            try {
                vectorEmbeddingService.synchronizeEmbeddings(metadata);
            } catch (Exception e) {
                logger.error("Embedding synchronization failed for PDF ID: {}: {}", metadata.getId(), e.getMessage());
            }
        }
    }

    private List<String> splitTextIntoChunks(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }

        int length = text.length();
        int start = 0;

        while (start < length) {
            int end = Math.min(start + chunkSize, length);
            chunks.add(text.substring(start, end));
            
            if (end == length) {
                break;
            }
            start += (chunkSize - overlap);
        }
        return chunks;
    }

    public PDFMetadata getMetadata(UUID id, User user) {
        return metadataRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("PDF metadata record not found"));
    }

    public List<PDFMetadata> getAllPDFsForUser(User user) {
        return metadataRepository.findByUser(user);
    }

    @Transactional
    public void deletePDF(UUID id, User user) {
        PDFMetadata metadata = metadataRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("PDF metadata record not found"));
        
        // Delete associated course (if any) first to avoid foreign key constraint violations
        entityManager.createQuery("DELETE FROM ChatMessage cm WHERE cm.session IN (SELECT cs FROM ChatSession cs WHERE cs.course IN (SELECT c FROM Course c WHERE c.pdfMetadata = :metadata))")
                .setParameter("metadata", metadata)
                .executeUpdate();

        entityManager.createQuery("DELETE FROM ChatSession cs WHERE cs.course IN (SELECT c FROM Course c WHERE c.pdfMetadata = :metadata)")
                .setParameter("metadata", metadata)
                .executeUpdate();

        entityManager.createQuery("DELETE FROM QuizAttempt qa WHERE qa.chapter IN (SELECT ch FROM Chapter ch WHERE ch.course IN (SELECT c FROM Course c WHERE c.pdfMetadata = :metadata))")
                .setParameter("metadata", metadata)
                .executeUpdate();

        entityManager.createQuery("DELETE FROM LearningProgress lp WHERE lp.lesson IN (SELECT l FROM Lesson l WHERE l.chapter IN (SELECT ch FROM Chapter ch WHERE ch.course IN (SELECT c FROM Course c WHERE c.pdfMetadata = :metadata)))")
                .setParameter("metadata", metadata)
                .executeUpdate();

        entityManager.createQuery("DELETE FROM Lesson l WHERE l.chapter IN (SELECT ch FROM Chapter ch WHERE ch.course IN (SELECT c FROM Course c WHERE c.pdfMetadata = :metadata))")
                .setParameter("metadata", metadata)
                .executeUpdate();

        entityManager.createQuery("DELETE FROM Flashcard f WHERE f.chapter IN (SELECT ch FROM Chapter ch WHERE ch.course IN (SELECT c FROM Course c WHERE c.pdfMetadata = :metadata))")
                .setParameter("metadata", metadata)
                .executeUpdate();

        entityManager.createQuery("DELETE FROM MindMap m WHERE m.chapter IN (SELECT ch FROM Chapter ch WHERE ch.course IN (SELECT c FROM Course c WHERE c.pdfMetadata = :metadata))")
                .setParameter("metadata", metadata)
                .executeUpdate();

        entityManager.createQuery("DELETE FROM Chapter ch WHERE ch.course IN (SELECT c FROM Course c WHERE c.pdfMetadata = :metadata)")
                .setParameter("metadata", metadata)
                .executeUpdate();

        entityManager.createQuery("DELETE FROM Course c WHERE c.pdfMetadata = :metadata")
                .setParameter("metadata", metadata)
                .executeUpdate();

        // Chunks
        entityManager.createQuery("DELETE FROM PDFChunkEmbedding pce WHERE pce.pdfChunk IN (SELECT pc FROM PDFChunk pc WHERE pc.pdfMetadata = :metadata)")
                .setParameter("metadata", metadata)
                .executeUpdate();

        entityManager.createQuery("DELETE FROM PDFChunk pc WHERE pc.pdfMetadata = :metadata")
                .setParameter("metadata", metadata)
                .executeUpdate();
        
        // Delete physical file
        try {
            Path path = Paths.get(metadata.getFilePath());
            Files.deleteIfExists(path);
            logger.info("Deleted physical PDF file: {}", metadata.getFilePath());
        } catch (IOException e) {
            logger.error("Failed to delete physical file: {}", metadata.getFilePath(), e);
        }
        
        // Delete database record
        metadataRepository.delete(metadata);
        logger.info("Deleted PDF metadata record ID: {} from database", id);
    }
}
