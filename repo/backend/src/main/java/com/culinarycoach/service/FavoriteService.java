package com.culinarycoach.service;

import com.culinarycoach.domain.entity.AudioAsset;
import com.culinarycoach.domain.entity.AudioFavorite;
import com.culinarycoach.domain.repository.AudioAssetRepository;
import com.culinarycoach.domain.repository.AudioFavoriteRepository;
import com.culinarycoach.web.dto.response.AudioAssetResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    private final AudioFavoriteRepository audioFavoriteRepository;
    private final AudioAssetRepository audioAssetRepository;

    public FavoriteService(AudioFavoriteRepository audioFavoriteRepository,
                           AudioAssetRepository audioAssetRepository) {
        this.audioFavoriteRepository = audioFavoriteRepository;
        this.audioAssetRepository = audioAssetRepository;
    }

    @Transactional
    public boolean toggleFavorite(Long userId, Long audioAssetId) {
        audioAssetRepository.findById(audioAssetId)
            .orElseThrow(() -> new IllegalArgumentException("Audio asset not found: " + audioAssetId));

        if (audioFavoriteRepository.existsByUserIdAndAudioAssetId(userId, audioAssetId)) {
            audioFavoriteRepository.deleteByUserIdAndAudioAssetId(userId, audioAssetId);
            return false; // unfavorited
        } else {
            AudioFavorite favorite = new AudioFavorite();
            favorite.setUserId(userId);
            favorite.setAudioAssetId(audioAssetId);
            audioFavoriteRepository.save(favorite);
            return true; // favorited
        }
    }

    @Transactional
    public void addFavorite(Long userId, Long audioAssetId) {
        audioAssetRepository.findById(audioAssetId)
            .orElseThrow(() -> new IllegalArgumentException("Audio asset not found: " + audioAssetId));

        if (!audioFavoriteRepository.existsByUserIdAndAudioAssetId(userId, audioAssetId)) {
            AudioFavorite favorite = new AudioFavorite();
            favorite.setUserId(userId);
            favorite.setAudioAssetId(audioAssetId);
            audioFavoriteRepository.save(favorite);
        }
    }

    @Transactional
    public void removeFavorite(Long userId, Long audioAssetId) {
        audioFavoriteRepository.deleteByUserIdAndAudioAssetId(userId, audioAssetId);
    }

    @Transactional(readOnly = true)
    public List<AudioAssetResponse> getUserFavorites(Long userId) {
        List<AudioFavorite> favorites = audioFavoriteRepository.findByUserId(userId);
        List<Long> assetIds = favorites.stream().map(AudioFavorite::getAudioAssetId).toList();
        Map<Long, AudioAsset> assetsById = audioAssetRepository.findAllById(assetIds).stream()
            .collect(Collectors.toMap(AudioAsset::getId, Function.identity()));

        return favorites.stream()
            .map(fav -> {
                AudioAsset asset = assetsById.get(fav.getAudioAssetId());
                if (asset == null) return null;
                return new AudioAssetResponse(
                    asset.getId(), asset.getTitle(), asset.getDescription(),
                    asset.getCoverArtPath(), asset.getDurationSeconds(),
                    asset.getCategory(), asset.getDifficulty(), true
                );
            })
            .filter(r -> r != null)
            .toList();
    }
}
