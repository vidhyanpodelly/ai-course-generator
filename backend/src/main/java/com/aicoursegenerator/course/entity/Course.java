package com.aicoursegenerator.course.entity;

import com.aicoursegenerator.pdf.entity.PDFMetadata;
import com.aicoursegenerator.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pdf_metadata_id", nullable = false)
    private PDFMetadata pdfMetadata;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "estimated_duration", length = 50)
    private String estimatedDuration;

    @Column(name = "difficulty_level", length = 50)
    private String difficultyLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "prerequisites", columnDefinition = "jsonb")
    private List<String> prerequisites;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "learning_objectives", columnDefinition = "jsonb")
    private List<String> learningObjectives;

    @Column(nullable = false, length = 50)
    private String status; // PENDING, GENERATING_OUTLINE, OUTLINE_GENERATED, READY, FAILED

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    public Course() {
    }

    public Course(UUID id, User user, PDFMetadata pdfMetadata, String title, String description, String estimatedDuration, String difficultyLevel, List<String> prerequisites, List<String> learningObjectives, String status, String failureReason, ZonedDateTime createdAt, ZonedDateTime updatedAt) {
        this.id = id;
        this.user = user;
        this.pdfMetadata = pdfMetadata;
        this.title = title;
        this.description = description;
        this.estimatedDuration = estimatedDuration;
        this.difficultyLevel = difficultyLevel;
        this.prerequisites = prerequisites;
        this.learningObjectives = learningObjectives;
        this.status = status;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = ZonedDateTime.now();
        updatedAt = ZonedDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = ZonedDateTime.now();
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

    public PDFMetadata getPdfMetadata() {
        return pdfMetadata;
    }

    public void setPdfMetadata(PDFMetadata pdfMetadata) {
        this.pdfMetadata = pdfMetadata;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEstimatedDuration() {
        return estimatedDuration;
    }

    public void setEstimatedDuration(String estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
    }

    public String getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(String difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public List<String> getPrerequisites() {
        return prerequisites;
    }

    public void setPrerequisites(List<String> prerequisites) {
        this.prerequisites = prerequisites;
    }

    public List<String> getLearningObjectives() {
        return learningObjectives;
    }

    public void setLearningObjectives(List<String> learningObjectives) {
        this.learningObjectives = learningObjectives;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
