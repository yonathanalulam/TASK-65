package com.culinarycoach.service;

import com.culinarycoach.domain.entity.AnomalyAlert;
import com.culinarycoach.domain.enums.AlertSeverity;
import com.culinarycoach.domain.enums.AlertStatus;
import com.culinarycoach.domain.repository.AnomalyAlertRepository;
import com.culinarycoach.web.dto.response.AnomalyAlertResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class AnomalyAlertService {

    private static final Logger log = LoggerFactory.getLogger(AnomalyAlertService.class);

    private final AnomalyAlertRepository anomalyAlertRepository;

    public AnomalyAlertService(AnomalyAlertRepository anomalyAlertRepository) {
        this.anomalyAlertRepository = anomalyAlertRepository;
    }

    @Transactional
    public AnomalyAlert createAlert(String type, AlertSeverity severity, String message,
                                     String metricName, BigDecimal threshold, BigDecimal actual) {
        AnomalyAlert alert = new AnomalyAlert();
        alert.setAlertType(type);
        alert.setSeverity(severity);
        alert.setMessage(message);
        alert.setMetricName(metricName);
        alert.setThresholdValue(threshold);
        alert.setActualValue(actual);
        alert.setStatus(AlertStatus.OPEN);

        log.warn("Anomaly alert created: type={}, severity={}, metric={}, message={}",
            type, severity, metricName, message);

        return anomalyAlertRepository.save(alert);
    }

    @Transactional
    public AnomalyAlertResponse acknowledgeAlert(Long alertId, String adminUsername) {
        AnomalyAlert alert = anomalyAlertRepository.findById(alertId)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));

        if (alert.getStatus() != AlertStatus.OPEN) {
            throw new IllegalStateException("Can only acknowledge OPEN alerts, current: "
                + alert.getStatus());
        }

        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedBy(adminUsername);
        alert.setAcknowledgedAt(Instant.now());

        log.info("Alert {} acknowledged by {}", alertId, adminUsername);
        return AnomalyAlertResponse.from(anomalyAlertRepository.save(alert));
    }

    @Transactional
    public AnomalyAlertResponse resolveAlert(Long alertId, String adminUsername) {
        AnomalyAlert alert = anomalyAlertRepository.findById(alertId)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));

        if (alert.getStatus() == AlertStatus.RESOLVED) {
            throw new IllegalStateException("Alert is already resolved");
        }

        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(Instant.now());
        if (alert.getAcknowledgedBy() == null) {
            alert.setAcknowledgedBy(adminUsername);
            alert.setAcknowledgedAt(Instant.now());
        }

        log.info("Alert {} resolved by {}", alertId, adminUsername);
        return AnomalyAlertResponse.from(anomalyAlertRepository.save(alert));
    }

    public Page<AnomalyAlertResponse> listAlerts(AlertStatus status, Pageable pageable) {
        if (status != null) {
            return anomalyAlertRepository.findByStatus(status, pageable)
                .map(AnomalyAlertResponse::from);
        }
        return anomalyAlertRepository.findAll(pageable)
            .map(AnomalyAlertResponse::from);
    }

    public long getOpenAlertCount() {
        return anomalyAlertRepository.countByStatus(AlertStatus.OPEN);
    }
}
