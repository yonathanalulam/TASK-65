package com.culinarycoach.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audio_bundle_entitlements",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "bundle_id"}))
public class AudioBundleEntitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "bundle_id", nullable = false)
    private Long bundleId;

    @Column(name = "granted_at", nullable = false, updatable = false)
    private Instant grantedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "granted_by", length = 64)
    private String grantedBy;

    @Column(nullable = false, length = 50)
    private String source = "MOCK_CHECKOUT";

    @PrePersist
    protected void onCreate() {
        if (this.grantedAt == null) {
            this.grantedAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getBundleId() { return bundleId; }
    public void setBundleId(Long bundleId) { this.bundleId = bundleId; }

    public Instant getGrantedAt() { return grantedAt; }
    public void setGrantedAt(Instant grantedAt) { this.grantedAt = grantedAt; }

    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }

    public String getGrantedBy() { return grantedBy; }
    public void setGrantedBy(String grantedBy) { this.grantedBy = grantedBy; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
