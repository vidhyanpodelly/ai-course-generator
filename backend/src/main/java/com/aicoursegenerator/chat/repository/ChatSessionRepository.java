package com.aicoursegenerator.chat.repository;

import com.aicoursegenerator.chat.entity.ChatSession;
import com.aicoursegenerator.course.entity.Course;
import com.aicoursegenerator.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    List<ChatSession> findByUserAndCourseOrderByUpdatedAtDesc(User user, Course course);
    Optional<ChatSession> findByIdAndUser(UUID id, User user);
    void deleteByCourseAndUser(Course course, User user);
}
