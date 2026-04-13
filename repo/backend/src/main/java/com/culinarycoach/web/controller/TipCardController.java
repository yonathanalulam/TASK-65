package com.culinarycoach.web.controller;

import com.culinarycoach.security.auth.AuthenticatedUserResolver;
import com.culinarycoach.service.TipCardService;
import com.culinarycoach.web.dto.request.ConfigureTipRequest;
import com.culinarycoach.web.dto.response.ApiResponse;
import com.culinarycoach.web.dto.response.TipCardResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/tips")
@PreAuthorize("hasRole('ADMIN')")
public class TipCardController {

    private final TipCardService tipCardService;
    private final AuthenticatedUserResolver userResolver;

    public TipCardController(TipCardService tipCardService,
                              AuthenticatedUserResolver userResolver) {
        this.tipCardService = tipCardService;
        this.userResolver = userResolver;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TipCardResponse>>> listTipCards() {
        List<TipCardResponse> tips = tipCardService.listTipCards();
        return ResponseEntity.ok(ApiResponse.ok(tips));
    }

    @PostMapping("/{id}/configure")
    public ResponseEntity<ApiResponse<TipCardResponse>> configureTipDisplayMode(
            @PathVariable Long id,
            @Valid @RequestBody ConfigureTipRequest request,
            Authentication authentication) {
        String changedBy = userResolver.require(authentication).getUsername();
        TipCardResponse response = tipCardService.configureTipDisplayMode(
            id, request.scope(), request.scopeId(), request.displayMode(), changedBy);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<TipCardResponse>> toggleTipCard(
            @PathVariable Long id,
            Authentication authentication) {
        String changedBy = userResolver.require(authentication).getUsername();
        TipCardResponse response = tipCardService.toggleTipCard(id, changedBy);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
