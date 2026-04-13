package com.culinarycoach.service;

import com.culinarycoach.domain.entity.*;
import com.culinarycoach.domain.enums.AttemptClassification;
import com.culinarycoach.domain.enums.DrillType;
import com.culinarycoach.domain.repository.*;
import com.culinarycoach.web.dto.response.DrillRunResponse;
import com.culinarycoach.web.dto.response.SubmitAnswerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class DrillService {

    private static final Logger log = LoggerFactory.getLogger(DrillService.class);

    private final DrillRunRepository drillRunRepository;
    private final WrongNotebookEntryRepository entryRepository;
    private final QuestionRepository questionRepository;
    private final QuestionVariantRepository variantRepository;
    private final QuestionSimilarityLinkRepository similarityLinkRepository;
    private final QuestionAttemptRepository attemptRepository;
    private final AttemptEvaluationRepository evaluationRepository;

    public DrillService(DrillRunRepository drillRunRepository,
                        WrongNotebookEntryRepository entryRepository,
                        QuestionRepository questionRepository,
                        QuestionVariantRepository variantRepository,
                        QuestionSimilarityLinkRepository similarityLinkRepository,
                        QuestionAttemptRepository attemptRepository,
                        AttemptEvaluationRepository evaluationRepository) {
        this.drillRunRepository = drillRunRepository;
        this.entryRepository = entryRepository;
        this.questionRepository = questionRepository;
        this.variantRepository = variantRepository;
        this.similarityLinkRepository = similarityLinkRepository;
        this.attemptRepository = attemptRepository;
        this.evaluationRepository = evaluationRepository;
    }

    @Transactional
    public DrillRunResponse launchRetryDrill(Long userId, Long entryId) {
        WrongNotebookEntry entry = loadEntryForUser(entryId, userId);

        DrillRun run = new DrillRun();
        run.setUserId(userId);
        run.setDrillType(DrillType.RETRY);
        run.setSourceEntryId(entryId);
        run.setSourceQuestionId(entry.getQuestionId());
        run.setTotalQuestions(1);
        run = drillRunRepository.save(run);

        log.info("Launched RETRY drill {} for user {} entry {}", run.getId(), userId, entryId);
        return toResponse(run);
    }

    @Transactional
    public DrillRunResponse launchSimilarDrill(Long userId, Long entryId) {
        WrongNotebookEntry entry = loadEntryForUser(entryId, userId);

        List<QuestionSimilarityLink> links = similarityLinkRepository
            .findSimilarQuestions(entry.getQuestionId());

        int questionCount = Math.max(1, links.size());

        DrillRun run = new DrillRun();
        run.setUserId(userId);
        run.setDrillType(DrillType.SIMILAR);
        run.setSourceEntryId(entryId);
        run.setSourceQuestionId(entry.getQuestionId());
        run.setTotalQuestions(questionCount);
        run = drillRunRepository.save(run);

        log.info("Launched SIMILAR drill {} for user {} entry {}, {} similar questions",
            run.getId(), userId, entryId, links.size());
        return toResponse(run);
    }

    @Transactional
    public DrillRunResponse launchVariantDrill(Long userId, Long entryId) {
        WrongNotebookEntry entry = loadEntryForUser(entryId, userId);

        List<QuestionVariant> variants = variantRepository
            .findByOriginalQuestionId(entry.getQuestionId());

        int questionCount = Math.max(1, variants.size());

        DrillRun run = new DrillRun();
        run.setUserId(userId);
        run.setDrillType(DrillType.VARIANT);
        run.setSourceEntryId(entryId);
        run.setSourceQuestionId(entry.getQuestionId());
        run.setTotalQuestions(questionCount);
        run = drillRunRepository.save(run);

        log.info("Launched VARIANT drill {} for user {} entry {}, {} variants",
            run.getId(), userId, entryId, variants.size());
        return toResponse(run);
    }

    @Transactional
    public SubmitAnswerResponse submitDrillAnswer(Long userId, Long drillRunId,
                                                    Long questionId, String answer) {
        DrillRun run = loadDrillRunForUser(drillRunId, userId);

        if ("COMPLETED".equals(run.getStatus())) {
            throw new IllegalStateException("Drill run is already completed");
        }

        // Load question for evaluation
        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));

        AttemptClassification classification = evaluate(answer, question.getCanonicalAnswer());

        // Create attempt
        QuestionAttempt attempt = new QuestionAttempt();
        attempt.setUserId(userId);
        attempt.setQuestionId(questionId);
        attempt.setUserAnswer(answer);
        attempt.setClassification(classification);
        attempt.setDrillRunId(drillRunId);
        attempt = attemptRepository.save(attempt);

        // Create evaluation
        AttemptEvaluation evaluation = new AttemptEvaluation();
        evaluation.setAttemptId(attempt.getId());
        evaluation.setClassification(classification);
        evaluation.setDetails(buildEvaluationDetails(classification, question.getCanonicalAnswer()));
        evaluationRepository.save(evaluation);

        // Update drill run counts
        if (classification == AttemptClassification.CORRECT) {
            run.setCorrectCount(run.getCorrectCount() + 1);
            drillRunRepository.save(run);
        }

        log.info("Drill {} user {} answered question {}: {}", drillRunId, userId, questionId, classification);

        return new SubmitAnswerResponse(
            classification.name(),
            classification == AttemptClassification.CORRECT,
            question.getExplanation(),
            false
        );
    }

    @Transactional
    public DrillRunResponse completeDrill(Long userId, Long drillRunId) {
        DrillRun run = loadDrillRunForUser(drillRunId, userId);

        if ("COMPLETED".equals(run.getStatus())) {
            throw new IllegalStateException("Drill run is already completed");
        }

        run.setStatus("COMPLETED");
        run.setCompletedAt(Instant.now());
        drillRunRepository.save(run);

        log.info("Completed drill {} for user {}: {}/{} correct",
            drillRunId, userId, run.getCorrectCount(), run.getTotalQuestions());
        return toResponse(run);
    }

    private AttemptClassification evaluate(String userAnswer, String canonicalAnswer) {
        String normalizedUser = userAnswer.trim().toLowerCase();
        String normalizedCanonical = canonicalAnswer.trim().toLowerCase();

        if (normalizedUser.equals(normalizedCanonical)) {
            return AttemptClassification.CORRECT;
        }
        if (normalizedUser.contains(normalizedCanonical)) {
            return AttemptClassification.PARTIAL;
        }
        return AttemptClassification.WRONG;
    }

    private String buildEvaluationDetails(AttemptClassification classification, String canonicalAnswer) {
        return switch (classification) {
            case CORRECT -> "Exact match with canonical answer.";
            case PARTIAL -> "Answer contains the canonical answer but is not an exact match.";
            case WRONG -> "Answer does not match. Expected: " + canonicalAnswer;
            case FLAGGED_BY_USER -> "User flagged this question for review.";
        };
    }

    private WrongNotebookEntry loadEntryForUser(Long entryId, Long userId) {
        WrongNotebookEntry entry = entryRepository.findById(entryId)
            .orElseThrow(() -> new IllegalArgumentException("Notebook entry not found: " + entryId));
        if (!entry.getUserId().equals(userId)) {
            throw new SecurityException("Access denied to notebook entry " + entryId);
        }
        return entry;
    }

    private DrillRun loadDrillRunForUser(Long drillRunId, Long userId) {
        DrillRun run = drillRunRepository.findById(drillRunId)
            .orElseThrow(() -> new IllegalArgumentException("Drill run not found: " + drillRunId));
        if (!run.getUserId().equals(userId)) {
            throw new SecurityException("Access denied to drill run " + drillRunId);
        }
        return run;
    }

    private DrillRunResponse toResponse(DrillRun run) {
        return new DrillRunResponse(
            run.getId(),
            run.getDrillType().name(),
            run.getStatus(),
            run.getTotalQuestions(),
            run.getCorrectCount(),
            run.getStartedAt()
        );
    }
}
