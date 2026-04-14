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
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the User Management admin endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class UserManagementEndpointTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private AuthSessionRepository authSessionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private TestHelper helper;
    private TestHelper.SessionInfo adminSession;
    private String testSuffix;

    @BeforeEach
    void setUp(TestInfo testInfo) throws Exception {
        helper = new TestHelper(mockMvc, userRepository, roleRepository,
            authSessionRepository, passwordEncoder);
        testSuffix = testInfo.getTestMethod().orElseThrow().getName();
        adminSession = helper.createUserWithSession("umgmt_adm_" + testSuffix, "ROLE_ADMIN");
    }

    @Test
    void listUsers_admin_returns200() throws Exception {
        helper.authGet("/api/v1/admin/users", adminSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void createUser_admin_returns201() throws Exception {
        helper.authPost("/api/v1/admin/users", adminSession,
                """
                {
                  "username": "created-alpha",
                  "password": "StrongP@ss12345!",
                  "displayName": "New User",
                  "roles": ["ROLE_USER"]
                }
                """)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.username").value("created-alpha"));
    }

    @Test
    void getUser_admin_returns200() throws Exception {
        MvcResult createResult = helper.authPost("/api/v1/admin/users", adminSession,
                """
                {
                  "username": "get-target-beta",
                  "password": "StrongP@ss12345!",
                  "displayName": "Get User Target",
                  "roles": ["ROLE_USER"]
                }
                """)
            .andExpect(status().isCreated())
            .andReturn();

        Long userId = helper.extractLong(createResult, "data.id");

        helper.authGet("/api/v1/admin/users/" + userId, adminSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(userId));
    }

    @Test
    void disableUser_admin_returns200() throws Exception {
        MvcResult createResult = helper.authPost("/api/v1/admin/users", adminSession,
                """
                {
                  "username": "disable-gamma",
                  "password": "StrongP@ss12345!",
                  "displayName": "Disable Target",
                  "roles": ["ROLE_USER"]
                }
                """)
            .andExpect(status().isCreated())
            .andReturn();

        Long userId = helper.extractLong(createResult, "data.id");

        helper.authPostEmpty("/api/v1/admin/users/" + userId + "/disable", adminSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void enableUser_admin_afterDisable_returns200() throws Exception {
        MvcResult createResult = helper.authPost("/api/v1/admin/users", adminSession,
                """
                {
                  "username": "enable-delta",
                  "password": "StrongP@ss12345!",
                  "displayName": "Enable Target",
                  "roles": ["ROLE_USER"]
                }
                """)
            .andExpect(status().isCreated())
            .andReturn();

        Long userId = helper.extractLong(createResult, "data.id");

        // Disable first
        helper.authPostEmpty("/api/v1/admin/users/" + userId + "/disable", adminSession)
            .andExpect(status().isOk());

        // Then enable
        helper.authPostEmpty("/api/v1/admin/users/" + userId + "/enable", adminSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void adminEndpoints_nonAdmin_returns403() throws Exception {
        TestHelper.SessionInfo regularUser = helper.createUserWithSession(
            "umgmt_reg_" + testSuffix, "ROLE_USER");

        helper.authGet("/api/v1/admin/users", regularUser)
            .andExpect(status().isForbidden());
    }
}
