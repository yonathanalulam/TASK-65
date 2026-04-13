package com.culinarycoach.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audio_segments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"audio_asset_id", "segment_index"}))
public class AudioSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audio_asset_id", nullable = false)
    private Long audioAssetId;

    @Column(name = "segment_index", nullable = false)
    private int segmentIndex;

    @Column(name = "start_offset_ms", nullable = false)
    private long startOffsetMs = 0;

    @Column(name = "end_offset_ms", nullable = false)
    private long endOffsetMs;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(nullable = false, length = 64)
    private String checksum;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAudioAssetId() { return audioAssetId; }
    public void setAudioAssetId(Long audioAssetId) { this.audioAssetId = audioAssetId; }

    public int getSegmentIndex() { return segmentIndex; }
    public void setSegmentIndex(int segmentIndex) { this.segmentIndex = segmentIndex; }

    public long getStartOffsetMs() { return startOffsetMs; }
    public void setStartOffsetMs(long startOffsetMs) { this.startOffsetMs = startOffsetMs; }

    public long getEndOffsetMs() { return endOffsetMs; }
    public void setEndOffsetMs(long endOffsetMs) { this.endOffsetMs = endOffsetMs; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }

    public Instant getCreatedAt() { return createdAt; }
}
