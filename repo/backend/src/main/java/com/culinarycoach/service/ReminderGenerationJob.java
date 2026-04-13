package com.culinarycoach.service;

import com.culinarycoach.domain.entity.WrongNotebookEntry;
import com.culinarycoach.domain.enums.NotebookEntryStatus;
import com.culinarycoach.domain.enums.NotificationType;
import com.culinarycoach.domain.repository.WrongNotebookEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Component
public class ReminderGenerationJob {

    private static final Logger log = LoggerFactory.getLogger(ReminderGenerationJob.class);

    private static final long DEFAULT_DUE_DELAY_HOURS = 24;

    private final WrongNotebookEntryRepository entryRepository;
    private final NotificationService notificationService;

    public ReminderGenerationJob(WrongNotebookEntryRepository entryRepository,
                                  NotificationService notificationService) {
        this.entryRepository = entryRepository;
        this.notificationService = notificationService;
    }

    /**
     * Runs every 15 minutes.
     * For each active/favorited notebook entry:
     * - If next_due_at (lastAttemptAt + 24h) <= now, create PRACTICE_DUE notification
     * - If current time > next_due_at + 24h, create PRACTICE_OVERDUE notification
     * Dedup: same suppressionKey within 12h -> skip (handled by NotificationService)
     */
    @Scheduled(cron = "0 */15 * * * *")
    public void generateReminders() {
        log.info("Running reminder generation job");

        try {
            List<WrongNotebookEntry> activeEntries = entryRepository
                .findByStatusIn(Set.of(NotebookEntryStatus.ACTIVE, NotebookEntryStatus.FAVORITED));

            Instant now = Instant.now();
            int dueCount = 0;
            int overdueCount = 0;

            for (WrongNotebookEntry entry : activeEntries) {
                Instant nextDueAt = entry.getLastAttemptAt()
                    .plus(DEFAULT_DUE_DELAY_HOURS, ChronoUnit.HOURS);
                Instant overdueAt = nextDueAt.plus(DEFAULT_DUE_DELAY_HOURS, ChronoUnit.HOURS);

                if (now.isAfter(overdueAt) || now.equals(overdueAt)) {
                    // Overdue: more than 24h past due time
                    String suppressionKey = "PRACTICE_OVERDUE_" + entry.getUserId() + "_" + entry.getId();
                    var created = notificationService.createNotification(
                        entry.getUserId(),
                        NotificationType.PRACTICE_OVERDUE,
                        "Practice overdue",
                        "You have an overdue practice item that needs attention.",
                        "NOTEBOOK_ENTRY",
                        entry.getId(),
                        nextDueAt,
                        suppressionKey
                    );
                    if (created != null) {
                        overdueCount++;
                    }
                } else if (now.isAfter(nextDueAt) || now.equals(nextDueAt)) {
                    // Due: past the due time but not overdue yet
                    String suppressionKey = "PRACTICE_DUE_" + entry.getUserId() + "_" + entry.getId();
                    var created = notificationService.createNotification(
                        entry.getUserId(),
                        NotificationType.PRACTICE_DUE,
                        "Time to practice",
                        "A question you got wrong is due for review.",
                        "NOTEBOOK_ENTRY",
                        entry.getId(),
                        nextDueAt,
                        suppressionKey
                    );
                    if (created != null) {
                        dueCount++;
                    }
                }
            }

            if (dueCount > 0 || overdueCount > 0) {
                log.info("Reminder generation: {} due, {} overdue notifications created",
                    dueCount, overdueCount);
            }
        } catch (Exception e) {
            log.error("Reminder generation job failed: {}", e.getMessage(), e);
        }
    }
}
