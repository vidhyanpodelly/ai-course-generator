package com.aicoursegenerator.course.repository;

import com.aicoursegenerator.course.entity.Chapter;
import com.aicoursegenerator.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, UUID> {
    List<Chapter> findByCourseOrderBySequenceNumberAsc(Course course);
    Optional<Chapter> findByIdAndCourse(UUID id, Course course);
}
