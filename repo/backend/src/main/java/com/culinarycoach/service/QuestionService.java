package com.culinarycoach.service;

import com.culinarycoach.domain.entity.*;
import com.culinarycoach.domain.enums.AttemptClassification;
import com.culinarycoach.domain.enums.NotebookEntryStatus;
import com.culinarycoach.domain.repository.*;
import com.culinarycoach.web.dto.response.QuestionResponse;
import com.culinarycoach.web.dto.response.SubmitAnswerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Service
public class QuestionService {

    private static final Logger log = LoggerFactory.getLogger(QuestionService.class);

    private static final Set<NotebookEntryStatus> ACTIVE_NOTEBOOK_STATUSES = Set.of(
        NotebookEntryStatus.ACTIVE, NotebookEntryStatus.FAVORITED
    );

    private final QuestionRepository questionRepository;
    private final QuestionVariantRepository questionVariantRepository;
    private final QuestionAttemptRepository questionAttemptRepository;
    private final AttemptEvaluationRepository attemptEvaluationRepository;
    private final WrongNotebookEntryRepository wrongNotebookEntryRepository;

    public QuestionService(QuestionRepository questionRepository,
                           QuestionVariantRepository questionVariantRepository,
                           QuestionAttemptRepository questionAttemptRepository,
                           AttemptEvaluationRepository attemptEvaluationRepository,
                           WrongNotebookEntryRepository wrongNotebookEntryRepository) {
        this.questionRepository = questionRepository;
        this.questionVariantRepository = questionVariantRepository;
        this.questionAttemptRepository = questionAttemptRepository;
        this.attemptEvaluationRepository = attemptEvaluationRepository;
        this.wrongNotebookEntryRepository = wrongNotebookEntryRepository;
    }

    @Transactional(readOnly = true)
    public Page<QuestionResponse> getQuestionsByLesson(Long lessonId, Pageable pageable) {
        Page<Question> questions = questionRepository.findByLessonIdAndActiveTrue(lessonId, pageable);
        return questions.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public QuestionResponse getQuestion(Long questionId) {
        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));
        return toResponse(question);
    }

    @Transactional
    public SubmitAnswerResponse submitAnswer(Long userId, Long questionId, Long variantId,
                                              String userAnswer, boolean flagged) {
        // Load the question (or variant) to get the canonical answer
        String canonicalAnswer;
        String explanation;

        if (variantId != null) {
            QuestionVariant variant = questionVariantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Question variant not found: " + variantId));
            canonicalAnswer = variant.getCanonicalAnswer();
            explanation = variant.getExplanation();
        } else {
            Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));
            canonicalAnswer = question.getCanonicalAnswer();
            explanation = question.getExplanation();
        }

        // Evaluate the answer
        AttemptClassification classification = evaluate(userAnswer, canonicalAnswer, flagged);

        // Create the attempt record
        QuestionAttempt attempt = new QuestionAttempt();
        attempt.setUserId(userId);
        attempt.setQuestionId(questionId);
        attempt.setVariantId(variantId);
        attempt.setUserAnswer(userAnswer);
        attempt.setClassification(classification);
        attempt.setFlaggedByUser(flagged);
        attempt = questionAttemptRepository.save(attempt);

        // Create the evaluation record
        AttemptEvaluation evaluation = new AttemptEvaluation();
        evaluation.setAttemptId(attempt.getId());
        evaluation.setClassification(classification);
        evaluation.setDetails(buildEvaluationDetails(classification, canonicalAnswer));
        attemptEvaluationRepository.save(evaluation);

        // Auto-create/update WrongNotebookEntry for WRONG, PARTIAL, or FLAGGED_BY_USER
        boolean notebookEntryCreated = false;
        if (classification != AttemptClassification.CORRECT) {
            notebookEntryCreated = createOrUpdateNotebookEntry(userId, questionId);
        }

        log.info("User {} submitted answer for question {}: classification={}", userId, questionId, classification);

        return new SubmitAnswerResponse(
            classification.name(),
            classification == AttemptClassification.CORRECT,
            explanation,
            notebookEntryCreated
        );
    }

    private AttemptClassification evaluate(String userAnswer, String canonicalAnswer, boolean flagged) {
        if (flagged) {
            return AttemptClassification.FLAGGED_BY_USER;
        }

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

    private boolean createOrUpdateNotebookEntry(Long userId, Long questionId) {
        var existing = wrongNotebookEntryRepository
            .findByUserIdAndQuestionIdAndStatusIn(userId, questionId, ACTIVE_NOTEBOOK_STATUSES);

        if (existing.isPresent()) {
            WrongNotebookEntry entry = existing.get();
            entry.setFailCount(entry.getFailCount() + 1);
            entry.setLastAttemptAt(Instant.now());
            wrongNotebookEntryRepository.save(entry);
            return false; // updated, not newly created
        }

        WrongNotebookEntry entry = new WrongNotebookEntry();
        entry.setUserId(userId);
        entry.setQuestionId(questionId);
        entry.setStatus(NotebookEntryStatus.ACTIVE);
        entry.setFailCount(1);
        entry.setLastAttemptAt(Instant.now());
        wrongNotebookEntryRepository.save(entry);

        log.info("Created new wrong notebook entry for user {} question {}", userId, questionId);
        return true;
    }

    private QuestionResponse toResponse(Question question) {
        return new QuestionResponse(
            question.getId(),
            question.getQuestionText(),
            question.getQuestionType(),
            question.getDifficulty(),
            question.getLessonId()
        );
    }
}
