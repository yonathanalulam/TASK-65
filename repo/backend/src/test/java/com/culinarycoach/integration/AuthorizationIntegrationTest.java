package com.culinarycoach.integration;

import com.culinarycoach.domain.entity.*;
import com.culinarycoach.domain.enums.AccountStatus;
import com.culinarycoach.domain.enums.SessionStatus;
import com.culinarycoach.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.KeyGenerator;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for object-level authorization (audit finding #6)
 * and role-based access (Parent/Coach scoping, admin-only routes).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private AuthSessionRepository authSessionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String userASessionId;
    private String userBSessionId;
    private String adminSessionId;
    private Long userAId;
    private Long userBId;

    @BeforeEach
    void setUp() throws Exception {
        // Create user A
        User userA = userRepository.findByUsernameIgnoreCase("authz_user_a").orElseGet(() -> {
            Role role = roleRepository.findByName("ROLE_USER").orElseThrow();
            User u = new User();
            u.setUsername("authz_user_a");
            u.setPasswordHash(passwordEncoder.encode("TestPassword1!xy"));
            u.setStatus(AccountStatus.ACTIVE);
            u.setRoles(Set.of(role));
            return userRepository.save(u);
        });
        userAId = userA.getId();

        // Create user B
        User userB = userRepository.findByUsernameIgnoreCase("authz_user_b").orElseGet(() -> {
            Role role = roleRepository.findByName("ROLE_USER").orElseThrow();
            User u = new User();
            u.setUsername("authz_user_b");
            u.setPasswordHash(passwordEncoder.encode("TestPassword1!xy"));
            u.setStatus(AccountStatus.ACTIVE);
            u.setRoles(Set.of(role));
            return userRepository.save(u);
        });
        userBId = userB.getId();

        // Create admin
        User admin = userRepository.findByUsernameIgnoreCase("authz_admin").orElseGet(() -> {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
            User u = new User();
            u.setUsername("authz_admin");
            u.setPasswordHash(passwordEncoder.encode("TestPassword1!xy"));
            u.setStatus(AccountStatus.ACTIVE);
            u.setRoles(Set.of(adminRole));
            return userRepository.save(u);
        });

        userASessionId = createSession(userA.getId());
        userBSessionId = createSession(userB.getId());
        adminSessionId = createSession(admin.getId());
    }

    @Test
    void userCanAccessOwnProfile() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                .header("X-Session-Id", userASessionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.userId").value(userAId));
    }

    @Test
    void nonAdminCannotAccessAdminRoutes() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                .header("X-Session-Id", userASessionId))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAdminRoutes() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                .header("X-Session-Id", adminSessionId))
            .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedCannotAccessProtectedRoutes() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isForbidden());
    }

    @Test
    void parentCoachCannotAccessUnassignedStudentData() throws Exception {
        // Create parent/coach user
        User coach = userRepository.findByUsernameIgnoreCase("authz_coach").orElseGet(() -> {
            Role role = roleRepository.findByName("ROLE_PARENT_COACH").orElseThrow();
            User u = new User();
            u.setUsername("authz_coach");
            u.setPasswordHash(passwordEncoder.encode("TestPassword1!xy"));
            u.setStatus(AccountStatus.ACTIVE);
            u.setRoles(Set.of(role));
            return userRepository.save(u);
        });
        String coachSessionId = createSession(coach.getId());

        // Try to review an unassigned student's notebook — should fail
        mockMvc.perform(get("/api/v1/review/students/" + userAId + "/notebook")
                .header("X-Session-Id", coachSessionId)
                .param("reason", "PROGRESS_CHECK"))
            .andExpect(status().isForbidden());
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
