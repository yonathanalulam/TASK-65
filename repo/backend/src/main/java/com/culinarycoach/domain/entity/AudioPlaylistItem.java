package com.culinarycoach.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audio_playlist_items",
       uniqueConstraints = @UniqueConstraint(columnNames = {"playlist_id", "audio_asset_id"}))
public class AudioPlaylistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "playlist_id", nullable = false)
    private Long playlistId;

    @Column(name = "audio_asset_id", nullable = false)
    private Long audioAssetId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    @PrePersist
    protected void onCreate() {
        this.addedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPlaylistId() { return playlistId; }
    public void setPlaylistId(Long playlistId) { this.playlistId = playlistId; }

    public Long getAudioAssetId() { return audioAssetId; }
    public void setAudioAssetId(Long audioAssetId) { this.audioAssetId = audioAssetId; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public Instant getAddedAt() { return addedAt; }
}
