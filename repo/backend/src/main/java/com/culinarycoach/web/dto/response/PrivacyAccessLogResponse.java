package com.culinarycoach.web.dto.response;

import com.culinarycoach.domain.entity.PrivacyAccessLog;

import java.time.Instant;

public record PrivacyAccessLogResponse(
    Long id,
    String viewerUsername,
    String viewerRole,
    String subjectUsername,
    String resourceType,
    String resourceId,
    String reasonCode,
    String traceId,
    Instant createdAt
) {
    public static PrivacyAccessLogResponse from(PrivacyAccessLog log,
                                                 String viewerUsername,
                                                 String subjectUsername) {
        return new PrivacyAccessLogResponse(
            log.getId(),
            viewerUsername,
            log.getViewerRole(),
            subjectUsername,
            log.getResourceType(),
            log.getResourceId(),
            log.getReasonCode(),
            log.getTraceId(),
            log.getCreatedAt()
        );
    }
}
