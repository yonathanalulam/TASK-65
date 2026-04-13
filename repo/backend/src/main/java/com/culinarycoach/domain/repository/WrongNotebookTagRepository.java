package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.WrongNotebookTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WrongNotebookTagRepository extends JpaRepository<WrongNotebookTag, Long> {

    Optional<WrongNotebookTag> findByLabel(String label);
}
