package com.culinarycoach.security.filter;

import com.culinarycoach.audit.AuditEventType;
import com.culinarycoach.audit.TraceContext;
import com.culinarycoach.config.AppProperties;
import com.culinarycoach.domain.entity.AuthSession;
import com.culinarycoach.domain.repository.AuthSessionRepository;
import com.culinarycoach.security.captcha.RequestFailureTracker;
import com.culinarycoach.security.nonce.NonceService;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class RequestSignatureFilter extends OncePerRequestFilter {

    private static final Set<String> EXEMPT_PATHS = Set.of(
        "/api/v1/auth/login",
        "/api/v1/auth/csrf-token",
        "/api/v1/auth/mfa-verify",
        "/api/v1/captcha/challenge"
    );

    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final NonceService nonceService;
    private final AuthSessionRepository authSessionRepository;
    private final AppProperties appProperties;
    private final AuditService auditService;
    private final RequestFailureTracker requestFailureTracker;
    private final ObjectMapper objectMapper;

    public RequestSignatureFilter(NonceService nonceService,
                                   AuthSessionRepository authSessionRepository,
                                   AppProperties appProperties,
                                   AuditService auditService,
                                   RequestFailureTracker requestFailureTracker,
                                   ObjectMapper objectMapper) {
        this.nonceService = nonceService;
        this.authSessionRepository = authSessionRepository;
        this.appProperties = appProperties;
        this.auditService = auditService;
        this.requestFailureTracker = requestFailureTracker;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Exempt paths skip signing
        if (EXEMPT_PATHS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Read-only methods skip signing
        if (READ_METHODS.contains(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // State-changing methods REQUIRE signature headers
        String timestamp = request.getHeader("X-Timestamp");
        String nonce = request.getHeader("X-Nonce");
        String signature = request.getHeader("X-Signature");
        String sessionId = request.getHeader("X-Session-Id");

        // All four headers are REQUIRED for state-changing requests
        if (timestamp == null || nonce == null || signature == null || sessionId == null) {
            rejectRequest(request, response, "SIGNATURE_REQUIRED",
                "Signed request headers required for state-changing operations");
            return;
        }

        // Validate timestamp within window
        try {
            Instant requestTime = Instant.parse(timestamp);
            int validityMinutes = appProperties.getSecurity().getSignatureValidityMinutes();
            if (requestTime.isBefore(Instant.now().minus(validityMinutes, ChronoUnit.MINUTES))) {
                rejectRequest(request, response, "SIGNATURE_EXPIRED", "Request timestamp too old");
                return;
            }
            if (requestTime.isAfter(Instant.now().plus(1, ChronoUnit.MINUTES))) {
                rejectRequest(request, response, "SIGNATURE_FUTURE", "Request timestamp in the future");
                return;
            }
        } catch (Exception e) {
            rejectRequest(request, response, "INVALID_TIMESTAMP", "Cannot parse timestamp");
            return;
        }

        // Validate session exists and has a signing key
        AuthSession session = authSessionRepository.findById(sessionId).orElse(null);
        if (session == null || session.getSigningKey() == null || !session.isActive()) {
            rejectRequest(request, response, "INVALID_SESSION", "Invalid or expired session for signing");
            return;
        }

        // Validate nonce (single-use per session)
        if (!nonceService.validateAndConsumeNonce(nonce, sessionId)) {
            auditService.logSync(AuditEventType.NONCE_REPLAY_DETECTED, session.getUserId(), null,
                request.getRemoteAddr(), null, "Nonce replay: " + nonce);
            rejectRequest(request, response, "NONCE_REPLAY", "Duplicate nonce detected");
            return;
        }

        // Validate HMAC signature using the canonical signed path
        String canonicalPath = canonicalSignedPath(request);
        String expectedPayload = method + "\n" + canonicalPath + "\n" + timestamp + "\n" + nonce;
        String expectedSignature = computeHmac(expectedPayload, session.getSigningKey());
        if (!MessageDigest.isEqual(
                signature.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8))) {
            auditService.logSync(AuditEventType.SIGNATURE_INVALID, session.getUserId(), null,
                request.getRemoteAddr(), null, "Invalid signature for session: " + sessionId);
            rejectRequest(request, response, "SIGNATURE_INVALID", "Request signature verification failed");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Returns the canonical path used for HMAC signature computation.
     *
     * Contract: the signed path is always the full servlet request URI
     * (e.g. {@code /api/v1/auth/change-password}). The frontend's
     * {@code canonicalSigningPath()} function must produce the same value
     * by prepending the {@code /api/v1} base when the axios-relative URL
     * does not already include it.
     */
    static String canonicalSignedPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    private String computeHmac(String data, String keyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC computation failed", e);
        }
    }

    private void rejectRequest(HttpServletRequest request, HttpServletResponse response,
                               String code, String message) throws IOException {
        // Track failure for CAPTCHA escalation
        requestFailureTracker.recordFailure(request.getRemoteAddr());
        response.setStatus(403);
        response.setContentType("application/json");
        Map<String, Object> body = Map.of(
            "traceId", TraceContext.get(),
            "success", false,
            "error", Map.of("code", code, "message", message)
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}
