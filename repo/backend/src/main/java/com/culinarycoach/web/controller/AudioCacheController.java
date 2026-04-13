package com.culinarycoach.web.controller;

import com.culinarycoach.security.auth.AuthenticatedUserResolver;
import com.culinarycoach.service.AudioCacheService;
import com.culinarycoach.web.dto.response.ApiResponse;
import com.culinarycoach.web.dto.response.CacheEntryResponse;
import com.culinarycoach.web.dto.response.StorageMeterResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audio/cache")
public class AudioCacheController {

    private final AudioCacheService audioCacheService;
    private final AuthenticatedUserResolver userResolver;

    public AudioCacheController(AudioCacheService audioCacheService,
                                 AuthenticatedUserResolver userResolver) {
        this.audioCacheService = audioCacheService;
        this.userResolver = userResolver;
    }

    @PostMapping("/download/{segmentId}")
    public ResponseEntity<ApiResponse<CacheEntryResponse>> downloadSegment(
            @PathVariable Long segmentId,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        CacheEntryResponse entry = audioCacheService.downloadSegment(userId, segmentId);
        return ResponseEntity.ok(ApiResponse.ok(entry));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<List<CacheEntryResponse>>> getCacheStatus(
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        List<CacheEntryResponse> entries = audioCacheService.getCacheStatus(userId);
        return ResponseEntity.ok(ApiResponse.ok(entries));
    }

    @DeleteMapping("/{manifestId}")
    public ResponseEntity<ApiResponse<Void>> deleteManifest(
            @PathVariable Long manifestId,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        audioCacheService.deleteManifest(manifestId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/storage-meter")
    public ResponseEntity<ApiResponse<StorageMeterResponse>> getStorageMeter(
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        StorageMeterResponse meter = audioCacheService.getStorageMeter(userId);
        return ResponseEntity.ok(ApiResponse.ok(meter));
    }

    /**
     * Download all segments for a given audio asset into the cache.
     */
    @PostMapping("/download-asset/{assetId}")
    public ResponseEntity<ApiResponse<List<CacheEntryResponse>>> downloadAsset(
            @PathVariable Long assetId,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        List<CacheEntryResponse> entries = audioCacheService.downloadAssetSegments(userId, assetId);
        return ResponseEntity.ok(ApiResponse.ok(entries));
    }

    /**
     * Stream a cached audio file for playback.
     */
    @GetMapping("/{manifestId}/stream")
    public ResponseEntity<Resource> streamCachedSegment(
            @PathVariable Long manifestId,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        AudioCacheService.CachedFileInfo fileInfo = audioCacheService.getCachedFile(manifestId, userId);
        Resource resource = new FileSystemResource(fileInfo.path());

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileInfo.fileName() + "\"")
            .body(resource);
    }
}
