package com.culinarycoach.integration;

import com.culinarycoach.domain.repository.AuthSessionRepository;
import com.culinarycoach.domain.repository.RoleRepository;
import com.culinarycoach.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the Question endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class QuestionEndpointTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private AuthSessionRepository authSessionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private TestHelper helper;
    private TestHelper.SessionInfo userSession;

    @BeforeEach
    void setUp() throws Exception {
        helper = new TestHelper(mockMvc, userRepository, roleRepository,
            authSessionRepository, passwordEncoder);
        userSession = helper.createUserWithSession("question_user", "ROLE_USER");
    }

    @Test
    void listQuestions_authenticated_returnsList() throws Exception {
        helper.authGet("/api/v1/questions?lessonId=1", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.content").isNotEmpty());
    }

    @Test
    void listQuestions_filterByLesson_returnsFilteredResults() throws Exception {
        helper.authGet("/api/v1/questions?lessonId=1", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.content").isNotEmpty());
    }

    @Test
    void getQuestion_existingId_returnsQuestion() throws Exception {
        helper.authGet("/api/v1/questions/1", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void submitAnswer_correctAnswer_returnsCorrectClassification() throws Exception {
        helper.authPost("/api/v1/questions/1/answer", userSession,
                """
                {"userAnswer":"claw grip","flagged":false}
                """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.classification").value("CORRECT"))
            .andExpect(jsonPath("$.data.correct").value(true));
    }

    @Test
    void submitAnswer_wrongAnswer_returnsWrongClassification() throws Exception {
        helper.authPost("/api/v1/questions/1/answer", userSession,
                """
                {"userAnswer":"wrong answer","flagged":false}
                """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.classification").value("WRONG"))
            .andExpect(jsonPath("$.data.correct").value(false));
    }

    @Test
    void listQuestions_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/questions?lessonId=1"))
            .andExpect(status().isForbidden());
    }
}
