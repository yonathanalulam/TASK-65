package com.culinarycoach.security.auth;

import com.culinarycoach.config.AppProperties;
import com.culinarycoach.domain.entity.PasswordHistory;
import com.culinarycoach.domain.repository.PasswordHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordHistoryServiceTest {

    @Mock private PasswordHistoryRepository repository;

    private PasswordHistoryService service;
    private PasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder(4); // low cost for test speed
        AppProperties props = new AppProperties();
        props.getSecurity().setPasswordHistoryCount(5);
        service = new PasswordHistoryService(repository, encoder, props);
    }

    @Test
    void isPasswordReused_matchesPreviousHash() {
        String encoded = encoder.encode("OldPassword1!");
        PasswordHistory ph = new PasswordHistory();
        ph.setPasswordHash(encoded);

        when(repository.findRecentByUserId(1L)).thenReturn(List.of(ph));

        assertTrue(service.isPasswordReused(1L, "OldPassword1!"));
    }

    @Test
    void isPasswordReused_noMatch() {
        String encoded = encoder.encode("OldPassword1!");
        PasswordHistory ph = new PasswordHistory();
        ph.setPasswordHash(encoded);

        when(repository.findRecentByUserId(1L)).thenReturn(List.of(ph));

        assertFalse(service.isPasswordReused(1L, "DifferentPass1!"));
    }

    @Test
    void isPasswordReused_emptyHistory() {
        when(repository.findRecentByUserId(1L)).thenReturn(List.of());
        assertFalse(service.isPasswordReused(1L, "AnyPassword1!"));
    }

    @Test
    void recordPassword_savesEntry() {
        service.recordPassword(1L, "encoded-hash");
        verify(repository).save(any(PasswordHistory.class));
    }
}
