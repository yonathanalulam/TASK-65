package com.culinarycoach.web.controller;

import com.culinarycoach.security.auth.AuthenticatedUserResolver;
import com.culinarycoach.service.TimerService;
import com.culinarycoach.web.dto.request.CreateTimerRequest;
import com.culinarycoach.web.dto.response.ApiResponse;
import com.culinarycoach.web.dto.response.TimerResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cooking/sessions/{sessionId}/timers")
public class TimerController {

    private final TimerService timerService;
    private final AuthenticatedUserResolver userResolver;

    public TimerController(TimerService timerService,
                            AuthenticatedUserResolver userResolver) {
        this.timerService = timerService;
        this.userResolver = userResolver;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TimerResponse>> createTimer(
            @PathVariable Long sessionId,
            @Valid @RequestBody CreateTimerRequest request,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        TimerResponse response = timerService.createTimer(
            sessionId, userId, request.stepId(), request.label(), request.durationSeconds());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{timerId}/pause")
    public ResponseEntity<ApiResponse<TimerResponse>> pauseTimer(
            @PathVariable Long sessionId,
            @PathVariable Long timerId,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        TimerResponse response = timerService.pauseTimer(sessionId, timerId, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{timerId}/resume")
    public ResponseEntity<ApiResponse<TimerResponse>> resumeTimer(
            @PathVariable Long sessionId,
            @PathVariable Long timerId,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        TimerResponse response = timerService.resumeTimer(sessionId, timerId, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{timerId}/cancel")
    public ResponseEntity<ApiResponse<TimerResponse>> cancelTimer(
            @PathVariable Long sessionId,
            @PathVariable Long timerId,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        TimerResponse response = timerService.cancelTimer(sessionId, timerId, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{timerId}/acknowledge")
    public ResponseEntity<ApiResponse<TimerResponse>> acknowledgeTimer(
            @PathVariable Long sessionId,
            @PathVariable Long timerId,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        TimerResponse response = timerService.acknowledgeTimer(sessionId, timerId, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{timerId}/dismiss")
    public ResponseEntity<ApiResponse<TimerResponse>> dismissTimer(
            @PathVariable Long sessionId,
            @PathVariable Long timerId,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        TimerResponse response = timerService.dismissTimer(sessionId, timerId, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

}
