package com.culinarycoach.service;

import com.culinarycoach.domain.entity.CookingSession;
import com.culinarycoach.domain.enums.CookingSessionStatus;
import com.culinarycoach.domain.repository.CookingSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class SessionCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(SessionCleanupJob.class);

    private final CookingSessionRepository sessionRepository;

    public SessionCleanupJob(CookingSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * Runs every 15 minutes.
     * - ACTIVE/PAUSED sessions with no activity for 24h -> ABANDONED
     * - ABANDONED sessions with no activity for 7 days -> EXPIRED (not resumable)
     */
    @Scheduled(cron = "0 */15 * * * *")
    public void cleanupSessions() {
        log.info("Running session cleanup job");

        try {
            int abandonedCount = abandonInactiveSessions();
            int expiredCount = expireAbandonedSessions();

            if (abandonedCount > 0 || expiredCount > 0) {
                log.info("Session cleanup: {} sessions abandoned, {} sessions expired",
                    abandonedCount, expiredCount);
            }
        } catch (Exception e) {
            log.error("Session cleanup job failed: {}", e.getMessage(), e);
        }
    }

    /**
     * ACTIVE or PAUSED sessions with no activity for 24 hours -> ABANDONED.
     */
    private int abandonInactiveSessions() {
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        int count = 0;

        List<CookingSession> activeSessions = sessionRepository
            .findByStatusAndLastActivityAtBefore(CookingSessionStatus.ACTIVE, cutoff);
        List<CookingSession> pausedSessions = sessionRepository
            .findByStatusAndLastActivityAtBefore(CookingSessionStatus.PAUSED, cutoff);

        Instant now = Instant.now();

        for (CookingSession session : activeSessions) {
            session.setStatus(CookingSessionStatus.ABANDONED);
            session.setAbandonedAt(now);
            sessionRepository.save(session);
            count++;
            log.debug("Abandoned inactive ACTIVE session {}", session.getId());
        }

        for (CookingSession session : pausedSessions) {
            session.setStatus(CookingSessionStatus.ABANDONED);
            session.setAbandonedAt(now);
            sessionRepository.save(session);
            count++;
            log.debug("Abandoned inactive PAUSED session {}", session.getId());
        }

        return count;
    }

    /**
     * ABANDONED sessions with abandoned_at older than 7 days -> EXPIRED.
     */
    private int expireAbandonedSessions() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        int count = 0;

        List<CookingSession> abandonedSessions = sessionRepository
            .findByStatusAndAbandonedAtBefore(CookingSessionStatus.ABANDONED, cutoff);

        for (CookingSession session : abandonedSessions) {
            session.setStatus(CookingSessionStatus.EXPIRED);
            sessionRepository.save(session);
            count++;
            log.debug("Expired abandoned session {}", session.getId());
        }

        return count;
    }
}
