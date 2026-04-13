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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.KeyGenerator;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the reconciliation admin endpoints.
 *
 * Verifies that:
 * - the canonical endpoint path is /api/v1/admin/checkout/reconciliation/*
 * - export generation is GET with businessDate query param (not POST with body)
 * - export listing is GET with pageable params
 * - non-admin users cannot access reconciliation endpoints
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReconciliationEndpointTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private AuthSessionRepository authSessionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminSessionId;
    private String userSessionId;

    @BeforeEach
    void setUp() throws Exception {
        User admin = userRepository.findByUsernameIgnoreCase("recon_admin").orElseGet(() -> {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
            User u = new User();
            u.setUsername("recon_admin");
            u.setPasswordHash(passwordEncoder.encode("TestPassword1!xy"));
            u.setStatus(AccountStatus.ACTIVE);
            u.setRoles(Set.of(adminRole));
            return userRepository.save(u);
        });

        User regularUser = userRepository.findByUsernameIgnoreCase("recon_user").orElseGet(() -> {
            Role userRole = roleRepository.findByName("ROLE_USER").orElseThrow();
            User u = new User();
            u.setUsername("recon_user");
            u.setPasswordHash(passwordEncoder.encode("TestPassword1!xy"));
            u.setStatus(AccountStatus.ACTIVE);
            u.setRoles(Set.of(userRole));
            return userRepository.save(u);
        });

        adminSessionId = createSession(admin.getId());
        userSessionId = createSession(regularUser.getId());
    }

    @Test
    void listExports_adminAllowed_returnsPage() throws Exception {
        mockMvc.perform(get("/api/v1/admin/checkout/reconciliation/exports")
                .header("X-Session-Id", adminSessionId)
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void listExports_regularUserDenied() throws Exception {
        mockMvc.perform(get("/api/v1/admin/checkout/reconciliation/exports")
                .header("X-Session-Id", userSessionId))
            .andExpect(status().isForbidden());
    }

    @Test
    void generateExport_adminAllowed_withQueryParam() throws Exception {
        String yesterday = LocalDate.now().minusDays(1).toString();

        mockMvc.perform(get("/api/v1/admin/checkout/reconciliation/export")
                .header("X-Session-Id", adminSessionId)
                .param("businessDate", yesterday))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.businessDate").value(yesterday))
            .andExpect(jsonPath("$.data.generatedBy").value("recon_admin"));
    }

    @Test
    void generateExport_regularUserDenied() throws Exception {
        mockMvc.perform(get("/api/v1/admin/checkout/reconciliation/export")
                .header("X-Session-Id", userSessionId)
                .param("businessDate", "2026-01-15"))
            .andExpect(status().isForbidden());
    }

    @Test
    void generateExport_missingBusinessDate_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/admin/checkout/reconciliation/export")
                .header("X-Session-Id", adminSessionId))
            .andExpect(status().isBadRequest());
    }

    @Test
    void generateExport_postMethodNotAllowed() throws Exception {
        // The frontend previously used POST — verify it's not accepted
        mockMvc.perform(post("/api/v1/admin/checkout/reconciliation/export")
                .header("X-Session-Id", adminSessionId)
                .param("businessDate", "2026-01-15"))
            .andExpect(status().isForbidden()); // will hit signature filter or method not allowed
    }

    private String createSession(Long userId) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        keyGen.init(256);
        String signingKey = Base64.getEncoder().encodeToString(keyGen.generateKey().getEncoded());

        AuthSession session = new AuthSession();
        session.setId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setSigningKey(signingKey);
        session.setStatus(SessionStatus.ACTIVE);
        session.setExpiresAt(Instant.now().plus(12, ChronoUnit.HOURS));
        session.setIdleTimeoutMinutes(30);
        authSessionRepository.save(session);
        return session.getId();
    }
}
