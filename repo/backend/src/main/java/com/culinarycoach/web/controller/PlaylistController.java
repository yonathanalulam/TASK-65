package com.culinarycoach.web.controller;

import com.culinarycoach.security.auth.AuthenticatedUserResolver;
import com.culinarycoach.service.PlaylistService;
import com.culinarycoach.web.dto.request.AddPlaylistItemRequest;
import com.culinarycoach.web.dto.request.CreatePlaylistRequest;
import com.culinarycoach.web.dto.request.ReorderPlaylistRequest;
import com.culinarycoach.web.dto.request.UpdatePlaylistRequest;
import com.culinarycoach.web.dto.response.ApiResponse;
import com.culinarycoach.web.dto.response.PlaylistDetailResponse;
import com.culinarycoach.web.dto.response.PlaylistResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audio/playlists")
public class PlaylistController {

    private final PlaylistService playlistService;
    private final AuthenticatedUserResolver userResolver;

    public PlaylistController(PlaylistService playlistService,
                               AuthenticatedUserResolver userResolver) {
        this.playlistService = playlistService;
        this.userResolver = userResolver;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PlaylistResponse>>> getUserPlaylists(
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        List<PlaylistResponse> playlists = playlistService.getUserPlaylists(userId);
        return ResponseEntity.ok(ApiResponse.ok(playlists));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlaylistDetailResponse>> getPlaylistDetail(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        PlaylistDetailResponse detail = playlistService.getPlaylistDetail(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PlaylistResponse>> createPlaylist(
            @Valid @RequestBody CreatePlaylistRequest request,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        PlaylistResponse playlist = playlistService.createPlaylist(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(playlist));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PlaylistResponse>> updatePlaylist(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePlaylistRequest request,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        PlaylistResponse playlist = playlistService.updatePlaylist(id, userId, request);
        return ResponseEntity.ok(ApiResponse.ok(playlist));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePlaylist(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        playlistService.deletePlaylist(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<ApiResponse<Void>> addItem(
            @PathVariable Long id,
            @Valid @RequestBody AddPlaylistItemRequest request,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        playlistService.addItem(id, userId, request.audioAssetId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping("/{id}/items/{assetId}")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @PathVariable Long id,
            @PathVariable Long assetId,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        playlistService.removeItem(id, userId, assetId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PutMapping("/{id}/items/reorder")
    public ResponseEntity<ApiResponse<Void>> reorderItems(
            @PathVariable Long id,
            @Valid @RequestBody ReorderPlaylistRequest request,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        playlistService.reorderItems(id, userId, request.orderedAssetIds());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

}
