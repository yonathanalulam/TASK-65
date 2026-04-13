package com.culinarycoach.integration;

import com.culinarycoach.domain.entity.AuthSession;
import com.culinarycoach.domain.entity.Role;
import com.culinarycoach.domain.entity.User;
import com.culinarycoach.domain.enums.AccountStatus;
import com.culinarycoach.domain.enums.SessionStatus;
import com.culinarycoach.domain.repository.AuthSessionRepository;
import com.culinarycoach.domain.repository.RoleRepository;
import com.culinarycoach.domain.repository.UserRepository;
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

import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for signed request enforcement (audit finding #3).
 * Validates that state-changing requests REQUIRE valid signatures and
 * that bypass paths are closed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS)
class SignedRequestIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private AuthSessionRepository authSessionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AuthSession validSession;

    @BeforeEach
    void setUp() throws Exception {
        User user = userRepository.findByUsernameIgnoreCase("signtest").orElseGet(() -> {
            Role role = roleRepository.findByName("ROLE_USER").orElseThrow();
            User u = new User();
            u.setUsername("signtest");
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
    void stateChangingRequest_missingAllSignatureHeaders_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/change-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Session-Id", validSession.getId())
                .content("""
                    {"currentPassword":"old","newPassword":"new"}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("SIGNATURE_REQUIRED"));
    }

    @Test
    void stateChangingRequest_missingSessionId_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/change-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Timestamp", Instant.now().toString())
                .header("X-Nonce", UUID.randomUUID().toString())
                .header("X-Signature", "some-signature")
                .content("""
                    {"currentPassword":"old","newPassword":"new"}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("SIGNATURE_REQUIRED"));
    }

    @Test
    void stateChangingRequest_staleTimestamp_rejected() throws Exception {
        String staleTimestamp = Instant.now().minus(10, ChronoUnit.MINUTES).toString();
        String nonce = UUID.randomUUID().toString();
        String path = "/api/v1/auth/change-password";
        String signature = sign("POST", path, staleTimestamp, nonce, validSession.getSigningKey());

        mockMvc.perform(post(path)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Session-Id", validSession.getId())
                .header("X-Timestamp", staleTimestamp)
                .header("X-Nonce", nonce)
                .header("X-Signature", signature)
                .content("""
                    {"currentPassword":"old","newPassword":"new"}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("SIGNATURE_EXPIRED"));
    }

    @Test
    void stateChangingRequest_duplicateNonce_rejected() throws Exception {
        String timestamp = Instant.now().toString();
        String nonce = UUID.randomUUID().toString();
        String path = "/api/v1/auth/change-password";
        String signature = sign("POST", path, timestamp, nonce, validSession.getSigningKey());

        // First request should pass signature validation (may fail on business logic, that's OK)
        mockMvc.perform(post(path)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Session-Id", validSession.getId())
            .header("X-Timestamp", timestamp)
            .header("X-Nonce", nonce)
            .header("X-Signature", signature)
            .content("""
                {"currentPassword":"old","newPassword":"new"}
                """));

        // Second request with SAME nonce should be rejected
        mockMvc.perform(post(path)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Session-Id", validSession.getId())
                .header("X-Timestamp", timestamp)
                .header("X-Nonce", nonce)
                .header("X-Signature", signature)
                .content("""
                    {"currentPassword":"old","newPassword":"new"}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("NONCE_REPLAY"));
    }

    @Test
    void stateChangingRequest_invalidSignature_rejected() throws Exception {
        String timestamp = Instant.now().toString();
        String nonce = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/v1/auth/change-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Session-Id", validSession.getId())
                .header("X-Timestamp", timestamp)
                .header("X-Nonce", nonce)
                .header("X-Signature", "invalid-signature-value")
                .content("""
                    {"currentPassword":"old","newPassword":"new"}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("SIGNATURE_INVALID"));
    }

    @Test
    void stateChangingRequest_unknownSession_rejected() throws Exception {
        String timestamp = Instant.now().toString();
        String nonce = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/v1/auth/change-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Session-Id", "nonexistent-session-id")
                .header("X-Timestamp", timestamp)
                .header("X-Nonce", nonce)
                .header("X-Signature", "some-signature")
                .content("""
                    {"currentPassword":"old","newPassword":"new"}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("INVALID_SESSION"));
    }

    @Test
    void stateChangingRequest_validSignature_passesSignatureFilter() throws Exception {
        String timestamp = Instant.now().toString();
        String nonce = UUID.randomUUID().toString();
        String path = "/api/v1/auth/change-password";
        String signature = sign("POST", path, timestamp, nonce, validSession.getSigningKey());

        // Request passes signature validation; may fail on business logic (wrong password)
        // but should NOT fail on signature-related errors
        mockMvc.perform(post(path)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Session-Id", validSession.getId())
                .header("X-Timestamp", timestamp)
                .header("X-Nonce", nonce)
                .header("X-Signature", signature)
                .content("""
                    {"currentPassword":"WrongOldPass1!","newPassword":"NewPassword1!xy"}
                    """))
            // Should not be a signature-related error
            .andExpect(jsonPath("$.error.code").value(not("SIGNATURE_REQUIRED")))
            .andExpect(jsonPath("$.error.code").value(not("SIGNATURE_EXPIRED")))
            .andExpect(jsonPath("$.error.code").value(not("SIGNATURE_INVALID")))
            .andExpect(jsonPath("$.error.code").value(not("NONCE_REPLAY")))
            .andExpect(jsonPath("$.error.code").value(not("INVALID_SESSION")));
    }

    @Test
    void loginEndpoint_exemptFromSignature() throws Exception {
        // Login should work without any signature headers
        mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"signtest","password":"TestPassword1!xy"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.sessionId").isNotEmpty());
    }

    @Test
    void stateChangingRequest_signedWithNonCanonicalPath_rejected() throws Exception {
        // Regression: frontend used to sign "/auth/change-password" (without /api/v1 prefix)
        // but backend verifies against "/api/v1/auth/change-password".
        String timestamp = Instant.now().toString();
        String nonce = UUID.randomUUID().toString();
        String wrongPath = "/auth/change-password"; // missing /api/v1 prefix
        String signature = sign("POST", wrongPath, timestamp, nonce, validSession.getSigningKey());

        mockMvc.perform(post("/api/v1/auth/change-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Session-Id", validSession.getId())
                .header("X-Timestamp", timestamp)
                .header("X-Nonce", nonce)
                .header("X-Signature", signature)
                .content("""
                    {"currentPassword":"old","newPassword":"new"}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("SIGNATURE_INVALID"));
    }

    @Test
    void stateChangingRequest_signedWithCanonicalPath_passesSignatureValidation() throws Exception {
        // Canonical path includes the /api/v1 prefix, matching request URI
        String timestamp = Instant.now().toString();
        String nonce = UUID.randomUUID().toString();
        String canonicalPath = "/api/v1/auth/change-password";
        String signature = sign("POST", canonicalPath, timestamp, nonce, validSession.getSigningKey());

        mockMvc.perform(post(canonicalPath)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Session-Id", validSession.getId())
                .header("X-Timestamp", timestamp)
                .header("X-Nonce", nonce)
                .header("X-Signature", signature)
                .content("""
                    {"currentPassword":"WrongOldPass1!","newPassword":"NewPassword1!xy"}
                    """))
            // Should pass signature filter (may fail on business logic)
            .andExpect(jsonPath("$.error.code").value(not("SIGNATURE_INVALID")))
            .andExpect(jsonPath("$.error.code").value(not("SIGNATURE_REQUIRED")));
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
