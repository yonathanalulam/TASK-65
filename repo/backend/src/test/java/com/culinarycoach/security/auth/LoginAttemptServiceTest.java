package com.culinarycoach.security.auth;

import com.culinarycoach.config.AppProperties;
import com.culinarycoach.domain.entity.User;
import com.culinarycoach.domain.enums.AccountStatus;
import com.culinarycoach.domain.repository.LoginAttemptRepository;
import com.culinarycoach.domain.repository.UserRepository;
import com.culinarycoach.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock private LoginAttemptRepository loginAttemptRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;

    private LoginAttemptService service;
    private AppProperties appProperties;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.getSecurity().setMaxFailedAttempts(5);
        appProperties.getSecurity().setFailedAttemptWindowMinutes(30);
        appProperties.getSecurity().setLockoutDurationMinutes(15);
        appProperties.getSecurity().setCaptchaFailureThresholdLogin(3);
        appProperties.getSecurity().setCaptchaFailureWindowMinutes(10);
        service = new LoginAttemptService(loginAttemptRepository, userRepository,
            appProperties, auditService);
    }

    @Test
    void accountNotLocked_returnsFalse() {
        User user = new User();
        user.setAccountLocked(false);
        assertFalse(service.isAccountLocked(user));
    }

    @Test
    void accountLocked_notExpired_returnsTrue() {
        User user = new User();
        user.setAccountLocked(true);
        user.setLockExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        assertTrue(service.isAccountLocked(user));
    }

    @Test
    void accountLocked_expired_autoUnlocks() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setAccountLocked(true);
        user.setLockExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));

        when(userRepository.save(any(User.class))).thenReturn(user);

        assertFalse(service.isAccountLocked(user));
        assertFalse(user.isAccountLocked());
        assertEquals(0, user.getFailedLoginCount());
    }

    @Test
    void recordSuccess_resetsFailureCount() {
        User user = new User();
        user.setUsername("testuser");
        user.setFailedLoginCount(3);
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        service.recordSuccess("testuser", "127.0.0.1", "fp1");

        assertEquals(0, user.getFailedLoginCount());
    }

    @Test
    void recordFailure_locksAccountAfterThreshold() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(user));
        when(loginAttemptRepository.countRecentFailures(eq("testuser"), any(Instant.class)))
            .thenReturn(5L);
        when(userRepository.save(any())).thenReturn(user);

        service.recordFailure("testuser", "127.0.0.1", "fp1", "INVALID_PASSWORD");

        assertTrue(user.isAccountLocked());
        assertNotNull(user.getLockExpiresAt());
    }

    @Test
    void captchaRequired_afterThreeFailures() {
        when(loginAttemptRepository.countRecentFailuresByUsernameAndIp(
            eq("testuser"), eq("127.0.0.1"), any(Instant.class)))
            .thenReturn(3L);

        assertTrue(service.isCaptchaRequired("testuser", "127.0.0.1"));
    }

    @Test
    void captchaNotRequired_belowThreshold() {
        when(loginAttemptRepository.countRecentFailuresByUsernameAndIp(
            eq("testuser"), eq("127.0.0.1"), any(Instant.class)))
            .thenReturn(2L);

        assertFalse(service.isCaptchaRequired("testuser", "127.0.0.1"));
    }
}
