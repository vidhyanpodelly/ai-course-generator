package com.aicoursegenerator.course.controller;

import com.aicoursegenerator.common.dto.ApiResponse;
import com.aicoursegenerator.common.exception.ResourceNotFoundException;
import com.aicoursegenerator.course.dto.ChapterResponse;
import com.aicoursegenerator.course.dto.CourseResponse;
import com.aicoursegenerator.course.dto.LessonResponse;
import com.aicoursegenerator.course.entity.Chapter;
import com.aicoursegenerator.course.entity.Course;
import com.aicoursegenerator.course.entity.LearningProgress;
import com.aicoursegenerator.course.entity.Lesson;
import com.aicoursegenerator.course.repository.ChapterRepository;
import com.aicoursegenerator.course.repository.CourseRepository;
import com.aicoursegenerator.course.repository.LearningProgressRepository;
import com.aicoursegenerator.course.repository.LessonRepository;
import com.aicoursegenerator.course.service.CourseGenerationService;
import com.aicoursegenerator.course.service.LessonGenerationService;
import com.aicoursegenerator.pdf.entity.PDFMetadata;
import com.aicoursegenerator.pdf.service.PDFService;
import com.aicoursegenerator.security.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final LearningProgressRepository progressRepository;
    private final CourseGenerationService generationService;
    private final LessonGenerationService lessonGenerationService;
    private final PDFService pdfService;

    public CourseController(
            CourseRepository courseRepository,
            ChapterRepository chapterRepository,
            LessonRepository lessonRepository,
            LearningProgressRepository progressRepository,
            CourseGenerationService generationService,
            LessonGenerationService lessonGenerationService,
            PDFService pdfService) {
        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
        this.lessonRepository = lessonRepository;
        this.progressRepository = progressRepository;
        this.generationService = generationService;
        this.lessonGenerationService = lessonGenerationService;
        this.pdfService = pdfService;
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<CourseResponse>> generateCourse(
            @RequestParam("pdfId") UUID pdfId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PDFMetadata pdfMetadata = pdfService.getMetadata(pdfId, userDetails.getUser());
        Course course = generationService.initiateCourseGeneration(pdfMetadata, userDetails.getUser());
        CourseResponse response = mapToResponse(course);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Course outline generation initiated", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getAllCourses(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Course> courses = courseRepository.findByUser(userDetails.getUser());
        List<CourseResponse> responses = courses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Retrieved user courses", responses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourseById(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Course course = courseRepository.findByIdAndUser(id, userDetails.getUser())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        return ResponseEntity.ok(ApiResponse.success("Retrieved course details", mapToResponse(course)));
    }

    @GetMapping("/{id}/chapters")
    public ResponseEntity<ApiResponse<List<ChapterResponse>>> getCourseChapters(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Course course = courseRepository.findByIdAndUser(id, userDetails.getUser())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        List<Chapter> chapters = chapterRepository.findByCourseOrderBySequenceNumberAsc(course);
        List<ChapterResponse> responses = chapters.stream().map(ch -> {
            List<Lesson> lessons = lessonRepository.findByChapterOrderBySequenceNumberAsc(ch);
            List<LessonResponse> lessonResponses = lessons.stream().map(l -> {
                boolean completed = progressRepository.findByUserAndLesson(userDetails.getUser(), l)
                        .map(LearningProgress::getCompleted)
                        .orElse(false);
                return mapToLessonResponse(l, completed);
            }).collect(Collectors.toList());

            boolean hasQuiz = ch.getQuizData() != null && !ch.getQuizData().isEmpty();
            return new ChapterResponse(ch.getId(), ch.getCourse().getId(), ch.getTitle(), ch.getSummary(), ch.getSequenceNumber(), lessonResponses, hasQuiz);
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Retrieved course chapters", responses));
    }

    @GetMapping("/lessons/{lessonId}")
    public ResponseEntity<ApiResponse<LessonResponse>> getLessonDetails(
            @PathVariable("lessonId") UUID lessonId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Lesson lesson = lessonGenerationService.getOrGenerateLessonContent(lessonId, userDetails.getUser());
        boolean completed = progressRepository.findByUserAndLesson(userDetails.getUser(), lesson)
                .map(LearningProgress::getCompleted)
                .orElse(false);
        return ResponseEntity.ok(ApiResponse.success("Retrieved lesson details", mapToLessonResponse(lesson, completed)));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<ApiResponse<CourseResponse>> retryCourse(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Course course = generationService.retryCourseGeneration(id, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("Retrieved course retry status", mapToResponse(course)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> searchCourses(
            @RequestParam("q") String query,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Course> courses = courseRepository.searchCoursesByKeyword(userDetails.getUser(), query);
        List<CourseResponse> responses = courses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Retrieved search results", responses));
    }

    private CourseResponse mapToResponse(Course c) {
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

    private LessonResponse mapToLessonResponse(Lesson l, boolean completed) {
        return new LessonResponse(
                l.getId(),
                l.getChapter().getId(),
                l.getTitle(),
                l.getExplanation(),
                l.getKeyTakeaways(),
                l.getImportantNotes(),
                l.getRealWorldExamples(),
                l.getSequenceNumber(),
                completed
        );
    }
}
