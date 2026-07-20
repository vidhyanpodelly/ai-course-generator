package com.aicoursegenerator.course.repository;

import com.aicoursegenerator.course.entity.Flashcard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface FlashcardRepository extends JpaRepository<Flashcard, UUID> {
    List<Flashcard> findByChapterId(UUID chapterId);
    List<Flashcard> findByChapterCourseId(UUID courseId);

    @Query("SELECT f FROM Flashcard f WHERE f.chapter.course.user.id = :userId AND f.nextReview <= :now")
    List<Flashcard> findDueReviewsForUser(@Param("userId") UUID userId, @Param("now") ZonedDateTime now);

    @Query("SELECT COUNT(f) FROM Flashcard f WHERE f.chapter.course.user.id = :userId AND f.nextReview <= :now")
    long countDueReviewsForUser(@Param("userId") UUID userId, @Param("now") ZonedDateTime now);
}
