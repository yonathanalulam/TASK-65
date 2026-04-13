package com.culinarycoach.web.controller;

import com.culinarycoach.domain.entity.ParentCoachAssignment;
import com.culinarycoach.domain.repository.ParentCoachAssignmentRepository;
import com.culinarycoach.security.auth.AuthenticatedUserResolver;
import com.culinarycoach.service.ParentCoachService;
import com.culinarycoach.web.dto.request.AssignStudentRequest;
import com.culinarycoach.web.dto.response.ApiResponse;
import com.culinarycoach.web.dto.response.ReviewStudentResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/review")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminReviewController {

    private final ParentCoachService parentCoachService;
    private final ParentCoachAssignmentRepository assignmentRepository;
    private final AuthenticatedUserResolver userResolver;

    public AdminReviewController(ParentCoachService parentCoachService,
                                  ParentCoachAssignmentRepository assignmentRepository,
                                  AuthenticatedUserResolver userResolver) {
        this.parentCoachService = parentCoachService;
        this.assignmentRepository = assignmentRepository;
        this.userResolver = userResolver;
    }

    @PostMapping("/assignments")
    public ResponseEntity<ApiResponse<Void>> assignStudent(
            @Valid @RequestBody AssignStudentRequest request,
            Authentication authentication) {
        String adminUsername = userResolver.require(authentication).getUsername();
        parentCoachService.assignStudent(request.coachUserId(), request.studentUserId(), adminUsername);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping("/assignments")
    public ResponseEntity<ApiResponse<Void>> revokeAssignment(
            @RequestParam Long coachUserId,
            @RequestParam Long studentUserId,
            Authentication authentication) {
        String adminUsername = userResolver.require(authentication).getUsername();
        parentCoachService.revokeAssignment(coachUserId, studentUserId, adminUsername);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/assignments")
    public ResponseEntity<ApiResponse<List<ReviewStudentResponse>>> listAssignments(
            @RequestParam Long coachUserId) {
        List<ReviewStudentResponse> students = parentCoachService.listAssignedStudents(coachUserId);
        return ResponseEntity.ok(ApiResponse.ok(students));
    }
}
