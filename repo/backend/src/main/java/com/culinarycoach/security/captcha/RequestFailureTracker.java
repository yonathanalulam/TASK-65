package com.culinarycoach.security.captcha;

import com.culinarycoach.config.AppProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tracks non-login request failures per IP address (rate-limit violations,
 * signature failures, etc.) and decides when CAPTCHA escalation is required.
 *
 * Uses the app.security.captchaFailureThresholdRequest and
 * captchaFailureWindowMinutes properties.
 */
@Service
public class RequestFailureTracker {

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Instant>> failuresByIp = new ConcurrentHashMap<>();
    private final AppProperties appProperties;

    public RequestFailureTracker(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    /**
     * Record a request failure for the given IP address.
     */
    public void recordFailure(String ipAddress) {
        failuresByIp.computeIfAbsent(ipAddress, k -> new CopyOnWriteArrayList<>())
                .add(Instant.now());
    }

    /**
     * Check if CAPTCHA is required for this IP based on recent request failures.
     */
    public boolean isCaptchaRequired(String ipAddress) {
        CopyOnWriteArrayList<Instant> failures = failuresByIp.get(ipAddress);
        if (failures == null) return false;

        int windowMinutes = appProperties.getSecurity().getCaptchaFailureWindowMinutes();
        int threshold = appProperties.getSecurity().getCaptchaFailureThresholdRequest();
        Instant windowStart = Instant.now().minus(windowMinutes, ChronoUnit.MINUTES);

        long recentCount = failures.stream()
                .filter(t -> t.isAfter(windowStart))
                .count();

        return recentCount >= threshold;
    }

    /**
     * Reset failure count for an IP (e.g., after successful CAPTCHA verification).
     */
    public void resetFailures(String ipAddress) {
        failuresByIp.remove(ipAddress);
    }

    /**
     * Evict stale entries to prevent unbounded memory growth.
     * Called periodically by the nonce cleanup scheduler or similar.
     */
    public void evictStale() {
        int windowMinutes = appProperties.getSecurity().getCaptchaFailureWindowMinutes();
        Instant cutoff = Instant.now().minus(windowMinutes * 2L, ChronoUnit.MINUTES);

        failuresByIp.forEach((ip, failures) -> {
            failures.removeIf(t -> t.isBefore(cutoff));
            if (failures.isEmpty()) {
                failuresByIp.remove(ip);
            }
        });
    }
}
