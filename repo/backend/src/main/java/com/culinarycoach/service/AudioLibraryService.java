package com.culinarycoach.service;

import com.culinarycoach.domain.entity.AudioAsset;
import com.culinarycoach.domain.repository.AudioAssetRepository;
import com.culinarycoach.domain.repository.AudioBundleEntitlementRepository;
import com.culinarycoach.domain.repository.AudioFavoriteRepository;
import com.culinarycoach.web.dto.response.AudioAssetResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AudioLibraryService {

    private final AudioAssetRepository audioAssetRepository;
    private final AudioFavoriteRepository audioFavoriteRepository;
    private final AudioBundleEntitlementRepository audioBundleEntitlementRepository;

    public AudioLibraryService(AudioAssetRepository audioAssetRepository,
                               AudioFavoriteRepository audioFavoriteRepository,
                               AudioBundleEntitlementRepository audioBundleEntitlementRepository) {
        this.audioAssetRepository = audioAssetRepository;
        this.audioFavoriteRepository = audioFavoriteRepository;
        this.audioBundleEntitlementRepository = audioBundleEntitlementRepository;
    }

    @Transactional(readOnly = true)
    public Page<AudioAssetResponse> browseAssets(Long userId, String search, Pageable pageable) {
        Page<AudioAsset> assets;
        if (search != null && !search.isBlank()) {
            assets = audioAssetRepository.findByActiveTrueAndTitleContainingIgnoreCase(search.trim(), pageable);
        } else {
            assets = audioAssetRepository.findByActiveTrue(pageable);
        }
        return assets.map(asset -> toResponse(asset, userId));
    }

    @Transactional(readOnly = true)
    public AudioAssetResponse getAssetDetails(Long assetId, Long userId) {
        AudioAsset asset = audioAssetRepository.findById(assetId)
            .orElseThrow(() -> new IllegalArgumentException("Audio asset not found: " + assetId));
        return toResponse(asset, userId);
    }

    @Transactional(readOnly = true)
    public boolean hasEntitlement(Long userId, Long bundleId) {
        if (bundleId == null) {
            return true; // free content
        }
        return audioBundleEntitlementRepository.existsByUserIdAndBundleIdAndRevokedAtIsNull(userId, bundleId);
    }

    private AudioAssetResponse toResponse(AudioAsset asset, Long userId) {
        boolean favorite = userId != null
            && audioFavoriteRepository.existsByUserIdAndAudioAssetId(userId, asset.getId());
        return new AudioAssetResponse(
            asset.getId(),
            asset.getTitle(),
            asset.getDescription(),
            asset.getCoverArtPath(),
            asset.getDurationSeconds(),
            asset.getCategory(),
            asset.getDifficulty(),
            favorite
        );
    }
}
