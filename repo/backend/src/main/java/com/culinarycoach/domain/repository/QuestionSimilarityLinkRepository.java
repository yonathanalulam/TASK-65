package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.QuestionSimilarityLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionSimilarityLinkRepository extends JpaRepository<QuestionSimilarityLink, Long> {

    @Query("SELECT l FROM QuestionSimilarityLink l WHERE l.questionIdA = :questionId OR l.questionIdB = :questionId")
    List<QuestionSimilarityLink> findSimilarQuestions(@Param("questionId") Long questionId);
}
