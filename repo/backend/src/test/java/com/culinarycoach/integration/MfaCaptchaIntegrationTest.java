package com.culinarycoach.integration;

import com.culinarycoach.domain.entity.Role;
import com.culinarycoach.domain.entity.User;
import com.culinarycoach.domain.enums.AccountStatus;
import com.culinarycoach.domain.repository.RoleRepository;
import com.culinarycoach.domain.repository.UserRepository;
import com.culinarycoach.security.mfa.TotpService;
import com.culinarycoach.web.dto.response.MfaSetupResponse;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.time.SystemTimeProvider;
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

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for MFA login completion (audit finding #4)
 * and CAPTCHA enforcement (audit finding #5).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS)
class MfaCaptchaIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TotpService totpService;

    private static final String MFA_USER = "mfatestuser";
    private static final String PASSWORD = "MfaTestPass1!xy";
    private static final String CAPTCHA_USER = "captchatestuser";

    @BeforeEach
    void setUp() {
        // Create MFA-enabled user
        if (userRepository.findByUsernameIgnoreCase(MFA_USER).isEmpty()) {
            Role role = roleRepository.findByName("ROLE_USER").orElseThrow();
            User u = new User();
            u.setUsername(MFA_USER);
            u.setPasswordHash(passwordEncoder.encode(PASSWORD));
            u.setStatus(AccountStatus.ACTIVE);
            u.setMfaEnabled(false); // Will enable via service
            u.setRoles(Set.of(role));
            userRepository.save(u);
        }

        // Create user for CAPTCHA tests
        if (userRepository.findByUsernameIgnoreCase(CAPTCHA_USER).isEmpty()) {
            Role role = roleRepository.findByName("ROLE_USER").orElseThrow();
            User u = new User();
            u.setUsername(CAPTCHA_USER);
            u.setPasswordHash(passwordEncoder.encode(PASSWORD));
            u.setStatus(AccountStatus.ACTIVE);
            u.setRoles(Set.of(role));
            userRepository.save(u);
        }
    }

    @Test
    void mfaEnabledUser_loginReturnsChallenge_notSession() throws Exception {
        // First set up MFA for the user using the service directly
        User user = userRepository.findByUsernameIgnoreCase(MFA_USER).orElseThrow();
        if (!user.isMfaEnabled()) {
            MfaSetupResponse setup = totpService.setupMfa(user.getId());
            // Generate a valid code from the secret
            DefaultCodeGenerator codeGen = new DefaultCodeGenerator();
            String code = codeGen.generate(setup.secretKey(), Math.floorDiv(System.currentTimeMillis() / 1000, 30));
            totpService.verifyAndEnable(user.getId(), code);
        }

        // Login should return MFA challenge, not a session
        mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","password":"%s"}
                    """.formatted(MFA_USER, PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.mfaRequired").value(true))
            .andExpect(jsonPath("$.data.mfaToken").isNotEmpty());
    }

    @Test
    void mfaVerify_invalidCode_rejected() throws Exception {
        User user = userRepository.findByUsernameIgnoreCase(MFA_USER).orElseThrow();
        if (!user.isMfaEnabled()) {
            MfaSetupResponse setup = totpService.setupMfa(user.getId());
            DefaultCodeGenerator codeGen = new DefaultCodeGenerator();
            String code = codeGen.generate(setup.secretKey(), Math.floorDiv(System.currentTimeMillis() / 1000, 30));
            totpService.verifyAndEnable(user.getId(), code);
        }

        // Get MFA token
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","password":"%s"}
                    """.formatted(MFA_USER, PASSWORD)))
            .andReturn();

        String body = loginResult.getResponse().getContentAsString();
        int idx = body.indexOf("\"mfaToken\":\"") + 12;
        int end = body.indexOf("\"", idx);
        String mfaToken = body.substring(idx, end);

        // Submit invalid MFA code
        mockMvc.perform(post("/api/v1/auth/mfa-verify")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code":"000000","mfaToken":"%s"}
                    """.formatted(mfaToken)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void captcha_repeatedFailures_requiresCaptcha() throws Exception {
        String user = createCaptchaTestUser("captcha1");

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","password":"WrongPassword1!"}
                    """.formatted(user)));
        }

        // Next attempt without CAPTCHA should be rejected
        mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","password":"%s"}
                    """.formatted(user, PASSWORD)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.message", containsString("CAPTCHA")));
    }

    @Test
    void captcha_invalidCaptchaAfterThreshold_rejected() throws Exception {
        String user = createCaptchaTestUser("captcha2");

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","password":"WrongPassword1!"}
                    """.formatted(user)));
        }

        mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","password":"%s","captchaId":"fake-id","captchaAnswer":"wrong"}
                    """.formatted(user, PASSWORD)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.message", containsString("CAPTCHA")));
    }

    private String createCaptchaTestUser(String suffix) {
        String username = suffix + "_" + System.nanoTime();
        Role role = roleRepository.findByName("ROLE_USER").orElseThrow();
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(PASSWORD));
        u.setStatus(AccountStatus.ACTIVE);
        u.setRoles(Set.of(role));
        userRepository.save(u);
        return username;
    }
}
