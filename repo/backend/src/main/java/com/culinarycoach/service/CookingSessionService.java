package com.culinarycoach.service;

import com.culinarycoach.domain.entity.CookingSession;
import com.culinarycoach.domain.entity.CookingSessionStep;
import com.culinarycoach.domain.entity.CookingSessionTimer;
import com.culinarycoach.domain.entity.StepCompletionEvent;
import com.culinarycoach.domain.enums.CookingSessionStatus;
import com.culinarycoach.domain.enums.TimerStatus;
import com.culinarycoach.domain.repository.CookingSessionRepository;
import com.culinarycoach.domain.repository.CookingSessionStepRepository;
import com.culinarycoach.domain.repository.CookingSessionTimerRepository;
import com.culinarycoach.domain.repository.StepCompletionEventRepository;
import com.culinarycoach.web.dto.request.StartSessionRequest;
import com.culinarycoach.web.dto.response.CookingSessionDetailResponse;
import com.culinarycoach.web.dto.response.CookingSessionResponse;
import com.culinarycoach.web.dto.response.SessionStepResponse;
import com.culinarycoach.web.dto.response.TimerResponse;
import com.culinarycoach.web.dto.response.TipCardResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class CookingSessionService {

    private static final Logger log = LoggerFactory.getLogger(CookingSessionService.class);

    private static final int MAX_ACTIVE_SESSIONS_PER_USER = 3;
    private static final Set<CookingSessionStatus> ACTIVE_STATUSES = Set.of(
        CookingSessionStatus.CREATED, CookingSessionStatus.ACTIVE, CookingSessionStatus.PAUSED
    );

    private final CookingSessionRepository sessionRepository;
    private final CookingSessionStepRepository stepRepository;
    private final CookingSessionTimerRepository timerRepository;
    private final StepCompletionEventRepository completionEventRepository;
    private final TipCardService tipCardService;

    public CookingSessionService(CookingSessionRepository sessionRepository,
                                  CookingSessionStepRepository stepRepository,
                                  CookingSessionTimerRepository timerRepository,
                                  StepCompletionEventRepository completionEventRepository,
                                  TipCardService tipCardService) {
        this.sessionRepository = sessionRepository;
        this.stepRepository = stepRepository;
        this.timerRepository = timerRepository;
        this.completionEventRepository = completionEventRepository;
        this.tipCardService = tipCardService;
    }

    @Transactional
    public CookingSessionResponse startSession(Long userId, StartSessionRequest request) {
        // Enforce max 1 active session per recipe/lesson
        if (request.lessonId() != null) {
            List<CookingSession> existingForLesson = sessionRepository
                .findByUserIdAndLessonIdAndStatusIn(userId, request.lessonId(), ACTIVE_STATUSES);
            if (!existingForLesson.isEmpty()) {
                throw new IllegalStateException(
                    "An active session already exists for this recipe/lesson");
            }
        }

        // Enforce max 3 active sessions total
        long activeCount = sessionRepository.countByUserIdAndStatusIn(userId, ACTIVE_STATUSES);
        if (activeCount >= MAX_ACTIVE_SESSIONS_PER_USER) {
            throw new IllegalStateException(
                "Maximum of " + MAX_ACTIVE_SESSIONS_PER_USER + " active sessions allowed");
        }

        Instant now = Instant.now();

        CookingSession session = new CookingSession();
        session.setUserId(userId);
        session.setRecipeTitle(request.recipeTitle());
        session.setLessonId(request.lessonId());
        session.setStatus(CookingSessionStatus.ACTIVE);
        session.setTotalSteps(request.steps().size());
        session.setLastCompletedStepOrder(-1);
        session.setStartedAt(now);
        session.setLastActivityAt(now);
        session = sessionRepository.save(session);

        // Snapshot steps (immutable once created)
        for (int i = 0; i < request.steps().size(); i++) {
            StartSessionRequest.StepInput stepInput = request.steps().get(i);
            CookingSessionStep step = new CookingSessionStep();
            step.setSessionId(session.getId());
            step.setStepOrder(i);
            step.setTitle(stepInput.title());
            step.setDescription(stepInput.description());
            step.setExpectedDurationSeconds(stepInput.expectedDurationSeconds());
            step.setHasTimer(stepInput.hasTimer());
            step.setTimerDurationSeconds(stepInput.timerDurationSeconds());
            step.setReminderText(stepInput.reminderText());
            stepRepository.save(step);
        }

        log.info("Started cooking session {} for user {}, recipe '{}'",
            session.getId(), userId, request.recipeTitle());

        return toResponse(session);
    }

    @Transactional(readOnly = true)
    public List<CookingSessionResponse> listSessions(Long userId, List<CookingSessionStatus> statuses) {
        List<CookingSession> sessions;
        if (statuses != null && !statuses.isEmpty()) {
            sessions = sessionRepository.findByUserIdAndStatusIn(userId, statuses);
        } else {
            sessions = sessionRepository.findByUserIdAndStatusIn(userId,
                List.of(CookingSessionStatus.values()));
        }
        return sessions.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CookingSessionDetailResponse getSession(Long sessionId, Long userId) {
        CookingSession session = loadSessionForUser(sessionId, userId);

        List<CookingSessionStep> steps = stepRepository.findBySessionIdOrderByStepOrder(sessionId);
        List<CookingSessionTimer> timers = timerRepository.findBySessionId(sessionId);

        // Build step responses with tips
        List<SessionStepResponse> stepResponses = new ArrayList<>();
        for (CookingSessionStep step : steps) {
            List<TipCardResponse> tips = tipCardService.getEffectiveTipsForStep(
                step.getId(), session.getLessonId());
            stepResponses.add(toStepResponse(step, tips));
        }

        // Build timer responses with reconstructed state
        List<TimerResponse> timerResponses = timers.stream()
            .map(this::toTimerResponse)
            .toList();

        return new CookingSessionDetailResponse(
            session.getId(),
            session.getRecipeTitle(),
            session.getLessonId(),
            session.getStatus().name(),
            session.getTotalSteps(),
            session.getLastCompletedStepOrder(),
            session.getStartedAt(),
            session.getResumedAt(),
            session.getCompletedAt(),
            session.getAbandonedAt(),
            session.getLastActivityAt(),
            stepResponses,
            timerResponses
        );
    }

    @Transactional
    public CookingSessionDetailResponse completeStep(Long sessionId, int stepOrder, Long userId) {
        CookingSession session = loadSessionForUser(sessionId, userId);

        if (session.getStatus() != CookingSessionStatus.ACTIVE) {
            throw new IllegalStateException("Session is not active");
        }

        // Enforce ordered completion (no skipping)
        int expectedNextStep = session.getLastCompletedStepOrder() + 1;
        if (stepOrder != expectedNextStep) {
            throw new IllegalArgumentException(
                "Steps must be completed in order. Expected step " + expectedNextStep
                + " but received step " + stepOrder);
        }

        CookingSessionStep step = stepRepository.findBySessionIdAndStepOrder(sessionId, stepOrder)
            .orElseThrow(() -> new IllegalArgumentException("Step not found: " + stepOrder));

        if (step.isCompleted()) {
            throw new IllegalStateException("Step " + stepOrder + " is already completed");
        }

        Instant now = Instant.now();

        // Mark step completed
        step.setCompleted(true);
        step.setCompletedAt(now);
        stepRepository.save(step);

        // Create immutable completion event
        StepCompletionEvent event = new StepCompletionEvent();
        event.setSessionId(sessionId);
        event.setStepId(step.getId());
        event.setUserId(userId);
        event.setEventType("COMPLETED");
        completionEventRepository.save(event);

        // Update session
        session.setLastCompletedStepOrder(stepOrder);
        session.setLastActivityAt(now);

        // Check auto-complete: last step done AND all timers ack'd/dismissed/cancelled
        if (stepOrder == session.getTotalSteps() - 1) {
            if (allTimersResolved(sessionId)) {
                session.setStatus(CookingSessionStatus.COMPLETED);
                session.setCompletedAt(now);
                log.info("Session {} auto-completed", sessionId);
            }
        }

        sessionRepository.save(session);

        return getSession(sessionId, userId);
    }

    @Transactional
    public CookingSessionDetailResponse resumeSession(Long sessionId, Long userId) {
        CookingSession session = loadSessionForUser(sessionId, userId);

        if (session.getStatus() != CookingSessionStatus.PAUSED
            && session.getStatus() != CookingSessionStatus.ABANDONED) {
            throw new IllegalStateException(
                "Only PAUSED or ABANDONED sessions can be resumed. Current status: "
                + session.getStatus());
        }

        if (session.getStatus() == CookingSessionStatus.ABANDONED) {
            // Re-check active session limit when resuming an abandoned session
            long activeCount = sessionRepository.countByUserIdAndStatusIn(userId, ACTIVE_STATUSES);
            if (activeCount >= MAX_ACTIVE_SESSIONS_PER_USER) {
                throw new IllegalStateException(
                    "Maximum of " + MAX_ACTIVE_SESSIONS_PER_USER
                    + " active sessions allowed. Abandon or complete another session first.");
            }
        }

        Instant now = Instant.now();

        // Reconstruct timer states from wall-clock timestamps
        List<CookingSessionTimer> timers = timerRepository.findBySessionId(sessionId);
        for (CookingSessionTimer timer : timers) {
            reconstructTimerState(timer, now);
            timerRepository.save(timer);
        }

        session.setStatus(CookingSessionStatus.ACTIVE);
        session.setResumedAt(now);
        session.setLastActivityAt(now);
        session.setAbandonedAt(null);
        sessionRepository.save(session);

        log.info("Session {} resumed by user {}", sessionId, userId);

        return getSession(sessionId, userId);
    }

    @Transactional
    public CookingSessionResponse pauseSession(Long sessionId, Long userId) {
        CookingSession session = loadSessionForUser(sessionId, userId);

        if (session.getStatus() != CookingSessionStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE sessions can be paused");
        }

        Instant now = Instant.now();

        session.setStatus(CookingSessionStatus.PAUSED);
        session.setLastActivityAt(now);
        sessionRepository.save(session);

        log.info("Session {} paused by user {}", sessionId, userId);

        return toResponse(session);
    }

    @Transactional
    public CookingSessionResponse abandonSession(Long sessionId, Long userId) {
        CookingSession session = loadSessionForUser(sessionId, userId);

        if (session.getStatus() != CookingSessionStatus.ACTIVE
            && session.getStatus() != CookingSessionStatus.PAUSED) {
            throw new IllegalStateException("Only ACTIVE or PAUSED sessions can be abandoned");
        }

        Instant now = Instant.now();

        session.setStatus(CookingSessionStatus.ABANDONED);
        session.setAbandonedAt(now);
        session.setLastActivityAt(now);
        sessionRepository.save(session);

        log.info("Session {} abandoned by user {}", sessionId, userId);

        return toResponse(session);
    }

    /**
     * Called by TimerService after timer acknowledgement/dismissal/cancellation
     * to check if session should auto-complete.
     */
    @Transactional
    public void checkAutoComplete(Long sessionId) {
        CookingSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        if (session.getStatus() != CookingSessionStatus.ACTIVE) {
            return;
        }

        boolean lastStepDone = session.getLastCompletedStepOrder() == session.getTotalSteps() - 1;
        if (lastStepDone && allTimersResolved(sessionId)) {
            Instant now = Instant.now();
            session.setStatus(CookingSessionStatus.COMPLETED);
            session.setCompletedAt(now);
            session.setLastActivityAt(now);
            sessionRepository.save(session);
            log.info("Session {} auto-completed after all timers resolved", sessionId);
        }
    }

    /**
     * Reconstruct a timer's state based on wall-clock timestamps.
     * For RUNNING timers past their target_end_at, transition to ELAPSED_PENDING_ACK.
     * PAUSED timers remain as-is.
     */
    private void reconstructTimerState(CookingSessionTimer timer, Instant now) {
        if (timer.getStatus() == TimerStatus.RUNNING) {
            if (now.isAfter(timer.getTargetEndAt()) || now.equals(timer.getTargetEndAt())) {
                timer.setStatus(TimerStatus.ELAPSED_PENDING_ACK);
                log.debug("Timer {} transitioned to ELAPSED_PENDING_ACK during reconstruction",
                    timer.getId());
            }
        }
        // PAUSED timers keep their state as-is
    }

    private boolean allTimersResolved(Long sessionId) {
        long unresolvedCount = timerRepository.countBySessionIdAndStatusIn(
            sessionId, Set.of(TimerStatus.RUNNING, TimerStatus.PAUSED, TimerStatus.ELAPSED_PENDING_ACK));
        return unresolvedCount == 0;
    }

    private CookingSession loadSessionForUser(Long sessionId, Long userId) {
        CookingSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        if (!session.getUserId().equals(userId)) {
            throw new SecurityException("Access denied to session " + sessionId);
        }
        return session;
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

    /**
     * Compute remaining seconds based on wall-clock timestamps (NOT in-memory countdowns).
     */
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
                // ACKNOWLEDGED, DISMISSED, CANCELLED
                return null;
        }
    }

    private CookingSessionResponse toResponse(CookingSession session) {
        return new CookingSessionResponse(
            session.getId(),
            session.getRecipeTitle(),
            session.getLessonId(),
            session.getStatus().name(),
            session.getTotalSteps(),
            session.getLastCompletedStepOrder(),
            session.getStartedAt(),
            session.getCompletedAt(),
            session.getLastActivityAt()
        );
    }

    private SessionStepResponse toStepResponse(CookingSessionStep step, List<TipCardResponse> tips) {
        return new SessionStepResponse(
            step.getId(),
            step.getStepOrder(),
            step.getTitle(),
            step.getDescription(),
            step.getExpectedDurationSeconds(),
            step.isHasTimer(),
            step.getTimerDurationSeconds(),
            step.getReminderText(),
            step.isCompleted(),
            step.getCompletedAt(),
            tips.isEmpty() ? null : tips
        );
    }
}
