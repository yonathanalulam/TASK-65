package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.CookingSessionTimer;
import com.culinarycoach.domain.enums.TimerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CookingSessionTimerRepository extends JpaRepository<CookingSessionTimer, Long> {

    List<CookingSessionTimer> findBySessionIdAndStatusIn(Long sessionId, Collection<TimerStatus> statuses);

    long countBySessionIdAndStatusIn(Long sessionId, Collection<TimerStatus> statuses);

    List<CookingSessionTimer> findBySessionId(Long sessionId);
}
