package com.culinarycoach.integration;

import com.culinarycoach.domain.entity.AuthSession;
import com.culinarycoach.domain.entity.Role;
import com.culinarycoach.domain.entity.User;
import com.culinarycoach.domain.enums.AccountStatus;
import com.culinarycoach.domain.enums.SessionStatus;
import com.culinarycoach.domain.repository.AuthSessionRepository;
import com.culinarycoach.domain.repository.RoleRepository;
import com.culinarycoach.domain.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String adminSessionId;
    private String adminSigningKey;
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

        String[] adminSessionParts = createSessionWithKey(admin.getId());
        adminSessionId = adminSessionParts[0];
        adminSigningKey = adminSessionParts[1];
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

    // ── Void Transaction ───────────────────────────────────────────

    @Test
    void voidTransaction_admin_returns200() throws Exception {
        // First create and complete a transaction as a regular user
        TestHelper testHelper = new TestHelper(mockMvc, userRepository, roleRepository,
            authSessionRepository, passwordEncoder);
        TestHelper.SessionInfo buyerSession = testHelper.createUserWithSession(
            "recon_buyer", "ROLE_USER");

        MvcResult initiateResult = testHelper.authPost("/api/v1/checkout/initiate", buyerSession,
                """
                {"bundleIds":[1]}
                """)
            .andExpect(status().isOk())
            .andReturn();

        Long transactionId = testHelper.extractLong(initiateResult, "data.id");

        testHelper.authPostEmpty("/api/v1/checkout/complete/" + transactionId, buyerSession)
            .andExpect(status().isOk());

        // Now void the transaction as admin (signed POST)
        String path = "/api/v1/admin/checkout/transactions/" + transactionId + "/void";
        String body = """
            {"reason":"Test void reason"}
            """;
        String timestamp = Instant.now().toString();
        String nonce = UUID.randomUUID().toString();
        String signature = sign("POST", path, timestamp, nonce, adminSigningKey);

        mockMvc.perform(post(path)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Session-Id", adminSessionId)
                .header("X-Timestamp", timestamp)
                .header("X-Nonce", nonce)
                .header("X-Signature", signature)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(transactionId))
            .andExpect(jsonPath("$.data.status").value("VOIDED"));
    }

    @Test
    void voidTransaction_regularUser_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/checkout/transactions/1/void")
                .header("X-Session-Id", userSessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"reason":"Try void"}
                    """))
            .andExpect(status().isForbidden());
    }

    // ── Get Export By ID ────────────────────────────────────────────

    @Test
    void getExportById_admin_returns200() throws Exception {
        // First generate an export to get an ID
        String yesterday = LocalDate.now().minusDays(1).toString();

        MvcResult exportResult = mockMvc.perform(get("/api/v1/admin/checkout/reconciliation/export")
                .header("X-Session-Id", adminSessionId)
                .param("businessDate", yesterday))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode root = objectMapper.readTree(exportResult.getResponse().getContentAsString());
        long exportId = root.path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/admin/checkout/reconciliation/exports/" + exportId)
                .header("X-Session-Id", adminSessionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(exportId))
            .andExpect(jsonPath("$.data.businessDate").value(yesterday));
    }

    @Test
    void getExportById_regularUser_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/checkout/reconciliation/exports/1")
                .header("X-Session-Id", userSessionId))
            .andExpect(status().isForbidden());
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private String createSession(Long userId) throws Exception {
        return createSessionWithKey(userId)[0];
    }

    private String[] createSessionWithKey(Long userId) throws Exception {
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
        return new String[] { session.getId(), signingKey };
    }

    private String sign(String method, String path, String timestamp, String nonce,
                         String keyBase64) throws Exception {
        String payload = method + "\n" + path + "\n" + timestamp + "\n" + nonce;
        byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
        return Base64.getEncoder().encodeToString(
            mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
