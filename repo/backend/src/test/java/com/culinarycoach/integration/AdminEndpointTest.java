package com.culinarycoach.integration;

import com.culinarycoach.domain.repository.AuthSessionRepository;
import com.culinarycoach.domain.repository.RoleRepository;
import com.culinarycoach.domain.repository.UserRepository;
import com.culinarycoach.service.AnomalyAlertService;
import com.culinarycoach.domain.enums.AlertSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

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
    @Autowired private AnomalyAlertService anomalyAlertService;

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

    // ── Dashboard Job Runs ──────────────────────────────────────────

    @Test
    void listJobRuns_admin_returns200() throws Exception {
        // Use a known seeded job name; "cache-cleanup" is seeded in V11
        helper.authGet("/api/v1/admin/dashboard/jobs/cache-cleanup/runs", adminSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void listJobRuns_nonAdmin_returns403() throws Exception {
        TestHelper.SessionInfo regularUser = helper.createUserWithSession(
            "admin_jobruns_regular", "ROLE_USER");

        helper.authGet("/api/v1/admin/dashboard/jobs/cache-cleanup/runs", regularUser)
            .andExpect(status().isForbidden());
    }

    // ── Dashboard Metrics ───────────────────────────────────────────

    @Test
    void getMetrics_admin_returns200() throws Exception {
        Instant from = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant to = Instant.now();

        helper.authGet("/api/v1/admin/dashboard/metrics?name=capacity_report&from="
                + from.toString() + "&to=" + to.toString(), adminSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getMetrics_nonAdmin_returns403() throws Exception {
        TestHelper.SessionInfo regularUser = helper.createUserWithSession(
            "admin_metrics_regular", "ROLE_USER");

        Instant from = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant to = Instant.now();

        helper.authGet("/api/v1/admin/dashboard/metrics?name=capacity_report&from="
                + from.toString() + "&to=" + to.toString(), regularUser)
            .andExpect(status().isForbidden());
    }

    // ── Alert Acknowledge / Resolve ─────────────────────────────────

    @Test
    void acknowledgeAlert_admin_returns200() throws Exception {
        // Create an alert to acknowledge
        var alert = anomalyAlertService.createAlert(
            "TEST_ANOMALY", AlertSeverity.WARNING, "Test alert for acknowledge",
            "test_metric", BigDecimal.valueOf(100), BigDecimal.valueOf(150));

        helper.authPostEmpty("/api/v1/admin/dashboard/alerts/" + alert.getId() + "/acknowledge",
                adminSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(alert.getId()))
            .andExpect(jsonPath("$.data.status").value("ACKNOWLEDGED"));
    }

    @Test
    void resolveAlert_admin_returns200() throws Exception {
        // Create an alert to resolve
        var alert = anomalyAlertService.createAlert(
            "TEST_ANOMALY", AlertSeverity.WARNING, "Test alert for resolve",
            "test_metric", BigDecimal.valueOf(100), BigDecimal.valueOf(200));

        helper.authPostEmpty("/api/v1/admin/dashboard/alerts/" + alert.getId() + "/resolve",
                adminSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(alert.getId()))
            .andExpect(jsonPath("$.data.status").value("RESOLVED"));
    }

    @Test
    void acknowledgeAlert_nonAdmin_returns403() throws Exception {
        TestHelper.SessionInfo regularUser = helper.createUserWithSession(
            "admin_alert_regular", "ROLE_USER");

        var alert = anomalyAlertService.createAlert(
            "TEST_ANOMALY", AlertSeverity.INFO, "Test alert denied",
            "test_metric", BigDecimal.valueOf(50), BigDecimal.valueOf(75));

        helper.authPostEmpty("/api/v1/admin/dashboard/alerts/" + alert.getId() + "/acknowledge",
                regularUser)
            .andExpect(status().isForbidden());
    }

    // ── Tip Card Configure / Toggle ─────────────────────────────────

    @Test
    void configureTip_admin_returns200() throws Exception {
        // Tip ID 1 is seeded in V12
        String body = """
            {"scope":"GLOBAL","scopeId":null,"displayMode":"DETAILED"}
            """;

        helper.authPost("/api/v1/admin/tips/1/configure", adminSession, body)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void configureTip_nonAdmin_returns403() throws Exception {
        TestHelper.SessionInfo regularUser = helper.createUserWithSession(
            "admin_tipcfg_regular", "ROLE_USER");

        String body = """
            {"scope":"GLOBAL","scopeId":null,"displayMode":"DETAILED"}
            """;

        helper.authPost("/api/v1/admin/tips/1/configure", regularUser, body)
            .andExpect(status().isForbidden());
    }

    @Test
    void toggleTip_admin_returns200() throws Exception {
        // Tip ID 1 is seeded in V12
        helper.authPostEmpty("/api/v1/admin/tips/1/toggle", adminSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.enabled").isBoolean());
    }

    @Test
    void toggleTip_nonAdmin_returns403() throws Exception {
        TestHelper.SessionInfo regularUser = helper.createUserWithSession(
            "admin_tiptog_regular", "ROLE_USER");

        helper.authPostEmpty("/api/v1/admin/tips/1/toggle", regularUser)
            .andExpect(status().isForbidden());
    }
}
