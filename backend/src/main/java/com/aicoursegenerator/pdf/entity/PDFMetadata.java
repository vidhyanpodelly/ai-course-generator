package com.aicoursegenerator.pdf.entity;

import com.aicoursegenerator.user.entity.User;
import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "pdf_metadata")
public class PDFMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(name = "file_path", nullable = false, length = 512)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "total_pages")
    private Integer totalPages;

    @Column(nullable = false, length = 50)
    private String status; // PENDING, PARSED, FAILED

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    public PDFMetadata() {
    }

    public PDFMetadata(UUID id, User user, String filename, String filePath, Long fileSize, Integer totalPages, String status, String failureReason, ZonedDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.filename = filename;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.totalPages = totalPages;
        this.status = status;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = ZonedDateTime.now();
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

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
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

    // Builder
    public static class Builder {
        private UUID id;
        private User user;
        private String filename;
        private String filePath;
        private Long fileSize;
        private Integer totalPages;
        private String status;
        private String failureReason;
        private ZonedDateTime createdAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder filename(String filename) {
            this.filename = filename;
            return this;
        }

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder fileSize(Long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public Builder totalPages(Integer totalPages) {
            this.totalPages = totalPages;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public Builder createdAt(ZonedDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public PDFMetadata build() {
            return new PDFMetadata(id, user, filename, filePath, fileSize, totalPages, status, failureReason, createdAt);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
