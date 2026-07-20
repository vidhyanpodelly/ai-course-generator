package com.aicoursegenerator.quiz.repository;

import com.aicoursegenerator.course.entity.Chapter;
import com.aicoursegenerator.quiz.entity.QuizAttempt;
import com.aicoursegenerator.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {
    List<QuizAttempt> findByUser(User user);
    List<QuizAttempt> findByUserAndChapter(User user, Chapter chapter);
}
