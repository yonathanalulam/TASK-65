package com.culinarycoach.domain.entity;

import com.culinarycoach.domain.enums.CookingSessionStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "cooking_sessions")
public class CookingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "recipe_title", nullable = false, length = 255)
    private String recipeTitle;

    @Column(name = "lesson_id")
    private Long lessonId;

    @Column(name = "workflow_version", length = 50)
    private String workflowVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CookingSessionStatus status = CookingSessionStatus.CREATED;

    @Column(name = "total_steps", nullable = false)
    private int totalSteps = 0;

    @Column(name = "last_completed_step_order", nullable = false)
    private int lastCompletedStepOrder = -1;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "resumed_at")
    private Instant resumedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "abandoned_at")
    private Instant abandonedAt;

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version = 0L;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.lastActivityAt == null) {
            this.lastActivityAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getRecipeTitle() { return recipeTitle; }
    public void setRecipeTitle(String recipeTitle) { this.recipeTitle = recipeTitle; }

    public Long getLessonId() { return lessonId; }
    public void setLessonId(Long lessonId) { this.lessonId = lessonId; }

    public String getWorkflowVersion() { return workflowVersion; }
    public void setWorkflowVersion(String workflowVersion) { this.workflowVersion = workflowVersion; }

    public CookingSessionStatus getStatus() { return status; }
    public void setStatus(CookingSessionStatus status) { this.status = status; }

    public int getTotalSteps() { return totalSteps; }
    public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }

    public int getLastCompletedStepOrder() { return lastCompletedStepOrder; }
    public void setLastCompletedStepOrder(int lastCompletedStepOrder) { this.lastCompletedStepOrder = lastCompletedStepOrder; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getResumedAt() { return resumedAt; }
    public void setResumedAt(Instant resumedAt) { this.resumedAt = resumedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Instant getAbandonedAt() { return abandonedAt; }
    public void setAbandonedAt(Instant abandonedAt) { this.abandonedAt = abandonedAt; }

    public Instant getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(Instant lastActivityAt) { this.lastActivityAt = lastActivityAt; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
