package com.culinarycoach.service;

import com.culinarycoach.audit.TraceContext;
import com.culinarycoach.domain.entity.MetricSnapshot;
import com.culinarycoach.domain.entity.ScheduledJob;
import com.culinarycoach.domain.enums.AlertSeverity;
import com.culinarycoach.domain.repository.MetricSnapshotRepository;
import com.culinarycoach.domain.repository.ScheduledJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class AnomalyEvaluationJob {

    private static final Logger log = LoggerFactory.getLogger(AnomalyEvaluationJob.class);

    private static final BigDecimal ERROR_RATE_THRESHOLD = new BigDecimal("2.0");
    private static final long JOB_FAILURE_THRESHOLD = 3;

    private final MetricSnapshotRepository metricSnapshotRepository;
    private final ScheduledJobRepository scheduledJobRepository;
    private final AnomalyAlertService anomalyAlertService;
    private final JobRunService jobRunService;

    public AnomalyEvaluationJob(MetricSnapshotRepository metricSnapshotRepository,
                                 ScheduledJobRepository scheduledJobRepository,
                                 AnomalyAlertService anomalyAlertService,
                                 JobRunService jobRunService) {
        this.metricSnapshotRepository = metricSnapshotRepository;
        this.scheduledJobRepository = scheduledJobRepository;
        this.anomalyAlertService = anomalyAlertService;
        this.jobRunService = jobRunService;
    }

    @Scheduled(cron = "0 */10 * * * *")
    public void evaluate() {
        TraceContext.init();
        log.info("Running anomaly evaluation job");

        try {
            checkErrorRate();
            checkJobFailures();
            log.info("Anomaly evaluation completed");
        } catch (Exception e) {
            log.error("Anomaly evaluation job failed: {}", e.getMessage(), e);
        } finally {
            TraceContext.clear();
        }
    }

    private void checkErrorRate() {
        Instant now = Instant.now();
        Instant tenMinutesAgo = now.minus(10, ChronoUnit.MINUTES);

        List<MetricSnapshot> errorRateSnapshots = metricSnapshotRepository
            .findByMetricNameAndWindowStartBetween(
                MetricsService.METRIC_ERROR_RATE, tenMinutesAgo, now);

        if (errorRateSnapshots.isEmpty()) {
            return;
        }

        BigDecimal avgErrorRate = errorRateSnapshots.stream()
            .map(MetricSnapshot::getMetricValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(errorRateSnapshots.size()), 4,
                java.math.RoundingMode.HALF_UP);

        if (avgErrorRate.compareTo(ERROR_RATE_THRESHOLD) > 0) {
            anomalyAlertService.createAlert(
                "HIGH_ERROR_RATE",
                AlertSeverity.WARNING,
                "Error rate exceeded threshold: " + avgErrorRate + "% (threshold: "
                    + ERROR_RATE_THRESHOLD + "%)",
                MetricsService.METRIC_ERROR_RATE,
                ERROR_RATE_THRESHOLD,
                avgErrorRate
            );
        }
    }

    private void checkJobFailures() {
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);

        List<ScheduledJob> enabledJobs = scheduledJobRepository.findByEnabledTrue();

        for (ScheduledJob job : enabledJobs) {
            long failureCount = jobRunService.countFailuresSince(job.getId(), oneHourAgo);

            if (failureCount >= JOB_FAILURE_THRESHOLD) {
                anomalyAlertService.createAlert(
                    "JOB_REPEATED_FAILURE",
                    AlertSeverity.CRITICAL,
                    "Job '" + job.getJobName() + "' failed " + failureCount
                        + " times in the last hour",
                    "job.failure." + job.getJobName(),
                    BigDecimal.valueOf(JOB_FAILURE_THRESHOLD),
                    BigDecimal.valueOf(failureCount)
                );
            }
        }
    }
}
