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

import java.time.Instant;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the Parent/Coach review endpoints and admin assignment management.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class ParentCoachEndpointTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private AuthSessionRepository authSessionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private TestHelper helper;
    private TestHelper.SessionInfo adminSession;
    private TestHelper.SessionInfo coachSession;
    private TestHelper.SessionInfo studentSession;
    private String testSuffix;

    @BeforeEach
    void setUp(TestInfo testInfo) throws Exception {
        helper = new TestHelper(mockMvc, userRepository, roleRepository,
            authSessionRepository, passwordEncoder);
        testSuffix = testInfo.getTestMethod().orElseThrow().getName();
        adminSession = helper.createUserWithSession("pc_adm_" + testSuffix, "ROLE_ADMIN");
        coachSession = helper.createUserWithSession("pc_cch_" + testSuffix, "ROLE_PARENT_COACH");
        studentSession = helper.createUserWithSession("pc_stu_" + testSuffix, "ROLE_USER");
    }

    /** Assign the student to the coach via admin endpoint. */
    private void assignStudentToCoach() throws Exception {
        String body = "{\"coachUserId\":" + coachSession.userId()
            + ",\"studentUserId\":" + studentSession.userId() + "}";
        helper.authPost("/api/v1/admin/review/assignments", adminSession, body)
            .andExpect(status().isOk());
    }

    @Test
    void assignAndListStudents_coachSeesAssignedStudent() throws Exception {
        assignStudentToCoach();

        helper.authGet("/api/v1/review/students", coachSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data").isNotEmpty());
    }

    @Test
    void reviewStudentNotebook_coachCanReview() throws Exception {
        assignStudentToCoach();

        helper.authGet("/api/v1/review/students/" + studentSession.userId()
                + "/notebook?reason=PROGRESS_CHECK", coachSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void reviewStudentAttempts_coachCanReview() throws Exception {
        assignStudentToCoach();

        helper.authGet("/api/v1/review/students/" + studentSession.userId()
                + "/attempts?reason=PROGRESS_CHECK", coachSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void reviewStudentCookingHistory_coachCanReview() throws Exception {
        assignStudentToCoach();

        helper.authGet("/api/v1/review/students/" + studentSession.userId()
                + "/cooking-history?reason=PROGRESS_CHECK", coachSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void reviewUnassignedStudent_coachDenied_returns403() throws Exception {
        // Create a different student who is NOT assigned to the coach
        TestHelper.SessionInfo unassignedStudent = helper.createUserWithSession(
            "pc_unassigned_" + testSuffix, "ROLE_USER");

        helper.authGet("/api/v1/review/students/" + unassignedStudent.userId()
                + "/notebook?reason=PROGRESS_CHECK", coachSession)
            .andExpect(status().isForbidden());
    }

    @Test
    void adminListAssignments_returns200() throws Exception {
        assignStudentToCoach();

        helper.authGet("/api/v1/admin/review/assignments?coachUserId=" + coachSession.userId(),
                adminSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void adminRevokeAssignment_returns200() throws Exception {
        assignStudentToCoach();

        // Sign with URI path only (not query params) since the server uses getRequestURI()
        String path = "/api/v1/admin/review/assignments";
        String timestamp = Instant.now().toString();
        String nonce = UUID.randomUUID().toString();
        String signature = helper.sign("DELETE", path, timestamp, nonce, adminSession.signingKey());

        mockMvc.perform(delete(path)
                .with(csrf())
                .header("X-Session-Id", adminSession.sessionId())
                .header("X-Timestamp", timestamp)
                .header("X-Nonce", nonce)
                .header("X-Signature", signature)
                .param("coachUserId", coachSession.userId().toString())
                .param("studentUserId", studentSession.userId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
