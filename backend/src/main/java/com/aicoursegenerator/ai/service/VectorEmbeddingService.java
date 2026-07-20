package com.aicoursegenerator.ai.service;

import com.aicoursegenerator.pdf.entity.PDFChunk;
import com.aicoursegenerator.pdf.entity.PDFChunkEmbedding;
import com.aicoursegenerator.pdf.entity.PDFMetadata;
import com.aicoursegenerator.pdf.repository.PDFChunkEmbeddingRepository;
import com.aicoursegenerator.pdf.repository.PDFChunkRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VectorEmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(VectorEmbeddingService.class);

    private final PDFChunkRepository chunkRepository;
    private final PDFChunkEmbeddingRepository embeddingRepository;
    private final ObjectMapper objectMapper;

    public VectorEmbeddingService(
            PDFChunkRepository chunkRepository,
            PDFChunkEmbeddingRepository embeddingRepository,
            ObjectMapper objectMapper) {
        this.chunkRepository = chunkRepository;
        this.embeddingRepository = embeddingRepository;
        this.objectMapper = objectMapper;
    }

    public double[] getEmbedding(String text) {
        return generateMockEmbedding(text);
    }

    private double[] generateMockEmbedding(String text) {
        // Generate a stable pseudo-random 768-dimension vector based on text content
        double[] vector = new double[768];
        long seed = text.hashCode();
        Random rand = new Random(seed);
        double sumSquare = 0;
        for (int i = 0; i < 768; i++) {
            vector[i] = rand.nextGaussian();
            sumSquare += vector[i] * vector[i];
        }
        // Normalize the vector (so cosine similarity is just dot product)
        double norm = Math.sqrt(sumSquare);
        if (norm > 0) {
            for (int i = 0; i < 768; i++) {
                vector[i] /= norm;
            }
        }
        return vector;
    }

    @Transactional
    public void synchronizeEmbeddings(PDFMetadata pdfMetadata) {
        logger.info("Synchronizing vector embeddings for PDF ID: {}", pdfMetadata.getId());
        List<PDFChunk> chunks = chunkRepository.findByPdfMetadataOrderByChunkIndexAsc(pdfMetadata);
        
        // Remove any outdated Chroma collection first to ensure consistency
        executeChromaCommand("delete", pdfMetadata.getId().toString(), null, null, null);

        for (PDFChunk chunk : chunks) {
            double[] vector = getEmbedding(chunk.getContent());
            
            // Save to PostgreSQL (source of truth)
            PDFChunkEmbedding embeddingEntity = new PDFChunkEmbedding(
                    UUID.randomUUID(),
                    chunk,
                    vector
            );
            embeddingRepository.save(embeddingEntity);

            // Save to ChromaDB
            try {
                String vectorJson = objectMapper.writeValueAsString(vector);
                executeChromaCommand(
                        "add",
                        pdfMetadata.getId().toString(),
                        String.valueOf(chunk.getChunkIndex()),
                        vectorJson,
                        chunk.getContent()
                );
            } catch (Exception e) {
                logger.error("Failed to sync chunk {} to ChromaDB: {}", chunk.getChunkIndex(), e.getMessage());
            }
        }
        logger.info("Successfully indexed and synchronized {} chunks to ChromaDB & PostgreSQL", chunks.size());
    }

    public List<PDFChunk> searchSemantic(UUID pdfMetadataId, String queryText, int limit) {
        logger.info("Performing semantic search in PDF ID: {} for: '{}'", pdfMetadataId, queryText);
        double[] queryEmbedding = getEmbedding(queryText);

        try {
            String queryEmbeddingJson = objectMapper.writeValueAsString(queryEmbedding);
            String outputJson = executeChromaCommand(
                    "query",
                    pdfMetadataId.toString(),
                    queryEmbeddingJson,
                    String.valueOf(limit),
                    null
            );

            if (outputJson != null) {
                Map<String, Object> respMap = objectMapper.readValue(outputJson, new TypeReference<Map<String, Object>>() {});
                if (Boolean.TRUE.equals(respMap.get("success"))) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> results = (List<Map<String, Object>>) respMap.get("results");
                    if (results != null && !results.isEmpty()) {
                        List<PDFChunk> matchedChunks = new ArrayList<>();
                        for (Map<String, Object> item : results) {
                            int chunkIndex = ((Number) item.get("chunkIndex")).intValue();
                            // Fetch chunk from DB
                            Optional<PDFChunk> optChunk = findChunkByIndex(pdfMetadataId, chunkIndex);
                            optChunk.ifPresent(matchedChunks::add);
                        }
                        if (!matchedChunks.isEmpty()) {
                            logger.info("ChromaDB search successful. Retrieved {} matched chunks.", matchedChunks.size());
                            return matchedChunks;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("ChromaDB query failed: {}. Falling back to PostgreSQL cosine similarity search.", e.getMessage());
        }

        // Database/Java Fallback
        return searchPostgresFallback(pdfMetadataId, queryEmbedding, limit);
    }

    private Optional<PDFChunk> findChunkByIndex(UUID pdfMetadataId, int chunkIndex) {
        return chunkRepository.findByPdfMetadataIdAndChunkIndex(pdfMetadataId, chunkIndex);
    }

    private List<PDFChunk> searchPostgresFallback(UUID pdfMetadataId, double[] queryEmbedding, int limit) {
        logger.info("Executing database-backed cosine similarity fallback search for PDF: {}", pdfMetadataId);
        List<PDFChunkEmbedding> allEmbeddings = embeddingRepository.findByPdfMetadataId(pdfMetadataId);
        
        if (allEmbeddings.isEmpty()) {
            logger.warn("No stored embeddings found for PDF ID: {} in PostgreSQL", pdfMetadataId);
            return Collections.emptyList();
        }

        // Calculate cosine similarities
        List<Map.Entry<PDFChunk, Double>> scoredChunks = new ArrayList<>();
        for (PDFChunkEmbedding emb : allEmbeddings) {
            double similarity = calculateCosineSimilarity(queryEmbedding, emb.getEmbedding());
            scoredChunks.add(new AbstractMap.SimpleEntry<>(emb.getPdfChunk(), similarity));
        }

        // Sort descending by score, limit results, and return chunks
        return scoredChunks.stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private double calculateCosineSimilarity(double[] vectorA, double[] vectorB) {
        if (vectorA.length != vectorB.length) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private String executeChromaCommand(String action, String collectionName, String arg1, String arg2, String arg3) {
        try {
            List<String> command = new ArrayList<>();
            command.add("python");
            command.add("chroma_client.py");
            command.add(action);
            command.add(collectionName);

            if (arg1 != null) command.add(arg1);
            if (arg2 != null) command.add(arg2);
            if (arg3 != null) command.add(arg3);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(".")); // Root directory where chroma_client.py sits
            pb.redirectErrorStream(true);
            Process p = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                logger.error("chroma_client.py script error, exit code: {}, output: {}", exitCode, output);
                return null;
            }
            return output.toString();
        } catch (Exception e) {
            logger.error("Failed to execute Chroma Python CLI command: {}", e.getMessage());
            return null;
        }
    }
}
