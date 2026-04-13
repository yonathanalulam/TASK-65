package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.QuestionVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionVariantRepository extends JpaRepository<QuestionVariant, Long> {

    List<QuestionVariant> findByOriginalQuestionId(Long originalQuestionId);
}
