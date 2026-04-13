package com.culinarycoach.service;

import com.culinarycoach.domain.entity.Notification;
import com.culinarycoach.domain.enums.NotificationStatus;
import com.culinarycoach.domain.enums.NotificationType;
import com.culinarycoach.domain.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

@Component
public class OverdueRecalculationJob {

    private static final Logger log = LoggerFactory.getLogger(OverdueRecalculationJob.class);

    private static final Set<NotificationStatus> UNREAD_STATUSES = Set.of(
        NotificationStatus.GENERATED, NotificationStatus.DELIVERED
    );

    private final NotificationRepository notificationRepository;

    public OverdueRecalculationJob(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Runs every 15 minutes.
     * Updates priority of existing unread notifications based on overdue status.
     * Priority increases as the notification ages without being read.
     */
    @Scheduled(cron = "0 */15 * * * *")
    public void recalculateOverdue() {
        log.info("Running overdue recalculation job");

        try {
            int updatedCount = 0;
            int page = 0;
            int pageSize = 100;

            Page<Notification> notifications;
            do {
                // Process all users' unread notifications in pages
                notifications = notificationRepository
                    .findByUserIdAndStatusIn(null, UNREAD_STATUSES, PageRequest.of(page, pageSize));

                // Since findByUserIdAndStatusIn requires a userId, we process differently
                // We'll iterate all unread notifications via a custom approach
                break; // break out -- we use a different strategy below
            } while (notifications.hasNext());

            // Process all unread PRACTICE_DUE and PRACTICE_OVERDUE notifications
            var allUnread = notificationRepository.findAll();
            Instant now = Instant.now();

            for (Notification notification : allUnread) {
                if (!UNREAD_STATUSES.contains(notification.getStatus())) {
                    continue;
                }
                if (notification.getType() != NotificationType.PRACTICE_DUE
                    && notification.getType() != NotificationType.PRACTICE_OVERDUE) {
                    continue;
                }

                int newPriority = calculatePriority(notification, now);
                if (newPriority != notification.getPriority()) {
                    notification.setPriority(newPriority);
                    notificationRepository.save(notification);
                    updatedCount++;
                }
            }

            if (updatedCount > 0) {
                log.info("Overdue recalculation: updated priority on {} notifications", updatedCount);
            }
        } catch (Exception e) {
            log.error("Overdue recalculation job failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Calculate priority based on how overdue the notification is.
     * 0 = normal, 1 = due, 2 = overdue (>24h), 3 = severely overdue (>48h)
     */
    private int calculatePriority(Notification notification, Instant now) {
        if (notification.getNextDueAt() == null) {
            return 0;
        }

        long hoursOverdue = ChronoUnit.HOURS.between(notification.getNextDueAt(), now);

        if (hoursOverdue >= 48) {
            return 3;
        } else if (hoursOverdue >= 24) {
            return 2;
        } else if (hoursOverdue >= 0) {
            return 1;
        }
        return 0;
    }
}
