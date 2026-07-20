package com.aicoursegenerator.course.dto;

import java.util.List;
import java.util.UUID;

public record DashboardStats(
    long totalPdfsCount,
    long totalCoursesCount,
    long totalCompletedLessonsCount,
    double averageQuizScore,
    List<CourseProgressDetail> recentCourses
) {
    public record CourseProgressDetail(
        UUID courseId,
        UUID pdfMetadataId,
        String title,
        String status,
        double completionPercentage,
        int completedLessons,
        int totalLessons,
        String failureReason
    ) {}
}
