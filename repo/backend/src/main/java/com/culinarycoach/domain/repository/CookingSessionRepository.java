package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.CookingSession;
import com.culinarycoach.domain.enums.CookingSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface CookingSessionRepository extends JpaRepository<CookingSession, Long> {

    List<CookingSession> findByUserIdAndStatusIn(Long userId, Collection<CookingSessionStatus> statuses);

    long countByUserIdAndStatusIn(Long userId, Collection<CookingSessionStatus> statuses);

    List<CookingSession> findByUserIdAndLessonIdAndStatusIn(Long userId, Long lessonId,
                                                             Collection<CookingSessionStatus> statuses);

    List<CookingSession> findByStatusAndLastActivityAtBefore(CookingSessionStatus status, Instant cutoff);

    List<CookingSession> findByStatusAndAbandonedAtBefore(CookingSessionStatus status, Instant cutoff);
}
