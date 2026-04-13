package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.AttemptEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttemptEvaluationRepository extends JpaRepository<AttemptEvaluation, Long> {

    Optional<AttemptEvaluation> findByAttemptId(Long attemptId);
}
