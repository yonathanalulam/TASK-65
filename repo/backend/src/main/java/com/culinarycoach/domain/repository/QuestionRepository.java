package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    Page<Question> findByLessonIdAndActiveTrue(Long lessonId, Pageable pageable);

    @Query("SELECT q FROM Question q WHERE q.active = true AND " +
           "LOWER(q.questionText) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Question> searchByText(@Param("search") String search, Pageable pageable);
}
