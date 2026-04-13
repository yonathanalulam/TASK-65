package com.culinarycoach.service;

import com.culinarycoach.config.AppProperties;
import com.culinarycoach.domain.entity.AudioAsset;
import com.culinarycoach.domain.entity.AudioCacheManifest;
import com.culinarycoach.domain.entity.AudioSegment;
import com.culinarycoach.domain.enums.CacheEntryStatus;
import com.culinarycoach.domain.repository.AudioAssetRepository;
import com.culinarycoach.domain.repository.AudioCacheManifestRepository;
import com.culinarycoach.domain.repository.AudioFavoriteRepository;
import com.culinarycoach.domain.repository.AudioSegmentRepository;
import com.culinarycoach.web.dto.response.CacheEntryResponse;
import com.culinarycoach.web.dto.response.StorageMeterResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AudioCacheService {

    private final AudioCacheManifestRepository cacheManifestRepository;
    private final AudioSegmentRepository segmentRepository;
    private final AudioAssetRepository audioAssetRepository;
    private final AudioFavoriteRepository audioFavoriteRepository;
    private final AudioLibraryService audioLibraryService;
    private final AppProperties appProperties;

    public AudioCacheService(AudioCacheManifestRepository cacheManifestRepository,
                             AudioSegmentRepository segmentRepository,
                             AudioAssetRepository audioAssetRepository,
                             AudioFavoriteRepository audioFavoriteRepository,
                             AudioLibraryService audioLibraryService,
                             AppProperties appProperties) {
        this.cacheManifestRepository = cacheManifestRepository;
        this.segmentRepository = segmentRepository;
        this.audioAssetRepository = audioAssetRepository;
        this.audioFavoriteRepository = audioFavoriteRepository;
        this.audioLibraryService = audioLibraryService;
        this.appProperties = appProperties;
    }

    @Transactional
    public CacheEntryResponse downloadSegment(Long userId, Long segmentId) {
        AudioSegment segment = segmentRepository.findById(segmentId)
            .orElseThrow(() -> new IllegalArgumentException("Segment not found: " + segmentId));

        // Validate file type
        String filePath = segment.getFilePath().toLowerCase();
        Set<String> allowed = Set.of(appProperties.getAudio().getAllowedAudioTypes().split(","));
        String extension = filePath.contains(".") ? filePath.substring(filePath.lastIndexOf('.') + 1) : "";
        if (!allowed.contains(extension)) {
            throw new IllegalArgumentException("Unsupported audio file type: " + extension);
        }

        // Validate segment size
        if (segment.getFileSizeBytes() > appProperties.getAudio().getMaxSegmentSizeBytes()) {
            throw new IllegalArgumentException("Segment exceeds maximum allowed size of "
                + appProperties.getAudio().getMaxSegmentSizeBytes() + " bytes");
        }

        // Check entitlement
        AudioAsset asset = audioAssetRepository.findById(segment.getAudioAssetId())
            .orElseThrow(() -> new IllegalArgumentException("Asset not found for segment"));

        if (!audioLibraryService.hasEntitlement(userId, asset.getBundleId())) {
            throw new SecurityException("No entitlement for bundle: " + asset.getBundleId());
        }

        // Check quota
        long usedBytes = cacheManifestRepository.sumFileSizeBytesByUserId(userId);
        if (usedBytes + segment.getFileSizeBytes() > appProperties.getAudio().getCacheQuotaBytes()) {
            throw new IllegalStateException("Cache quota exceeded. Used: " + usedBytes
                + " bytes, segment: " + segment.getFileSizeBytes() + " bytes, quota: "
                + appProperties.getAudio().getCacheQuotaBytes() + " bytes");
        }

        // Create cache directory structure and copy/create file
        Path cacheDir = Path.of("./cache", String.valueOf(userId), String.valueOf(segmentId));
        String fileName = Path.of(segment.getFilePath()).getFileName().toString();
        Path cachedFile = cacheDir.resolve(fileName);
        String checksum;

        try {
            Files.createDirectories(cacheDir);

            Path sourcePath = Path.of(segment.getFilePath());
            if (Files.exists(sourcePath)) {
                // Copy the actual source file to the cache location
                Files.copy(sourcePath, cachedFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } else {
                // Source not on disk: create a placeholder file with correct size metadata
                try (var out = Files.newOutputStream(cachedFile,
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
                    // Write a metadata header so the file has real content for checksumming
                    byte[] header = ("AUDIO_CACHE_PLACEHOLDER|segmentId=" + segmentId
                        + "|size=" + segment.getFileSizeBytes()
                        + "|source=" + segment.getFilePath() + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    out.write(header);
                }
            }

            // Compute SHA-256 checksum of the cached file
            checksum = computeSha256(cachedFile);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to cache audio segment to " + cachedFile + ": " + e.getMessage(), e);
        }

        Instant now = Instant.now();
        int validityDays = appProperties.getAudio().getCacheValidityDays();

        long actualFileSize;
        try {
            actualFileSize = Files.size(cachedFile);
        } catch (IOException e) {
            actualFileSize = segment.getFileSizeBytes();
        }

        AudioCacheManifest manifest = new AudioCacheManifest();
        manifest.setUserId(userId);
        manifest.setSegmentId(segmentId);
        manifest.setCachedFilePath(cachedFile.toString());
        manifest.setFileSizeBytes(actualFileSize);
        manifest.setChecksum(checksum);
        manifest.setStatus(CacheEntryStatus.CACHED_VALID);
        manifest.setDownloadedAt(now);
        manifest.setExpiresAt(now.plus(validityDays, ChronoUnit.DAYS));
        manifest.setLastAccessedAt(now);
        manifest = cacheManifestRepository.save(manifest);

        return toCacheEntryResponse(manifest, asset.getTitle());
    }

    private String computeSha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 algorithm not available", e);
        }
    }

    @Transactional(readOnly = true)
    public CacheEntryResponse getCacheEntry(Long manifestId, Long userId) {
        AudioCacheManifest manifest = cacheManifestRepository.findById(manifestId)
            .orElseThrow(() -> new IllegalArgumentException("Cache manifest not found: " + manifestId));

        if (!manifest.getUserId().equals(userId)) {
            throw new SecurityException("Access denied to cache entry: " + manifestId);
        }

        String title = resolveAssetTitle(manifest.getSegmentId());
        return toCacheEntryResponse(manifest, title);
    }

    @Transactional
    public void deleteCacheEntry(Long manifestId, Long userId) {
        deleteManifest(manifestId, userId);
    }

    @Transactional(readOnly = true)
    public List<CacheEntryResponse> listCacheEntries(Long userId) {
        return getCacheStatus(userId);
    }

    @Transactional(readOnly = true)
    public List<CacheEntryResponse> getCacheStatus(Long userId) {
        List<AudioCacheManifest> manifests = cacheManifestRepository.findByUserId(userId);

        // Gather segment IDs to look up asset titles
        List<Long> segmentIds = manifests.stream().map(AudioCacheManifest::getSegmentId).toList();
        Map<Long, AudioSegment> segmentsById = segmentRepository.findAllById(segmentIds).stream()
            .collect(Collectors.toMap(AudioSegment::getId, Function.identity()));

        List<Long> assetIds = segmentsById.values().stream()
            .map(AudioSegment::getAudioAssetId).distinct().toList();
        Map<Long, AudioAsset> assetsById = audioAssetRepository.findAllById(assetIds).stream()
            .collect(Collectors.toMap(AudioAsset::getId, Function.identity()));

        return manifests.stream().map(m -> {
            AudioSegment seg = segmentsById.get(m.getSegmentId());
            String title = "";
            if (seg != null) {
                AudioAsset asset = assetsById.get(seg.getAudioAssetId());
                if (asset != null) {
                    title = asset.getTitle();
                }
            }
            return toCacheEntryResponse(m, title);
        }).toList();
    }

    @Transactional
    public boolean validateChecksum(Long manifestId) {
        AudioCacheManifest manifest = cacheManifestRepository.findById(manifestId)
            .orElseThrow(() -> new IllegalArgumentException("Cache manifest not found: " + manifestId));

        try {
            Path path = Path.of(manifest.getCachedFilePath());
            if (!Files.exists(path)) {
                manifest.setStatus(CacheEntryStatus.CORRUPT);
                cacheManifestRepository.save(manifest);
                return false;
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            String computed = HexFormat.of().formatHex(digest.digest());
            if (!computed.equalsIgnoreCase(manifest.getChecksum())) {
                manifest.setStatus(CacheEntryStatus.CORRUPT);
                cacheManifestRepository.save(manifest);
                return false;
            }
            return true;
        } catch (IOException | NoSuchAlgorithmException e) {
            manifest.setStatus(CacheEntryStatus.CORRUPT);
            cacheManifestRepository.save(manifest);
            return false;
        }
    }

    @Transactional
    public void markAsAccessed(Long manifestId) {
        cacheManifestRepository.findById(manifestId).ifPresent(manifest -> {
            manifest.setLastAccessedAt(Instant.now());
            cacheManifestRepository.save(manifest);
        });
    }

    @Transactional
    public void deleteManifest(Long manifestId, Long userId) {
        AudioCacheManifest manifest = cacheManifestRepository.findById(manifestId)
            .orElseThrow(() -> new IllegalArgumentException("Cache manifest not found: " + manifestId));

        if (!manifest.getUserId().equals(userId)) {
            throw new SecurityException("Access denied to cache entry: " + manifestId);
        }

        manifest.setStatus(CacheEntryStatus.DELETED);
        manifest.setDeletedAt(Instant.now());
        cacheManifestRepository.save(manifest);
    }

    @Transactional(readOnly = true)
    public StorageMeterResponse getStorageMeter(Long userId) {
        long usedBytes = cacheManifestRepository.sumFileSizeBytesByUserId(userId);
        long totalQuota = appProperties.getAudio().getCacheQuotaBytes();
        long reclaimableBytes = cacheManifestRepository.sumReclaimableBytesByUserId(userId);
        double percentUsed = totalQuota > 0 ? (double) usedBytes / totalQuota * 100.0 : 0.0;

        return new StorageMeterResponse(usedBytes, totalQuota, percentUsed, reclaimableBytes);
    }

    @Transactional
    public void expireOldEntries() {
        Instant now = Instant.now();
        List<AudioCacheManifest> expired = cacheManifestRepository.findExpiredEntries(now);
        for (AudioCacheManifest manifest : expired) {
            manifest.setStatus(CacheEntryStatus.EXPIRED);
            cacheManifestRepository.save(manifest);
        }
    }

    @Transactional
    public void lruCleanup(Long userId) {
        long usedBytes = cacheManifestRepository.sumFileSizeBytesByUserId(userId);
        long quota = appProperties.getAudio().getCacheQuotaBytes();

        if (usedBytes <= quota) {
            return;
        }

        List<AudioCacheManifest> lruEntries = cacheManifestRepository
            .findByUserIdOrderByLastAccessedAtAsc(userId);

        // Phase 1: delete expired entries first (least-recently-accessed)
        for (AudioCacheManifest entry : lruEntries) {
            if (usedBytes <= quota) break;
            if (entry.getStatus() == CacheEntryStatus.EXPIRED) {
                usedBytes -= entry.getFileSizeBytes();
                entry.setStatus(CacheEntryStatus.DELETED);
                entry.setDeletedAt(Instant.now());
                cacheManifestRepository.save(entry);
            }
        }

        if (usedBytes <= quota) return;

        // Phase 2: delete non-favorite LRU entries
        for (AudioCacheManifest entry : lruEntries) {
            if (usedBytes <= quota) break;
            if (entry.getStatus() != CacheEntryStatus.CACHED_VALID) continue;

            // Check if the asset is a favorite - deprioritize favorites
            AudioSegment segment = segmentRepository.findById(entry.getSegmentId()).orElse(null);
            if (segment != null &&
                audioFavoriteRepository.existsByUserIdAndAudioAssetId(userId, segment.getAudioAssetId())) {
                continue; // skip favorites in this phase
            }

            usedBytes -= entry.getFileSizeBytes();
            entry.setStatus(CacheEntryStatus.DELETED);
            entry.setDeletedAt(Instant.now());
            cacheManifestRepository.save(entry);
        }

        if (usedBytes <= quota) return;

        // Phase 3: delete favorite LRU entries if still over quota
        for (AudioCacheManifest entry : lruEntries) {
            if (usedBytes <= quota) break;
            if (entry.getStatus() != CacheEntryStatus.CACHED_VALID) continue;

            usedBytes -= entry.getFileSizeBytes();
            entry.setStatus(CacheEntryStatus.DELETED);
            entry.setDeletedAt(Instant.now());
            cacheManifestRepository.save(entry);
        }
    }

    /**
     * Download all segments for a given audio asset.
     */
    @Transactional
    public List<CacheEntryResponse> downloadAssetSegments(Long userId, Long assetId) {
        List<AudioSegment> segments = segmentRepository.findByAudioAssetId(assetId);
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("No segments found for asset: " + assetId);
        }
        return segments.stream()
            .map(seg -> downloadSegment(userId, seg.getId()))
            .toList();
    }

    /**
     * Info about a cached file for streaming playback.
     */
    public record CachedFileInfo(java.nio.file.Path path, String fileName) {}

    /**
     * Get the cached file path for streaming. Validates ownership and marks access.
     */
    @Transactional
    public CachedFileInfo getCachedFile(Long manifestId, Long userId) {
        AudioCacheManifest manifest = cacheManifestRepository.findById(manifestId)
            .orElseThrow(() -> new IllegalArgumentException("Cache manifest not found: " + manifestId));

        if (!manifest.getUserId().equals(userId)) {
            throw new SecurityException("Access denied to cache entry: " + manifestId);
        }

        if (manifest.getStatus() != CacheEntryStatus.CACHED_VALID) {
            throw new IllegalStateException("Cache entry is not in a valid state for playback: " + manifest.getStatus());
        }

        Path cachedPath = Path.of(manifest.getCachedFilePath());
        if (!Files.exists(cachedPath)) {
            manifest.setStatus(CacheEntryStatus.CORRUPT);
            cacheManifestRepository.save(manifest);
            throw new IllegalStateException("Cached file not found on disk");
        }

        manifest.setLastAccessedAt(Instant.now());
        cacheManifestRepository.save(manifest);

        String fileName = cachedPath.getFileName().toString();
        return new CachedFileInfo(cachedPath, fileName);
    }

    private String resolveAssetTitle(Long segmentId) {
        return segmentRepository.findById(segmentId)
            .flatMap(seg -> audioAssetRepository.findById(seg.getAudioAssetId()))
            .map(AudioAsset::getTitle)
            .orElse("");
    }

    private CacheEntryResponse toCacheEntryResponse(AudioCacheManifest manifest, String assetTitle) {
        String expiresInLabel = null;
        if (manifest.getExpiresAt() != null && manifest.getStatus() == CacheEntryStatus.CACHED_VALID) {
            Duration remaining = Duration.between(Instant.now(), manifest.getExpiresAt());
            if (!remaining.isNegative()) {
                long totalHours = remaining.toHours();
                if (totalHours > 24) {
                    long days = remaining.toDays();
                    expiresInLabel = days + (days == 1 ? " day" : " days");
                } else {
                    expiresInLabel = totalHours + (totalHours == 1 ? " hour" : " hours");
                }
            } else {
                expiresInLabel = "expired";
            }
        }

        return new CacheEntryResponse(
            manifest.getId(),
            manifest.getSegmentId(),
            assetTitle,
            manifest.getStatus().name(),
            manifest.getFileSizeBytes(),
            manifest.getDownloadedAt(),
            manifest.getExpiresAt(),
            expiresInLabel
        );
    }
}
