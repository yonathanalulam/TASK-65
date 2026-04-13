package com.culinarycoach.security.auth;

import com.culinarycoach.audit.AuditEventType;
import com.culinarycoach.audit.TraceContext;
import com.culinarycoach.config.AppProperties;
import com.culinarycoach.domain.entity.LoginAttempt;
import com.culinarycoach.domain.entity.User;
import com.culinarycoach.domain.repository.LoginAttemptRepository;
import com.culinarycoach.domain.repository.UserRepository;
import com.culinarycoach.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;
    private final UserRepository userRepository;
    private final AppProperties appProperties;
    private final AuditService auditService;

    public LoginAttemptService(LoginAttemptRepository loginAttemptRepository,
                                UserRepository userRepository,
                                AppProperties appProperties,
                                AuditService auditService) {
        this.loginAttemptRepository = loginAttemptRepository;
        this.userRepository = userRepository;
        this.appProperties = appProperties;
        this.auditService = auditService;
    }

    public boolean isAccountLocked(User user) {
        if (!user.isAccountLocked()) {
            return false;
        }
        if (user.getLockExpiresAt() != null && Instant.now().isAfter(user.getLockExpiresAt())) {
            user.setAccountLocked(false);
            user.setLockExpiresAt(null);
            user.setFailedLoginCount(0);
            userRepository.save(user);
            auditService.log(AuditEventType.ACCOUNT_UNLOCKED, user.getId(), user.getUsername(),
                null, null, "Lock expired, auto-unlocked");
            return false;
        }
        return true;
    }

    @Transactional
    public void recordSuccess(String username, String ipAddress, String deviceFingerprint) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setUsername(username.toLowerCase());
        attempt.setIpAddress(ipAddress);
        attempt.setDeviceFingerprint(deviceFingerprint);
        attempt.setSuccess(true);
        attempt.setTraceId(TraceContext.get());
        loginAttemptRepository.save(attempt);

        userRepository.findByUsernameIgnoreCase(username).ifPresent(user -> {
            user.setFailedLoginCount(0);
            user.setLastFailedLoginAt(null);
            userRepository.save(user);
        });
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void recordFailure(String username, String ipAddress, String deviceFingerprint,
                               String reason) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setUsername(username.toLowerCase());
        attempt.setIpAddress(ipAddress);
        attempt.setDeviceFingerprint(deviceFingerprint);
        attempt.setSuccess(false);
        attempt.setFailureReason(reason);
        attempt.setTraceId(TraceContext.get());
        loginAttemptRepository.save(attempt);

        userRepository.findByUsernameIgnoreCase(username).ifPresent(user -> {
            int windowMinutes = appProperties.getSecurity().getFailedAttemptWindowMinutes();
            Instant windowStart = Instant.now().minus(windowMinutes, ChronoUnit.MINUTES);
            long recentFailures = loginAttemptRepository.countRecentFailures(
                username.toLowerCase(), windowStart);

            user.setFailedLoginCount((int) recentFailures);
            user.setLastFailedLoginAt(Instant.now());

            if (recentFailures >= appProperties.getSecurity().getMaxFailedAttempts()) {
                int lockoutMinutes = appProperties.getSecurity().getLockoutDurationMinutes();
                user.setAccountLocked(true);
                user.setLockExpiresAt(Instant.now().plus(lockoutMinutes, ChronoUnit.MINUTES));
                auditService.log(AuditEventType.ACCOUNT_LOCKED, user.getId(), user.getUsername(),
                    ipAddress, null, "Locked after " + recentFailures + " failed attempts");
            }

            userRepository.save(user);
        });
    }

    public boolean isCaptchaRequired(String username, String ipAddress) {
        int windowMinutes = appProperties.getSecurity().getCaptchaFailureWindowMinutes();
        Instant since = Instant.now().minus(windowMinutes, ChronoUnit.MINUTES);

        long loginFailures = loginAttemptRepository.countRecentFailuresByUsernameAndIp(
            username.toLowerCase(), ipAddress, since);

        return loginFailures >= appProperties.getSecurity().getCaptchaFailureThresholdLogin();
    }
}
