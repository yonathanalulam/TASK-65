package com.culinarycoach.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Optional scheduled daily close for reconciliation exports.
 * Disabled by default (cron expression set to "-").
 * Admin can trigger manually via the admin endpoint.
 */
@Component
public class ReconciliationExportJob {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationExportJob.class);

    private final ReconciliationService reconciliationService;

    public ReconciliationExportJob(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    /**
     * Scheduled daily close at midnight. Disabled by default.
     * Enable by setting app.checkout.reconciliation-cron in application.yml.
     */
    @Scheduled(cron = "${app.checkout.reconciliation-cron:-}")
    public void dailyClose() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Running scheduled reconciliation export for {}", yesterday);

        try {
            reconciliationService.generateExport(yesterday, "SYSTEM_SCHEDULER");
            log.info("Scheduled reconciliation export completed for {}", yesterday);
        } catch (Exception e) {
            log.error("Scheduled reconciliation export failed for {}: {}",
                    yesterday, e.getMessage(), e);
        }
    }
}
