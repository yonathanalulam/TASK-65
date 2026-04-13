package com.culinarycoach.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "question_variants")
public class QuestionVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_question_id", nullable = false)
    private Long originalQuestionId;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "canonical_answer", nullable = false, columnDefinition = "TEXT")
    private String canonicalAnswer;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOriginalQuestionId() { return originalQuestionId; }
    public void setOriginalQuestionId(Long originalQuestionId) { this.originalQuestionId = originalQuestionId; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getCanonicalAnswer() { return canonicalAnswer; }
    public void setCanonicalAnswer(String canonicalAnswer) { this.canonicalAnswer = canonicalAnswer; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public Instant getCreatedAt() { return createdAt; }
}
