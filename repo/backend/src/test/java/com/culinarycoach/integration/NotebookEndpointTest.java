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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the Notebook endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class NotebookEndpointTest {

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
        String uniqueName = "nb_" + testInfo.getTestMethod().orElseThrow().getName();
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

        // Fetch entries to find the newly created one
        MvcResult listResult = helper.authGet("/api/v1/notebook/entries", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").isNotEmpty())
            .andReturn();

        JsonNode root = objectMapper.readTree(listResult.getResponse().getContentAsString());
        return root.path("data").path("content").get(0).path("id").asLong();
    }

    @Test
    void listEntries_afterWrongAnswer_hasContent() throws Exception {
        createNotebookEntry();

        helper.authGet("/api/v1/notebook/entries", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.content").isNotEmpty());
    }

    @Test
    void getEntryDetail_returnsEntry() throws Exception {
        Long entryId = createNotebookEntry();

        helper.authGet("/api/v1/notebook/entries/" + entryId, userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(entryId));
    }

    @Test
    void addNote_returnsUpdatedEntry() throws Exception {
        Long entryId = createNotebookEntry();

        helper.authPost("/api/v1/notebook/entries/" + entryId + "/notes", userSession,
                """
                {"noteText":"My note"}
                """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void addTag_returnsUpdatedEntry() throws Exception {
        Long entryId = createNotebookEntry();

        helper.authPost("/api/v1/notebook/entries/" + entryId + "/tags", userSession,
                """
                {"tagLabel":"Technique Error"}
                """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void removeTag_afterAddingTag_returns200() throws Exception {
        Long entryId = createNotebookEntry();

        // Add a tag first
        helper.authPost("/api/v1/notebook/entries/" + entryId + "/tags", userSession,
                """
                {"tagLabel":"Technique Error"}
                """)
            .andExpect(status().isOk());

        // Tag ID 1 is seeded "Technique Error"
        helper.authDelete("/api/v1/notebook/entries/" + entryId + "/tags/1", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void toggleFavorite_returns200() throws Exception {
        Long entryId = createNotebookEntry();

        helper.authPostEmpty("/api/v1/notebook/entries/" + entryId + "/favorite", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void resolveEntry_returns200() throws Exception {
        Long entryId = createNotebookEntry();

        helper.authPostEmpty("/api/v1/notebook/entries/" + entryId + "/resolve", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void reactivateEntry_afterResolve_returns200() throws Exception {
        Long entryId = createNotebookEntry();

        // Resolve first
        helper.authPostEmpty("/api/v1/notebook/entries/" + entryId + "/resolve", userSession)
            .andExpect(status().isOk());

        // Then reactivate
        helper.authPostEmpty("/api/v1/notebook/entries/" + entryId + "/reactivate", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void archiveEntry_returns200() throws Exception {
        Long entryId = createNotebookEntry();

        // Resolve first (must resolve before archiving)
        helper.authPostEmpty("/api/v1/notebook/entries/" + entryId + "/resolve", userSession)
            .andExpect(status().isOk());

        helper.authPostEmpty("/api/v1/notebook/entries/" + entryId + "/archive", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void listEntries_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/notebook/entries"))
            .andExpect(status().isForbidden());
    }
}
