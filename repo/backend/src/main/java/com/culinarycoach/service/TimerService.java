package com.culinarycoach.service;

import com.culinarycoach.domain.entity.CookingSession;
import com.culinarycoach.domain.entity.CookingSessionTimer;
import com.culinarycoach.domain.enums.CookingSessionStatus;
import com.culinarycoach.domain.enums.TimerStatus;
import com.culinarycoach.domain.repository.CookingSessionRepository;
import com.culinarycoach.domain.repository.CookingSessionTimerRepository;
import com.culinarycoach.web.dto.response.TimerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Service
public class TimerService {

    private static final Logger log = LoggerFactory.getLogger(TimerService.class);

    private static final int MAX_ACTIVE_TIMERS_PER_SESSION = 6;
    private static final int MAX_TIMER_DURATION_SECONDS = 86400;
    private static final Set<TimerStatus> ACTIVE_TIMER_STATUSES = Set.of(
        TimerStatus.RUNNING, TimerStatus.PAUSED
    );

    private final CookingSessionTimerRepository timerRepository;
    private final CookingSessionRepository sessionRepository;
    private final CookingSessionService cookingSessionService;

    public TimerService(CookingSessionTimerRepository timerRepository,
                        CookingSessionRepository sessionRepository,
                        CookingSessionService cookingSessionService) {
        this.timerRepository = timerRepository;
        this.sessionRepository = sessionRepository;
        this.cookingSessionService = cookingSessionService;
    }

    @Transactional
    public TimerResponse createTimer(Long sessionId, Long userId, Long stepId,
                                      String label, int durationSeconds) {
        CookingSession session = loadSessionForUser(sessionId, userId);

        if (session.getStatus() != CookingSessionStatus.ACTIVE) {
            throw new IllegalStateException("Timers can only be created in ACTIVE sessions");
        }

        if (durationSeconds <= 0 || durationSeconds > MAX_TIMER_DURATION_SECONDS) {
            throw new IllegalArgumentException(
                "Timer duration must be between 1 and " + MAX_TIMER_DURATION_SECONDS + " seconds");
        }

        // Enforce max 6 active timers per session
        long activeCount = timerRepository.countBySessionIdAndStatusIn(sessionId, ACTIVE_TIMER_STATUSES);
        if (activeCount >= MAX_ACTIVE_TIMERS_PER_SESSION) {
            throw new IllegalStateException(
                "Maximum of " + MAX_ACTIVE_TIMERS_PER_SESSION + " active timers per session");
        }

        Instant now = Instant.now();

        CookingSessionTimer timer = new CookingSessionTimer();
        timer.setSessionId(sessionId);
        timer.setStepId(stepId);
        timer.setLabel(label);
        timer.setTimerType("STEP");
        timer.setStatus(TimerStatus.RUNNING);
        timer.setDurationSeconds(durationSeconds);
        timer.setStartedAt(now);
        timer.setTargetEndAt(now.plusSeconds(durationSeconds));
        timer = timerRepository.save(timer);

        // Update session activity
        session.setLastActivityAt(now);
        sessionRepository.save(session);

        log.info("Created timer {} for session {}, duration {}s", timer.getId(), sessionId, durationSeconds);

        return toTimerResponse(timer);
    }

    @Transactional
    public TimerResponse pauseTimer(Long sessionId, Long timerId, Long userId) {
        CookingSession session = loadSessionForUser(sessionId, userId);
        CookingSessionTimer timer = loadTimerForSession(timerId, sessionId);

        if (timer.getStatus() != TimerStatus.RUNNING) {
            throw new IllegalStateException(
                "Only RUNNING timers can be paused. Current status: " + timer.getStatus());
        }

        Instant now = Instant.now();

        // Compute elapsed time before pause
        long elapsedSinceStart = Duration.between(timer.getStartedAt(), now).getSeconds();
        int totalElapsed = timer.getElapsedBeforePauseSeconds() + (int) elapsedSinceStart;

        timer.setStatus(TimerStatus.PAUSED);
        timer.setPausedAt(now);
        timer.setElapsedBeforePauseSeconds(totalElapsed);
        timerRepository.save(timer);

        session.setLastActivityAt(now);
        sessionRepository.save(session);

        log.info("Paused timer {} (elapsed {}s)", timerId, totalElapsed);

        return toTimerResponse(timer);
    }

    @Transactional
    public TimerResponse resumeTimer(Long sessionId, Long timerId, Long userId) {
        CookingSession session = loadSessionForUser(sessionId, userId);
        CookingSessionTimer timer = loadTimerForSession(timerId, sessionId);

        if (timer.getStatus() != TimerStatus.PAUSED) {
            throw new IllegalStateException(
                "Only PAUSED timers can be resumed. Current status: " + timer.getStatus());
        }

        Instant now = Instant.now();

        // Compute remaining time and set new target
        int remainingSeconds = timer.getDurationSeconds() - timer.getElapsedBeforePauseSeconds();
        if (remainingSeconds <= 0) {
            timer.setStatus(TimerStatus.ELAPSED_PENDING_ACK);
            timer.setTargetEndAt(now);
        } else {
            timer.setStatus(TimerStatus.RUNNING);
            timer.setStartedAt(now);
            timer.setTargetEndAt(now.plusSeconds(remainingSeconds));
        }
        timer.setPausedAt(null);
        timerRepository.save(timer);

        session.setLastActivityAt(now);
        sessionRepository.save(session);

        log.info("Resumed timer {} with {}s remaining", timerId, remainingSeconds);

        return toTimerResponse(timer);
    }

    @Transactional
    public TimerResponse cancelTimer(Long sessionId, Long timerId, Long userId) {
        CookingSession session = loadSessionForUser(sessionId, userId);
        CookingSessionTimer timer = loadTimerForSession(timerId, sessionId);

        if (timer.getStatus() != TimerStatus.RUNNING && timer.getStatus() != TimerStatus.PAUSED) {
            throw new IllegalStateException(
                "Only RUNNING or PAUSED timers can be cancelled. Current status: " + timer.getStatus());
        }

        Instant now = Instant.now();

        timer.setStatus(TimerStatus.CANCELLED);
        timerRepository.save(timer);

        session.setLastActivityAt(now);
        sessionRepository.save(session);

        log.info("Cancelled timer {}", timerId);

        // Check if session should auto-complete
        cookingSessionService.checkAutoComplete(sessionId);

        return toTimerResponse(timer);
    }

    @Transactional
    public TimerResponse acknowledgeTimer(Long sessionId, Long timerId, Long userId) {
        CookingSession session = loadSessionForUser(sessionId, userId);
        CookingSessionTimer timer = loadTimerForSession(timerId, sessionId);

        if (timer.getStatus() != TimerStatus.ELAPSED_PENDING_ACK) {
            throw new IllegalStateException(
                "Only ELAPSED_PENDING_ACK timers can be acknowledged. Current status: "
                + timer.getStatus());
        }

        Instant now = Instant.now();

        timer.setStatus(TimerStatus.ACKNOWLEDGED);
        timer.setAcknowledgedAt(now);
        timerRepository.save(timer);

        session.setLastActivityAt(now);
        sessionRepository.save(session);

        log.info("Acknowledged timer {}", timerId);

        // Check if session should auto-complete
        cookingSessionService.checkAutoComplete(sessionId);

        return toTimerResponse(timer);
    }

    @Transactional
    public TimerResponse dismissTimer(Long sessionId, Long timerId, Long userId) {
        CookingSession session = loadSessionForUser(sessionId, userId);
        CookingSessionTimer timer = loadTimerForSession(timerId, sessionId);

        if (timer.getStatus() != TimerStatus.ELAPSED_PENDING_ACK) {
            throw new IllegalStateException(
                "Only ELAPSED_PENDING_ACK timers can be dismissed. Current status: "
                + timer.getStatus());
        }

        Instant now = Instant.now();

        timer.setStatus(TimerStatus.DISMISSED);
        timerRepository.save(timer);

        session.setLastActivityAt(now);
        sessionRepository.save(session);

        log.info("Dismissed timer {}", timerId);

        // Check if session should auto-complete
        cookingSessionService.checkAutoComplete(sessionId);

        return toTimerResponse(timer);
    }

    private CookingSession loadSessionForUser(Long sessionId, Long userId) {
        CookingSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        if (!session.getUserId().equals(userId)) {
            throw new SecurityException("Access denied to session " + sessionId);
        }
        return session;
    }

    private CookingSessionTimer loadTimerForSession(Long timerId, Long sessionId) {
        CookingSessionTimer timer = timerRepository.findById(timerId)
            .orElseThrow(() -> new IllegalArgumentException("Timer not found: " + timerId));
        if (!timer.getSessionId().equals(sessionId)) {
            throw new IllegalArgumentException(
                "Timer " + timerId + " does not belong to session " + sessionId);
        }
        return timer;
    }

    private TimerResponse toTimerResponse(CookingSessionTimer timer) {
        Long remainingSeconds = computeRemainingSeconds(timer);
        return new TimerResponse(
            timer.getId(),
            timer.getStepId(),
            timer.getLabel(),
            timer.getTimerType(),
            timer.getStatus().name(),
            timer.getDurationSeconds(),
            remainingSeconds,
            timer.getStartedAt(),
            timer.getTargetEndAt(),
            timer.getPausedAt(),
            timer.getAcknowledgedAt()
        );
    }

    private Long computeRemainingSeconds(CookingSessionTimer timer) {
        switch (timer.getStatus()) {
            case RUNNING:
                long remaining = Duration.between(Instant.now(), timer.getTargetEndAt()).getSeconds();
                return Math.max(0, remaining);
            case PAUSED:
                long totalElapsed = timer.getElapsedBeforePauseSeconds();
                return Math.max(0, (long) timer.getDurationSeconds() - totalElapsed);
            case ELAPSED_PENDING_ACK:
                return 0L;
            default:
                return null;
        }
    }
}
