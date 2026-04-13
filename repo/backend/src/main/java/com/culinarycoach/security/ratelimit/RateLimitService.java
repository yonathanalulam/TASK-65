package com.culinarycoach.security.ratelimit;

import com.culinarycoach.config.AppProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AppProperties appProperties;

    public RateLimitService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public ConsumptionProbe tryConsume(String key, BucketType type) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(type));
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    private Bucket createBucket(BucketType type) {
        AppProperties.RateLimit rl = appProperties.getRateLimit();

        int perMinute;
        switch (type) {
            case AUTHENTICATED -> perMinute = rl.getAuthenticatedPerMinute();
            case ADMIN -> perMinute = rl.getAdminPerMinute();
            default -> perMinute = rl.getUnauthenticatedPerMinute();
        }

        return Bucket.builder()
            .addLimit(Bandwidth.builder()
                .capacity(perMinute)
                .refillGreedy(perMinute, Duration.ofMinutes(1))
                .build())
            .addLimit(Bandwidth.builder()
                .capacity(rl.getBurstCapacity())
                .refillGreedy(rl.getBurstCapacity(), Duration.ofSeconds(rl.getBurstPeriodSeconds()))
                .build())
            .build();
    }

    public enum BucketType {
        UNAUTHENTICATED,
        AUTHENTICATED,
        ADMIN
    }
}
