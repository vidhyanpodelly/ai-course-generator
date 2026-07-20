package com.aicoursegenerator.course.entity;

import com.aicoursegenerator.user.entity.User;
import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "learning_progress",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "lesson_id"})
    }
)
public class LearningProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(nullable = false)
    private Boolean completed = false;

    @Column(name = "completed_at")
    private ZonedDateTime completedAt;

    public LearningProgress() {
    }

    public LearningProgress(UUID id, User user, Lesson lesson, Boolean completed, ZonedDateTime completedAt) {
        this.id = id;
        this.user = user;
        this.lesson = lesson;
        this.completed = completed;
        this.completedAt = completedAt;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public void setLesson(Lesson lesson) {
        this.lesson = lesson;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public ZonedDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(ZonedDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
