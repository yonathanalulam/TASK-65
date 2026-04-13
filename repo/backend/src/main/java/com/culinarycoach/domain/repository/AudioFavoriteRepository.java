package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.AudioFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AudioFavoriteRepository extends JpaRepository<AudioFavorite, Long> {

    List<AudioFavorite> findByUserId(Long userId);

    boolean existsByUserIdAndAudioAssetId(Long userId, Long audioAssetId);

    void deleteByUserIdAndAudioAssetId(Long userId, Long audioAssetId);
}
