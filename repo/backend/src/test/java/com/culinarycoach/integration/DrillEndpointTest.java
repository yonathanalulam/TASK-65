package com.culinarycoach.integration;

import com.culinarycoach.domain.repository.AuthSessionRepository;
import com.culinarycoach.domain.repository.RoleRepository;
import com.culinarycoach.domain.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Integration tests for the Drill endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class DrillEndpointTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private AuthSessionRepository authSessionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private TestHelper helper;
    private TestHelper.SessionInfo userSession;

    @BeforeEach
    void setUp(TestInfo testInfo) throws Exception {
        helper = new TestHelper(mockMvc, userRepository, roleRepository,
            authSessionRepository, passwordEncoder);
        String uniqueName = "drill_" + testInfo.getTestMethod().orElseThrow().getName();
        userSession = helper.createUserWithSession(uniqueName, "ROLE_USER");
    }

    /** Submit a wrong answer to question 2, creating a notebook entry. Returns the entry ID. */
    private Long createNotebookEntry() throws Exception {
        helper.authPost("/api/v1/questions/2/answer", userSession,
            """
            {"userAnswer":"totally wrong","flagged":false}
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.correct").value(false));

        MvcResult listResult = helper.authGet("/api/v1/notebook/entries", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").isNotEmpty())
            .andReturn();

        JsonNode root = objectMapper.readTree(listResult.getResponse().getContentAsString());
        return root.path("data").path("content").get(0).path("id").asLong();
    }

    @Test
    void launchRetryDrill_returns200() throws Exception {
        Long entryId = createNotebookEntry();

        helper.authPost("/api/v1/drills/retry", userSession,
                "{\"entryId\":" + entryId + "}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").exists())
            .andExpect(jsonPath("$.data.drillType").exists());
    }

    @Test
    void submitDrillAnswer_returns200() throws Exception {
        Long entryId = createNotebookEntry();

        MvcResult drillResult = helper.authPost("/api/v1/drills/retry", userSession,
                "{\"entryId\":" + entryId + "}")
            .andExpect(status().isOk())
            .andReturn();

        Long drillId = helper.extractLong(drillResult, "data.id");

        // Question 2 is lesson 1: "What knife cut produces matchstick-sized strips?"
        helper.authPost("/api/v1/drills/" + drillId + "/answer", userSession,
                """
                {"questionId":2,"answer":"julienne"}
                """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void completeDrill_returns200() throws Exception {
        Long entryId = createNotebookEntry();

        MvcResult drillResult = helper.authPost("/api/v1/drills/retry", userSession,
                "{\"entryId\":" + entryId + "}")
            .andExpect(status().isOk())
            .andReturn();

        Long drillId = helper.extractLong(drillResult, "data.id");

        // Submit an answer first
        helper.authPost("/api/v1/drills/" + drillId + "/answer", userSession,
                """
                {"questionId":2,"answer":"julienne"}
                """)
            .andExpect(status().isOk());

        // Complete the drill
        helper.authPostEmpty("/api/v1/drills/" + drillId + "/complete", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").exists());
    }

    @Test
    void drills_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/drills/retry"))
            .andExpect(status().isForbidden());
    }
}
