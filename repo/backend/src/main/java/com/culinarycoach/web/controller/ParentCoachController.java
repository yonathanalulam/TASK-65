package com.culinarycoach.web.controller;

import com.culinarycoach.security.auth.AuthenticatedUserResolver;
import com.culinarycoach.security.auth.UserPrincipal;
import com.culinarycoach.service.ParentCoachService;
import com.culinarycoach.web.dto.response.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/review")
@PreAuthorize("hasAnyAuthority('ROLE_PARENT_COACH', 'ROLE_ADMIN')")
public class ParentCoachController {

    private final ParentCoachService parentCoachService;
    private final AuthenticatedUserResolver userResolver;

    public ParentCoachController(ParentCoachService parentCoachService,
                                  AuthenticatedUserResolver userResolver) {
        this.parentCoachService = parentCoachService;
        this.userResolver = userResolver;
    }

    @GetMapping("/students")
    public ResponseEntity<ApiResponse<List<ReviewStudentResponse>>> listAssignedStudents(
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        List<ReviewStudentResponse> students = parentCoachService.listAssignedStudents(userId);
        return ResponseEntity.ok(ApiResponse.ok(students));
    }

    @GetMapping("/students/{studentId}/notebook")
    public ResponseEntity<ApiResponse<List<NotebookEntryResponse>>> reviewNotebook(
            @PathVariable Long studentId,
            @RequestParam String reason,
            Authentication authentication) {
        if (reason == null || reason.isBlank()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.fail("MISSING_REASON", "A reason code is required for review access"));
        }
        UserPrincipal principal = userResolver.require(authentication);
        String role = resolveReviewRole(principal);
        List<NotebookEntryResponse> entries = parentCoachService.reviewStudentNotebook(
            principal.getUserId(), role, studentId, reason);
        return ResponseEntity.ok(ApiResponse.ok(entries));
    }

    @GetMapping("/students/{studentId}/attempts")
    public ResponseEntity<ApiResponse<List<AttemptHistoryResponse>>> reviewAttempts(
            @PathVariable Long studentId,
            @RequestParam String reason,
            Authentication authentication) {
        if (reason == null || reason.isBlank()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.fail("MISSING_REASON", "A reason code is required for review access"));
        }
        UserPrincipal principal = userResolver.require(authentication);
        String role = resolveReviewRole(principal);
        List<AttemptHistoryResponse> attempts = parentCoachService.reviewStudentAttemptHistory(
            principal.getUserId(), role, studentId, reason);
        return ResponseEntity.ok(ApiResponse.ok(attempts));
    }

    @GetMapping("/students/{studentId}/cooking-history")
    public ResponseEntity<ApiResponse<List<CookingSessionResponse>>> reviewCookingHistory(
            @PathVariable Long studentId,
            @RequestParam String reason,
            Authentication authentication) {
        if (reason == null || reason.isBlank()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.fail("MISSING_REASON", "A reason code is required for review access"));
        }
        UserPrincipal principal = userResolver.require(authentication);
        String role = resolveReviewRole(principal);
        List<CookingSessionResponse> sessions = parentCoachService.reviewStudentCookingHistory(
            principal.getUserId(), role, studentId, reason);
        return ResponseEntity.ok(ApiResponse.ok(sessions));
    }

    private String resolveReviewRole(UserPrincipal principal) {
        if (principal.hasRole("ROLE_ADMIN")) {
            return "ROLE_ADMIN";
        }
        return "ROLE_PARENT_COACH";
    }
}
