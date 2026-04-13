package com.culinarycoach.service;

import com.culinarycoach.audit.AuditEventType;
import com.culinarycoach.config.AppProperties;
import com.culinarycoach.domain.entity.AuthSession;
import com.culinarycoach.domain.enums.SessionStatus;
import com.culinarycoach.domain.repository.AuthSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);
    private static final List<SessionStatus> ACTIVE_STATUSES =
        List.of(SessionStatus.ISSUED, SessionStatus.ACTIVE);

    private final AuthSessionRepository authSessionRepository;
    private final AppProperties appProperties;
    private final AuditService auditService;

    public SessionService(AuthSessionRepository authSessionRepository,
                           AppProperties appProperties,
                           AuditService auditService) {
        this.authSessionRepository = authSessionRepository;
        this.appProperties = appProperties;
        this.auditService = auditService;
    }

    @Transactional
    public void enforceSessionLimits(Long userId, Long deviceId) {
        int maxPerDevice = appProperties.getSecurity().getMaxSessionsPerDevice();
        int maxPerUser = appProperties.getSecurity().getMaxSessionsPerUser();

        if (deviceId != null) {
            long deviceCount = authSessionRepository.countByUserIdAndDeviceIdAndStatusIn(
                userId, deviceId, ACTIVE_STATUSES);
            if (deviceCount >= maxPerDevice) {
                List<AuthSession> oldest = authSessionRepository.findOldestByUserAndDevice(
                    userId, deviceId, ACTIVE_STATUSES);
                for (int i = 0; i <= oldest.size() - maxPerDevice; i++) {
                    evictSession(oldest.get(i));
                }
            }
        }

        long userCount = authSessionRepository.countByUserIdAndStatusIn(userId, ACTIVE_STATUSES);
        if (userCount >= maxPerUser) {
            List<AuthSession> oldest = authSessionRepository.findOldestByUser(userId, ACTIVE_STATUSES);
            for (int i = 0; i <= oldest.size() - maxPerUser; i++) {
                evictSession(oldest.get(i));
            }
        }
    }

    @Transactional
    public void touchSession(String sessionId) {
        authSessionRepository.findById(sessionId).ifPresent(session -> {
            if (session.isActive()) {
                session.setLastAccessedAt(Instant.now());
                session.setStatus(SessionStatus.ACTIVE);
                authSessionRepository.save(session);
            }
        });
    }

    @Scheduled(fixedRate = 300000) // every 5 minutes
    @Transactional
    public void cleanupExpiredSessions() {
        int idleMinutes = appProperties.getSecurity().getIdleTimeoutMinutes();
        int absoluteHours = appProperties.getSecurity().getAbsoluteLifetimeHours();

        Instant idleThreshold = Instant.now().minus(idleMinutes, ChronoUnit.MINUTES);
        Instant absoluteThreshold = Instant.now().minus(absoluteHours, ChronoUnit.HOURS);

        List<AuthSession> expired = authSessionRepository.findExpiredSessions(
            ACTIVE_STATUSES, idleThreshold, absoluteThreshold);

        for (AuthSession session : expired) {
            if (session.getCreatedAt().isBefore(absoluteThreshold)) {
                session.setStatus(SessionStatus.ABSOLUTE_EXPIRED);
            } else {
                session.setStatus(SessionStatus.IDLE_EXPIRED);
            }
            authSessionRepository.save(session);
            auditService.log(AuditEventType.SESSION_EXPIRED, session.getUserId(), null,
                session.getIpAddress(), null, "Session " + session.getId() + " expired: " + session.getStatus());
        }

        if (!expired.isEmpty()) {
            log.info("Cleaned up {} expired sessions", expired.size());
        }
    }

    private void evictSession(AuthSession session) {
        session.setStatus(SessionStatus.REVOKED);
        authSessionRepository.save(session);
        auditService.log(AuditEventType.SESSION_EVICTED, session.getUserId(), null,
            session.getIpAddress(), null, "Session evicted due to limit: " + session.getId());
    }
}
