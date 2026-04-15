package com.culinarycoach.integration;

import com.culinarycoach.domain.repository.AuthSessionRepository;
import com.culinarycoach.domain.repository.RoleRepository;
import com.culinarycoach.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the CAPTCHA challenge, CSRF token, and auth logout endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class AuthCaptchaEndpointTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private AuthSessionRepository authSessionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private TestHelper helper;

    @BeforeEach
    void setUp() throws Exception {
        helper = new TestHelper(mockMvc, userRepository, roleRepository,
            authSessionRepository, passwordEncoder);
    }

    // ── CAPTCHA Challenge ───────────────────────────────────────────

    @Test
    void getCaptchaChallenge_returnsImageAndChallengeId() throws Exception {
        mockMvc.perform(get("/api/v1/captcha/challenge"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.challengeId").isNotEmpty())
            .andExpect(jsonPath("$.data.image").isNotEmpty());
    }

    @Test
    void getCaptchaChallenge_authenticated_alsoWorks() throws Exception {
        TestHelper.SessionInfo session = helper.createUserWithSession(
            "captcha_auth_user", "ROLE_USER");

        helper.authGet("/api/v1/captcha/challenge", session)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.challengeId").isNotEmpty());
    }

    // ── CSRF Token ──────────────────────────────────────────────────

    @Test
    void getCsrfToken_returnsTokenSetStatus() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("csrf_token_set"));
    }

    @Test
    void getCsrfToken_authenticated_returnsTokenSetStatus() throws Exception {
        TestHelper.SessionInfo session = helper.createUserWithSession(
            "csrf_auth_user", "ROLE_USER");

        helper.authGet("/api/v1/auth/csrf-token", session)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("csrf_token_set"));
    }

    // ── Logout ──────────────────────────────────────────────────────

    @Test
    void logout_validSession_returnsLoggedOut() throws Exception {
        TestHelper.SessionInfo session = helper.createUserWithSession(
            "logout_user", "ROLE_USER");

        helper.authPostEmpty("/api/v1/auth/logout", session)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").value("Logged out"));
    }

    @Test
    void logout_thenAccessProtectedRoute_returnsForbidden() throws Exception {
        TestHelper.SessionInfo session = helper.createUserWithSession(
            "logout_denied_user", "ROLE_USER");

        // Logout first
        helper.authPostEmpty("/api/v1/auth/logout", session)
            .andExpect(status().isOk());

        // Now the session should be invalidated
        helper.authGet("/api/v1/auth/me", session)
            .andExpect(status().isForbidden());
    }

    @Test
    void logout_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/auth/logout"))
            .andExpect(status().isForbidden());
    }
}
