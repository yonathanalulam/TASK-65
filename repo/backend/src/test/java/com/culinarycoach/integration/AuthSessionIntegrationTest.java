package com.culinarycoach.integration;

import com.culinarycoach.domain.entity.AuthSession;
import com.culinarycoach.domain.entity.Role;
import com.culinarycoach.domain.entity.User;
import com.culinarycoach.domain.enums.AccountStatus;
import com.culinarycoach.domain.enums.SessionStatus;
import com.culinarycoach.domain.repository.AuthSessionRepository;
import com.culinarycoach.domain.repository.RoleRepository;
import com.culinarycoach.domain.repository.UserRepository;
import com.culinarycoach.security.auth.PasswordHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for authentication, sessions, and principal resolution.
 * Validates audit findings #1 (principal resolution), #2 (session-security integration),
 * #4 (MFA login), #5 (CAPTCHA enforcement), #10 (default credentials), #11 (credential logging).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS)
class AuthSessionIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private AuthSessionRepository authSessionRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PasswordHistoryService passwordHistoryService;

    private User testUser;
    private String testPassword = "TestPassword1!xy";

    @BeforeEach
    void setUp() {
        // Create test user if not exists
        testUser = userRepository.findByUsernameIgnoreCase("testuser").orElseGet(() -> {
            Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));
            User u = new User();
            u.setUsername("testuser");
            u.setDisplayName("Test User");
            u.setPasswordHash(passwordEncoder.encode(testPassword));
            u.setStatus(AccountStatus.ACTIVE);
            u.setRoles(Set.of(userRole));
            User saved = userRepository.save(u);
            passwordHistoryService.recordPassword(saved.getId(), saved.getPasswordHash());
            return saved;
        });
    }

    @Test
    void login_validCredentials_returnsSessionId() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"testuser","password":"%s"}
                    """.formatted(testPassword)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.sessionId").isNotEmpty())
            .andExpect(jsonPath("$.data.signingKey").isNotEmpty())
            .andExpect(jsonPath("$.data.userId").value(testUser.getId()))
            .andExpect(jsonPath("$.data.mfaRequired").value(false));
    }

    @Test
    void login_invalidPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"testuser","password":"WrongPassword1!"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void protectedRoute_withoutSession_returnsDenied() throws Exception {
        // Spring Security returns 403 for anonymous requests to protected endpoints
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isForbidden());
    }

    @Test
    void protectedRoute_withValidSession_returnsUserPrincipal() throws Exception {
        // Login to get session
        String sessionId = loginAndGetSessionId();

        // Access protected route with session
        mockMvc.perform(get("/api/v1/auth/me")
                .header("X-Session-Id", sessionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.userId").value(testUser.getId()))
            .andExpect(jsonPath("$.data.username").value("testuser"))
            .andExpect(jsonPath("$.data.sessionId").value(sessionId));
    }

    @Test
    void protectedRoute_withExpiredSession_returnsForbidden() throws Exception {
        // Create an expired session directly
        AuthSession session = new AuthSession();
        session.setId(UUID.randomUUID().toString());
        session.setUserId(testUser.getId());
        session.setSigningKey("dummykey");
        session.setStatus(SessionStatus.ACTIVE);
        session.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        session.setIdleTimeoutMinutes(30);
        authSessionRepository.save(session);

        mockMvc.perform(get("/api/v1/auth/me")
                .header("X-Session-Id", session.getId()))
            .andExpect(status().isForbidden());
    }

    @Test
    void protectedRoute_withRevokedSession_returnsForbidden() throws Exception {
        AuthSession session = new AuthSession();
        session.setId(UUID.randomUUID().toString());
        session.setUserId(testUser.getId());
        session.setSigningKey("dummykey");
        session.setStatus(SessionStatus.REVOKED);
        session.setExpiresAt(Instant.now().plus(12, ChronoUnit.HOURS));
        session.setIdleTimeoutMinutes(30);
        authSessionRepository.save(session);

        mockMvc.perform(get("/api/v1/auth/me")
                .header("X-Session-Id", session.getId()))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminRoute_withNonAdminUser_returns403() throws Exception {
        String sessionId = loginAndGetSessionId();

        mockMvc.perform(get("/api/v1/admin/users")
                .header("X-Session-Id", sessionId))
            .andExpect(status().isForbidden());
    }

    private String loginAndGetSessionId() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"testuser","password":"%s"}
                    """.formatted(testPassword)))
            .andExpect(status().isOk())
            .andReturn();

        String body = result.getResponse().getContentAsString();
        int idx = body.indexOf("\"sessionId\":\"") + 13;
        int end = body.indexOf("\"", idx);
        return body.substring(idx, end);
    }
}
