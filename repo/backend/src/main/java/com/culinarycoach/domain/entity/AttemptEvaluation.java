package com.culinarycoach.domain.entity;

import com.culinarycoach.domain.enums.AttemptClassification;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "attempt_evaluations")
public class AttemptEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attempt_id", nullable = false, unique = true)
    private Long attemptId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptClassification classification;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "evaluated_at", nullable = false, updatable = false)
    private Instant evaluatedAt;

    @PrePersist
    protected void onCreate() {
        this.evaluatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAttemptId() { return attemptId; }
    public void setAttemptId(Long attemptId) { this.attemptId = attemptId; }

    public AttemptClassification getClassification() { return classification; }
    public void setClassification(AttemptClassification classification) { this.classification = classification; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public Instant getEvaluatedAt() { return evaluatedAt; }
}
