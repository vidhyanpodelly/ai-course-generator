package com.aicoursegenerator.course.controller;

import com.aicoursegenerator.common.dto.ApiResponse;
import com.aicoursegenerator.common.exception.ResourceNotFoundException;
import com.aicoursegenerator.course.dto.CourseProgress;
import com.aicoursegenerator.course.entity.Chapter;
import com.aicoursegenerator.course.entity.Course;
import com.aicoursegenerator.course.entity.LearningProgress;
import com.aicoursegenerator.course.entity.Lesson;
import com.aicoursegenerator.course.repository.ChapterRepository;
import com.aicoursegenerator.course.repository.CourseRepository;
import com.aicoursegenerator.course.repository.LearningProgressRepository;
import com.aicoursegenerator.course.repository.LessonRepository;
import com.aicoursegenerator.security.CustomUserDetails;
import com.aicoursegenerator.user.entity.User;
import com.aicoursegenerator.user.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/progress")
@Transactional
public class ProgressController {

    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final LearningProgressRepository progressRepository;
    private final UserRepository userRepository;

    public ProgressController(
            CourseRepository courseRepository,
            ChapterRepository chapterRepository,
            LessonRepository lessonRepository,
            LearningProgressRepository progressRepository,
            UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
        this.lessonRepository = lessonRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<CourseProgress>> markLessonComplete(
            @RequestParam("lessonId") UUID lessonId,
            @RequestParam("completed") boolean completed,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            return ResponseEntity
                    .status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }
        
        User user = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));

        Course course = lesson.getChapter().getCourse();
        if (!course.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Lesson not found for this user");
        }

        Optional<LearningProgress> progressOpt = progressRepository.findByUserAndLesson(user, lesson);
        LearningProgress progress;
        if (progressOpt.isPresent()) {
            progress = progressOpt.get();
            progress.setCompleted(completed);
            progress.setCompletedAt(completed ? ZonedDateTime.now() : null);
        } else {
            progress = new LearningProgress(null, user, lesson, completed, completed ? ZonedDateTime.now() : null);
        }
        progressRepository.save(progress);

        if (completed) {
            updateStreakAndActivity(user);
        }

        CourseProgress courseProgress = calculateProgress(course, user);
        return ResponseEntity.ok(ApiResponse.success("Lesson progress updated successfully", courseProgress));
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<ApiResponse<CourseProgress>> getCourseProgress(
            @PathVariable("courseId") UUID courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Course course = courseRepository.findByIdAndUser(courseId, userDetails.getUser())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        CourseProgress progress = calculateProgress(course, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("Retrieved course progress stats", progress));
    }

    private CourseProgress calculateProgress(Course course, com.aicoursegenerator.user.entity.User user) {
        List<Chapter> chapters = chapterRepository.findByCourseOrderBySequenceNumberAsc(course);
        List<Lesson> lessons = new ArrayList<>();
        for (Chapter ch : chapters) {
            lessons.addAll(lessonRepository.findByChapterOrderBySequenceNumberAsc(ch));
        }

        if (lessons.isEmpty()) {
            return new CourseProgress(0, 0, 0.0, new ArrayList<>());
        }

        List<LearningProgress> userProgress = progressRepository.findByUserAndLessonIn(user, lessons);
        List<UUID> completedLessonIds = userProgress.stream()
                .filter(LearningProgress::getCompleted)
                .map(p -> p.getLesson().getId())
                .collect(Collectors.toList());

        int completedCount = completedLessonIds.size();
        int totalCount = lessons.size();
        double percentage = (completedCount * 100.0) / totalCount;
        
        // Round to two decimal places
        percentage = Math.round(percentage * 100.0) / 100.0;

        return new CourseProgress(completedCount, totalCount, percentage, completedLessonIds);
    }

    private void updateStreakAndActivity(User user) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime lastActive = user.getLastActiveAt();

        if (lastActive == null) {
            user.setLearningStreak(1);
        } else {
            long diffDays = java.time.temporal.ChronoUnit.DAYS.between(lastActive.toLocalDate(), now.toLocalDate());
            if (diffDays == 1) {
                user.setLearningStreak(user.getLearningStreak() + 1);
            } else if (diffDays > 1) {
                user.setLearningStreak(1);
            }
        }
        user.setLastActiveAt(now);
        userRepository.save(user);
    }
}
