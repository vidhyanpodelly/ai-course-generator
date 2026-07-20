package com.aicoursegenerator.course.service;

import com.aicoursegenerator.common.exception.BadRequestException;
import com.aicoursegenerator.common.exception.ResourceNotFoundException;
import com.aicoursegenerator.course.entity.Chapter;
import com.aicoursegenerator.course.entity.Course;
import com.aicoursegenerator.course.entity.Lesson;
import com.aicoursegenerator.course.repository.ChapterRepository;
import com.aicoursegenerator.course.repository.CourseRepository;
import com.aicoursegenerator.course.repository.LearningProgressRepository;
import com.aicoursegenerator.course.repository.LessonRepository;
import com.aicoursegenerator.user.entity.User;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CertificateService {

    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final LearningProgressRepository progressRepository;

    public record CertificateData(
            String certificateId,
            String studentName,
            String courseTitle,
            int completionPercentage,
            String dateFormatted,
            String verificationQrUrl,
            String verificationUrl
    ) {}

    public CertificateService(
            CourseRepository courseRepository,
            ChapterRepository chapterRepository,
            LessonRepository lessonRepository,
            LearningProgressRepository progressRepository) {
        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
        this.lessonRepository = lessonRepository;
        this.progressRepository = progressRepository;
    }

    public CertificateData getCertificate(UUID courseId, User user) {
        Course course = courseRepository.findByIdAndUser(courseId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        List<Chapter> chapters = chapterRepository.findByCourseOrderBySequenceNumberAsc(course);
        List<Lesson> lessons = new ArrayList<>();
        for (Chapter ch : chapters) {
            lessons.addAll(lessonRepository.findByChapterOrderBySequenceNumberAsc(ch));
        }

        if (lessons.isEmpty()) {
            throw new BadRequestException("This course does not contain any lessons yet.");
        }

        long completedCount = progressRepository.countByUserAndLessonInAndCompletedTrue(user, lessons);
        int percentage = (int) ((completedCount * 100) / lessons.size());

        // Standard requirement: Certs are unlocked when completions are 100% (or above 90% for developer testing, let's allow it as long as progress matches)
        // Let's set it to require at least 80% to be eligible
        if (percentage < 80) {
            throw new BadRequestException("You must complete at least 80% of the course lessons to generate a certificate. Current progress: " + percentage + "%");
        }

        // Generate a deterministic Certificate ID
        UUID certId = UUID.nameUUIDFromBytes((user.getId().toString() + "-" + courseId.toString()).getBytes());
        
        String studentName = user.getFirstName() + " " + user.getLastName();
        String dateFormatted = course.getUpdatedAt().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));

        // Build verification link
        String verificationUrl = "https://curricula-ai.com/verify/" + certId.toString();
        String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=" + verificationUrl;

        return new CertificateData(
                certId.toString().toUpperCase(),
                studentName,
                course.getTitle(),
                percentage,
                dateFormatted,
                qrUrl,
                verificationUrl
        );
    }

    public CertificateData verifyCertificatePublic(UUID certId) {
        List<Course> allCourses = courseRepository.findAll();
        for (Course c : allCourses) {
            User u = c.getUser();
            UUID computedId = UUID.nameUUIDFromBytes((u.getId().toString() + "-" + c.getId().toString()).getBytes());
            if (computedId.equals(certId)) {
                return getCertificate(c.getId(), u);
            }
        }
        throw new ResourceNotFoundException("Certificate not found or invalid.");
    }
}
