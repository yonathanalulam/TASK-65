package com.culinarycoach.domain.entity;

import com.culinarycoach.domain.enums.AttemptClassification;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "question_attempts")
public class QuestionAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "user_answer", nullable = false, columnDefinition = "TEXT")
    private String userAnswer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptClassification classification;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "flagged_by_user", nullable = false)
    private boolean flaggedByUser = false;

    @Column(name = "drill_run_id")
    private Long drillRunId;

    @Column(name = "attempted_at", nullable = false, updatable = false)
    private Instant attemptedAt;

    @PrePersist
    protected void onCreate() {
        this.attemptedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public Long getVariantId() { return variantId; }
    public void setVariantId(Long variantId) { this.variantId = variantId; }

    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }

    public AttemptClassification getClassification() { return classification; }
    public void setClassification(AttemptClassification classification) { this.classification = classification; }

    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }

    public boolean isFlaggedByUser() { return flaggedByUser; }
    public void setFlaggedByUser(boolean flaggedByUser) { this.flaggedByUser = flaggedByUser; }

    public Long getDrillRunId() { return drillRunId; }
    public void setDrillRunId(Long drillRunId) { this.drillRunId = drillRunId; }

    public Instant getAttemptedAt() { return attemptedAt; }
}
