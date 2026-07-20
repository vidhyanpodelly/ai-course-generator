package com.aicoursegenerator.user.controller;

import com.aicoursegenerator.common.dto.ApiResponse;
import com.aicoursegenerator.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aicoursegenerator.course.entity.Chapter;
import com.aicoursegenerator.course.entity.Course;
import com.aicoursegenerator.course.entity.LearningProgress;
import com.aicoursegenerator.course.entity.Lesson;
import com.aicoursegenerator.course.repository.ChapterRepository;
import com.aicoursegenerator.course.repository.CourseRepository;
import com.aicoursegenerator.course.repository.LearningProgressRepository;
import com.aicoursegenerator.course.repository.LessonRepository;
import com.aicoursegenerator.pdf.entity.PDFMetadata;
import com.aicoursegenerator.pdf.repository.PDFMetadataRepository;
import com.aicoursegenerator.quiz.entity.QuizAttempt;
import com.aicoursegenerator.quiz.repository.QuizAttemptRepository;
import com.aicoursegenerator.security.CustomUserDetails;
import com.aicoursegenerator.user.entity.User;
import com.aicoursegenerator.user.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users/profile")
@Transactional
public class ProfileController {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final LearningProgressRepository progressRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final PDFMetadataRepository pdfMetadataRepository;

    public record ProfileResponse(
            UUID id,
            String email,
            String firstName,
            String lastName,
            int learningStreak,
            ZonedDateTime lastActiveAt,
            String avatarUrl,
            List<String> achievements,
            String preferences,
            List<CertificateInfo> certificates,
            List<ActivityLog> recentActivity,
            List<Double> weeklyStudyHours,
            List<Integer> heatmapLevels
    ) {}

    public record CertificateInfo(
            UUID courseId,
            String courseTitle,
            String certificateId,
            String issueDate
    ) {}

    public record ActivityLog(
            String description,
            String type, // UPLOAD, COMPLETE, QUIZ
            ZonedDateTime timestamp
    ) {}

    public ProfileController(
            UserRepository userRepository,
            CourseRepository courseRepository,
            ChapterRepository chapterRepository,
            LessonRepository lessonRepository,
            LearningProgressRepository progressRepository,
            QuizAttemptRepository quizAttemptRepository,
            PDFMetadataRepository pdfMetadataRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
        this.lessonRepository = lessonRepository;
        this.progressRepository = progressRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.pdfMetadataRepository = pdfMetadataRepository;
    }

    @GetMapping
    @Transactional
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        User user = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Update streak and activity dynamically on profile fetch
        updateStreakAndActivity(user);

        // 1. Compile Achievements dynamically
        List<String> achievements = new ArrayList<>();
        
        List<PDFMetadata> pdfs = pdfMetadataRepository.findByUser(user);
        if (!pdfs.isEmpty()) {
            achievements.add("Curator: Uploaded your first study materials");
        }

        List<Course> courses = courseRepository.findByUser(user);
        if (!courses.isEmpty()) {
            achievements.add("Initiator: Built your first educational course");
        }

        // Fetch completed progress elements
        List<LearningProgress> progress = progressRepository.findAll().stream()
                .filter(p -> p.getUser().getId().equals(user.getId()) && p.getCompleted())
                .toList();

        if (!progress.isEmpty()) {
            achievements.add("Scholar: Completed your first lesson checkpoint");
        }
        if (progress.size() >= 5) {
            achievements.add("Dedication: Completed 5 or more lesson checkpoints");
        }

        List<QuizAttempt> quizAttempts = quizAttemptRepository.findByUser(user);
        boolean perfectQuiz = quizAttempts.stream().anyMatch(q -> q.getScore().equals(q.getTotalQuestions()));
        if (perfectQuiz) {
            achievements.add("Academic Master: Scored 100% on a chapter quiz");
        }

        if (user.getLearningStreak() >= 3) {
            achievements.add("Consistent: Maintained a 3-day learning streak");
        }

        // 2. Compile Certificates Info
        List<CertificateInfo> certificates = new ArrayList<>();
        for (Course c : courses) {
            List<Chapter> chapters = chapterRepository.findByCourseOrderBySequenceNumberAsc(c);
            List<Lesson> lessons = new ArrayList<>();
            for (Chapter ch : chapters) {
                lessons.addAll(lessonRepository.findByChapterOrderBySequenceNumberAsc(ch));
            }
            if (!lessons.isEmpty()) {
                long completed = progressRepository.countByUserAndLessonInAndCompletedTrue(user, lessons);
                int pct = (int) ((completed * 100) / lessons.size());
                if (pct >= 80) {
                    UUID certUUID = UUID.nameUUIDFromBytes((user.getId().toString() + "-" + c.getId().toString()).getBytes());
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    certificates.add(new CertificateInfo(
                            c.getId(),
                            c.getTitle(),
                            certUUID.toString().toUpperCase(),
                            c.getUpdatedAt().format(formatter)
                    ));
                }
            }
        }

        // 3. Compile Recent Activity Timeline
        List<ActivityLog> activityList = new ArrayList<>();
        
        for (PDFMetadata pdf : pdfs) {
            activityList.add(new ActivityLog("Uploaded document: " + pdf.getFilename(), "UPLOAD", pdf.getCreatedAt()));
        }
        
        for (LearningProgress p : progress) {
            if (p.getCompletedAt() != null) {
                activityList.add(new ActivityLog("Studied lesson: " + p.getLesson().getTitle(), "COMPLETE", p.getCompletedAt()));
            }
        }

        for (QuizAttempt qa : quizAttempts) {
            activityList.add(new ActivityLog("Scored " + qa.getScore() + "/" + qa.getTotalQuestions() + " on quiz: " + qa.getChapter().getTitle(), "QUIZ", qa.getCreatedAt()));
        }

        // Sort descending by timestamp, limit to top 15
        List<ActivityLog> sortedActivity = activityList.stream()
                .sorted((a, b) -> b.timestamp().compareTo(a.timestamp()))
                .limit(15)
                .collect(Collectors.toList());

        // 4. Calculate Weekly Study Hours (Mon-Sun of current week)
        List<Double> weeklyStudyHours = new ArrayList<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate monday = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        
        for (int i = 0; i < 7; i++) {
            java.time.LocalDate day = monday.plusDays(i);
            
            long completedLessons = 0;
            for (LearningProgress lp : progress) {
                if (lp.getCompletedAt() != null && lp.getCompletedAt().toLocalDate().equals(day)) {
                    completedLessons++;
                }
            }
            
            long qaAttempts = 0;
            for (QuizAttempt qa : quizAttempts) {
                if (qa.getCreatedAt() != null && qa.getCreatedAt().toLocalDate().equals(day)) {
                    qaAttempts++;
                }
            }
            
            double hours = completedLessons * 0.5 + qaAttempts * 0.2;
            hours = Math.round(hours * 10.0) / 10.0;
            weeklyStudyHours.add(hours);
        }

        // 5. Calculate Heatmap Levels (Past 28 Days: level 0 to 3)
        List<Integer> heatmapLevels = new ArrayList<>();
        for (int i = 27; i >= 0; i--) {
            java.time.LocalDate day = today.minusDays(i);
            
            long completedLessons = 0;
            for (LearningProgress lp : progress) {
                if (lp.getCompletedAt() != null && lp.getCompletedAt().toLocalDate().equals(day)) {
                    completedLessons++;
                }
            }
            
            long qaAttempts = 0;
            for (QuizAttempt qa : quizAttempts) {
                if (qa.getCreatedAt() != null && qa.getCreatedAt().toLocalDate().equals(day)) {
                    qaAttempts++;
                }
            }
            
            long uploads = 0;
            for (PDFMetadata pdf : pdfs) {
                if (pdf.getCreatedAt() != null && pdf.getCreatedAt().toLocalDate().equals(day)) {
                    uploads++;
                }
            }
            
            long totalActivities = completedLessons + qaAttempts + uploads;
            int level = 0;
            if (totalActivities == 1) {
                level = 1;
            } else if (totalActivities == 2) {
                level = 2;
            } else if (totalActivities >= 3) {
                level = 3;
            }
            heatmapLevels.add(level);
        }

        String preferencesJson = "{\"language\":\"en\"}";
        try {
            if (user.getPreferences() != null && !user.getPreferences().isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                preferencesJson = mapper.writeValueAsString(user.getPreferences());
            }
        } catch (Exception e) {
            // ignore
        }

        ProfileResponse response = new ProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getLearningStreak(),
                user.getLastActiveAt(),
                user.getAvatarUrl(),
                achievements,
                preferencesJson,
                certificates,
                sortedActivity,
                weeklyStudyHours,
                heatmapLevels
        );

        return ResponseEntity.ok(ApiResponse.success("User profile statistics compiled", response));
    }

    @PutMapping("/preferences")
    public ResponseEntity<ApiResponse<Void>> updatePreferences(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        User user = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPreferences(body);
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success("Preferences updated successfully", null));
    }

    @PutMapping("/avatar")
    public ResponseEntity<ApiResponse<Void>> updateAvatar(
            @RequestParam("avatarUrl") String avatarUrl,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        User user = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success("Avatar updated successfully", null));
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
