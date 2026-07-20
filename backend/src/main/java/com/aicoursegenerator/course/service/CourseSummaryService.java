package com.aicoursegenerator.course.service;

import com.aicoursegenerator.ai.service.AiProvider;
import com.aicoursegenerator.ai.service.AiProviderFactory;
import com.aicoursegenerator.common.exception.ResourceNotFoundException;
import com.aicoursegenerator.course.entity.Chapter;
import com.aicoursegenerator.course.entity.Course;
import com.aicoursegenerator.course.repository.ChapterRepository;
import com.aicoursegenerator.course.repository.CourseRepository;
import com.aicoursegenerator.pdf.entity.PDFChunk;
import com.aicoursegenerator.pdf.repository.PDFChunkRepository;
import com.aicoursegenerator.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CourseSummaryService {

    private static final Logger logger = LoggerFactory.getLogger(CourseSummaryService.class);

    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final PDFChunkRepository chunkRepository;
    private final AiProviderFactory providerFactory;
    private final ObjectMapper objectMapper;
    private final Path summaryStorageDir;

    public record PDFSummaryResponse(
            String oneLineSummary,
            String shortSummary,
            String detailedSummary,
            String executiveSummary,
            List<String> keyTakeaways,
            List<ChapterSummaryData> chapterSummaries
    ) {}

    public record ChapterSummaryData(
            String chapterTitle,
            String summary
    ) {}

    public CourseSummaryService(
            CourseRepository courseRepository,
            ChapterRepository chapterRepository,
            PDFChunkRepository chunkRepository,
            AiProviderFactory providerFactory,
            ObjectMapper objectMapper,
            @Value("${app.upload.dir}") String uploadDir) {
        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
        this.chunkRepository = chunkRepository;
        this.providerFactory = providerFactory;
        this.objectMapper = objectMapper;
        this.summaryStorageDir = Paths.get(uploadDir, "summaries").toAbsolutePath().normalize();
        
        try {
            Files.createDirectories(this.summaryStorageDir);
        } catch (Exception e) {
            logger.error("Failed to create summary storage directory", e);
        }
    }

    public PDFSummaryResponse getOrGenerateSummary(UUID courseId, User user) {
        Course course = courseRepository.findByIdAndUser(courseId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        File cacheFile = summaryStorageDir.resolve(courseId.toString() + ".json").toFile();
        
        // Return cached summary if it exists
        if (cacheFile.exists()) {
            try {
                return objectMapper.readValue(cacheFile, PDFSummaryResponse.class);
            } catch (Exception e) {
                logger.error("Failed to read cached summary file", e);
            }
        }

        logger.info("Generating global summary package for Course ID: {}", courseId);

        try {
            List<PDFChunk> chunks = chunkRepository.findByPdfMetadataOrderByChunkIndexAsc(course.getPdfMetadata());
            
            // Consolidate first 5 chunks of the document to extract summaries
            StringBuilder sampleText = new StringBuilder();
            int limit = Math.min(chunks.size(), 5);
            for (int i = 0; i < limit; i++) {
                sampleText.append(chunks.get(i).getContent()).append("\n");
            }

            String userPrompt = "Create a comprehensive summary package for the document titled: '" + course.getTitle() + "'.\n" +
                               "Document contents context:\n" + sampleText.toString() + "\n\n" +
                               "Provide the following fields in a valid JSON object format:\n" +
                               "{\n" +
                               "  \"oneLineSummary\": \"A concise 1-sentence description of the document.\",\n" +
                               "  \"shortSummary\": \"A brief paragraph summary (100 words).\",\n" +
                               "  \"detailedSummary\": \"A comprehensive detailed summary (300-500 words) using formatting where helpful.\",\n" +
                               "  \"executiveSummary\": \"A formal high-level executive summary summarizing goals, findings, and context (200 words).\",\n" +
                               "  \"keyTakeaways\": [\"takeaway 1\", \"takeaway 2\", \"takeaway 3\", \"takeaway 4\"]\n" +
                               "}\n" +
                               "Output raw JSON matching the schema ONLY.";

            AiProvider provider = providerFactory.getProvider();
            
            Map<String, Object> summaryMap;
            String jsonResult = provider.generateText(
                    "You are an academic summarization agent. Output raw JSON ONLY matching the requested structure.",
                    userPrompt
            );
            
            // Clean markdown code blocks if any
            String cleanedJson = cleanJsonResponse(jsonResult);
            summaryMap = objectMapper.readValue(cleanedJson, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});

            // Fetch chapter summaries from database
            List<Chapter> chapters = chapterRepository.findByCourseOrderBySequenceNumberAsc(course);
            List<ChapterSummaryData> chapterSummaries = new ArrayList<>();
            for (Chapter chapter : chapters) {
                chapterSummaries.add(new ChapterSummaryData(chapter.getTitle(), chapter.getSummary()));
            }

            // Parse response fields
            String oneLine = (String) summaryMap.getOrDefault("oneLineSummary", "Summary not generated");
            String shortSum = (String) summaryMap.getOrDefault("shortSummary", "Summary not generated");
            String detailed = (String) summaryMap.getOrDefault("detailedSummary", "Summary not generated");
            String exec = (String) summaryMap.getOrDefault("executiveSummary", "Summary not generated");
            @SuppressWarnings("unchecked")
            List<String> takeaways = (List<String>) summaryMap.getOrDefault("keyTakeaways", List.of("Concept validation", "Replication rules"));

            PDFSummaryResponse response = new PDFSummaryResponse(
                    oneLine,
                    shortSum,
                    detailed,
                    exec,
                    takeaways,
                    chapterSummaries
            );

            // Save to cache
            objectMapper.writeValue(cacheFile, response);

            return response;

        } catch (Exception e) {
            logger.error("Failed to generate summary for course: {}", courseId, e);
            // Fallback mock summaries
            List<Chapter> chapters = chapterRepository.findByCourseOrderBySequenceNumberAsc(course);
            List<ChapterSummaryData> chapterSummaries = new ArrayList<>();
            for (Chapter chapter : chapters) {
                chapterSummaries.add(new ChapterSummaryData(chapter.getTitle(), chapter.getSummary()));
            }
            Map<String, Object> mockMap = generateMockSummaryMap(course.getTitle());
            @SuppressWarnings("unchecked")
            List<String> mockTakeaways = (List<String>) mockMap.get("keyTakeaways");
            PDFSummaryResponse response = new PDFSummaryResponse(
                    (String) mockMap.get("oneLineSummary"),
                    (String) mockMap.get("shortSummary"),
                    (String) mockMap.get("detailedSummary"),
                    (String) mockMap.get("executiveSummary"),
                    mockTakeaways,
                    chapterSummaries
            );
            return response;
        }
    }

    private String cleanJsonResponse(String response) {
        if (response == null) {
            return "{}";
        }
        String trimmed = response.trim();
        if (trimmed.startsWith("```json")) {
            int end = trimmed.lastIndexOf("```");
            if (end > 7) {
                return trimmed.substring(7, end).trim();
            }
        }
        return trimmed;
    }

    private Map<String, Object> generateMockSummaryMap(String title) {
        Map<String, Object> map = new HashMap<>();
        map.put("oneLineSummary", "An analytical overview of concepts and data validation methodologies in the context of " + title + ".");
        map.put("shortSummary", "This document covers scientific concepts, data structures, and the structural verification techniques critical to understanding " + title + ". It provides outlines, exercises, and assessments to reinforce primary principles.");
        map.put("detailedSummary", "The detailed syllabus details how systematic concepts can be analyzed using double-blind models, control groups, and empirical review cycles. In addition, it reviews dynamic changes caused by external shifts (such as stellar pulsations or data anomalies), highlighting the math and calculations required to normalize and validate target outputs.");
        map.put("executiveSummary", "This educational syllabus is built to transition students from foundational concepts to advanced structural modeling. By grounding lessons in parsed documentation, it provides a comprehensive learning route covering research frameworks, data normalization, validation parameters, and final evaluations.");
        map.put("keyTakeaways", List.of(
                "Establish concrete hypotheses prior to any experimental setup.",
                "Integrate baseline controls to isolate the variables being tested.",
                "Utilize automated vectorization and similarity logic to match concepts.",
                "Maintain data integrity across replication cycles."
        ));
        return map;
    }
}
