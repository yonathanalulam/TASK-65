package com.culinarycoach.integration;

import com.culinarycoach.domain.entity.AuthSession;
import com.culinarycoach.domain.entity.Role;
import com.culinarycoach.domain.entity.User;
import com.culinarycoach.domain.enums.AccountStatus;
import com.culinarycoach.domain.enums.SessionStatus;
import com.culinarycoach.domain.repository.AuthSessionRepository;
import com.culinarycoach.domain.repository.RoleRepository;
import com.culinarycoach.domain.repository.UserRepository;
import com.culinarycoach.security.captcha.RequestFailureTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CAPTCHA enforcement on non-login request failures.
 * Validates that repeated request failures (rate-limit, signature, etc.)
 * trigger CAPTCHA requirement for subsequent requests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class RequestCaptchaIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private AuthSessionRepository authSessionRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private RequestFailureTracker requestFailureTracker;

    private AuthSession validSession;

    @BeforeEach
    void setUp() throws Exception {
        User user = userRepository.findByUsernameIgnoreCase("captchareqtest").orElseGet(() -> {
            Role role = roleRepository.findByName("ROLE_USER").orElseThrow();
            User u = new User();
            u.setUsername("captchareqtest");
            u.setPasswordHash(passwordEncoder.encode("TestPassword1!xy"));
            u.setStatus(AccountStatus.ACTIVE);
            u.setRoles(Set.of(role));
            return userRepository.save(u);
        });

        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        keyGen.init(256);
        String signingKey = Base64.getEncoder().encodeToString(keyGen.generateKey().getEncoded());

        validSession = new AuthSession();
        validSession.setId(UUID.randomUUID().toString());
        validSession.setUserId(user.getId());
        validSession.setSigningKey(signingKey);
        validSession.setStatus(SessionStatus.ACTIVE);
        validSession.setExpiresAt(Instant.now().plus(12, ChronoUnit.HOURS));
        validSession.setIdleTimeoutMinutes(30);
        authSessionRepository.save(validSession);
    }

    @Test
    void repeatedRequestFailures_triggersCaptchaRequirement() throws Exception {
        // Directly record failures via the tracker (avoids rate-limit interference)
        // Default captchaFailureThresholdRequest = 10
        for (int i = 0; i < 10; i++) {
            requestFailureTracker.recordFailure("127.0.0.1");
        }

        // Next state-changing request should require CAPTCHA
        String timestamp = Instant.now().toString();
        String nonce = UUID.randomUUID().toString();
        String path = "/api/v1/auth/change-password";
        String sig = sign("POST", path, timestamp, nonce, validSession.getSigningKey());

        mockMvc.perform(post(path)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Session-Id", validSession.getId())
                .header("X-Timestamp", timestamp)
                .header("X-Nonce", nonce)
                .header("X-Signature", sig)
                .content("""
                    {"currentPassword":"old","newPassword":"new"}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("CAPTCHA_REQUIRED"));
    }

    @Test
    void invalidCaptchaAfterEscalation_rejected() throws Exception {
        // Directly simulate threshold failures via the tracker
        for (int i = 0; i < 10; i++) {
            requestFailureTracker.recordFailure("127.0.0.1");
        }

        String timestamp = Instant.now().toString();
        String nonce = UUID.randomUUID().toString();
        String path = "/api/v1/auth/change-password";
        String sig = sign("POST", path, timestamp, nonce, validSession.getSigningKey());

        mockMvc.perform(post(path)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Session-Id", validSession.getId())
                .header("X-Timestamp", timestamp)
                .header("X-Nonce", nonce)
                .header("X-Signature", sig)
                .header("X-Captcha-Id", "fake-captcha-id")
                .header("X-Captcha-Answer", "wrong-answer")
                .content("""
                    {"currentPassword":"old","newPassword":"new"}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("CAPTCHA_INVALID"));
    }

    @Test
    void belowThreshold_noCaptchaRequired() throws Exception {
        // Only 5 failures (below threshold of 10)
        for (int i = 0; i < 5; i++) {
            requestFailureTracker.recordFailure("127.0.0.1");
        }

        // Should pass CAPTCHA filter (may fail on other filters, that's OK)
        String timestamp = Instant.now().toString();
        String nonce = UUID.randomUUID().toString();
        String path = "/api/v1/auth/change-password";
        String sig = sign("POST", path, timestamp, nonce, validSession.getSigningKey());

        mockMvc.perform(post(path)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Session-Id", validSession.getId())
                .header("X-Timestamp", timestamp)
                .header("X-Nonce", nonce)
                .header("X-Signature", sig)
                .content("""
                    {"currentPassword":"WrongOldPass1!","newPassword":"NewPassword1!xy"}
                    """))
            // Should NOT be CAPTCHA_REQUIRED — may be other errors, but not CAPTCHA
            .andExpect(jsonPath("$.error.code").value(
                org.hamcrest.Matchers.not("CAPTCHA_REQUIRED")));
    }

    private String sign(String method, String path, String timestamp, String nonce, String keyBase64) throws Exception {
        String payload = method + "\n" + path + "\n" + timestamp + "\n" + nonce;
        byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
        return Base64.getEncoder().encodeToString(
            mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
