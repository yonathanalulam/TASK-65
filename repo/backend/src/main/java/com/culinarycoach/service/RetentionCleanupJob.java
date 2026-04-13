package com.culinarycoach.service;

import com.culinarycoach.audit.TraceContext;
import com.culinarycoach.domain.repository.AuditLogRepository;
import com.culinarycoach.domain.repository.JobRunRepository;
import com.culinarycoach.domain.repository.LoginAttemptRepository;
import com.culinarycoach.domain.repository.MetricSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class RetentionCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(RetentionCleanupJob.class);

    private final LoginAttemptRepository loginAttemptRepository;
    private final AuditLogRepository auditLogRepository;
    private final JobRunRepository jobRunRepository;
    private final MetricSnapshotRepository metricSnapshotRepository;
    private final MetricsService metricsService;

    public RetentionCleanupJob(LoginAttemptRepository loginAttemptRepository,
                                AuditLogRepository auditLogRepository,
                                JobRunRepository jobRunRepository,
                                MetricSnapshotRepository metricSnapshotRepository,
                                MetricsService metricsService) {
        this.loginAttemptRepository = loginAttemptRepository;
        this.auditLogRepository = auditLogRepository;
        this.jobRunRepository = jobRunRepository;
        this.metricSnapshotRepository = metricSnapshotRepository;
        this.metricsService = metricsService;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void runRetentionCleanup() {
        TraceContext.init();
        log.info("Running retention cleanup job");

        try {
            Instant now = Instant.now();
            Instant windowStart = now.minusSeconds(1);

            // Delete auth logs (login attempts) older than 1 year
            Instant oneYearAgo = now.minus(365, ChronoUnit.DAYS);
            int authLogsDeleted = deleteLoginAttemptsOlderThan(oneYearAgo);
            log.info("Retention cleanup: deleted {} auth logs older than 1 year", authLogsDeleted);

            // Delete audit logs older than 2 years
            Instant twoYearsAgo = now.minus(730, ChronoUnit.DAYS);
            int auditLogsDeleted = deleteAuditLogsOlderThan(twoYearsAgo);
            log.info("Retention cleanup: deleted {} audit logs older than 2 years", auditLogsDeleted);

            // Delete job runs older than 180 days
            Instant sixMonthsAgo = now.minus(180, ChronoUnit.DAYS);
            int jobRunsDeleted = jobRunRepository.deleteByCreatedAtBefore(sixMonthsAgo);
            log.info("Retention cleanup: deleted {} job runs older than 180 days", jobRunsDeleted);

            // Delete metric snapshots older than 180 days
            int metricsDeleted = metricSnapshotRepository.deleteByCreatedAtBefore(sixMonthsAgo);
            log.info("Retention cleanup: deleted {} metric snapshots older than 180 days",
                metricsDeleted);

            // Record cleanup actions as metrics
            metricsService.recordMetric("retention_cleanup_auth_logs",
                java.math.BigDecimal.valueOf(authLogsDeleted),
                "retention", "auth_logs", windowStart, now);
            metricsService.recordMetric("retention_cleanup_audit_logs",
                java.math.BigDecimal.valueOf(auditLogsDeleted),
                "retention", "audit_logs", windowStart, now);
            metricsService.recordMetric("retention_cleanup_job_runs",
                java.math.BigDecimal.valueOf(jobRunsDeleted),
                "retention", "job_runs", windowStart, now);
            metricsService.recordMetric("retention_cleanup_metrics",
                java.math.BigDecimal.valueOf(metricsDeleted),
                "retention", "metric_snapshots", windowStart, now);

            log.info("Retention cleanup completed: authLogs={}, auditLogs={}, jobRuns={}, metrics={}",
                authLogsDeleted, auditLogsDeleted, jobRunsDeleted, metricsDeleted);

        } catch (Exception e) {
            log.error("Retention cleanup job failed: {}", e.getMessage(), e);
        } finally {
            TraceContext.clear();
        }
    }

    private int deleteLoginAttemptsOlderThan(Instant cutoff) {
        var all = loginAttemptRepository.findAll();
        int count = 0;
        for (var attempt : all) {
            if (attempt.getAttemptedAt() != null && attempt.getAttemptedAt().isBefore(cutoff)) {
                loginAttemptRepository.delete(attempt);
                count++;
            }
        }
        return count;
    }

    private int deleteAuditLogsOlderThan(Instant cutoff) {
        var all = auditLogRepository.findAll();
        int count = 0;
        for (var log : all) {
            if (log.getCreatedAt() != null && log.getCreatedAt().isBefore(cutoff)) {
                auditLogRepository.delete(log);
                count++;
            }
        }
        return count;
    }
}
