package com.aicoursegenerator.course.repository;

import com.aicoursegenerator.course.entity.Chapter;
import com.aicoursegenerator.course.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {
    List<Lesson> findByChapterOrderBySequenceNumberAsc(Chapter chapter);
    Optional<Lesson> findByIdAndChapter(UUID id, Chapter chapter);
}
