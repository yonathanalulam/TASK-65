package com.culinarycoach.service;

import com.culinarycoach.audit.TraceContext;
import com.culinarycoach.domain.entity.*;
import com.culinarycoach.domain.enums.CookingSessionStatus;
import com.culinarycoach.domain.enums.NotebookEntryStatus;
import com.culinarycoach.domain.repository.*;
import com.culinarycoach.web.dto.response.AttemptHistoryResponse;
import com.culinarycoach.web.dto.response.CookingSessionResponse;
import com.culinarycoach.web.dto.response.NotebookEntryResponse;
import com.culinarycoach.web.dto.response.ReviewStudentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
public class ParentCoachService {

    private static final Logger log = LoggerFactory.getLogger(ParentCoachService.class);

    private final ParentCoachAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final WrongNotebookEntryRepository notebookEntryRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAttemptRepository questionAttemptRepository;
    private final CookingSessionRepository cookingSessionRepository;
    private final PrivacyAccessLogRepository privacyAccessLogRepository;
    private final WrongNotebookEntryTagRepository entryTagRepository;
    private final WrongNotebookTagRepository tagRepository;

    public ParentCoachService(ParentCoachAssignmentRepository assignmentRepository,
                               UserRepository userRepository,
                               WrongNotebookEntryRepository notebookEntryRepository,
                               QuestionRepository questionRepository,
                               QuestionAttemptRepository questionAttemptRepository,
                               CookingSessionRepository cookingSessionRepository,
                               PrivacyAccessLogRepository privacyAccessLogRepository,
                               WrongNotebookEntryTagRepository entryTagRepository,
                               WrongNotebookTagRepository tagRepository) {
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.notebookEntryRepository = notebookEntryRepository;
        this.questionRepository = questionRepository;
        this.questionAttemptRepository = questionAttemptRepository;
        this.cookingSessionRepository = cookingSessionRepository;
        this.privacyAccessLogRepository = privacyAccessLogRepository;
        this.entryTagRepository = entryTagRepository;
        this.tagRepository = tagRepository;
    }

    // ---- Assignment management (admin) ----

    @Transactional
    public ParentCoachAssignment assignStudent(Long coachUserId, Long studentUserId, String adminUsername) {
        // Validate both users exist
        userRepository.findById(coachUserId)
            .orElseThrow(() -> new IllegalArgumentException("Coach user not found: " + coachUserId));
        userRepository.findById(studentUserId)
            .orElseThrow(() -> new IllegalArgumentException("Student user not found: " + studentUserId));

        // Check for existing assignment (active or revoked)
        var existing = assignmentRepository.findByCoachUserIdAndStudentUserId(coachUserId, studentUserId);
        if (existing.isPresent()) {
            ParentCoachAssignment assignment = existing.get();
            if (assignment.getRevokedAt() == null) {
                throw new IllegalStateException("Assignment already exists and is active");
            }
            // Reactivate revoked assignment
            assignment.setRevokedAt(null);
            assignment.setAssignedBy(adminUsername);
            log.info("Reactivated assignment: coach={}, student={}, by={}", coachUserId, studentUserId, adminUsername);
            return assignmentRepository.save(assignment);
        }

        ParentCoachAssignment assignment = new ParentCoachAssignment();
        assignment.setCoachUserId(coachUserId);
        assignment.setStudentUserId(studentUserId);
        assignment.setAssignedBy(adminUsername);
        log.info("Created assignment: coach={}, student={}, by={}", coachUserId, studentUserId, adminUsername);
        return assignmentRepository.save(assignment);
    }

    @Transactional
    public void revokeAssignment(Long coachUserId, Long studentUserId, String adminUsername) {
        var assignment = assignmentRepository.findByCoachUserIdAndStudentUserId(coachUserId, studentUserId)
            .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        if (assignment.getRevokedAt() != null) {
            throw new IllegalStateException("Assignment is already revoked");
        }

        assignment.setRevokedAt(Instant.now());
        assignmentRepository.save(assignment);
        log.info("Revoked assignment: coach={}, student={}, by={}", coachUserId, studentUserId, adminUsername);
    }

    // ---- Query methods ----

    @Transactional(readOnly = true)
    public List<ReviewStudentResponse> listAssignedStudents(Long coachUserId) {
        List<ParentCoachAssignment> assignments = assignmentRepository.findByCoachUserIdAndRevokedAtIsNull(coachUserId);
        return assignments.stream()
            .map(a -> userRepository.findById(a.getStudentUserId()).orElse(null))
            .filter(u -> u != null)
            .map(ReviewStudentResponse::from)
            .toList();
    }

    public boolean isAssigned(Long coachUserId, Long studentUserId) {
        return assignmentRepository.existsByCoachUserIdAndStudentUserIdAndRevokedAtIsNull(coachUserId, studentUserId);
    }

    public void requireAssigned(Long coachUserId, Long studentUserId) {
        if (!isAssigned(coachUserId, studentUserId)) {
            throw new AccessDeniedException(
                "Coach " + coachUserId + " is not assigned to student " + studentUserId);
        }
    }

    // ---- Review methods ----

    @Transactional(readOnly = true)
    public List<NotebookEntryResponse> reviewStudentNotebook(Long coachUserId, String coachRole,
                                                              Long studentUserId, String reasonCode) {
        validateReviewAccess(coachUserId, coachRole, studentUserId);
        logPrivacyAccess(coachUserId, coachRole, studentUserId, "NOTEBOOK", null, reasonCode);

        var statuses = Set.of(NotebookEntryStatus.ACTIVE, NotebookEntryStatus.FAVORITED,
                              NotebookEntryStatus.RESOLVED);
        var page = notebookEntryRepository.findByUserIdAndStatusIn(
            studentUserId, statuses,
            org.springframework.data.domain.Pageable.unpaged());

        return page.getContent().stream()
            .map(this::toNotebookEntryResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<AttemptHistoryResponse> reviewStudentAttemptHistory(Long coachUserId, String coachRole,
                                                                     Long studentUserId, String reasonCode) {
        validateReviewAccess(coachUserId, coachRole, studentUserId);
        logPrivacyAccess(coachUserId, coachRole, studentUserId, "ATTEMPT_HISTORY", null, reasonCode);

        List<QuestionAttempt> attempts = questionAttemptRepository.findByUserId(studentUserId);
        return attempts.stream()
            .map(a -> {
                String questionText = questionRepository.findById(a.getQuestionId())
                    .map(Question::getQuestionText)
                    .orElse(null);
                return new AttemptHistoryResponse(
                    a.getId(),
                    questionText,
                    a.getUserAnswer(),
                    a.getClassification().name(),
                    a.getAttemptedAt()
                );
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public List<CookingSessionResponse> reviewStudentCookingHistory(Long coachUserId, String coachRole,
                                                                      Long studentUserId, String reasonCode) {
        validateReviewAccess(coachUserId, coachRole, studentUserId);
        logPrivacyAccess(coachUserId, coachRole, studentUserId, "COOKING_HISTORY", null, reasonCode);

        var statuses = Set.of(CookingSessionStatus.CREATED, CookingSessionStatus.ACTIVE,
                              CookingSessionStatus.PAUSED, CookingSessionStatus.COMPLETED,
                              CookingSessionStatus.ABANDONED, CookingSessionStatus.EXPIRED);
        List<CookingSession> sessions = cookingSessionRepository.findByUserIdAndStatusIn(studentUserId, statuses);
        return sessions.stream()
            .map(s -> new CookingSessionResponse(
                s.getId(),
                s.getRecipeTitle(),
                s.getLessonId(),
                s.getStatus().name(),
                s.getTotalSteps(),
                s.getLastCompletedStepOrder(),
                s.getStartedAt(),
                s.getCompletedAt(),
                s.getLastActivityAt()
            ))
            .toList();
    }

    // ---- Helpers ----

    private void validateReviewAccess(Long coachUserId, String coachRole, Long studentUserId) {
        // Admins can review any student without assignment
        if ("ROLE_ADMIN".equals(coachRole)) {
            return;
        }
        requireAssigned(coachUserId, studentUserId);
    }

    private NotebookEntryResponse toNotebookEntryResponse(WrongNotebookEntry entry) {
        List<String> tags = loadTagLabels(entry.getId());
        String questionText = questionRepository.findById(entry.getQuestionId())
            .map(Question::getQuestionText)
            .orElse(null);
        return new NotebookEntryResponse(
            entry.getId(),
            questionText,
            entry.getStatus().name(),
            entry.getFailCount(),
            entry.isFavorite(),
            tags.isEmpty() ? null : tags,
            entry.getLatestNote(),
            entry.getLastAttemptAt()
        );
    }

    private List<String> loadTagLabels(Long entryId) {
        var entryTags = entryTagRepository.findByEntryId(entryId);
        return entryTags.stream()
            .map(et -> tagRepository.findById(et.getTagId())
                .map(WrongNotebookTag::getLabel)
                .orElse(null))
            .filter(label -> label != null)
            .toList();
    }

    private void logPrivacyAccess(Long viewerUserId, String viewerRole, Long subjectUserId,
                                   String resourceType, String resourceId, String reasonCode) {
        try {
            PrivacyAccessLog accessLog = new PrivacyAccessLog();
            accessLog.setViewerUserId(viewerUserId);
            accessLog.setViewerRole(viewerRole);
            accessLog.setSubjectUserId(subjectUserId);
            accessLog.setResourceType(resourceType);
            accessLog.setResourceId(resourceId);
            accessLog.setReasonCode(reasonCode);
            accessLog.setTraceId(TraceContext.get());
            privacyAccessLogRepository.save(accessLog);
        } catch (Exception e) {
            log.error("Failed to log privacy access: viewer={}, subject={}, resource={}",
                viewerUserId, subjectUserId, resourceType, e);
        }
    }
}
