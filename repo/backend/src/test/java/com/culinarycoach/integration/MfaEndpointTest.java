package com.culinarycoach.integration;

import com.culinarycoach.domain.repository.AuthSessionRepository;
import com.culinarycoach.domain.repository.RoleRepository;
import com.culinarycoach.domain.repository.UserRepository;
import dev.samstevens.totp.code.DefaultCodeGenerator;
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
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the MFA (Multi-Factor Authentication) endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class MfaEndpointTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private AuthSessionRepository authSessionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private TestHelper helper;
    private TestHelper.SessionInfo userSession;

    @BeforeEach
    void setUp(TestInfo testInfo) throws Exception {
        helper = new TestHelper(mockMvc, userRepository, roleRepository,
            authSessionRepository, passwordEncoder);
        String uniqueName = "mfa_" + testInfo.getTestMethod().orElseThrow().getName();
        userSession = helper.createUserWithSession(uniqueName, "ROLE_USER");
    }

    @Test
    void setupMfa_authenticated_returnsQrCodeAndRecoveryCodes() throws Exception {
        helper.authPostEmpty("/api/v1/mfa/setup", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.qrCodeDataUri").exists())
            .andExpect(jsonPath("$.data.recoveryCodes").isArray())
            .andExpect(jsonPath("$.data.recoveryCodes").isNotEmpty());
    }

    @Test
    void verifyMfa_withValidCode_returns200() throws Exception {
        // Setup MFA first, retrieve secret key
        MvcResult setupResult = helper.authPostEmpty("/api/v1/mfa/setup", userSession)
            .andExpect(status().isOk())
            .andReturn();

        String secretKey = helper.extractField(setupResult, "data.secretKey");

        // Generate a valid TOTP code
        DefaultCodeGenerator codeGen = new DefaultCodeGenerator();
        String code = codeGen.generate(secretKey,
            Math.floorDiv(System.currentTimeMillis() / 1000, 30));

        helper.authPost("/api/v1/mfa/verify", userSession,
                "{\"code\":\"" + code + "\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.verified").value(true));
    }

    @Test
    void disableMfa_afterEnabling_returns200() throws Exception {
        // Setup and verify MFA
        MvcResult setupResult = helper.authPostEmpty("/api/v1/mfa/setup", userSession)
            .andExpect(status().isOk())
            .andReturn();

        String secretKey = helper.extractField(setupResult, "data.secretKey");

        DefaultCodeGenerator codeGen = new DefaultCodeGenerator();
        String code = codeGen.generate(secretKey,
            Math.floorDiv(System.currentTimeMillis() / 1000, 30));

        helper.authPost("/api/v1/mfa/verify", userSession,
                "{\"code\":\"" + code + "\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.verified").value(true));

        // Disable MFA
        helper.authPostEmpty("/api/v1/mfa/disable", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void mfaSetup_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/mfa/setup"))
            .andExpect(status().isForbidden());
    }
}
