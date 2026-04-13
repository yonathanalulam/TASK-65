package com.culinarycoach.web.controller;

import com.culinarycoach.security.auth.AuthenticatedUserResolver;
import com.culinarycoach.service.AudioLibraryService;
import com.culinarycoach.web.dto.response.ApiResponse;
import com.culinarycoach.web.dto.response.AudioAssetResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/audio/assets")
public class AudioLibraryController {

    private final AudioLibraryService audioLibraryService;
    private final AuthenticatedUserResolver userResolver;

    public AudioLibraryController(AudioLibraryService audioLibraryService,
                                   AuthenticatedUserResolver userResolver) {
        this.audioLibraryService = audioLibraryService;
        this.userResolver = userResolver;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AudioAssetResponse>>> browseAssets(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        Page<AudioAssetResponse> assets = audioLibraryService.browseAssets(userId, search, pageable);
        return ResponseEntity.ok(ApiResponse.ok(assets));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AudioAssetResponse>> getAssetDetails(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        AudioAssetResponse asset = audioLibraryService.getAssetDetails(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(asset));
    }
}
