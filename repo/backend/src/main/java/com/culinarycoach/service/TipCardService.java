package com.culinarycoach.service;

import com.culinarycoach.audit.TraceContext;
import com.culinarycoach.domain.entity.StepTipBinding;
import com.culinarycoach.domain.entity.TipCard;
import com.culinarycoach.domain.entity.TipCardAuditLog;
import com.culinarycoach.domain.entity.TipCardConfiguration;
import com.culinarycoach.domain.enums.TipDisplayMode;
import com.culinarycoach.domain.repository.StepTipBindingRepository;
import com.culinarycoach.domain.repository.TipCardAuditLogRepository;
import com.culinarycoach.domain.repository.TipCardConfigurationRepository;
import com.culinarycoach.domain.repository.TipCardRepository;
import com.culinarycoach.web.dto.response.TipCardResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TipCardService {

    private static final Logger log = LoggerFactory.getLogger(TipCardService.class);

    private final TipCardRepository tipCardRepository;
    private final TipCardConfigurationRepository configRepository;
    private final TipCardAuditLogRepository auditLogRepository;
    private final StepTipBindingRepository stepTipBindingRepository;

    public TipCardService(TipCardRepository tipCardRepository,
                           TipCardConfigurationRepository configRepository,
                           TipCardAuditLogRepository auditLogRepository,
                           StepTipBindingRepository stepTipBindingRepository) {
        this.tipCardRepository = tipCardRepository;
        this.configRepository = configRepository;
        this.auditLogRepository = auditLogRepository;
        this.stepTipBindingRepository = stepTipBindingRepository;
    }

    /**
     * Resolve effective tips for a step.
     * Display mode resolution: per-step binding > lesson config > global config.
     * Tips with DISABLED display mode are excluded.
     */
    @Transactional(readOnly = true)
    public List<TipCardResponse> getEffectiveTipsForStep(Long stepId, Long lessonId) {
        List<StepTipBinding> bindings = stepTipBindingRepository.findByStepId(stepId);
        List<TipCardResponse> result = new ArrayList<>();

        for (StepTipBinding binding : bindings) {
            Optional<TipCard> tipCardOpt = tipCardRepository.findById(binding.getTipCardId());
            if (tipCardOpt.isEmpty()) continue;

            TipCard tipCard = tipCardOpt.get();
            if (!tipCard.isEnabled()) continue;

            // Resolve display mode: per-step > lesson > global
            TipDisplayMode effectiveMode = resolveDisplayMode(binding, lessonId);

            if (effectiveMode == TipDisplayMode.DISABLED) continue;

            result.add(toResponse(tipCard, effectiveMode));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<TipCardResponse> listTipCards() {
        return tipCardRepository.findAll().stream()
            .map(tip -> {
                TipDisplayMode mode = resolveGlobalDisplayMode();
                return toResponse(tip, mode);
            })
            .toList();
    }

    @Transactional
    public TipCardResponse configureTipDisplayMode(Long tipCardId, String scope, Long scopeId,
                                                     String displayModeStr, String changedBy) {
        TipCard tipCard = tipCardRepository.findById(tipCardId)
            .orElseThrow(() -> new IllegalArgumentException("Tip card not found: " + tipCardId));

        TipDisplayMode newMode = TipDisplayMode.valueOf(displayModeStr.toUpperCase());

        Optional<TipCardConfiguration> existing = configRepository.findByScopeAndScopeId(scope, scopeId);
        TipCardConfiguration config;
        String oldValue;

        if (existing.isPresent()) {
            config = existing.get();
            oldValue = config.getDisplayMode().name();
            config.setDisplayMode(newMode);
        } else {
            config = new TipCardConfiguration();
            config.setScope(scope);
            config.setScopeId(scopeId);
            config.setDisplayMode(newMode);
            oldValue = null;
        }
        config.setUpdatedBy(changedBy);
        config = configRepository.save(config);

        // Audit log
        TipCardAuditLog auditLog = new TipCardAuditLog();
        auditLog.setTipCardId(tipCardId);
        auditLog.setConfigId(config.getId());
        auditLog.setAction("CONFIGURE_DISPLAY_MODE");
        auditLog.setOldValue(oldValue);
        auditLog.setNewValue(newMode.name());
        auditLog.setChangedBy(changedBy);
        auditLog.setTraceId(TraceContext.get());
        auditLogRepository.save(auditLog);

        log.info("Configured tip {} display mode to {} (scope={}, scopeId={})",
            tipCardId, newMode, scope, scopeId);

        return toResponse(tipCard, newMode);
    }

    @Transactional
    public TipCardResponse toggleTipCard(Long tipCardId, String changedBy) {
        TipCard tipCard = tipCardRepository.findById(tipCardId)
            .orElseThrow(() -> new IllegalArgumentException("Tip card not found: " + tipCardId));

        boolean oldEnabled = tipCard.isEnabled();
        tipCard.setEnabled(!oldEnabled);
        tipCardRepository.save(tipCard);

        // Audit log
        TipCardAuditLog auditLog = new TipCardAuditLog();
        auditLog.setTipCardId(tipCardId);
        auditLog.setAction("TOGGLE_ENABLED");
        auditLog.setOldValue(String.valueOf(oldEnabled));
        auditLog.setNewValue(String.valueOf(!oldEnabled));
        auditLog.setChangedBy(changedBy);
        auditLog.setTraceId(TraceContext.get());
        auditLogRepository.save(auditLog);

        log.info("Toggled tip {} enabled: {} -> {}", tipCardId, oldEnabled, !oldEnabled);

        TipDisplayMode mode = resolveGlobalDisplayMode();
        return toResponse(tipCard, mode);
    }

    /**
     * Resolve display mode with priority: per-step binding > lesson config > global config.
     */
    private TipDisplayMode resolveDisplayMode(StepTipBinding binding, Long lessonId) {
        // 1. Per-step override
        if (binding.getDisplayMode() != null) {
            return binding.getDisplayMode();
        }

        // 2. Lesson-level config
        if (lessonId != null) {
            Optional<TipCardConfiguration> lessonConfig = configRepository
                .findByScopeAndScopeId("LESSON", lessonId);
            if (lessonConfig.isPresent()) {
                return lessonConfig.get().getDisplayMode();
            }
        }

        // 3. Global default
        return resolveGlobalDisplayMode();
    }

    private TipDisplayMode resolveGlobalDisplayMode() {
        return configRepository.findByScopeAndScopeId("GLOBAL", null)
            .map(TipCardConfiguration::getDisplayMode)
            .orElse(TipDisplayMode.SHORT);
    }

    private TipCardResponse toResponse(TipCard tipCard, TipDisplayMode displayMode) {
        String shortText = null;
        String detailedText = null;

        if (displayMode == TipDisplayMode.SHORT || displayMode == TipDisplayMode.DETAILED) {
            shortText = tipCard.getShortText();
        }
        if (displayMode == TipDisplayMode.DETAILED) {
            detailedText = tipCard.getDetailedText();
        }

        return new TipCardResponse(
            tipCard.getId(),
            tipCard.getTitle(),
            shortText,
            detailedText,
            displayMode.name(),
            tipCard.isEnabled()
        );
    }
}
