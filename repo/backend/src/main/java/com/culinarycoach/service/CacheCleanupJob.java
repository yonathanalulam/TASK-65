package com.culinarycoach.service;

import com.culinarycoach.domain.entity.AudioCacheManifest;
import com.culinarycoach.domain.enums.CacheEntryStatus;
import com.culinarycoach.domain.repository.AudioCacheManifestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CacheCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(CacheCleanupJob.class);

    private final AudioCacheService audioCacheService;
    private final AudioCacheManifestRepository cacheManifestRepository;

    public CacheCleanupJob(AudioCacheService audioCacheService,
                           AudioCacheManifestRepository cacheManifestRepository) {
        this.audioCacheService = audioCacheService;
        this.cacheManifestRepository = cacheManifestRepository;
    }

    /**
     * Hourly: LRU cleanup for users over quota.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void hourlyLruCleanup() {
        log.info("Running hourly LRU cache cleanup");
        try {
            List<AudioCacheManifest> allCached = cacheManifestRepository
                .findAll()
                .stream()
                .filter(m -> m.getStatus() == CacheEntryStatus.CACHED_VALID)
                .toList();

            List<Long> distinctUserIds = allCached.stream()
                .map(AudioCacheManifest::getUserId)
                .distinct()
                .collect(Collectors.toList());

            for (Long userId : distinctUserIds) {
                try {
                    audioCacheService.lruCleanup(userId);
                } catch (Exception e) {
                    log.error("LRU cleanup failed for user {}: {}", userId, e.getMessage());
                }
            }
            log.info("Hourly LRU cleanup completed for {} users", distinctUserIds.size());
        } catch (Exception e) {
            log.error("Hourly LRU cleanup failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Daily at midnight: expire segments past 30 days validity.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void dailyExpireSegments() {
        log.info("Running daily cache expiry job");
        try {
            audioCacheService.expireOldEntries();
            log.info("Daily cache expiry job completed");
        } catch (Exception e) {
            log.error("Daily cache expiry job failed: {}", e.getMessage(), e);
        }
    }
}
