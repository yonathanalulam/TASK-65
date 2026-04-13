package com.culinarycoach.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audio_favorites",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "audio_asset_id"}))
public class AudioFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "audio_asset_id", nullable = false)
    private Long audioAssetId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getAudioAssetId() { return audioAssetId; }
    public void setAudioAssetId(Long audioAssetId) { this.audioAssetId = audioAssetId; }

    public Instant getCreatedAt() { return createdAt; }
}
