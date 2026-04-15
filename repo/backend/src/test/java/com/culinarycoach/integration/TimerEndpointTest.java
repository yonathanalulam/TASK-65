package com.culinarycoach.integration;

import com.culinarycoach.domain.entity.CookingSessionTimer;
import com.culinarycoach.domain.enums.TimerStatus;
import com.culinarycoach.domain.repository.AuthSessionRepository;
import com.culinarycoach.domain.repository.CookingSessionTimerRepository;
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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the Timer endpoints within cooking sessions.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class TimerEndpointTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private AuthSessionRepository authSessionRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private CookingSessionTimerRepository timerRepository;

    private TestHelper helper;
    private TestHelper.SessionInfo userSession;
    private Long sessionId;

    @BeforeEach
    void setUp(TestInfo testInfo) throws Exception {
        helper = new TestHelper(mockMvc, userRepository, roleRepository,
            authSessionRepository, passwordEncoder);
        // Use a unique username per test method to avoid session limit conflicts
        String uniqueName = "tmr_" + testInfo.getTestMethod().orElseThrow().getName();
        userSession = helper.createUserWithSession(uniqueName, "ROLE_USER");

        // Create a cooking session to attach timers to
        String sessionBody = """
            {
              "recipeTitle": "Timer Test Recipe",
              "steps": [
                {
                  "title": "Boil water",
                  "description": "Heat water to boiling",
                  "expectedDurationSeconds": 300
                }
              ]
            }
            """;

        MvcResult result = helper.authPost("/api/v1/cooking/sessions", userSession, sessionBody)
            .andExpect(status().isOk())
            .andReturn();

        sessionId = helper.extractLong(result, "data.id");
    }

    @Test
    void createTimer_returnsTimerResponse() throws Exception {
        String body = """
            {"durationSeconds":60,"label":"Test Timer"}
            """;

        helper.authPost("/api/v1/cooking/sessions/" + sessionId + "/timers", userSession, body)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").exists())
            .andExpect(jsonPath("$.data.label").value("Test Timer"))
            .andExpect(jsonPath("$.data.durationSeconds").value(60))
            .andExpect(jsonPath("$.data.status").value("RUNNING"));
    }

    @Test
    void pauseTimer_changesStatusToPaused() throws Exception {
        String body = """
            {"durationSeconds":120,"label":"Pause Me"}
            """;

        MvcResult createResult = helper.authPost(
            "/api/v1/cooking/sessions/" + sessionId + "/timers", userSession, body)
            .andExpect(status().isOk())
            .andReturn();

        Long timerId = helper.extractLong(createResult, "data.id");

        helper.authPostEmpty(
            "/api/v1/cooking/sessions/" + sessionId + "/timers/" + timerId + "/pause", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("PAUSED"));
    }

    @Test
    void resumeTimer_changesStatusBackToRunning() throws Exception {
        String body = """
            {"durationSeconds":120,"label":"Resume Me"}
            """;

        MvcResult createResult = helper.authPost(
            "/api/v1/cooking/sessions/" + sessionId + "/timers", userSession, body)
            .andExpect(status().isOk())
            .andReturn();

        Long timerId = helper.extractLong(createResult, "data.id");

        // Pause first
        helper.authPostEmpty(
            "/api/v1/cooking/sessions/" + sessionId + "/timers/" + timerId + "/pause", userSession)
            .andExpect(status().isOk());

        // Then resume
        helper.authPostEmpty(
            "/api/v1/cooking/sessions/" + sessionId + "/timers/" + timerId + "/resume", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("RUNNING"));
    }

    @Test
    void cancelTimer_changesStatusToCancelled() throws Exception {
        String body = """
            {"durationSeconds":60,"label":"Cancel Me"}
            """;

        MvcResult createResult = helper.authPost(
            "/api/v1/cooking/sessions/" + sessionId + "/timers", userSession, body)
            .andExpect(status().isOk())
            .andReturn();

        Long timerId = helper.extractLong(createResult, "data.id");

        helper.authPostEmpty(
            "/api/v1/cooking/sessions/" + sessionId + "/timers/" + timerId + "/cancel", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    // ── Acknowledge Timer ───────────────────────────────────────────

    @Test
    void acknowledgeTimer_elapsedTimer_changesStatusToAcknowledged() throws Exception {
        String body = """
            {"durationSeconds":60,"label":"Ack Me"}
            """;

        MvcResult createResult = helper.authPost(
            "/api/v1/cooking/sessions/" + sessionId + "/timers", userSession, body)
            .andExpect(status().isOk())
            .andReturn();

        Long timerId = helper.extractLong(createResult, "data.id");

        // Transition timer to ELAPSED_PENDING_ACK via repository (simulating elapsed timer)
        CookingSessionTimer timer = timerRepository.findById(timerId).orElseThrow();
        timer.setStatus(TimerStatus.ELAPSED_PENDING_ACK);
        timerRepository.save(timer);

        helper.authPostEmpty(
            "/api/v1/cooking/sessions/" + sessionId + "/timers/" + timerId + "/acknowledge",
            userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("ACKNOWLEDGED"));
    }

    @Test
    void acknowledgeTimer_runningTimer_returnsBadRequest() throws Exception {
        String body = """
            {"durationSeconds":120,"label":"Not Elapsed"}
            """;

        MvcResult createResult = helper.authPost(
            "/api/v1/cooking/sessions/" + sessionId + "/timers", userSession, body)
            .andExpect(status().isOk())
            .andReturn();

        Long timerId = helper.extractLong(createResult, "data.id");

        // Timer is still RUNNING, acknowledge should fail with 409 Conflict
        helper.authPostEmpty(
            "/api/v1/cooking/sessions/" + sessionId + "/timers/" + timerId + "/acknowledge",
            userSession)
            .andExpect(status().isConflict());
    }

    // ── Dismiss Timer ───────────────────────────────────────────────

    @Test
    void dismissTimer_elapsedTimer_changesStatusToDismissed() throws Exception {
        String body = """
            {"durationSeconds":60,"label":"Dismiss Me"}
            """;

        MvcResult createResult = helper.authPost(
            "/api/v1/cooking/sessions/" + sessionId + "/timers", userSession, body)
            .andExpect(status().isOk())
            .andReturn();

        Long timerId = helper.extractLong(createResult, "data.id");

        // Transition timer to ELAPSED_PENDING_ACK via repository
        CookingSessionTimer timer = timerRepository.findById(timerId).orElseThrow();
        timer.setStatus(TimerStatus.ELAPSED_PENDING_ACK);
        timerRepository.save(timer);

        helper.authPostEmpty(
            "/api/v1/cooking/sessions/" + sessionId + "/timers/" + timerId + "/dismiss",
            userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("DISMISSED"));
    }

    @Test
    void dismissTimer_runningTimer_returnsBadRequest() throws Exception {
        String body = """
            {"durationSeconds":120,"label":"Not Elapsed Dismiss"}
            """;

        MvcResult createResult = helper.authPost(
            "/api/v1/cooking/sessions/" + sessionId + "/timers", userSession, body)
            .andExpect(status().isOk())
            .andReturn();

        Long timerId = helper.extractLong(createResult, "data.id");

        // Timer is still RUNNING, dismiss should fail with 409 Conflict
        helper.authPostEmpty(
            "/api/v1/cooking/sessions/" + sessionId + "/timers/" + timerId + "/dismiss",
            userSession)
            .andExpect(status().isConflict());
    }
}
