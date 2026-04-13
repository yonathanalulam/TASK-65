package com.culinarycoach.web.controller;

import com.culinarycoach.domain.entity.PrivacyAccessLog;
import com.culinarycoach.domain.entity.User;
import com.culinarycoach.domain.repository.PrivacyAccessLogRepository;
import com.culinarycoach.domain.repository.UserRepository;
import com.culinarycoach.web.dto.response.ApiResponse;
import com.culinarycoach.web.dto.response.PrivacyAccessLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/admin/privacy")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminPrivacyController {

    private final PrivacyAccessLogRepository privacyAccessLogRepository;
    private final UserRepository userRepository;

    public AdminPrivacyController(PrivacyAccessLogRepository privacyAccessLogRepository,
                                   UserRepository userRepository) {
        this.privacyAccessLogRepository = privacyAccessLogRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/access-logs")
    public ResponseEntity<ApiResponse<Page<PrivacyAccessLogResponse>>> listAccessLogs(
            @RequestParam(required = false) Long subjectUserId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Map<Long, String> usernameCache = new ConcurrentHashMap<>();

        Page<PrivacyAccessLog> logs;
        if (subjectUserId != null) {
            logs = privacyAccessLogRepository.findBySubjectUserId(subjectUserId, pageable);
        } else {
            logs = privacyAccessLogRepository.findAll(pageable);
        }

        Page<PrivacyAccessLogResponse> responses = logs.map(log -> {
            String viewerUsername = usernameCache.computeIfAbsent(
                log.getViewerUserId(), this::resolveUsername);
            String subjectUsername = usernameCache.computeIfAbsent(
                log.getSubjectUserId(), this::resolveUsername);
            return PrivacyAccessLogResponse.from(log, viewerUsername, subjectUsername);
        });

        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    private String resolveUsername(Long userId) {
        return userRepository.findById(userId)
            .map(User::getUsername)
            .orElse("unknown-" + userId);
    }
}
