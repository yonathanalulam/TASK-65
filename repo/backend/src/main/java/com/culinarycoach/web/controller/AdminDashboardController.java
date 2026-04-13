package com.culinarycoach.web.controller;

import com.culinarycoach.domain.entity.JobRun;
import com.culinarycoach.domain.entity.MetricSnapshot;
import com.culinarycoach.domain.entity.ScheduledJob;
import com.culinarycoach.domain.enums.AlertStatus;
import com.culinarycoach.domain.repository.MetricSnapshotRepository;
import com.culinarycoach.domain.repository.ScheduledJobRepository;
import com.culinarycoach.security.auth.AuthenticatedUserResolver;
import com.culinarycoach.service.AnomalyAlertService;
import com.culinarycoach.service.JobRunService;
import com.culinarycoach.service.MetricsService;
import com.culinarycoach.web.dto.response.AnomalyAlertResponse;
import com.culinarycoach.web.dto.response.ApiResponse;
import com.culinarycoach.web.dto.response.CapacityReportResponse;
import com.culinarycoach.web.dto.response.JobRunResponse;
import com.culinarycoach.web.dto.response.KpiSummaryResponse;
import com.culinarycoach.web.dto.response.MetricSnapshotResponse;
import com.culinarycoach.web.dto.response.ScheduledJobResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminDashboardController {

    private final ScheduledJobRepository scheduledJobRepository;
    private final JobRunService jobRunService;
    private final MetricsService metricsService;
    private final MetricSnapshotRepository metricSnapshotRepository;
    private final AnomalyAlertService anomalyAlertService;
    private final AuthenticatedUserResolver userResolver;

    public AdminDashboardController(ScheduledJobRepository scheduledJobRepository,
                                     JobRunService jobRunService,
                                     MetricsService metricsService,
                                     MetricSnapshotRepository metricSnapshotRepository,
                                     AnomalyAlertService anomalyAlertService,
                                     AuthenticatedUserResolver userResolver) {
        this.scheduledJobRepository = scheduledJobRepository;
        this.jobRunService = jobRunService;
        this.metricsService = metricsService;
        this.metricSnapshotRepository = metricSnapshotRepository;
        this.anomalyAlertService = anomalyAlertService;
        this.userResolver = userResolver;
    }

    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<List<ScheduledJobResponse>>> listJobs() {
        List<ScheduledJob> jobs = scheduledJobRepository.findAll();

        List<ScheduledJobResponse> responses = jobs.stream()
            .map(job -> {
                Optional<JobRun> latestRun = jobRunService.getLatestRun(job.getJobName());
                String latestStatus = latestRun.map(r -> r.getStatus().name()).orElse(null);
                Instant latestRunAt = latestRun.map(JobRun::getStartedAt).orElse(null);
                return ScheduledJobResponse.from(job, latestStatus, latestRunAt);
            })
            .toList();

        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @GetMapping("/jobs/{jobName}/runs")
    public ResponseEntity<ApiResponse<Page<JobRunResponse>>> listJobRuns(
            @PathVariable String jobName,
            @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<JobRunResponse> runs = jobRunService.listRuns(jobName, pageable);
        return ResponseEntity.ok(ApiResponse.ok(runs));
    }

    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<List<MetricSnapshotResponse>>> getMetrics(
            @RequestParam String name,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        List<MetricSnapshotResponse> metrics = metricsService.getMetrics(name, from, to);
        return ResponseEntity.ok(ApiResponse.ok(metrics));
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<Page<AnomalyAlertResponse>>> listAlerts(
            @RequestParam(required = false) AlertStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<AnomalyAlertResponse> alerts = anomalyAlertService.listAlerts(status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(alerts));
    }

    @PostMapping("/alerts/{id}/acknowledge")
    public ResponseEntity<ApiResponse<AnomalyAlertResponse>> acknowledgeAlert(
            @PathVariable Long id, Authentication authentication) {
        String adminUsername = userResolver.require(authentication).getUsername();
        AnomalyAlertResponse alert = anomalyAlertService.acknowledgeAlert(
            id, adminUsername);
        return ResponseEntity.ok(ApiResponse.ok(alert));
    }

    @PostMapping("/alerts/{id}/resolve")
    public ResponseEntity<ApiResponse<AnomalyAlertResponse>> resolveAlert(
            @PathVariable Long id, Authentication authentication) {
        String adminUsername = userResolver.require(authentication).getUsername();
        AnomalyAlertResponse alert = anomalyAlertService.resolveAlert(
            id, adminUsername);
        return ResponseEntity.ok(ApiResponse.ok(alert));
    }

    @GetMapping("/alerts/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getOpenAlertCount() {
        long count = anomalyAlertService.getOpenAlertCount();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("openAlerts", count)));
    }

    @GetMapping("/capacity")
    public ResponseEntity<ApiResponse<CapacityReportResponse>> getCapacityReport() {
        Optional<MetricSnapshot> totalUsers = metricSnapshotRepository
            .findLatestByDimension("capacity_report", "total_users");
        Optional<MetricSnapshot> activeSessions = metricSnapshotRepository
            .findLatestByDimension("capacity_report", "active_sessions");
        Optional<MetricSnapshot> cacheBytes = metricSnapshotRepository
            .findLatestByDimension("capacity_report", "total_audio_cache_bytes");
        Optional<MetricSnapshot> transactions = metricSnapshotRepository
            .findLatestByDimension("capacity_report", "total_transactions");
        Optional<MetricSnapshot> pendingNotifications = metricSnapshotRepository
            .findLatestByDimension("capacity_report", "pending_notifications");

        Instant reportTime = totalUsers.map(MetricSnapshot::getWindowEnd).orElse(Instant.now());

        CapacityReportResponse report = new CapacityReportResponse(
            totalUsers.map(m -> m.getMetricValue().longValue()).orElse(0L),
            activeSessions.map(m -> m.getMetricValue().longValue()).orElse(0L),
            cacheBytes.map(MetricSnapshot::getMetricValue).orElse(BigDecimal.ZERO),
            transactions.map(m -> m.getMetricValue().longValue()).orElse(0L),
            pendingNotifications.map(m -> m.getMetricValue().longValue()).orElse(0L),
            reportTime
        );

        return ResponseEntity.ok(ApiResponse.ok(report));
    }

    @GetMapping("/kpis")
    public ResponseEntity<ApiResponse<KpiSummaryResponse>> getKpiSummary(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {

        if (from == null) {
            from = Instant.now().minus(1, ChronoUnit.HOURS);
        }
        if (to == null) {
            to = Instant.now();
        }

        KpiSummaryResponse kpis = metricsService.getP50P95Latency(from, to);
        return ResponseEntity.ok(ApiResponse.ok(kpis));
    }
}
