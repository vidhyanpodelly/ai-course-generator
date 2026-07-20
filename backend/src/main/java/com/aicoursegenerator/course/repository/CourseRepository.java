package com.aicoursegenerator.course.repository;

import com.aicoursegenerator.course.entity.Course;
import com.aicoursegenerator.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {
    List<Course> findByUser(User user);
    Optional<Course> findByIdAndUser(UUID id, User user);

    // Cross-entity search query: searches keywords across courses, chapters, and lessons for a user
    @Query("SELECT DISTINCT c FROM Course c " +
           "LEFT JOIN Chapter ch ON ch.course = c " +
           "LEFT JOIN Lesson l ON l.chapter = ch " +
           "WHERE c.user = :user AND (" +
           "LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(ch.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(l.explanation) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Course> searchCoursesByKeyword(@Param("user") User user, @Param("keyword") String keyword);
}
