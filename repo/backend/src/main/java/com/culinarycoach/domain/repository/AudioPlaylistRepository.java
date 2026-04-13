package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.AudioPlaylist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AudioPlaylistRepository extends JpaRepository<AudioPlaylist, Long> {

    List<AudioPlaylist> findByUserId(Long userId);
}
