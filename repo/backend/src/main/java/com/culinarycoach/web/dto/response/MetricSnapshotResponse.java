package com.culinarycoach.web.dto.response;

import com.culinarycoach.domain.entity.MetricSnapshot;

import java.math.BigDecimal;
import java.time.Instant;

public record MetricSnapshotResponse(
    Long id,
    String metricName,
    BigDecimal metricValue,
    String dimensionKey,
    String dimensionValue,
    Instant windowStart,
    Instant windowEnd
) {
    public static MetricSnapshotResponse from(MetricSnapshot snapshot) {
        return new MetricSnapshotResponse(
            snapshot.getId(),
            snapshot.getMetricName(),
            snapshot.getMetricValue(),
            snapshot.getDimensionKey(),
            snapshot.getDimensionValue(),
            snapshot.getWindowStart(),
            snapshot.getWindowEnd()
        );
    }
}
