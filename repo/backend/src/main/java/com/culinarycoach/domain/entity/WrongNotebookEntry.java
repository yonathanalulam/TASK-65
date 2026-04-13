package com.culinarycoach.domain.entity;

import com.culinarycoach.domain.enums.NotebookEntryStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "wrong_notebook_entries",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "question_id", "status"}))
public class WrongNotebookEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotebookEntryStatus status = NotebookEntryStatus.ACTIVE;

    @Column(name = "fail_count", nullable = false)
    private int failCount = 1;

    @Column(name = "last_attempt_at", nullable = false)
    private Instant lastAttemptAt;

    @Column(name = "latest_note", columnDefinition = "TEXT")
    private String latestNote;

    @Column(name = "is_favorite", nullable = false)
    private boolean isFavorite = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public NotebookEntryStatus getStatus() { return status; }
    public void setStatus(NotebookEntryStatus status) { this.status = status; }

    public int getFailCount() { return failCount; }
    public void setFailCount(int failCount) { this.failCount = failCount; }

    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(Instant lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }

    public String getLatestNote() { return latestNote; }
    public void setLatestNote(String latestNote) { this.latestNote = latestNote; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { this.isFavorite = favorite; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
