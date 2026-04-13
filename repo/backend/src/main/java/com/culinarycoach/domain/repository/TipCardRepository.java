package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.TipCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TipCardRepository extends JpaRepository<TipCard, Long> {

    List<TipCard> findByEnabledTrue();

    List<TipCard> findByLessonId(Long lessonId);

    List<TipCard> findByScope(String scope);
}
