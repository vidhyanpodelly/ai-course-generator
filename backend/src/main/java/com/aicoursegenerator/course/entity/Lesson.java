package com.aicoursegenerator.course.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "lessons")
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String explanation; // generated lazily

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "key_takeaways", columnDefinition = "jsonb")
    private List<String> keyTakeaways;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "important_notes", columnDefinition = "jsonb")
    private List<String> importantNotes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "real_world_examples", columnDefinition = "jsonb")
    private List<String> realWorldExamples;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    public Lesson() {
    }

    public Lesson(UUID id, Chapter chapter, String title, String explanation, List<String> keyTakeaways, List<String> importantNotes, List<String> realWorldExamples, Integer sequenceNumber) {
        this.id = id;
        this.chapter = chapter;
        this.title = title;
        this.explanation = explanation;
        this.keyTakeaways = keyTakeaways;
        this.importantNotes = importantNotes;
        this.realWorldExamples = realWorldExamples;
        this.sequenceNumber = sequenceNumber;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Chapter getChapter() {
        return chapter;
    }

    public void setChapter(Chapter chapter) {
        this.chapter = chapter;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public List<String> getKeyTakeaways() {
        return keyTakeaways;
    }

    public void setKeyTakeaways(List<String> keyTakeaways) {
        this.keyTakeaways = keyTakeaways;
    }

    public List<String> getImportantNotes() {
        return importantNotes;
    }

    public void setImportantNotes(List<String> importantNotes) {
        this.importantNotes = importantNotes;
    }

    public List<String> getRealWorldExamples() {
        return realWorldExamples;
    }

    public void setRealWorldExamples(List<String> realWorldExamples) {
        this.realWorldExamples = realWorldExamples;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
}
