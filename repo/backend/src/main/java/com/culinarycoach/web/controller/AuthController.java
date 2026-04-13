package com.culinarycoach.web.controller;

import com.culinarycoach.security.auth.AuthenticatedUserResolver;
import com.culinarycoach.security.auth.UserPrincipal;
import com.culinarycoach.service.AuthService;
import com.culinarycoach.web.dto.request.LoginRequest;
import com.culinarycoach.web.dto.request.MfaVerifyRequest;
import com.culinarycoach.web.dto.request.PasswordChangeRequest;
import com.culinarycoach.web.dto.response.ApiResponse;
import com.culinarycoach.web.dto.response.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticatedUserResolver userResolver;

    public AuthController(AuthService authService, AuthenticatedUserResolver userResolver) {
        this.authService = authService;
        this.userResolver = userResolver;
    }

    @GetMapping("/csrf-token")
    public ResponseEntity<ApiResponse<Map<String, String>>> getCsrfToken(HttpServletRequest request) {
        CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrf != null) {
            csrf.getToken();
        }
        return ResponseEntity.ok(ApiResponse.ok(Map.of("status", "csrf_token_set")));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        LoginResponse response = authService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/mfa-verify")
    public ResponseEntity<ApiResponse<LoginResponse>> verifyMfa(
            @Valid @RequestBody MfaVerifyRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        if (request.mfaToken() == null || request.mfaToken().isBlank()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.fail("INVALID_REQUEST", "MFA token is required"));
        }

        LoginResponse response = authService.completeMfaLogin(
            request.mfaToken(), request.code(), ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(Authentication authentication) {
        UserPrincipal principal = userResolver.require(authentication);
        authService.logout(principal.getSessionId());
        return ResponseEntity.ok(ApiResponse.ok("Logged out"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody PasswordChangeRequest request,
            Authentication authentication) {
        Long userId = userResolver.requireUserId(authentication);
        authService.changePassword(userId, request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(Authentication authentication) {
        UserPrincipal principal = userResolver.require(authentication);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
            "userId", principal.getUserId(),
            "username", principal.getUsername(),
            "authorities", principal.getAuthorities().stream()
                .map(a -> a.getAuthority()).toList(),
            "sessionId", principal.getSessionId()
        )));
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
