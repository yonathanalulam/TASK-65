package com.culinarycoach.web.controller;

import com.culinarycoach.security.auth.AuthenticatedUserResolver;
import com.culinarycoach.service.CheckoutService;
import com.culinarycoach.web.dto.request.InitiateCheckoutRequest;
import com.culinarycoach.web.dto.response.ApiResponse;
import com.culinarycoach.web.dto.response.ProductBundleResponse;
import com.culinarycoach.web.dto.response.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final AuthenticatedUserResolver userResolver;

    public CheckoutController(CheckoutService checkoutService,
                               AuthenticatedUserResolver userResolver) {
        this.checkoutService = checkoutService;
        this.userResolver = userResolver;
    }

    @GetMapping("/bundles")
    public ResponseEntity<ApiResponse<List<ProductBundleResponse>>> listBundles() {
        List<ProductBundleResponse> bundles = checkoutService.listBundles();
        return ResponseEntity.ok(ApiResponse.ok(bundles));
    }

    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<TransactionResponse>> initiateCheckout(
            @Valid @RequestBody InitiateCheckoutRequest request,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        TransactionResponse response = checkoutService.initiateCheckout(userId, request.bundleIds());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/complete/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionResponse>> completeCheckout(
            @PathVariable Long transactionId,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        TransactionResponse response = checkoutService.completeCheckout(transactionId, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> listUserTransactions(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        Page<TransactionResponse> transactions = checkoutService.listUserTransactions(userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(transactions));
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        TransactionResponse response = checkoutService.getTransaction(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
