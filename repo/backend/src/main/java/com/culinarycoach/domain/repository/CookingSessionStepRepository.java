package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.CookingSessionStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CookingSessionStepRepository extends JpaRepository<CookingSessionStep, Long> {

    List<CookingSessionStep> findBySessionIdOrderByStepOrder(Long sessionId);

    Optional<CookingSessionStep> findBySessionIdAndStepOrder(Long sessionId, int stepOrder);
}
