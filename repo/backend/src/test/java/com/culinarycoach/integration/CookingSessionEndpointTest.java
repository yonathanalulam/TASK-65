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
 * Integration tests for the Cooking Session endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class CookingSessionEndpointTest {

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
        // Use a unique username per test method to avoid session limit conflicts
        String uniqueName = "cs_" + testInfo.getTestMethod().orElseThrow().getName();
        userSession = helper.createUserWithSession(uniqueName, "ROLE_USER");
    }

    @Test
    void listSessions_initiallyEmpty() throws Exception {
        helper.authGet("/api/v1/cooking/sessions", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void createSession_returnsSessionResponse() throws Exception {
        String body = """
            {
              "recipeTitle": "Test Recipe",
              "steps": [
                {
                  "title": "Step 1",
                  "description": "Do the first thing",
                  "expectedDurationSeconds": 60
                }
              ]
            }
            """;

        helper.authPost("/api/v1/cooking/sessions", userSession, body)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").exists())
            .andExpect(jsonPath("$.data.recipeTitle").value("Test Recipe"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.totalSteps").value(1));
    }

    @Test
    void getSessionDetail_hasSteps() throws Exception {
        String body = """
            {
              "recipeTitle": "Detail Test",
              "steps": [
                {
                  "title": "Chop onions",
                  "description": "Dice them finely",
                  "expectedDurationSeconds": 120
                },
                {
                  "title": "Saute",
                  "description": "Cook until golden",
                  "expectedDurationSeconds": 300
                }
              ]
            }
            """;

        MvcResult createResult = helper.authPost("/api/v1/cooking/sessions", userSession, body)
            .andExpect(status().isOk())
            .andReturn();

        Long sessionId = helper.extractLong(createResult, "data.id");

        helper.authGet("/api/v1/cooking/sessions/" + sessionId, userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(sessionId))
            .andExpect(jsonPath("$.data.steps").isArray())
            .andExpect(jsonPath("$.data.steps.length()").value(2));
    }

    @Test
    void completeStep_updatesSession() throws Exception {
        String body = """
            {
              "recipeTitle": "Step Complete Test",
              "steps": [
                {
                  "title": "Only Step",
                  "description": "Do it",
                  "expectedDurationSeconds": 60
                }
              ]
            }
            """;

        MvcResult createResult = helper.authPost("/api/v1/cooking/sessions", userSession, body)
            .andExpect(status().isOk())
            .andReturn();

        Long sessionId = helper.extractLong(createResult, "data.id");

        // Complete step 0 (first step, 0-indexed)
        helper.authPostEmpty("/api/v1/cooking/sessions/" + sessionId + "/steps/0/complete", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.lastCompletedStepOrder").value(0));
    }

    @Test
    void pauseAndResume_changesStatus() throws Exception {
        String body = """
            {
              "recipeTitle": "Pause Resume Test",
              "steps": [
                {
                  "title": "A step",
                  "description": "Something",
                  "expectedDurationSeconds": 60
                }
              ]
            }
            """;

        MvcResult createResult = helper.authPost("/api/v1/cooking/sessions", userSession, body)
            .andExpect(status().isOk())
            .andReturn();

        Long sessionId = helper.extractLong(createResult, "data.id");

        // Pause
        helper.authPostEmpty("/api/v1/cooking/sessions/" + sessionId + "/pause", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("PAUSED"));

        // Resume
        helper.authPostEmpty("/api/v1/cooking/sessions/" + sessionId + "/resume", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void abandonSession_changesStatus() throws Exception {
        String body = """
            {
              "recipeTitle": "Abandon Test",
              "steps": [
                {
                  "title": "A step",
                  "description": "Something",
                  "expectedDurationSeconds": 60
                }
              ]
            }
            """;

        MvcResult createResult = helper.authPost("/api/v1/cooking/sessions", userSession, body)
            .andExpect(status().isOk())
            .andReturn();

        Long sessionId = helper.extractLong(createResult, "data.id");

        helper.authPostEmpty("/api/v1/cooking/sessions/" + sessionId + "/abandon", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("ABANDONED"));
    }

    @Test
    void otherUser_cannotAccessSession() throws Exception {
        // Create a session as the main user
        String body = """
            {
              "recipeTitle": "Private Session",
              "steps": [
                {
                  "title": "Secret step",
                  "description": "Hidden",
                  "expectedDurationSeconds": 60
                }
              ]
            }
            """;

        MvcResult createResult = helper.authPost("/api/v1/cooking/sessions", userSession, body)
            .andExpect(status().isOk())
            .andReturn();

        Long sessionId = helper.extractLong(createResult, "data.id");

        // Create a different user
        TestHelper.SessionInfo otherSession = helper.createUserWithSession("cs_other_user", "ROLE_USER");

        // Other user attempts to access the session
        helper.authGet("/api/v1/cooking/sessions/" + sessionId, otherSession)
            .andExpect(status().isForbidden());
    }

    @Test
    void listSessions_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/cooking/sessions"))
            .andExpect(status().isForbidden());
    }
}
