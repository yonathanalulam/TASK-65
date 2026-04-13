package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.WrongNotebookEntryTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WrongNotebookEntryTagRepository extends JpaRepository<WrongNotebookEntryTag, Long> {

    List<WrongNotebookEntryTag> findByEntryId(Long entryId);

    Optional<WrongNotebookEntryTag> findByEntryIdAndTagId(Long entryId, Long tagId);
}
