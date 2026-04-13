package com.culinarycoach.security.filter;

import com.culinarycoach.audit.AuditEventType;
import com.culinarycoach.audit.TraceContext;
import com.culinarycoach.security.captcha.CaptchaService;
import com.culinarycoach.security.captcha.RequestFailureTracker;
import com.culinarycoach.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Enforces CAPTCHA on non-login requests after repeated request failures
 * (rate-limit violations, signature failures, etc.) from the same IP.
 *
 * When the RequestFailureTracker indicates CAPTCHA is required for an IP,
 * state-changing requests must include valid X-Captcha-Id and X-Captcha-Answer
 * headers. GET/HEAD/OPTIONS are exempt.
 *
 * This runs after the RateLimitFilter and before the RequestSignatureFilter so
 * that rate-limit rejections are counted and CAPTCHA is enforced early.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 25) // after RateLimitFilter (+20), before SignatureFilter (+30)
public class CaptchaEnforcementFilter extends OncePerRequestFilter {

    private static final Set<String> EXEMPT_PATHS = Set.of(
        "/api/v1/auth/login",       // login has its own CAPTCHA logic
        "/api/v1/auth/csrf-token",
        "/api/v1/auth/mfa-verify",
        "/api/v1/captcha/challenge" // must be reachable to get a CAPTCHA
    );

    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final RequestFailureTracker failureTracker;
    private final CaptchaService captchaService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public CaptchaEnforcementFilter(RequestFailureTracker failureTracker,
                                     CaptchaService captchaService,
                                     AuditService auditService,
                                     ObjectMapper objectMapper) {
        this.failureTracker = failureTracker;
        this.captchaService = captchaService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Exempt paths and read-only methods skip CAPTCHA enforcement
        if (EXEMPT_PATHS.contains(path) || READ_METHODS.contains(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = getClientIp(request);

        if (!failureTracker.isCaptchaRequired(ip)) {
            filterChain.doFilter(request, response);
            return;
        }

        // CAPTCHA is required — check headers
        String captchaId = request.getHeader("X-Captcha-Id");
        String captchaAnswer = request.getHeader("X-Captcha-Answer");

        if (captchaId == null || captchaAnswer == null) {
            auditService.logSync(AuditEventType.CAPTCHA_REQUIRED, null, null,
                ip, null, "CAPTCHA required for request to " + path);
            rejectRequest(response, "CAPTCHA_REQUIRED",
                "CAPTCHA verification required due to repeated request failures");
            return;
        }

        if (!captchaService.verifyCaptcha(captchaId, captchaAnswer)) {
            auditService.logSync(AuditEventType.CAPTCHA_FAILED, null, null,
                ip, null, "Invalid CAPTCHA for request to " + path);
            failureTracker.recordFailure(ip); // count this as another failure
            rejectRequest(response, "CAPTCHA_INVALID", "CAPTCHA verification failed");
            return;
        }

        // Valid CAPTCHA — reset failure count and proceed
        auditService.logSync(AuditEventType.CAPTCHA_VERIFIED, null, null,
            ip, null, "CAPTCHA verified for request to " + path);
        failureTracker.resetFailures(ip);
        filterChain.doFilter(request, response);
    }

    private void rejectRequest(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(403);
        response.setContentType("application/json");
        Map<String, Object> body = Map.of(
            "traceId", TraceContext.get(),
            "success", false,
            "error", Map.of("code", code, "message", message)
        );
        objectMapper.writeValue(response.getWriter(), body);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
