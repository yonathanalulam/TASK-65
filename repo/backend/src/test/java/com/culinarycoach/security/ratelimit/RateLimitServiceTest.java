package com.culinarycoach.security.ratelimit;

import com.culinarycoach.config.AppProperties;
import io.github.bucket4j.ConsumptionProbe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitServiceTest {

    private RateLimitService service;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.getRateLimit().setAuthenticatedPerMinute(5);
        props.getRateLimit().setUnauthenticatedPerMinute(3);
        props.getRateLimit().setAdminPerMinute(4);
        props.getRateLimit().setBurstCapacity(10);
        props.getRateLimit().setBurstPeriodSeconds(2);
        service = new RateLimitService(props);
    }

    @Test
    void authenticatedUser_allowsWithinLimit() {
        for (int i = 0; i < 5; i++) {
            ConsumptionProbe probe = service.tryConsume("user:test", RateLimitService.BucketType.AUTHENTICATED);
            assertTrue(probe.isConsumed(), "Request " + (i + 1) + " should be allowed");
        }
    }

    @Test
    void authenticatedUser_blocksAfterLimit() {
        for (int i = 0; i < 5; i++) {
            service.tryConsume("user:test2", RateLimitService.BucketType.AUTHENTICATED);
        }
        ConsumptionProbe probe = service.tryConsume("user:test2", RateLimitService.BucketType.AUTHENTICATED);
        assertFalse(probe.isConsumed());
    }

    @Test
    void unauthenticatedUser_lowerLimit() {
        for (int i = 0; i < 3; i++) {
            ConsumptionProbe probe = service.tryConsume("ip:127.0.0.1", RateLimitService.BucketType.UNAUTHENTICATED);
            assertTrue(probe.isConsumed());
        }
        ConsumptionProbe probe = service.tryConsume("ip:127.0.0.1", RateLimitService.BucketType.UNAUTHENTICATED);
        assertFalse(probe.isConsumed());
    }

    @Test
    void differentKeys_independentBuckets() {
        for (int i = 0; i < 5; i++) {
            service.tryConsume("user:a", RateLimitService.BucketType.AUTHENTICATED);
        }
        // User B should still have tokens
        ConsumptionProbe probe = service.tryConsume("user:b", RateLimitService.BucketType.AUTHENTICATED);
        assertTrue(probe.isConsumed());
    }
}
