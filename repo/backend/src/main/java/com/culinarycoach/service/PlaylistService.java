package com.culinarycoach.service;

import com.culinarycoach.domain.entity.AudioAsset;
import com.culinarycoach.domain.entity.AudioPlaylist;
import com.culinarycoach.domain.entity.AudioPlaylistItem;
import com.culinarycoach.domain.repository.AudioAssetRepository;
import com.culinarycoach.domain.repository.AudioFavoriteRepository;
import com.culinarycoach.domain.repository.AudioPlaylistItemRepository;
import com.culinarycoach.domain.repository.AudioPlaylistRepository;
import com.culinarycoach.web.dto.request.CreatePlaylistRequest;
import com.culinarycoach.web.dto.request.UpdatePlaylistRequest;
import com.culinarycoach.web.dto.response.AudioAssetResponse;
import com.culinarycoach.web.dto.response.PlaylistDetailResponse;
import com.culinarycoach.web.dto.response.PlaylistResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PlaylistService {

    private final AudioPlaylistRepository playlistRepository;
    private final AudioPlaylistItemRepository playlistItemRepository;
    private final AudioAssetRepository audioAssetRepository;
    private final AudioFavoriteRepository audioFavoriteRepository;

    public PlaylistService(AudioPlaylistRepository playlistRepository,
                           AudioPlaylistItemRepository playlistItemRepository,
                           AudioAssetRepository audioAssetRepository,
                           AudioFavoriteRepository audioFavoriteRepository) {
        this.playlistRepository = playlistRepository;
        this.playlistItemRepository = playlistItemRepository;
        this.audioAssetRepository = audioAssetRepository;
        this.audioFavoriteRepository = audioFavoriteRepository;
    }

    @Transactional(readOnly = true)
    public List<PlaylistResponse> getUserPlaylists(Long userId) {
        return playlistRepository.findByUserId(userId).stream()
            .map(p -> new PlaylistResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                playlistItemRepository.countByPlaylistId(p.getId()),
                p.getCreatedAt()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public PlaylistDetailResponse getPlaylistDetail(Long playlistId, Long userId) {
        AudioPlaylist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new IllegalArgumentException("Playlist not found: " + playlistId));

        if (!playlist.getUserId().equals(userId)) {
            throw new SecurityException("Access denied to playlist: " + playlistId);
        }

        List<AudioPlaylistItem> items = playlistItemRepository.findByPlaylistIdOrderBySortOrder(playlistId);
        List<Long> assetIds = items.stream().map(AudioPlaylistItem::getAudioAssetId).toList();
        Map<Long, AudioAsset> assetsById = audioAssetRepository.findAllById(assetIds).stream()
            .collect(Collectors.toMap(AudioAsset::getId, Function.identity()));

        List<AudioAssetResponse> assetResponses = new ArrayList<>();
        for (AudioPlaylistItem item : items) {
            AudioAsset asset = assetsById.get(item.getAudioAssetId());
            if (asset != null) {
                boolean fav = audioFavoriteRepository.existsByUserIdAndAudioAssetId(userId, asset.getId());
                assetResponses.add(new AudioAssetResponse(
                    asset.getId(), asset.getTitle(), asset.getDescription(),
                    asset.getCoverArtPath(), asset.getDurationSeconds(),
                    asset.getCategory(), asset.getDifficulty(), fav
                ));
            }
        }

        return new PlaylistDetailResponse(
            playlist.getId(), playlist.getName(), playlist.getDescription(), assetResponses
        );
    }

    @Transactional
    public PlaylistResponse createPlaylist(Long userId, CreatePlaylistRequest request) {
        AudioPlaylist playlist = new AudioPlaylist();
        playlist.setUserId(userId);
        playlist.setName(request.name());
        playlist.setDescription(request.description());
        playlist = playlistRepository.save(playlist);

        return new PlaylistResponse(
            playlist.getId(), playlist.getName(), playlist.getDescription(), 0, playlist.getCreatedAt()
        );
    }

    @Transactional
    public PlaylistResponse updatePlaylist(Long playlistId, Long userId, UpdatePlaylistRequest request) {
        AudioPlaylist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new IllegalArgumentException("Playlist not found: " + playlistId));

        if (!playlist.getUserId().equals(userId)) {
            throw new SecurityException("Access denied to playlist: " + playlistId);
        }

        if (request.name() != null && !request.name().isBlank()) {
            playlist.setName(request.name());
        }
        if (request.description() != null) {
            playlist.setDescription(request.description());
        }
        playlist = playlistRepository.save(playlist);

        int itemCount = playlistItemRepository.countByPlaylistId(playlistId);
        return new PlaylistResponse(
            playlist.getId(), playlist.getName(), playlist.getDescription(), itemCount, playlist.getCreatedAt()
        );
    }

    @Transactional
    public void deletePlaylist(Long playlistId, Long userId) {
        AudioPlaylist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new IllegalArgumentException("Playlist not found: " + playlistId));

        if (!playlist.getUserId().equals(userId)) {
            throw new SecurityException("Access denied to playlist: " + playlistId);
        }

        playlistRepository.delete(playlist);
    }

    @Transactional
    public void addItem(Long playlistId, Long userId, Long audioAssetId) {
        AudioPlaylist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new IllegalArgumentException("Playlist not found: " + playlistId));

        if (!playlist.getUserId().equals(userId)) {
            throw new SecurityException("Access denied to playlist: " + playlistId);
        }

        audioAssetRepository.findById(audioAssetId)
            .orElseThrow(() -> new IllegalArgumentException("Audio asset not found: " + audioAssetId));

        int nextOrder = playlistItemRepository.countByPlaylistId(playlistId);

        AudioPlaylistItem item = new AudioPlaylistItem();
        item.setPlaylistId(playlistId);
        item.setAudioAssetId(audioAssetId);
        item.setSortOrder(nextOrder);
        playlistItemRepository.save(item);
    }

    @Transactional
    public void removeItem(Long playlistId, Long userId, Long audioAssetId) {
        AudioPlaylist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new IllegalArgumentException("Playlist not found: " + playlistId));

        if (!playlist.getUserId().equals(userId)) {
            throw new SecurityException("Access denied to playlist: " + playlistId);
        }

        playlistItemRepository.deleteByPlaylistIdAndAudioAssetId(playlistId, audioAssetId);
    }

    @Transactional
    public void reorderItems(Long playlistId, Long userId, List<Long> orderedAssetIds) {
        AudioPlaylist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new IllegalArgumentException("Playlist not found: " + playlistId));

        if (!playlist.getUserId().equals(userId)) {
            throw new SecurityException("Access denied to playlist: " + playlistId);
        }

        List<AudioPlaylistItem> items = playlistItemRepository.findByPlaylistIdOrderBySortOrder(playlistId);
        Map<Long, AudioPlaylistItem> itemsByAssetId = items.stream()
            .collect(Collectors.toMap(AudioPlaylistItem::getAudioAssetId, Function.identity()));

        for (int i = 0; i < orderedAssetIds.size(); i++) {
            AudioPlaylistItem item = itemsByAssetId.get(orderedAssetIds.get(i));
            if (item != null) {
                item.setSortOrder(i);
                playlistItemRepository.save(item);
            }
        }
    }
}
