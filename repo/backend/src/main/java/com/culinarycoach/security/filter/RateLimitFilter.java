package com.culinarycoach.security.filter;

import com.culinarycoach.audit.TraceContext;
import com.culinarycoach.security.captcha.RequestFailureTracker;
import com.culinarycoach.security.ratelimit.RateLimitService;
import com.culinarycoach.security.ratelimit.RateLimitService.BucketType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final RequestFailureTracker requestFailureTracker;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimitService rateLimitService,
                           RequestFailureTracker requestFailureTracker,
                           ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.requestFailureTracker = requestFailureTracker;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String key;
        BucketType type;

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            key = "user:" + auth.getName();
            type = isAdmin ? BucketType.ADMIN : BucketType.AUTHENTICATED;
        } else {
            key = "ip:" + getClientIp(request);
            type = BucketType.UNAUTHENTICATED;
        }

        ConsumptionProbe probe = rateLimitService.tryConsume(key, type);

        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            // Track this failure for CAPTCHA escalation
            requestFailureTracker.recordFailure(getClientIp(request));
            long waitSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(Math.max(1, waitSeconds)));

            Map<String, Object> body = new HashMap<>();
            body.put("traceId", TraceContext.get());
            body.put("success", false);
            body.put("data", null);
            body.put("error", Map.of(
                "code", "RATE_LIMITED",
                "message", "Too many requests",
                "details", Map.of("retryAfterSeconds", Math.max(1, waitSeconds))
            ));
            objectMapper.writeValue(response.getWriter(), body);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
