package com.culinarycoach.domain.entity;

import com.culinarycoach.domain.enums.TipDisplayMode;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tip_card_configurations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"scope", "scope_id"}))
public class TipCardConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String scope;

    @Column(name = "scope_id")
    private Long scopeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "display_mode", nullable = false, length = 20)
    private TipDisplayMode displayMode = TipDisplayMode.SHORT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", length = 64)
    private String updatedBy;

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

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public Long getScopeId() { return scopeId; }
    public void setScopeId(Long scopeId) { this.scopeId = scopeId; }

    public TipDisplayMode getDisplayMode() { return displayMode; }
    public void setDisplayMode(TipDisplayMode displayMode) { this.displayMode = displayMode; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
