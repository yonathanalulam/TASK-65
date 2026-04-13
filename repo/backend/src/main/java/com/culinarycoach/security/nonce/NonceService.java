package com.culinarycoach.security.nonce;

import com.culinarycoach.config.AppProperties;
import com.culinarycoach.domain.entity.NonceEntry;
import com.culinarycoach.domain.repository.NonceEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NonceService {

    private final NonceEntryRepository nonceEntryRepository;
    private final AppProperties appProperties;
    private final Set<String> recentNonces = ConcurrentHashMap.newKeySet();

    public NonceService(NonceEntryRepository nonceEntryRepository, AppProperties appProperties) {
        this.nonceEntryRepository = nonceEntryRepository;
        this.appProperties = appProperties;
    }

    @Transactional
    public boolean validateAndConsumeNonce(String nonce, String sessionId) {
        if (nonce == null || nonce.isBlank()) return false;

        // Fast check in-memory
        if (recentNonces.contains(nonce)) return false;

        // DB check
        if (nonceEntryRepository.existsByNonce(nonce)) return false;

        // Store nonce
        int validityMinutes = appProperties.getSecurity().getSignatureValidityMinutes();
        NonceEntry entry = new NonceEntry();
        entry.setNonce(nonce);
        entry.setSessionId(sessionId);
        entry.setExpiresAt(Instant.now().plus(validityMinutes, ChronoUnit.MINUTES));
        nonceEntryRepository.save(entry);

        recentNonces.add(nonce);

        return true;
    }

    @Transactional
    public int cleanupExpiredNonces() {
        int deleted = nonceEntryRepository.deleteExpiredNonces(Instant.now());
        // Also trim in-memory set periodically (simplified: just clear it)
        if (recentNonces.size() > 100000) {
            recentNonces.clear();
        }
        return deleted;
    }
}
