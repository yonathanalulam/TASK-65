package com.culinarycoach.web.controller;

import com.culinarycoach.security.auth.AuthenticatedUserResolver;
import com.culinarycoach.service.DrillService;
import com.culinarycoach.web.dto.request.LaunchDrillRequest;
import com.culinarycoach.web.dto.request.SubmitDrillAnswerRequest;
import com.culinarycoach.web.dto.response.ApiResponse;
import com.culinarycoach.web.dto.response.DrillRunResponse;
import com.culinarycoach.web.dto.response.SubmitAnswerResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/drills")
public class DrillController {

    private final DrillService drillService;
    private final AuthenticatedUserResolver userResolver;

    public DrillController(DrillService drillService,
                            AuthenticatedUserResolver userResolver) {
        this.drillService = drillService;
        this.userResolver = userResolver;
    }

    @PostMapping("/retry")
    public ResponseEntity<ApiResponse<DrillRunResponse>> launchRetryDrill(
            @Valid @RequestBody LaunchDrillRequest request,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        DrillRunResponse response = drillService.launchRetryDrill(userId, request.entryId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/similar")
    public ResponseEntity<ApiResponse<DrillRunResponse>> launchSimilarDrill(
            @Valid @RequestBody LaunchDrillRequest request,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        DrillRunResponse response = drillService.launchSimilarDrill(userId, request.entryId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/variant")
    public ResponseEntity<ApiResponse<DrillRunResponse>> launchVariantDrill(
            @Valid @RequestBody LaunchDrillRequest request,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        DrillRunResponse response = drillService.launchVariantDrill(userId, request.entryId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{drillId}/answer")
    public ResponseEntity<ApiResponse<SubmitAnswerResponse>> submitDrillAnswer(
            @PathVariable Long drillId,
            @Valid @RequestBody SubmitDrillAnswerRequest request,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        SubmitAnswerResponse response = drillService.submitDrillAnswer(
            userId, drillId, request.questionId(), request.answer());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{drillId}/complete")
    public ResponseEntity<ApiResponse<DrillRunResponse>> completeDrill(
            @PathVariable Long drillId,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        DrillRunResponse response = drillService.completeDrill(userId, drillId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

}
