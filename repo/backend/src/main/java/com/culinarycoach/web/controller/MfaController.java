package com.culinarycoach.web.controller;

import com.culinarycoach.audit.AuditEventType;
import com.culinarycoach.security.auth.AuthenticatedUserResolver;
import com.culinarycoach.security.mfa.TotpService;
import com.culinarycoach.service.AuditService;
import com.culinarycoach.web.dto.request.MfaVerifyRequest;
import com.culinarycoach.web.dto.response.ApiResponse;
import com.culinarycoach.web.dto.response.MfaSetupResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/mfa")
public class MfaController {

    private final TotpService totpService;
    private final AuditService auditService;
    private final AuthenticatedUserResolver userResolver;

    public MfaController(TotpService totpService,
                          AuditService auditService,
                          AuthenticatedUserResolver userResolver) {
        this.totpService = totpService;
        this.auditService = auditService;
        this.userResolver = userResolver;
    }

    @PostMapping("/setup")
    public ResponseEntity<ApiResponse<MfaSetupResponse>> setupMfa(Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        MfaSetupResponse response = totpService.setupMfa(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> verifyMfa(
            @Valid @RequestBody MfaVerifyRequest request,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        String username = userResolver.require(authentication).getUsername();
        boolean success = totpService.verifyAndEnable(userId, request.code());

        if (success) {
            auditService.log(AuditEventType.MFA_ENABLED, userId, username,
                null, null, "MFA enabled via TOTP");
        } else {
            auditService.log(AuditEventType.MFA_VERIFY_FAILURE, userId, username,
                null, null, "Invalid TOTP code during MFA setup");
        }

        return ResponseEntity.ok(ApiResponse.ok(Map.of("verified", success)));
    }

    @PostMapping("/disable")
    public ResponseEntity<ApiResponse<Void>> disableMfa(Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        String username = userResolver.require(authentication).getUsername();
        totpService.disableMfa(userId);
        auditService.log(AuditEventType.MFA_DISABLED, userId, username,
            null, null, "MFA disabled");
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
