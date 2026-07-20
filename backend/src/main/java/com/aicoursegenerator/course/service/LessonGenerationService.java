package com.aicoursegenerator.course.service;

import com.aicoursegenerator.ai.service.AiProvider;
import com.aicoursegenerator.ai.service.AiProviderFactory;
import com.aicoursegenerator.ai.service.PromptLoader;
import com.aicoursegenerator.common.exception.ResourceNotFoundException;
import com.aicoursegenerator.course.dto.LessonExplanationResponse;
import com.aicoursegenerator.course.entity.Lesson;
import com.aicoursegenerator.course.repository.LessonRepository;
import com.aicoursegenerator.pdf.entity.PDFChunk;
import com.aicoursegenerator.pdf.repository.PDFChunkRepository;
import com.aicoursegenerator.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LessonGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(LessonGenerationService.class);

    private final LessonRepository lessonRepository;
    private final PDFChunkRepository chunkRepository;
    private final PromptLoader promptLoader;
    private final AiProviderFactory providerFactory;

    public LessonGenerationService(
            LessonRepository lessonRepository,
            PDFChunkRepository chunkRepository,
            PromptLoader promptLoader,
            AiProviderFactory providerFactory) {
        this.lessonRepository = lessonRepository;
        this.chunkRepository = chunkRepository;
        this.promptLoader = promptLoader;
        this.providerFactory = providerFactory;
    }

    @Transactional
    public Lesson getOrGenerateLessonContent(UUID lessonId, User user) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));

        // Validate Ownership (IDOR Prevention)
        if (!lesson.getChapter().getCourse().getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Lesson not found for this user");
        }

        // If explanation is cached, return it directly
        if (lesson.getExplanation() != null && !lesson.getExplanation().trim().isEmpty()) {
            logger.info("Returning cached content for Lesson ID: {}", lessonId);
            return lesson;
        }

        logger.info("Lazy generating content for Lesson ID: {}", lessonId);

        try {
            UUID pdfMetadataId = lesson.getChapter().getCourse().getPdfMetadata().getId();
            
            // Search matching chunks in database
            List<PDFChunk> relevantChunks = chunkRepository.searchChunks(pdfMetadataId, lesson.getTitle(), 3);
            if (relevantChunks.isEmpty()) {
                logger.warn("No relevant chunks found via FTS for lesson title: {}. Falling back to default chunks.", lesson.getTitle());
                List<PDFChunk> allChunks = chunkRepository.findByPdfMetadataOrderByChunkIndexAsc(lesson.getChapter().getCourse().getPdfMetadata());
                if (!allChunks.isEmpty()) {
                    relevantChunks = allChunks.subList(0, Math.min(allChunks.size(), 3));
                }
            }

            StringBuilder contextBuilder = new StringBuilder();
            for (PDFChunk chunk : relevantChunks) {
                contextBuilder.append(chunk.getContent()).append("\n");
            }

            String template = promptLoader.loadPrompt("lesson_explanation.txt");
            Map<String, String> variables = Map.of(
                    "courseTitle", lesson.getChapter().getCourse().getTitle(),
                    "chapterTitle", lesson.getChapter().getTitle(),
                    "lessonTitle", lesson.getTitle(),
                    "chunksContext", contextBuilder.toString()
            );

            String userPrompt = promptLoader.interpolate(template, variables);
            
            AiProvider provider = providerFactory.getProvider();
            LessonExplanationResponse response = provider.generateStructuredJson(
                    "You are a professional educational lecturer. Write detailed academic lesson materials.",
                    userPrompt,
                    LessonExplanationResponse.class
            );

            // Cache generated fields
            lesson.setExplanation(response.explanation());
            lesson.setKeyTakeaways(response.keyTakeaways());
            lesson.setImportantNotes(response.importantNotes());
            lesson.setRealWorldExamples(response.realWorldExamples());

            return lessonRepository.save(lesson);

        } catch (Exception e) {
            logger.error("Failed to generate content for Lesson ID: {}", lessonId, e);
            throw new RuntimeException("AI Lesson content generation failed", e);
        }
    }
}
