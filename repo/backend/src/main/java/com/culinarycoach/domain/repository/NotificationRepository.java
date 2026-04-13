package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.Notification;
import com.culinarycoach.domain.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdAndStatusIn(Long userId, Collection<NotificationStatus> statuses, Pageable pageable);

    long countByUserIdAndStatusIn(Long userId, Collection<NotificationStatus> statuses);

    List<Notification> findBySuppressionKeyAndStatusInAndCreatedAtAfter(
            String suppressionKey, Collection<NotificationStatus> statuses, Instant createdAfter);
}
