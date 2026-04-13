package com.culinarycoach.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "cooking_session_steps",
       uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "step_order"}))
public class CookingSessionStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "expected_duration_seconds")
    private Integer expectedDurationSeconds;

    @Column(name = "has_timer", nullable = false)
    private boolean hasTimer = false;

    @Column(name = "timer_duration_seconds")
    private Integer timerDurationSeconds;

    @Column(name = "reminder_text", length = 500)
    private String reminderText;

    @Column(nullable = false)
    private boolean completed = false;

    @Column(name = "completed_at")
    private Instant completedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public int getStepOrder() { return stepOrder; }
    public void setStepOrder(int stepOrder) { this.stepOrder = stepOrder; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getExpectedDurationSeconds() { return expectedDurationSeconds; }
    public void setExpectedDurationSeconds(Integer expectedDurationSeconds) { this.expectedDurationSeconds = expectedDurationSeconds; }

    public boolean isHasTimer() { return hasTimer; }
    public void setHasTimer(boolean hasTimer) { this.hasTimer = hasTimer; }

    public Integer getTimerDurationSeconds() { return timerDurationSeconds; }
    public void setTimerDurationSeconds(Integer timerDurationSeconds) { this.timerDurationSeconds = timerDurationSeconds; }

    public String getReminderText() { return reminderText; }
    public void setReminderText(String reminderText) { this.reminderText = reminderText; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
