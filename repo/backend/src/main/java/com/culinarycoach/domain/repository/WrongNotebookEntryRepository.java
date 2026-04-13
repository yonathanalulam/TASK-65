package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.WrongNotebookEntry;
import com.culinarycoach.domain.enums.NotebookEntryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WrongNotebookEntryRepository extends JpaRepository<WrongNotebookEntry, Long> {

    Page<WrongNotebookEntry> findByUserIdAndStatusIn(Long userId, Collection<NotebookEntryStatus> statuses, Pageable pageable);

    Optional<WrongNotebookEntry> findByUserIdAndQuestionIdAndStatusIn(Long userId, Long questionId, Collection<NotebookEntryStatus> statuses);

    long countByUserIdAndStatus(Long userId, NotebookEntryStatus status);

    List<WrongNotebookEntry> findByStatusIn(Collection<NotebookEntryStatus> statuses);
}
