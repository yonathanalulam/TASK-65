package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.AudioCacheManifest;
import com.culinarycoach.domain.enums.CacheEntryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AudioCacheManifestRepository extends JpaRepository<AudioCacheManifest, Long> {

    List<AudioCacheManifest> findByUserId(Long userId);

    List<AudioCacheManifest> findByUserIdAndStatus(Long userId, CacheEntryStatus status);

    @Query("SELECT COALESCE(SUM(c.fileSizeBytes), 0) FROM AudioCacheManifest c " +
           "WHERE c.userId = :userId AND c.status = 'CACHED_VALID'")
    long sumFileSizeBytesByUserId(@Param("userId") Long userId);

    @Query("SELECT c FROM AudioCacheManifest c " +
           "WHERE c.expiresAt IS NOT NULL AND c.expiresAt < :now AND c.status = 'CACHED_VALID'")
    List<AudioCacheManifest> findExpiredEntries(@Param("now") Instant now);

    @Query("SELECT c FROM AudioCacheManifest c " +
           "WHERE c.userId = :userId AND c.status = 'CACHED_VALID' " +
           "ORDER BY c.lastAccessedAt ASC")
    List<AudioCacheManifest> findByUserIdOrderByLastAccessedAtAsc(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(c.fileSizeBytes), 0) FROM AudioCacheManifest c " +
           "WHERE c.userId = :userId AND c.status IN ('EXPIRED', 'CORRUPT')")
    long sumReclaimableBytesByUserId(@Param("userId") Long userId);
}
