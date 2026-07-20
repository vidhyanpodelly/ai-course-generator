package com.aicoursegenerator.course.repository;

import com.aicoursegenerator.course.entity.LearningProgress;
import com.aicoursegenerator.course.entity.Lesson;
import com.aicoursegenerator.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LearningProgressRepository extends JpaRepository<LearningProgress, UUID> {
    Optional<LearningProgress> findByUserAndLesson(User user, Lesson lesson);
    List<LearningProgress> findByUserAndLessonIn(User user, List<Lesson> lessons);
    long countByUserAndLessonInAndCompletedTrue(User user, List<Lesson> lessons);
}
