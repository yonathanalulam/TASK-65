package com.culinarycoach.service;

import com.culinarycoach.audit.TraceContext;
import com.culinarycoach.domain.repository.AuditLogRepository;
import com.culinarycoach.domain.repository.LoginAttemptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class MetricsRollupJob {

    private static final Logger log = LoggerFactory.getLogger(MetricsRollupJob.class);

    private final AuditLogRepository auditLogRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final MetricsService metricsService;

    public MetricsRollupJob(AuditLogRepository auditLogRepository,
                             LoginAttemptRepository loginAttemptRepository,
                             MetricsService metricsService) {
        this.auditLogRepository = auditLogRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.metricsService = metricsService;
    }

    @Scheduled(cron = "0 */10 * * * *")
    public void rollupMetrics() {
        TraceContext.init();
        log.info("Running metrics rollup job");

        try {
            Instant now = Instant.now();
            Instant windowStart = now.minus(10, ChronoUnit.MINUTES);

            // Request throughput: count of audit log entries in the window as proxy
            long totalRequests = auditLogRepository.count();
            metricsService.recordMetric(
                MetricsService.METRIC_REQUEST_THROUGHPUT,
                BigDecimal.valueOf(totalRequests),
                "rollup", "10min",
                windowStart, now
            );

            // Error count: count of failed login attempts in the window as proxy for errors
            // Using a broad "since" to capture window activity
            long errorCount = loginAttemptRepository.countRecentFailures("*", windowStart);
            metricsService.recordMetric(
                MetricsService.METRIC_ERROR_COUNT,
                BigDecimal.valueOf(errorCount),
                "rollup", "10min",
                windowStart, now
            );

            // Error rate calculation
            BigDecimal errorRate = BigDecimal.ZERO;
            if (totalRequests > 0) {
                errorRate = BigDecimal.valueOf(errorCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalRequests), 4, RoundingMode.HALF_UP);
            }
            metricsService.recordMetric(
                MetricsService.METRIC_ERROR_RATE,
                errorRate,
                "rollup", "10min",
                windowStart, now
            );

            // Record placeholder latency metrics
            // In a production system these would come from actual request timing
            metricsService.recordMetric(
                MetricsService.METRIC_P50_LATENCY,
                BigDecimal.ZERO,
                "rollup", "10min",
                windowStart, now
            );

            metricsService.recordMetric(
                MetricsService.METRIC_P95_LATENCY,
                BigDecimal.ZERO,
                "rollup", "10min",
                windowStart, now
            );

            log.info("Metrics rollup completed: throughput={}, errors={}, errorRate={}%",
                totalRequests, errorCount, errorRate);

        } catch (Exception e) {
            log.error("Metrics rollup job failed: {}", e.getMessage(), e);
        } finally {
            TraceContext.clear();
        }
    }
}
