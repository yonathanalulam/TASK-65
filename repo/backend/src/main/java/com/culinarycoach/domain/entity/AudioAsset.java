package com.culinarycoach.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audio_assets")
public class AudioAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_art_path", length = 500)
    private String coverArtPath;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "local_source_path", length = 500)
    private String localSourcePath;

    @Column(name = "bundle_id")
    private Long bundleId;

    @Column(length = 20)
    private String difficulty;

    @Column(length = 100)
    private String category;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCoverArtPath() { return coverArtPath; }
    public void setCoverArtPath(String coverArtPath) { this.coverArtPath = coverArtPath; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public String getLocalSourcePath() { return localSourcePath; }
    public void setLocalSourcePath(String localSourcePath) { this.localSourcePath = localSourcePath; }

    public Long getBundleId() { return bundleId; }
    public void setBundleId(Long bundleId) { this.bundleId = bundleId; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
