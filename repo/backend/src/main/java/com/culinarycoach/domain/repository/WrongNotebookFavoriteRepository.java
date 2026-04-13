package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.WrongNotebookFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WrongNotebookFavoriteRepository extends JpaRepository<WrongNotebookFavorite, Long> {

    Optional<WrongNotebookFavorite> findByUserIdAndEntryId(Long userId, Long entryId);

    boolean existsByUserIdAndEntryId(Long userId, Long entryId);
}
