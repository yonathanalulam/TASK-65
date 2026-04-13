package com.culinarycoach.security.nonce;

import com.culinarycoach.config.AppProperties;
import com.culinarycoach.domain.repository.NonceEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NonceServiceTest {

    @Mock private NonceEntryRepository nonceEntryRepository;

    private NonceService service;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.getSecurity().setSignatureValidityMinutes(5);
        service = new NonceService(nonceEntryRepository, props);
    }

    @Test
    void freshNonce_accepted() {
        when(nonceEntryRepository.existsByNonce("nonce-1")).thenReturn(false);
        when(nonceEntryRepository.save(any())).thenReturn(null);

        assertTrue(service.validateAndConsumeNonce("nonce-1", "session-1"));
    }

    @Test
    void duplicateNonce_inDb_rejected() {
        when(nonceEntryRepository.existsByNonce("nonce-2")).thenReturn(true);

        assertFalse(service.validateAndConsumeNonce("nonce-2", "session-1"));
    }

    @Test
    void duplicateNonce_inMemory_rejected() {
        when(nonceEntryRepository.existsByNonce("nonce-3")).thenReturn(false);
        when(nonceEntryRepository.save(any())).thenReturn(null);

        assertTrue(service.validateAndConsumeNonce("nonce-3", "session-1"));
        // Second attempt with same nonce should fail (in-memory check)
        assertFalse(service.validateAndConsumeNonce("nonce-3", "session-1"));
    }

    @Test
    void nullNonce_rejected() {
        assertFalse(service.validateAndConsumeNonce(null, "session-1"));
    }

    @Test
    void blankNonce_rejected() {
        assertFalse(service.validateAndConsumeNonce("", "session-1"));
    }
}
