package com.aicoursegenerator.course.service;

import com.aicoursegenerator.ai.service.AiProvider;
import com.aicoursegenerator.ai.service.AiProviderFactory;
import com.aicoursegenerator.ai.service.PromptLoader;
import com.aicoursegenerator.common.exception.BadRequestException;
import com.aicoursegenerator.common.exception.ResourceNotFoundException;
import com.aicoursegenerator.course.dto.CourseOutlineResponse;
import com.aicoursegenerator.course.entity.Chapter;
import com.aicoursegenerator.course.entity.Course;
import com.aicoursegenerator.course.entity.Lesson;
import com.aicoursegenerator.course.repository.ChapterRepository;
import com.aicoursegenerator.course.repository.CourseRepository;
import com.aicoursegenerator.course.repository.LessonRepository;
import com.aicoursegenerator.pdf.entity.PDFChunk;
import com.aicoursegenerator.pdf.entity.PDFMetadata;
import com.aicoursegenerator.pdf.repository.PDFChunkRepository;
import com.aicoursegenerator.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.ZonedDateTime;
import java.util.*;

@Service
public class CourseGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(CourseGenerationService.class);

    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final PDFChunkRepository chunkRepository;
    private final PromptLoader promptLoader;
    private final AiProviderFactory providerFactory;

    public CourseGenerationService(
            CourseRepository courseRepository,
            ChapterRepository chapterRepository,
            LessonRepository lessonRepository,
            PDFChunkRepository chunkRepository,
            PromptLoader promptLoader,
            AiProviderFactory providerFactory) {
        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
        this.lessonRepository = lessonRepository;
        this.chunkRepository = chunkRepository;
        this.promptLoader = promptLoader;
        this.providerFactory = providerFactory;
    }

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private CourseGenerationService self;

    @Transactional
    public Course initiateCourseGeneration(PDFMetadata pdfMetadata, User user) {
        // Verify if a course is already generated for this PDF
        // Optionally allow multiple, but let's check or create a new one
        Course course = new Course();
        course.setUser(user);
        course.setPdfMetadata(pdfMetadata);
        course.setTitle("Generating Course: " + pdfMetadata.getFilename());
        course.setStatus("PENDING");
        course.setCreatedAt(ZonedDateTime.now());
        course.setUpdatedAt(ZonedDateTime.now());

        Course savedCourse = courseRepository.save(course);

        // Trigger background processing after transaction commits to prevent race conditions
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        self.generateCourseOutlineAsync(savedCourse.getId());
                    }
                }
            );
        } else {
            self.generateCourseOutlineAsync(savedCourse.getId());
        }

        return savedCourse;
    }

    @Async("courseGenTaskExecutor")
    @Transactional
    public void generateCourseOutlineAsync(UUID courseId) {
        logger.info("Starting async course outline generation for Course ID: {}", courseId);
        
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (courseOpt.isEmpty()) {
            logger.error("Course record not found for async processing ID: {}", courseId);
            return;
        }

        Course course = courseOpt.get();
        course.setStatus("GENERATING_OUTLINE");
        courseRepository.save(course);

        try {
            PDFMetadata pdfMetadata = course.getPdfMetadata();
            List<PDFChunk> chunks = chunkRepository.findByPdfMetadataOrderByChunkIndexAsc(pdfMetadata);
            if (chunks.isEmpty()) {
                throw new BadRequestException("No parsed text segments found for this document");
            }

            // Consolidate first 6 chunks to extract outline
            StringBuilder sb = new StringBuilder();
            int limit = Math.min(chunks.size(), 6);
            for (int i = 0; i < limit; i++) {
                sb.append(chunks.get(i).getContent()).append("\n");
            }

            String template = promptLoader.loadPrompt("course_outline.txt");
            String userPrompt = promptLoader.interpolate(template, Map.of("pdfText", sb.toString()));
            logger.info("Stage: Prompt Creation - template: course_outline.txt, user prompt length: {}", userPrompt.length());

            AiProvider provider = providerFactory.getProvider();
            CourseOutlineResponse outline = provider.generateStructuredJson(
                    "You are a professional educational planner. Create valid JSON course outlines.",
                    userPrompt,
                    CourseOutlineResponse.class
            );

            // Populate course structures
            logger.info("Stage: Outline Creation - mapping outline to course fields. Title: {}", outline.title());
            course.setTitle(outline.title());
            course.setDescription(outline.description());
            course.setEstimatedDuration(outline.estimatedDuration());
            course.setDifficultyLevel(outline.difficultyLevel());
            course.setPrerequisites(outline.prerequisites());
            course.setLearningObjectives(outline.learningObjectives());
            course.setStatus("READY");
            course.setUpdatedAt(ZonedDateTime.now());

            Course updatedCourse = courseRepository.save(course);

            // Create chapters and lessons
            logger.info("Stage: Course Persistence - saving chapters and lessons. Total chapters: {}", outline.chapters().size());
            for (CourseOutlineResponse.ChapterOutline chOutline : outline.chapters()) {
                Chapter chapter = new Chapter();
                chapter.setCourse(updatedCourse);
                chapter.setTitle(chOutline.title());
                chapter.setSummary(chOutline.summary());
                chapter.setSequenceNumber(chOutline.sequenceNumber());
                
                // Chapter quiz will be populated dynamically or on quiz generation request
                Chapter savedChapter = chapterRepository.save(chapter);

                for (CourseOutlineResponse.LessonOutline lesOutline : chOutline.lessons()) {
                    Lesson lesson = new Lesson();
                    lesson.setChapter(savedChapter);
                    lesson.setTitle(lesOutline.title());
                    lesson.setSequenceNumber(lesOutline.sequenceNumber());
                    lessonRepository.save(lesson);
                }
            }

            logger.info("Stage: Course Persistence - complete. Course ID: {}", courseId);

        } catch (Exception e) {
            logger.error("Stage: Course Generation - failed. Course ID: {}", courseId, e);
            String failureMessage = determineFailureReason(e);
            try {
                self.persistFailure(courseId, failureMessage);
            } catch (Exception persistEx) {
                logger.error("Failed to persist failure status for Course ID: {}", courseId, persistEx);
            }
            throw e; // Do not swallow exceptions
        }
    }

    private String determineFailureReason(Throwable e) {
        if (e == null) {
            return "Unknown error";
        }
        
        // Traverse exception chain to find root causes
        Throwable cause = e;
        while (cause.getCause() != null && cause != cause.getCause()) {
            if (cause instanceof org.springframework.web.client.HttpStatusCodeException) {
                break;
            }
            cause = cause.getCause();
        }
        
        if (cause instanceof org.springframework.web.client.HttpStatusCodeException) {
            org.springframework.web.client.HttpStatusCodeException httpEx = (org.springframework.web.client.HttpStatusCodeException) cause;
            int status = httpEx.getStatusCode().value();
            String body = httpEx.getResponseBodyAsString();
            
            if (status == 400 && (body.contains("API_KEY_INVALID") || body.contains("key is not valid"))) {
                return "Invalid API key";
            }
            if (status == 403) {
                return "Invalid API key";
            }
            if (status == 429) {
                return "Rate limit exceeded";
            }
            if (body.contains("API key")) {
                return "Invalid API key";
            }
            if (body.contains("quota") || body.contains("limit")) {
                return "Rate limit exceeded";
            }
            return "AI service error (HTTP " + status + ")";
        }
        
        String msg = cause.getMessage() != null ? cause.getMessage() : "";
        if (cause instanceof java.net.SocketTimeoutException || msg.contains("timeout") || msg.contains("Timeout")) {
            return "AI timeout";
        }
        if (cause instanceof java.io.IOException || msg.contains("Connection refused") || msg.contains("connect")) {
            return "AI connection failed";
        }
        if (cause instanceof com.fasterxml.jackson.core.JsonProcessingException || msg.contains("JSON") || msg.contains("parsing") || msg.contains("readValue")) {
            return "JSON parsing failed";
        }
        if (cause instanceof NullPointerException) {
            return "NullPointerException";
        }
        if (cause instanceof IllegalArgumentException && msg.startsWith("Missing field")) {
            return msg;
        }
        
        if (msg.contains("API_KEY_INVALID") || msg.contains("key is not valid")) {
            return "Invalid API key";
        }
        if (msg.contains("quota") || msg.contains("rate limit") || msg.contains("429")) {
            return "Rate limit exceeded";
        }
        if (msg.contains("JSON parsing") || msg.contains("mapping to JSON") || msg.contains("Jackson")) {
            return "JSON parsing failed";
        }
        
        return msg.isEmpty() ? cause.getClass().getSimpleName() : msg;
    }

    @Transactional
    public Course retryCourseGeneration(UUID courseId, User user) {
        Course course = courseRepository.findByIdAndUser(courseId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        if (!"FAILED".equals(course.getStatus())) {
            throw new BadRequestException("Course can only be retried if it has FAILED status");
        }

        course.setStatus("PENDING");
        course.setFailureReason(null);
        course.setUpdatedAt(ZonedDateTime.now());
        Course savedCourse = courseRepository.save(course);

        // Delete old chapters/lessons if any were created before failure
        List<Chapter> oldChapters = chapterRepository.findByCourseOrderBySequenceNumberAsc(savedCourse);
        for (Chapter ch : oldChapters) {
            List<Lesson> oldLessons = lessonRepository.findByChapterOrderBySequenceNumberAsc(ch);
            lessonRepository.deleteAll(oldLessons);
        }
        chapterRepository.deleteAll(oldChapters);

        // Restart async task after transaction commits
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        self.generateCourseOutlineAsync(savedCourse.getId());
                    }
                }
            );
        } else {
            self.generateCourseOutlineAsync(savedCourse.getId());
        }
        return savedCourse;
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void persistFailure(UUID courseId, String reason) {
        courseRepository.findById(courseId).ifPresent(course -> {
            course.setStatus("FAILED");
            course.setFailureReason(reason);
            courseRepository.save(course);
        });
    }
}
