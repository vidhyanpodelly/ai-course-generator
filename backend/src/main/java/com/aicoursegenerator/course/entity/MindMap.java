package com.aicoursegenerator.course.entity;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "mind_maps")
public class MindMap {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(name = "mermaid_data", columnDefinition = "TEXT", nullable = false)
    private String mermaidData;

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    public MindMap() {}

    public MindMap(UUID id, Chapter chapter, String mermaidData) {
        this.id = id;
        this.chapter = chapter;
        this.mermaidData = mermaidData;
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

    public String getMermaidData() {
        return mermaidData;
    }

    public void setMermaidData(String mermaidData) {
        this.mermaidData = mermaidData;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
