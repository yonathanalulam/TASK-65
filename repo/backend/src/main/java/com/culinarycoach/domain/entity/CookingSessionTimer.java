package com.culinarycoach.domain.entity;

import com.culinarycoach.domain.enums.TimerStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "cooking_session_timers")
public class CookingSessionTimer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "step_id")
    private Long stepId;

    @Column(length = 200)
    private String label;

    @Column(name = "timer_type", nullable = false, length = 30)
    private String timerType = "STEP";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TimerStatus status = TimerStatus.RUNNING;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "target_end_at", nullable = false)
    private Instant targetEndAt;

    @Column(name = "paused_at")
    private Instant pausedAt;

    @Column(name = "elapsed_before_pause_seconds", nullable = false)
    private int elapsedBeforePauseSeconds = 0;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public Long getStepId() { return stepId; }
    public void setStepId(Long stepId) { this.stepId = stepId; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getTimerType() { return timerType; }
    public void setTimerType(String timerType) { this.timerType = timerType; }

    public TimerStatus getStatus() { return status; }
    public void setStatus(TimerStatus status) { this.status = status; }

    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getTargetEndAt() { return targetEndAt; }
    public void setTargetEndAt(Instant targetEndAt) { this.targetEndAt = targetEndAt; }

    public Instant getPausedAt() { return pausedAt; }
    public void setPausedAt(Instant pausedAt) { this.pausedAt = pausedAt; }

    public int getElapsedBeforePauseSeconds() { return elapsedBeforePauseSeconds; }
    public void setElapsedBeforePauseSeconds(int elapsedBeforePauseSeconds) { this.elapsedBeforePauseSeconds = elapsedBeforePauseSeconds; }

    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(Instant acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }

    public Instant getCreatedAt() { return createdAt; }
}
