package com.aicoursegenerator.course.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chapters")
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "quiz_data", columnDefinition = "jsonb")
    private List<QuizQuestion> quizData;

    public Chapter() {
    }

    public Chapter(UUID id, Course course, String title, String summary, Integer sequenceNumber, List<QuizQuestion> quizData) {
        this.id = id;
        this.course = course;
        this.title = title;
        this.summary = summary;
        this.sequenceNumber = sequenceNumber;
        this.quizData = quizData;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public List<QuizQuestion> getQuizData() {
        return quizData;
    }

    public void setQuizData(List<QuizQuestion> quizData) {
        this.quizData = quizData;
    }
}
