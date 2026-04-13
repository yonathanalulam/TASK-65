package com.culinarycoach.domain.entity;

import com.culinarycoach.domain.enums.DrillType;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "drill_runs")
public class DrillRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "drill_type", nullable = false, length = 30)
    private DrillType drillType;

    @Column(name = "source_entry_id")
    private Long sourceEntryId;

    @Column(name = "source_question_id")
    private Long sourceQuestionId;

    @Column(nullable = false, length = 20)
    private String status = "IN_PROGRESS";

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions = 0;

    @Column(name = "correct_count", nullable = false)
    private int correctCount = 0;

    @PrePersist
    protected void onCreate() {
        this.startedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public DrillType getDrillType() { return drillType; }
    public void setDrillType(DrillType drillType) { this.drillType = drillType; }

    public Long getSourceEntryId() { return sourceEntryId; }
    public void setSourceEntryId(Long sourceEntryId) { this.sourceEntryId = sourceEntryId; }

    public Long getSourceQuestionId() { return sourceQuestionId; }
    public void setSourceQuestionId(Long sourceQuestionId) { this.sourceQuestionId = sourceQuestionId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getStartedAt() { return startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }

    public int getCorrectCount() { return correctCount; }
    public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }
}
