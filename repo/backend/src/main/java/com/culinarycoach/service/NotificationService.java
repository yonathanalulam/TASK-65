package com.culinarycoach.service;

import com.culinarycoach.domain.entity.Notification;
import com.culinarycoach.domain.enums.NotificationStatus;
import com.culinarycoach.domain.enums.NotificationType;
import com.culinarycoach.domain.repository.NotificationRepository;
import com.culinarycoach.web.dto.response.NotificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private static final long SUPPRESSION_WINDOW_HOURS = 12;
    private static final Set<NotificationStatus> ACTIVE_STATUSES = Set.of(
        NotificationStatus.GENERATED, NotificationStatus.DELIVERED
    );

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> listNotifications(Long userId, NotificationStatus status,
                                                         Pageable pageable) {
        Collection<NotificationStatus> statuses;
        if (status != null) {
            statuses = Set.of(status);
        } else {
            statuses = Set.of(NotificationStatus.GENERATED, NotificationStatus.DELIVERED, NotificationStatus.READ);
        }

        Page<Notification> notifications = notificationRepository
            .findByUserIdAndStatusIn(userId, statuses, pageable);
        return notifications.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndStatusIn(userId, ACTIVE_STATUSES);
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId, Long userId) {
        Notification notification = loadNotificationForUser(notificationId, userId);
        notification.setStatus(NotificationStatus.READ);
        notification.setReadAt(Instant.now());
        notificationRepository.save(notification);
        log.debug("Notification {} marked read for user {}", notificationId, userId);
        return toResponse(notification);
    }

    @Transactional
    public NotificationResponse dismiss(Long notificationId, Long userId) {
        Notification notification = loadNotificationForUser(notificationId, userId);
        notification.setStatus(NotificationStatus.DISMISSED);
        notification.setDismissedAt(Instant.now());
        notificationRepository.save(notification);
        log.debug("Notification {} dismissed for user {}", notificationId, userId);
        return toResponse(notification);
    }

    @Transactional
    public Notification createNotification(Long userId, NotificationType type,
                                            String title, String message,
                                            String referenceType, Long referenceId,
                                            Instant nextDueAt, String suppressionKey) {
        // Dedup: same suppressionKey within 12 hours -> skip
        if (suppressionKey != null) {
            Instant windowStart = Instant.now().minus(SUPPRESSION_WINDOW_HOURS, ChronoUnit.HOURS);
            List<Notification> recent = notificationRepository
                .findBySuppressionKeyAndStatusInAndCreatedAtAfter(
                    suppressionKey, ACTIVE_STATUSES, windowStart);
            if (!recent.isEmpty()) {
                log.debug("Suppressed duplicate notification for key '{}' (found {} recent)",
                    suppressionKey, recent.size());
                return null;
            }
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notification.setStatus(NotificationStatus.GENERATED);
        notification.setNextDueAt(nextDueAt);
        notification.setSuppressionKey(suppressionKey);
        notification = notificationRepository.save(notification);

        log.info("Created notification {} type={} for user {}", notification.getId(), type, userId);
        return notification;
    }

    private Notification loadNotificationForUser(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        if (!notification.getUserId().equals(userId)) {
            throw new SecurityException("Access denied to notification " + notificationId);
        }
        return notification;
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
            notification.getId(),
            notification.getType().name(),
            notification.getTitle(),
            notification.getMessage(),
            notification.getStatus().name(),
            notification.getPriority(),
            notification.getCreatedAt()
        );
    }
}
