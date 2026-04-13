package com.culinarycoach.web.controller;

import com.culinarycoach.domain.enums.NotificationStatus;
import com.culinarycoach.security.auth.AuthenticatedUserResolver;
import com.culinarycoach.service.NotificationService;
import com.culinarycoach.web.dto.response.ApiResponse;
import com.culinarycoach.web.dto.response.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthenticatedUserResolver userResolver;

    public NotificationController(NotificationService notificationService,
                                   AuthenticatedUserResolver userResolver) {
        this.notificationService = notificationService;
        this.userResolver = userResolver;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> listNotifications(
            @RequestParam(required = false) NotificationStatus status,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        Page<NotificationResponse> notifications = notificationService
            .listNotifications(userId, status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(notifications));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("unreadCount", count)));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        NotificationResponse response = notificationService.markRead(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/dismiss")
    public ResponseEntity<ApiResponse<NotificationResponse>> dismiss(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        NotificationResponse response = notificationService.dismiss(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
