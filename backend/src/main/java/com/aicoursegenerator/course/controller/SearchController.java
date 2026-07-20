package com.aicoursegenerator.course.controller;

import com.aicoursegenerator.ai.service.VectorEmbeddingService;
import com.aicoursegenerator.common.dto.ApiResponse;
import com.aicoursegenerator.course.dto.CourseResponse;
import com.aicoursegenerator.course.entity.Chapter;
import com.aicoursegenerator.course.entity.Course;
import com.aicoursegenerator.course.entity.Lesson;
import com.aicoursegenerator.course.repository.ChapterRepository;
import com.aicoursegenerator.course.repository.CourseRepository;
import com.aicoursegenerator.course.repository.LessonRepository;
import com.aicoursegenerator.pdf.entity.PDFChunk;
import com.aicoursegenerator.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/courses/search")
public class SearchController {

    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final VectorEmbeddingService embeddingService;

    public record GlobalSearchResponse(
            List<CourseResponse> courses,
            List<LessonSearchItem> lessons,
            List<SemanticSearchItem> semanticMatches
    ) {}

    public record LessonSearchItem(
            UUID lessonId,
            UUID courseId,
            String courseTitle,
            String chapterTitle,
            String lessonTitle,
            String snippet
    ) {}

    public record SemanticSearchItem(
            UUID pdfChunkId,
            UUID courseId,
            String courseTitle,
            int chunkIndex,
            String content
    ) {}

    public SearchController(
            CourseRepository courseRepository,
            ChapterRepository chapterRepository,
            LessonRepository lessonRepository,
            VectorEmbeddingService embeddingService) {
        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
        this.lessonRepository = lessonRepository;
        this.embeddingService = embeddingService;
    }

    @GetMapping("/global")
    public ResponseEntity<ApiResponse<GlobalSearchResponse>> searchGlobal(
            @RequestParam("q") String query,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        String lowerQuery = query.toLowerCase();
        List<Course> userCourses = courseRepository.findByUser(userDetails.getUser());

        // 1. Filter courses matching title or description
        List<CourseResponse> coursesMatched = userCourses.stream()
                .filter(c -> c.getTitle().toLowerCase().contains(lowerQuery) ||
                        (c.getDescription() != null && c.getDescription().toLowerCase().contains(lowerQuery)))
                .map(this::mapToCourseResponse)
                .collect(Collectors.toList());

        // 2. Filter lessons matching title or explanation
        List<LessonSearchItem> lessonsMatched = new ArrayList<>();
        // 3. Search PDF chunks semantically
        List<SemanticSearchItem> semanticMatched = new ArrayList<>();

        for (Course course : userCourses) {
            List<Chapter> chapters = chapterRepository.findByCourseOrderBySequenceNumberAsc(course);
            for (Chapter ch : chapters) {
                List<Lesson> lessons = lessonRepository.findByChapterOrderBySequenceNumberAsc(ch);
                for (Lesson l : lessons) {
                    boolean titleMatch = l.getTitle().toLowerCase().contains(lowerQuery);
                    boolean bodyMatch = l.getExplanation() != null && l.getExplanation().toLowerCase().contains(lowerQuery);
                    
                    if (titleMatch || bodyMatch) {
                        String snippet = "";
                        if (l.getExplanation() != null) {
                            String text = l.getExplanation();
                            int idx = text.toLowerCase().indexOf(lowerQuery);
                            if (idx != -1) {
                                int start = Math.max(0, idx - 40);
                                int end = Math.min(text.length(), idx + lowerQuery.length() + 60);
                                snippet = "..." + text.substring(start, end).replace("\n", " ") + "...";
                            } else {
                                snippet = text.length() > 100 ? text.substring(0, 100) + "..." : text;
                            }
                        }
                        lessonsMatched.add(new LessonSearchItem(
                                l.getId(),
                                course.getId(),
                                course.getTitle(),
                                ch.getTitle(),
                                l.getTitle(),
                                snippet
                        ));
                    }
                }
            }

            // Semantic search on PDF chunks (returns top 2 segments by concept meaning)
            try {
                List<PDFChunk> chunks = embeddingService.searchSemantic(course.getPdfMetadata().getId(), query, 2);
                for (PDFChunk chunk : chunks) {
                    semanticMatched.add(new SemanticSearchItem(
                            chunk.getId(),
                            course.getId(),
                            course.getTitle(),
                            chunk.getChunkIndex(),
                            chunk.getContent()
                    ));
                }
            } catch (Exception e) {
                // Ignore failure for one course
            }
        }

        GlobalSearchResponse response = new GlobalSearchResponse(
                coursesMatched,
                lessonsMatched,
                semanticMatched
        );

        return ResponseEntity.ok(ApiResponse.success("Global search completed successfully", response));
    }

    private CourseResponse mapToCourseResponse(Course c) {
        return new CourseResponse(
                c.getId(),
                c.getPdfMetadata().getId(),
                c.getTitle(),
                c.getDescription(),
                c.getEstimatedDuration(),
                c.getDifficultyLevel(),
                c.getPrerequisites(),
                c.getLearningObjectives(),
                c.getStatus(),
                c.getFailureReason(),
                c.getCreatedAt()
        );
    }
}
