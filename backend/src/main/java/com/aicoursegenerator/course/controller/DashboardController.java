package com.aicoursegenerator.course.controller;

import com.aicoursegenerator.common.dto.ApiResponse;
import com.aicoursegenerator.common.exception.ResourceNotFoundException;
import com.aicoursegenerator.course.dto.DashboardStats;
import com.aicoursegenerator.course.entity.Chapter;
import com.aicoursegenerator.course.entity.Course;
import com.aicoursegenerator.course.entity.LearningProgress;
import com.aicoursegenerator.course.entity.Lesson;
import com.aicoursegenerator.course.repository.ChapterRepository;
import com.aicoursegenerator.course.repository.CourseRepository;
import com.aicoursegenerator.course.repository.LearningProgressRepository;
import com.aicoursegenerator.course.repository.LessonRepository;
import com.aicoursegenerator.pdf.repository.PDFMetadataRepository;
import com.aicoursegenerator.quiz.entity.QuizAttempt;
import com.aicoursegenerator.quiz.repository.QuizAttemptRepository;
import com.aicoursegenerator.security.CustomUserDetails;
import com.aicoursegenerator.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@Transactional(readOnly = true)
public class DashboardController {

    private final PDFMetadataRepository pdfMetadataRepository;
    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final LearningProgressRepository progressRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final UserRepository userRepository;

    public DashboardController(
            PDFMetadataRepository pdfMetadataRepository,
            CourseRepository courseRepository,
            ChapterRepository chapterRepository,
            LessonRepository lessonRepository,
            LearningProgressRepository progressRepository,
            QuizAttemptRepository quizAttemptRepository,
            UserRepository userRepository) {
        this.pdfMetadataRepository = pdfMetadataRepository;
        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
        this.lessonRepository = lessonRepository;
        this.progressRepository = progressRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardStats>> getDashboardStats(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }
        
        var user = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        long pdfsCount = pdfMetadataRepository.findByUser(user).size();
        List<Course> courses = courseRepository.findByUser(user);
        long coursesCount = courses.size();

        // Calculate progress details
        long totalCompletedLessons = 0;
        List<DashboardStats.CourseProgressDetail> recentCourses = new ArrayList<>();

        for (Course course : courses) {
            List<Chapter> chapters = chapterRepository.findByCourseOrderBySequenceNumberAsc(course);
            List<Lesson> lessons = new ArrayList<>();
            for (Chapter ch : chapters) {
                lessons.addAll(lessonRepository.findByChapterOrderBySequenceNumberAsc(ch));
            }

            if (lessons.isEmpty()) {
                recentCourses.add(new DashboardStats.CourseProgressDetail(
                        course.getId(),
                        course.getPdfMetadata().getId(),
                        course.getTitle(),
                        course.getStatus(),
                        0.0,
                        0,
                        0,
                        course.getFailureReason()
                ));
                continue;
            }

            List<LearningProgress> userProgress = progressRepository.findByUserAndLessonIn(user, lessons);
            long completedLessons = userProgress.stream().filter(LearningProgress::getCompleted).count();
            totalCompletedLessons += completedLessons;

            double percentage = (completedLessons * 100.0) / lessons.size();
            percentage = Math.round(percentage * 100.0) / 100.0;

            recentCourses.add(new DashboardStats.CourseProgressDetail(
                    course.getId(),
                    course.getPdfMetadata().getId(),
                    course.getTitle(),
                    course.getStatus(),
                    percentage,
                    (int) completedLessons,
                    lessons.size(),
                    course.getFailureReason()
            ));
        }

        // Quiz metrics
        List<QuizAttempt> attempts = quizAttemptRepository.findByUser(user);
        double averageQuizScore = 0.0;
        if (!attempts.isEmpty()) {
            double totalScorePercentage = 0.0;
            int validAttemptsCount = 0;
            for (QuizAttempt attempt : attempts) {
                if (attempt.getScore() != null && attempt.getTotalQuestions() != null && attempt.getTotalQuestions() > 0) {
                    totalScorePercentage += (attempt.getScore() * 100.0) / attempt.getTotalQuestions();
                    validAttemptsCount++;
                }
            }
            if (validAttemptsCount > 0) {
                averageQuizScore = totalScorePercentage / validAttemptsCount;
                averageQuizScore = Math.round(averageQuizScore * 100.0) / 100.0;
            }
        }

        DashboardStats stats = new DashboardStats(
                pdfsCount,
                coursesCount,
                totalCompletedLessons,
                averageQuizScore,
                recentCourses
        );

        return ResponseEntity.ok(ApiResponse.success("Successfully calculated dashboard statistics", stats));
    }
}
