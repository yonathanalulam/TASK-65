package com.culinarycoach.web.controller;

import com.culinarycoach.security.auth.AuthenticatedUserResolver;
import com.culinarycoach.service.CheckoutService;
import com.culinarycoach.service.ReconciliationService;
import com.culinarycoach.web.dto.request.GenerateExportRequest;
import com.culinarycoach.web.dto.request.VoidTransactionRequest;
import com.culinarycoach.web.dto.response.ApiResponse;
import com.culinarycoach.web.dto.response.ReconciliationExportResponse;
import com.culinarycoach.web.dto.response.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/checkout")
public class AdminCheckoutController {

    private final CheckoutService checkoutService;
    private final ReconciliationService reconciliationService;
    private final AuthenticatedUserResolver userResolver;

    public AdminCheckoutController(CheckoutService checkoutService,
                                    ReconciliationService reconciliationService,
                                    AuthenticatedUserResolver userResolver) {
        this.checkoutService = checkoutService;
        this.reconciliationService = reconciliationService;
        this.userResolver = userResolver;
    }

    @PostMapping("/transactions/{id}/void")
    public ResponseEntity<ApiResponse<TransactionResponse>> voidTransaction(
            @PathVariable Long id,
            @Valid @RequestBody VoidTransactionRequest request,
            Authentication authentication) {
        String adminUsername = userResolver.require(authentication).getUsername();
        TransactionResponse response = checkoutService.voidTransaction(id, adminUsername, request.reason());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/reconciliation/export")
    public ResponseEntity<ApiResponse<ReconciliationExportResponse>> generateExport(
            @Valid GenerateExportRequest request,
            Authentication authentication) {
        String adminUsername = userResolver.require(authentication).getUsername();
        ReconciliationExportResponse response =
                reconciliationService.generateExport(request.businessDate(), adminUsername);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/reconciliation/exports")
    public ResponseEntity<ApiResponse<Page<ReconciliationExportResponse>>> listExports(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ReconciliationExportResponse> exports = reconciliationService.listExports(pageable);
        return ResponseEntity.ok(ApiResponse.ok(exports));
    }

    @GetMapping("/reconciliation/exports/{id}")
    public ResponseEntity<ApiResponse<ReconciliationExportResponse>> getExport(
            @PathVariable Long id) {
        ReconciliationExportResponse response = reconciliationService.getExport(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
