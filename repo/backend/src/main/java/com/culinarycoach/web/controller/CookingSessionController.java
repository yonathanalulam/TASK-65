package com.culinarycoach.web.controller;

import com.culinarycoach.domain.enums.CookingSessionStatus;
import com.culinarycoach.security.auth.AuthenticatedUserResolver;
import com.culinarycoach.service.CookingSessionService;
import com.culinarycoach.web.dto.request.StartSessionRequest;
import com.culinarycoach.web.dto.response.ApiResponse;
import com.culinarycoach.web.dto.response.CookingSessionDetailResponse;
import com.culinarycoach.web.dto.response.CookingSessionResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cooking/sessions")
public class CookingSessionController {

    private final CookingSessionService cookingSessionService;
    private final AuthenticatedUserResolver userResolver;

    public CookingSessionController(CookingSessionService cookingSessionService,
                                     AuthenticatedUserResolver userResolver) {
        this.cookingSessionService = cookingSessionService;
        this.userResolver = userResolver;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CookingSessionResponse>> startSession(
            @Valid @RequestBody StartSessionRequest request,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        CookingSessionResponse response = cookingSessionService.startSession(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CookingSessionResponse>>> listSessions(
            @RequestParam(required = false) List<CookingSessionStatus> statuses,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        List<CookingSessionResponse> sessions = cookingSessionService.listSessions(userId, statuses);
        return ResponseEntity.ok(ApiResponse.ok(sessions));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CookingSessionDetailResponse>> getSession(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        CookingSessionDetailResponse detail = cookingSessionService.getSession(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<ApiResponse<CookingSessionDetailResponse>> resumeSession(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        CookingSessionDetailResponse detail = cookingSessionService.resumeSession(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<ApiResponse<CookingSessionResponse>> pauseSession(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        CookingSessionResponse response = cookingSessionService.pauseSession(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/abandon")
    public ResponseEntity<ApiResponse<CookingSessionResponse>> abandonSession(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        CookingSessionResponse response = cookingSessionService.abandonSession(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/steps/{stepOrder}/complete")
    public ResponseEntity<ApiResponse<CookingSessionDetailResponse>> completeStep(
            @PathVariable Long id,
            @PathVariable int stepOrder,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        CookingSessionDetailResponse detail = cookingSessionService.completeStep(id, stepOrder, userId);
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

}
