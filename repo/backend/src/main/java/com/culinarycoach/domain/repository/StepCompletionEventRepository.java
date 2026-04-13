package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.StepCompletionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StepCompletionEventRepository extends JpaRepository<StepCompletionEvent, Long> {

    List<StepCompletionEvent> findBySessionId(Long sessionId);
}
