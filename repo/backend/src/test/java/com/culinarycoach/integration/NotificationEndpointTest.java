package com.culinarycoach.integration;

import com.culinarycoach.domain.enums.NotificationType;
import com.culinarycoach.domain.repository.AuthSessionRepository;
import com.culinarycoach.domain.repository.RoleRepository;
import com.culinarycoach.domain.repository.UserRepository;
import com.culinarycoach.service.NotificationService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the Notification endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class NotificationEndpointTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private AuthSessionRepository authSessionRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private NotificationService notificationService;

    private TestHelper helper;
    private TestHelper.SessionInfo userSession;

    @BeforeEach
    void setUp(TestInfo testInfo) throws Exception {
        helper = new TestHelper(mockMvc, userRepository, roleRepository,
            authSessionRepository, passwordEncoder);
        String uniqueName = "notif_" + testInfo.getTestMethod().orElseThrow().getName();
        userSession = helper.createUserWithSession(uniqueName, "ROLE_USER");
    }

    @Test
    void listNotifications_authenticated_returns200() throws Exception {
        helper.authGet("/api/v1/notifications", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void getUnreadCount_authenticated_returnsNumericValue() throws Exception {
        helper.authGet("/api/v1/notifications/unread-count", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.unreadCount").isNumber());
    }

    @Test
    void listNotifications_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
            .andExpect(status().isForbidden());
    }

    @Test
    void getUnreadCount_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/unread-count"))
            .andExpect(status().isForbidden());
    }

    // ── Mark Notification Read ──────────────────────────────────────

    @Test
    void markNotificationRead_returns200() throws Exception {
        // Create a notification for this user
        var notification = notificationService.createNotification(
            userSession.userId(), NotificationType.PRACTICE_DUE,
            "Test Read Notification", "Please practice",
            "QUESTION", 1L, Instant.now(), null);

        helper.authPostEmpty("/api/v1/notifications/" + notification.getId() + "/read",
                userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(notification.getId()))
            .andExpect(jsonPath("$.data.status").value("READ"));
    }

    @Test
    void markNotificationRead_otherUser_returns403() throws Exception {
        // Create notification for the test user
        var notification = notificationService.createNotification(
            userSession.userId(), NotificationType.PRACTICE_DUE,
            "Other User Test", "Message",
            "QUESTION", 1L, Instant.now(), null);

        // Try to read it as a different user
        TestHelper.SessionInfo otherSession = helper.createUserWithSession(
            "notif_other_read", "ROLE_USER");

        helper.authPostEmpty("/api/v1/notifications/" + notification.getId() + "/read",
                otherSession)
            .andExpect(status().isForbidden());
    }

    // ── Dismiss Notification ────────────────────────────────────────

    @Test
    void dismissNotification_returns200() throws Exception {
        var notification = notificationService.createNotification(
            userSession.userId(), NotificationType.PRACTICE_DUE,
            "Test Dismiss Notification", "Please practice",
            "QUESTION", 2L, Instant.now(), null);

        helper.authPostEmpty("/api/v1/notifications/" + notification.getId() + "/dismiss",
                userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(notification.getId()))
            .andExpect(jsonPath("$.data.status").value("DISMISSED"));
    }

    @Test
    void dismissNotification_otherUser_returns403() throws Exception {
        var notification = notificationService.createNotification(
            userSession.userId(), NotificationType.PRACTICE_DUE,
            "Other User Dismiss", "Message",
            "QUESTION", 2L, Instant.now(), null);

        TestHelper.SessionInfo otherSession = helper.createUserWithSession(
            "notif_other_dismiss", "ROLE_USER");

        helper.authPostEmpty("/api/v1/notifications/" + notification.getId() + "/dismiss",
                otherSession)
            .andExpect(status().isForbidden());
    }
}
