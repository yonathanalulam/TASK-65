package com.culinarycoach.web.dto.response;

import com.culinarycoach.domain.entity.AnomalyAlert;

import java.math.BigDecimal;
import java.time.Instant;

public record AnomalyAlertResponse(
    Long id,
    String alertType,
    String severity,
    String message,
    String metricName,
    BigDecimal thresholdValue,
    BigDecimal actualValue,
    String status,
    String acknowledgedBy,
    Instant acknowledgedAt,
    Instant resolvedAt,
    Instant createdAt
) {
    public static AnomalyAlertResponse from(AnomalyAlert alert) {
        return new AnomalyAlertResponse(
            alert.getId(),
            alert.getAlertType(),
            alert.getSeverity().name(),
            alert.getMessage(),
            alert.getMetricName(),
            alert.getThresholdValue(),
            alert.getActualValue(),
            alert.getStatus().name(),
            alert.getAcknowledgedBy(),
            alert.getAcknowledgedAt(),
            alert.getResolvedAt(),
            alert.getCreatedAt()
        );
    }
}
