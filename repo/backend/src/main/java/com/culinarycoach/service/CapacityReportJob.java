package com.culinarycoach.service;

import com.culinarycoach.audit.TraceContext;
import com.culinarycoach.domain.enums.NotificationStatus;
import com.culinarycoach.domain.enums.SessionStatus;
import com.culinarycoach.domain.repository.AuthSessionRepository;
import com.culinarycoach.domain.repository.AudioCacheManifestRepository;
import com.culinarycoach.domain.repository.MockTransactionRepository;
import com.culinarycoach.domain.repository.NotificationRepository;
import com.culinarycoach.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Component
public class CapacityReportJob {

    private static final Logger log = LoggerFactory.getLogger(CapacityReportJob.class);

    private static final String DIMENSION_KEY = "capacity_report";

    private final UserRepository userRepository;
    private final AuthSessionRepository authSessionRepository;
    private final AudioCacheManifestRepository audioCacheManifestRepository;
    private final MockTransactionRepository mockTransactionRepository;
    private final NotificationRepository notificationRepository;
    private final MetricsService metricsService;

    public CapacityReportJob(UserRepository userRepository,
                              AuthSessionRepository authSessionRepository,
                              AudioCacheManifestRepository audioCacheManifestRepository,
                              MockTransactionRepository mockTransactionRepository,
                              NotificationRepository notificationRepository,
                              MetricsService metricsService) {
        this.userRepository = userRepository;
        this.authSessionRepository = authSessionRepository;
        this.audioCacheManifestRepository = audioCacheManifestRepository;
        this.mockTransactionRepository = mockTransactionRepository;
        this.notificationRepository = notificationRepository;
        this.metricsService = metricsService;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void generateCapacityReport() {
        TraceContext.init();
        log.info("Running capacity report job");

        try {
            Instant now = Instant.now();
            Instant windowStart = now.minusSeconds(86400); // 24-hour window

            // Total users
            long totalUsers = userRepository.count();
            metricsService.recordMetric("total_users", BigDecimal.valueOf(totalUsers),
                DIMENSION_KEY, "total_users", windowStart, now);

            // Active sessions (ISSUED or ACTIVE)
            List<SessionStatus> activeStatuses = List.of(SessionStatus.ISSUED, SessionStatus.ACTIVE);
            long activeSessions = authSessionRepository.findAll().stream()
                .filter(s -> activeStatuses.contains(s.getStatus()))
                .count();
            metricsService.recordMetric("active_sessions", BigDecimal.valueOf(activeSessions),
                DIMENSION_KEY, "active_sessions", windowStart, now);

            // Audio cache usage (sum all cached file sizes)
            long totalCacheBytes = audioCacheManifestRepository.findAll().stream()
                .filter(m -> "CACHED_VALID".equals(m.getStatus().name()))
                .mapToLong(m -> m.getFileSizeBytes())
                .sum();
            metricsService.recordMetric("total_audio_cache_bytes",
                BigDecimal.valueOf(totalCacheBytes),
                DIMENSION_KEY, "total_audio_cache_bytes", windowStart, now);

            // Total transactions
            long totalTransactions = mockTransactionRepository.count();
            metricsService.recordMetric("total_transactions",
                BigDecimal.valueOf(totalTransactions),
                DIMENSION_KEY, "total_transactions", windowStart, now);

            // Pending notifications (GENERATED or DELIVERED)
            Set<NotificationStatus> pendingStatuses = Set.of(
                NotificationStatus.GENERATED, NotificationStatus.DELIVERED);
            long pendingNotifications = notificationRepository.findAll().stream()
                .filter(n -> pendingStatuses.contains(n.getStatus()))
                .count();
            metricsService.recordMetric("pending_notifications",
                BigDecimal.valueOf(pendingNotifications),
                DIMENSION_KEY, "pending_notifications", windowStart, now);

            log.info("Capacity report generated: users={}, sessions={}, cache={}bytes, " +
                    "transactions={}, pendingNotifications={}",
                totalUsers, activeSessions, totalCacheBytes,
                totalTransactions, pendingNotifications);

        } catch (Exception e) {
            log.error("Capacity report job failed: {}", e.getMessage(), e);
        } finally {
            TraceContext.clear();
        }
    }
}
