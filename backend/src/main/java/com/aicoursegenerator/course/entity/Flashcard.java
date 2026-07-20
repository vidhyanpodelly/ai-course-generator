package com.aicoursegenerator.course.entity;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "flashcards")
public class Flashcard {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String front;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String back;

    @Column(nullable = false)
    private String difficulty; // EASY, MEDIUM, HARD

    @Column(nullable = false)
    private int box; // Leitner box number (1 to 5)

    @Column(name = "next_review", nullable = false)
    private ZonedDateTime nextReview;

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    public Flashcard() {}

    public Flashcard(UUID id, Chapter chapter, String front, String back, String difficulty, int box, ZonedDateTime nextReview) {
        this.id = id;
        this.chapter = chapter;
        this.front = front;
        this.back = back;
        this.difficulty = difficulty;
        this.box = box;
        this.nextReview = nextReview;
    }

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

    public String getFront() {
        return front;
    }

    public void setFront(String front) {
        this.front = front;
    }

    public String getBack() {
        return back;
    }

    public void setBack(String back) {
        this.back = back;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public int getBox() {
        return box;
    }

    public void setBox(int box) {
        this.box = box;
    }

    public ZonedDateTime getNextReview() {
        return nextReview;
    }

    public void setNextReview(ZonedDateTime nextReview) {
        this.nextReview = nextReview;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
