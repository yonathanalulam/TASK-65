package com.culinarycoach.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "question_similarity_links",
       uniqueConstraints = @UniqueConstraint(columnNames = {"question_id_a", "question_id_b"}))
public class QuestionSimilarityLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id_a", nullable = false)
    private Long questionIdA;

    @Column(name = "question_id_b", nullable = false)
    private Long questionIdB;

    @Column(name = "similarity_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal similarityScore = BigDecimal.ONE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getQuestionIdA() { return questionIdA; }
    public void setQuestionIdA(Long questionIdA) { this.questionIdA = questionIdA; }

    public Long getQuestionIdB() { return questionIdB; }
    public void setQuestionIdB(Long questionIdB) { this.questionIdB = questionIdB; }

    public BigDecimal getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(BigDecimal similarityScore) { this.similarityScore = similarityScore; }

    public Instant getCreatedAt() { return createdAt; }
}
