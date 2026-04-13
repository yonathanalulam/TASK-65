package com.culinarycoach.service;

import com.culinarycoach.audit.AuditEventType;
import com.culinarycoach.config.AppProperties;
import com.culinarycoach.domain.entity.AuthSession;
import com.culinarycoach.domain.entity.DeviceRegistration;
import com.culinarycoach.domain.entity.User;
import com.culinarycoach.domain.enums.AccountStatus;
import com.culinarycoach.domain.enums.SessionStatus;
import com.culinarycoach.domain.repository.AuthSessionRepository;
import com.culinarycoach.domain.repository.DeviceRegistrationRepository;
import com.culinarycoach.domain.repository.UserRepository;
import com.culinarycoach.security.auth.LoginAttemptService;
import com.culinarycoach.security.auth.PasswordHistoryService;
import com.culinarycoach.security.auth.PasswordPolicyValidator;
import com.culinarycoach.security.captcha.CaptchaService;
import com.culinarycoach.security.mfa.TotpService;
import com.culinarycoach.web.dto.request.LoginRequest;
import com.culinarycoach.web.dto.response.LoginResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.KeyGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AuthSessionRepository authSessionRepository;
    private final DeviceRegistrationRepository deviceRegistrationRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final PasswordHistoryService passwordHistoryService;
    private final SessionService sessionService;
    private final AuditService auditService;
    private final AppProperties appProperties;
    private final CaptchaService captchaService;
    private final TotpService totpService;

    /**
     * Short-lived MFA tokens mapping mfaToken -> MfaPendingLogin.
     * Tokens expire after 5 minutes.
     */
    private final ConcurrentHashMap<String, MfaPendingLogin> mfaPendingLogins = new ConcurrentHashMap<>();

    public record MfaPendingLogin(Long userId, String deviceFingerprint, String ipAddress,
                                   String userAgent, Instant expiresAt) {}

    public AuthService(UserRepository userRepository,
                        AuthSessionRepository authSessionRepository,
                        DeviceRegistrationRepository deviceRegistrationRepository,
                        PasswordEncoder passwordEncoder,
                        LoginAttemptService loginAttemptService,
                        PasswordPolicyValidator passwordPolicyValidator,
                        PasswordHistoryService passwordHistoryService,
                        SessionService sessionService,
                        AuditService auditService,
                        AppProperties appProperties,
                        CaptchaService captchaService,
                        TotpService totpService) {
        this.userRepository = userRepository;
        this.authSessionRepository = authSessionRepository;
        this.deviceRegistrationRepository = deviceRegistrationRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.passwordHistoryService = passwordHistoryService;
        this.sessionService = sessionService;
        this.auditService = auditService;
        this.appProperties = appProperties;
        this.captchaService = captchaService;
        this.totpService = totpService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        // CAPTCHA enforcement: check if required BEFORE any credential validation
        if (loginAttemptService.isCaptchaRequired(
                request.username() != null ? request.username() : "", ipAddress)) {
            if (request.captchaId() == null || request.captchaAnswer() == null
                    || !captchaService.verifyCaptcha(request.captchaId(), request.captchaAnswer())) {
                throw new IllegalArgumentException("CAPTCHA verification required and failed");
            }
        }

        User user = userRepository.findByUsernameIgnoreCase(request.username())
            .orElseThrow(() -> {
                loginAttemptService.recordFailure(request.username(), ipAddress,
                    request.deviceFingerprint(), "USER_NOT_FOUND");
                return new BadCredentialsException("Invalid username or password");
            });

        if (user.getStatus() != AccountStatus.ACTIVE) {
            loginAttemptService.recordFailure(request.username(), ipAddress,
                request.deviceFingerprint(), "ACCOUNT_NOT_ACTIVE");
            throw new BadCredentialsException("Invalid username or password");
        }

        if (loginAttemptService.isAccountLocked(user)) {
            throw new LockedException("Account is locked until " + user.getLockExpiresAt());
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptService.recordFailure(request.username(), ipAddress,
                request.deviceFingerprint(), "INVALID_PASSWORD");
            auditService.log(AuditEventType.LOGIN_FAILURE, user.getId(), user.getUsername(),
                ipAddress, userAgent, "Invalid password");
            throw new BadCredentialsException("Invalid username or password");
        }

        // Password valid. If MFA enabled, issue challenge token (NOT a session).
        if (user.isMfaEnabled()) {
            String mfaToken = UUID.randomUUID().toString();
            mfaPendingLogins.put(mfaToken, new MfaPendingLogin(
                user.getId(), request.deviceFingerprint(), ipAddress, userAgent,
                Instant.now().plus(5, ChronoUnit.MINUTES)));
            auditService.log(AuditEventType.LOGIN_SUCCESS, user.getId(), user.getUsername(),
                ipAddress, userAgent, "Password verified, MFA challenge issued");
            return LoginResponse.mfaRequired(mfaToken);
        }

        // No MFA: complete login
        return completeLogin(user, request.deviceFingerprint(), ipAddress, userAgent);
    }

    @Transactional
    public LoginResponse completeMfaLogin(String mfaToken, String code, String ipAddress, String userAgent) {
        MfaPendingLogin pending = mfaPendingLogins.remove(mfaToken);
        if (pending == null || pending.expiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("MFA token expired or invalid");
        }

        User user = userRepository.findById(pending.userId())
            .orElseThrow(() -> new BadCredentialsException("User not found"));

        // Try TOTP code first, then recovery code
        boolean valid = totpService.verifyCode(user.getId(), code);
        if (!valid) {
            valid = totpService.verifyRecoveryCode(user.getId(), code);
        }

        if (!valid) {
            // Put the pending login back for retry (until expiry)
            if (pending.expiresAt().isAfter(Instant.now())) {
                mfaPendingLogins.put(mfaToken, pending);
            }
            auditService.log(AuditEventType.MFA_VERIFY_FAILURE, user.getId(), user.getUsername(),
                ipAddress, userAgent, "Invalid MFA code");
            throw new BadCredentialsException("Invalid MFA code");
        }

        auditService.log(AuditEventType.MFA_VERIFY_SUCCESS, user.getId(), user.getUsername(),
            ipAddress, userAgent, "MFA verification succeeded");

        return completeLogin(user, pending.deviceFingerprint(), ipAddress, userAgent);
    }

    @Transactional
    public LoginResponse completeLogin(User user, String deviceFingerprint,
                                        String ipAddress, String userAgent) {
        loginAttemptService.recordSuccess(user.getUsername(), ipAddress, deviceFingerprint);

        DeviceRegistration device = getOrCreateDevice(user.getId(), deviceFingerprint, userAgent);
        sessionService.enforceSessionLimits(user.getId(), device != null ? device.getId() : null);

        String signingKey = generateSigningKey();
        AuthSession session = new AuthSession();
        session.setId(UUID.randomUUID().toString());
        session.setUserId(user.getId());
        session.setDeviceId(device != null ? device.getId() : null);
        session.setSigningKey(signingKey);
        session.setStatus(SessionStatus.ACTIVE);
        session.setExpiresAt(Instant.now().plus(
            appProperties.getSecurity().getAbsoluteLifetimeHours(), ChronoUnit.HOURS));
        session.setIdleTimeoutMinutes(appProperties.getSecurity().getIdleTimeoutMinutes());
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        authSessionRepository.save(session);

        Set<String> roles = user.getRoles().stream()
            .map(r -> r.getName())
            .collect(Collectors.toSet());

        auditService.log(AuditEventType.LOGIN_SUCCESS, user.getId(), user.getUsername(),
            ipAddress, userAgent, "Session created: " + session.getId());

        return LoginResponse.success(
            user.getId(),
            user.getUsername(),
            user.getDisplayName(),
            roles,
            user.isForcePasswordChange(),
            signingKey,
            session.getId()
        );
    }

    @Transactional
    public void logout(String sessionId) {
        authSessionRepository.findById(sessionId).ifPresent(session -> {
            session.setStatus(SessionStatus.REVOKED);
            authSessionRepository.save(session);
            auditService.log(AuditEventType.LOGOUT, session.getUserId(), null,
                session.getIpAddress(), null, "Session revoked");
        });
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            auditService.log(AuditEventType.PASSWORD_CHANGE_FAILURE, userId, user.getUsername(),
                null, null, "Current password incorrect");
            throw new BadCredentialsException("Current password is incorrect");
        }

        List<String> policyErrors = passwordPolicyValidator.validate(newPassword, user.getUsername());
        if (!policyErrors.isEmpty()) {
            throw new IllegalArgumentException("Password policy: " + String.join("; ", policyErrors));
        }

        if (passwordHistoryService.isPasswordReused(userId, newPassword)) {
            throw new IllegalArgumentException("Cannot reuse one of the last "
                + appProperties.getSecurity().getPasswordHistoryCount() + " passwords");
        }

        String encoded = passwordEncoder.encode(newPassword);
        user.setPasswordHash(encoded);
        user.setForcePasswordChange(false);
        userRepository.save(user);

        passwordHistoryService.recordPassword(userId, encoded);

        auditService.log(AuditEventType.PASSWORD_CHANGE, userId, user.getUsername(),
            null, null, "Password changed successfully");
    }

    private DeviceRegistration getOrCreateDevice(Long userId, String fingerprint, String userAgent) {
        if (fingerprint == null || fingerprint.isBlank()) return null;

        return deviceRegistrationRepository.findByUserIdAndDeviceFingerprint(userId, fingerprint)
            .map(device -> {
                device.setLastSeenAt(Instant.now());
                device.setUserAgent(userAgent);
                return deviceRegistrationRepository.save(device);
            })
            .orElseGet(() -> {
                DeviceRegistration device = new DeviceRegistration();
                device.setUserId(userId);
                device.setDeviceFingerprint(fingerprint);
                device.setUserAgent(userAgent);
                return deviceRegistrationRepository.save(device);
            });
    }

    private String generateSigningKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
            keyGen.init(256);
            return Base64.getEncoder().encodeToString(keyGen.generateKey().getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("HmacSHA256 not available", e);
        }
    }
}
