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
 * Integration tests for the Audio Library browsing endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class AudioLibraryEndpointTest {

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
        userSession = helper.createUserWithSession("audio_lib_user", "ROLE_USER");
    }

    @Test
    void browseAssets_authenticated_returnsPaginatedList() throws Exception {
        helper.authGet("/api/v1/audio/assets", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.content").isNotEmpty());
    }

    @Test
    void browseAssets_searchByKnife_returnsMatchingResults() throws Exception {
        helper.authGet("/api/v1/audio/assets?search=Knife", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.content[0].title").value("Knife Skills 101"));
    }

    @Test
    void getAssetDetails_existingId_returnsAsset() throws Exception {
        helper.authGet("/api/v1/audio/assets/1", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.title").value("Knife Skills 101"));
    }

    @Test
    void getAssetDetails_nonExistentId_returns400() throws Exception {
        helper.authGet("/api/v1/audio/assets/99999", userSession)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void browseAssets_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/audio/assets"))
            .andExpect(status().isForbidden());
    }
}
