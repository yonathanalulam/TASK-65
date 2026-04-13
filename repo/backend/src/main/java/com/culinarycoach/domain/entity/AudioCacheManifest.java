package com.culinarycoach.domain.entity;

import com.culinarycoach.domain.enums.CacheEntryStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audio_cache_manifests",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "segment_id"}))
public class AudioCacheManifest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "segment_id", nullable = false)
    private Long segmentId;

    @Column(name = "cached_file_path", nullable = false, length = 500)
    private String cachedFilePath;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(nullable = false, length = 64)
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CacheEntryStatus status = CacheEntryStatus.DOWNLOADING;

    @Column(name = "downloaded_at")
    private Instant downloadedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getSegmentId() { return segmentId; }
    public void setSegmentId(Long segmentId) { this.segmentId = segmentId; }

    public String getCachedFilePath() { return cachedFilePath; }
    public void setCachedFilePath(String cachedFilePath) { this.cachedFilePath = cachedFilePath; }

    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }

    public CacheEntryStatus getStatus() { return status; }
    public void setStatus(CacheEntryStatus status) { this.status = status; }

    public Instant getDownloadedAt() { return downloadedAt; }
    public void setDownloadedAt(Instant downloadedAt) { this.downloadedAt = downloadedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getLastAccessedAt() { return lastAccessedAt; }
    public void setLastAccessedAt(Instant lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
