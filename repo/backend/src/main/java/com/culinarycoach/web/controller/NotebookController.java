package com.culinarycoach.web.controller;

import com.culinarycoach.domain.enums.NotebookEntryStatus;
import com.culinarycoach.security.auth.AuthenticatedUserResolver;
import com.culinarycoach.service.NotebookService;
import com.culinarycoach.web.dto.request.AddNoteRequest;
import com.culinarycoach.web.dto.request.AddTagRequest;
import com.culinarycoach.web.dto.response.ApiResponse;
import com.culinarycoach.web.dto.response.NotebookEntryDetailResponse;
import com.culinarycoach.web.dto.response.NotebookEntryResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notebook/entries")
public class NotebookController {

    private final NotebookService notebookService;
    private final AuthenticatedUserResolver userResolver;

    public NotebookController(NotebookService notebookService,
                               AuthenticatedUserResolver userResolver) {
        this.notebookService = notebookService;
        this.userResolver = userResolver;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotebookEntryResponse>>> listEntries(
            @RequestParam(required = false) NotebookEntryStatus status,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        Page<NotebookEntryResponse> entries = notebookService.listEntries(userId, status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(entries));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotebookEntryDetailResponse>> getEntry(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        NotebookEntryDetailResponse detail = notebookService.getEntry(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<ApiResponse<NotebookEntryDetailResponse>> addNote(
            @PathVariable Long id,
            @Valid @RequestBody AddNoteRequest request,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        NotebookEntryDetailResponse detail = notebookService.addNote(id, userId, request.noteText());
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

    @PostMapping("/{id}/tags")
    public ResponseEntity<ApiResponse<NotebookEntryDetailResponse>> addTag(
            @PathVariable Long id,
            @Valid @RequestBody AddTagRequest request,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        NotebookEntryDetailResponse detail = notebookService.addTag(id, userId, request.tagLabel());
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

    @DeleteMapping("/{id}/tags/{tagId}")
    public ResponseEntity<ApiResponse<NotebookEntryDetailResponse>> removeTag(
            @PathVariable Long id,
            @PathVariable Long tagId,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        NotebookEntryDetailResponse detail = notebookService.removeTag(id, userId, tagId);
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

    @PostMapping("/{id}/favorite")
    public ResponseEntity<ApiResponse<NotebookEntryResponse>> toggleFavorite(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        NotebookEntryResponse response = notebookService.toggleFavorite(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<NotebookEntryResponse>> resolveEntry(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        NotebookEntryResponse response = notebookService.resolveEntry(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<NotebookEntryResponse>> archiveEntry(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        NotebookEntryResponse response = notebookService.archiveEntry(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<NotebookEntryResponse>> reactivateEntry(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        NotebookEntryResponse response = notebookService.reactivateEntry(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

}
