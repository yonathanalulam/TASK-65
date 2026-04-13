package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.QuestionAttempt;
import com.culinarycoach.domain.enums.AttemptClassification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionAttemptRepository extends JpaRepository<QuestionAttempt, Long> {

    List<QuestionAttempt> findByUserIdAndQuestionId(Long userId, Long questionId);

    List<QuestionAttempt> findByUserId(Long userId);

    long countByUserIdAndClassification(Long userId, AttemptClassification classification);
}
