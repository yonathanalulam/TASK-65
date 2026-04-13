package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.WrongNotebookNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WrongNotebookNoteRepository extends JpaRepository<WrongNotebookNote, Long> {

    List<WrongNotebookNote> findByEntryIdOrderByCreatedAtDesc(Long entryId);
}
