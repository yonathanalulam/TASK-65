package com.culinarycoach.service;

import com.culinarycoach.domain.entity.MetricSnapshot;
import com.culinarycoach.domain.repository.MetricSnapshotRepository;
import com.culinarycoach.web.dto.response.KpiSummaryResponse;
import com.culinarycoach.web.dto.response.MetricSnapshotResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class MetricsService {

    private static final Logger log = LoggerFactory.getLogger(MetricsService.class);

    public static final String METRIC_REQUEST_THROUGHPUT = "request_throughput";
    public static final String METRIC_ERROR_RATE = "error_rate";
    public static final String METRIC_ERROR_COUNT = "error_count";
    public static final String METRIC_P50_LATENCY = "p50_latency";
    public static final String METRIC_P95_LATENCY = "p95_latency";

    private final MetricSnapshotRepository metricSnapshotRepository;

    public MetricsService(MetricSnapshotRepository metricSnapshotRepository) {
        this.metricSnapshotRepository = metricSnapshotRepository;
    }

    @Transactional
    public MetricSnapshot recordMetric(String name, BigDecimal value,
                                        String dimensionKey, String dimensionValue,
                                        Instant windowStart, Instant windowEnd) {
        MetricSnapshot snapshot = new MetricSnapshot();
        snapshot.setMetricName(name);
        snapshot.setMetricValue(value);
        snapshot.setDimensionKey(dimensionKey);
        snapshot.setDimensionValue(dimensionValue);
        snapshot.setWindowStart(windowStart);
        snapshot.setWindowEnd(windowEnd);
        return metricSnapshotRepository.save(snapshot);
    }

    public List<MetricSnapshotResponse> getMetrics(String name, Instant from, Instant to) {
        return metricSnapshotRepository.findByMetricNameAndWindowStartBetween(name, from, to)
            .stream()
            .map(MetricSnapshotResponse::from)
            .toList();
    }

    public Optional<MetricSnapshotResponse> getLatestMetric(String name) {
        return metricSnapshotRepository.findLatestByMetricName(name)
            .map(MetricSnapshotResponse::from);
    }

    public BigDecimal getRequestThroughput(Instant from, Instant to) {
        List<MetricSnapshot> snapshots = metricSnapshotRepository
            .findByMetricNameAndWindowStartBetween(METRIC_REQUEST_THROUGHPUT, from, to);

        if (snapshots.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return snapshots.stream()
            .map(MetricSnapshot::getMetricValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(snapshots.size()), 4, java.math.RoundingMode.HALF_UP);
    }

    public BigDecimal getErrorRate(Instant from, Instant to) {
        List<MetricSnapshot> snapshots = metricSnapshotRepository
            .findByMetricNameAndWindowStartBetween(METRIC_ERROR_RATE, from, to);

        if (snapshots.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return snapshots.stream()
            .map(MetricSnapshot::getMetricValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(snapshots.size()), 4, java.math.RoundingMode.HALF_UP);
    }

    public KpiSummaryResponse getP50P95Latency(Instant from, Instant to) {
        Optional<MetricSnapshot> latestP50 = metricSnapshotRepository
            .findLatestByMetricName(METRIC_P50_LATENCY);
        Optional<MetricSnapshot> latestP95 = metricSnapshotRepository
            .findLatestByMetricName(METRIC_P95_LATENCY);

        BigDecimal throughput = getRequestThroughput(from, to);
        BigDecimal errorRate = getErrorRate(from, to);

        return new KpiSummaryResponse(
            throughput,
            errorRate,
            latestP50.map(MetricSnapshot::getMetricValue).orElse(BigDecimal.ZERO),
            latestP95.map(MetricSnapshot::getMetricValue).orElse(BigDecimal.ZERO),
            from,
            to
        );
    }
}
