package com.culinarycoach.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "privacy_access_logs")
public class PrivacyAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "viewer_user_id", nullable = false)
    private Long viewerUserId;

    @Column(name = "viewer_role", nullable = false, length = 30)
    private String viewerRole;

    @Column(name = "subject_user_id", nullable = false)
    private Long subjectUserId;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(name = "reason_code", nullable = false, length = 50)
    private String reasonCode;

    @Column(name = "trace_id", nullable = false, length = 36)
    private String traceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }

    public Long getViewerUserId() { return viewerUserId; }
    public void setViewerUserId(Long viewerUserId) { this.viewerUserId = viewerUserId; }

    public String getViewerRole() { return viewerRole; }
    public void setViewerRole(String viewerRole) { this.viewerRole = viewerRole; }

    public Long getSubjectUserId() { return subjectUserId; }
    public void setSubjectUserId(Long subjectUserId) { this.subjectUserId = subjectUserId; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public Instant getCreatedAt() { return createdAt; }
}
