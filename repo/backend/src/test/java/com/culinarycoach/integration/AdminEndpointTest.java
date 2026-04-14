package com.culinarycoach.integration;

import com.culinarycoach.domain.repository.AuthSessionRepository;
import com.culinarycoach.domain.repository.RoleRepository;
import com.culinarycoach.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the Admin Dashboard, Tip Cards, and Privacy admin endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class AdminEndpointTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private AuthSessionRepository authSessionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private TestHelper helper;
    private TestHelper.SessionInfo adminSession;

    @BeforeEach
    void setUp() throws Exception {
        helper = new TestHelper(mockMvc, userRepository, roleRepository,
            authSessionRepository, passwordEncoder);
        adminSession = helper.createUserWithSession("admin_dash_user", "ROLE_ADMIN");
    }

    // ── Dashboard Jobs ───────────────────────────────────────────────

    @Test
    void listJobs_admin_returns200() throws Exception {
        helper.authGet("/api/v1/admin/dashboard/jobs", adminSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    // ── Dashboard Alerts ─────────────────────────────────────────────

    @Test
    void listAlerts_admin_returns200() throws Exception {
        helper.authGet("/api/v1/admin/dashboard/alerts", adminSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void getAlertCount_admin_returns200() throws Exception {
        helper.authGet("/api/v1/admin/dashboard/alerts/count", adminSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.openAlerts").isNumber());
    }

    // ── Dashboard Capacity ───────────────────────────────────────────

    @Test
    void getCapacityReport_admin_returns200() throws Exception {
        helper.authGet("/api/v1/admin/dashboard/capacity", adminSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").exists());
    }

    // ── Dashboard KPIs ───────────────────────────────────────────────

    @Test
    void getKpiSummary_admin_returns200() throws Exception {
        helper.authGet("/api/v1/admin/dashboard/kpis", adminSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").exists());
    }

    // ── Tip Cards ────────────────────────────────────────────────────

    @Test
    void listTipCards_admin_returnsSeededTips() throws Exception {
        helper.authGet("/api/v1/admin/tips", adminSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data").isNotEmpty());
    }

    // ── Privacy Access Logs ──────────────────────────────────────────

    @Test
    void listPrivacyAccessLogs_admin_returns200() throws Exception {
        helper.authGet("/api/v1/admin/privacy/access-logs", adminSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray());
    }

    // ── Non-Admin Access Denied ──────────────────────────────────────

    @Test
    void dashboardJobs_nonAdmin_returns403() throws Exception {
        TestHelper.SessionInfo regularUser = helper.createUserWithSession(
            "admin_dash_regular", "ROLE_USER");

        helper.authGet("/api/v1/admin/dashboard/jobs", regularUser)
            .andExpect(status().isForbidden());
    }

    @Test
    void dashboardAlerts_nonAdmin_returns403() throws Exception {
        TestHelper.SessionInfo regularUser = helper.createUserWithSession(
            "admin_dash_regular2", "ROLE_USER");

        helper.authGet("/api/v1/admin/dashboard/alerts", regularUser)
            .andExpect(status().isForbidden());
    }

    @Test
    void tipCards_nonAdmin_returns403() throws Exception {
        TestHelper.SessionInfo regularUser = helper.createUserWithSession(
            "admin_dash_regular3", "ROLE_USER");

        helper.authGet("/api/v1/admin/tips", regularUser)
            .andExpect(status().isForbidden());
    }

    @Test
    void privacyLogs_nonAdmin_returns403() throws Exception {
        TestHelper.SessionInfo regularUser = helper.createUserWithSession(
            "admin_dash_regular4", "ROLE_USER");

        helper.authGet("/api/v1/admin/privacy/access-logs", regularUser)
            .andExpect(status().isForbidden());
    }
}
