package com.culinarycoach.web.controller;

import com.culinarycoach.security.auth.AuthenticatedUserResolver;
import com.culinarycoach.service.FavoriteService;
import com.culinarycoach.web.dto.response.ApiResponse;
import com.culinarycoach.web.dto.response.AudioAssetResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/audio/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final AuthenticatedUserResolver userResolver;

    public FavoriteController(FavoriteService favoriteService,
                               AuthenticatedUserResolver userResolver) {
        this.favoriteService = favoriteService;
        this.userResolver = userResolver;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AudioAssetResponse>>> getUserFavorites(
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        List<AudioAssetResponse> favorites = favoriteService.getUserFavorites(userId);
        return ResponseEntity.ok(ApiResponse.ok(favorites));
    }

    @PostMapping("/{assetId}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> addFavorite(
            @PathVariable Long assetId,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        favoriteService.addFavorite(userId, assetId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("favorited", true)));
    }

    @DeleteMapping("/{assetId}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> removeFavorite(
            @PathVariable Long assetId,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        favoriteService.removeFavorite(userId, assetId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("favorited", false)));
    }
}
