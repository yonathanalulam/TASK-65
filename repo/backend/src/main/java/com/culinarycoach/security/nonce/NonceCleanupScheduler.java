package com.culinarycoach.security.nonce;

import com.culinarycoach.security.captcha.RequestFailureTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NonceCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(NonceCleanupScheduler.class);

    private final NonceService nonceService;
    private final RequestFailureTracker requestFailureTracker;

    public NonceCleanupScheduler(NonceService nonceService,
                                  RequestFailureTracker requestFailureTracker) {
        this.nonceService = nonceService;
        this.requestFailureTracker = requestFailureTracker;
    }

    @Scheduled(fixedRate = 600000) // every 10 minutes
    public void cleanupExpiredNonces() {
        int deleted = nonceService.cleanupExpiredNonces();
        if (deleted > 0) {
            log.info("Cleaned up {} expired nonces", deleted);
        }
        requestFailureTracker.evictStale();
    }
}
