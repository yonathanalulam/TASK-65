package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.AudioPlaylistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AudioPlaylistItemRepository extends JpaRepository<AudioPlaylistItem, Long> {

    List<AudioPlaylistItem> findByPlaylistIdOrderBySortOrder(Long playlistId);

    void deleteByPlaylistIdAndAudioAssetId(Long playlistId, Long audioAssetId);

    int countByPlaylistId(Long playlistId);
}
