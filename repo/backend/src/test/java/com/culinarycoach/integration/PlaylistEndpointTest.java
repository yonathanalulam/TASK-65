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
 * Integration tests for the Playlist CRUD endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class PlaylistEndpointTest {

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
        // Use a unique username per test method to avoid state leaks between tests
        String uniqueName = "pl_" + testInfo.getTestMethod().orElseThrow().getName();
        userSession = helper.createUserWithSession(uniqueName, "ROLE_USER");
    }

    @Test
    void getUserPlaylists_initiallyEmpty() throws Exception {
        helper.authGet("/api/v1/audio/playlists", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void createPlaylist_returnsCreatedPlaylist() throws Exception {
        String body = """
            {"name":"Test Playlist","description":"A test description"}
            """;

        helper.authPost("/api/v1/audio/playlists", userSession, body)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").exists())
            .andExpect(jsonPath("$.data.name").value("Test Playlist"))
            .andExpect(jsonPath("$.data.description").value("A test description"));
    }

    @Test
    void getPlaylistDetail_afterCreate_returnsDetail() throws Exception {
        String body = """
            {"name":"Detail Test","description":"Desc"}
            """;

        MvcResult createResult = helper.authPost("/api/v1/audio/playlists", userSession, body)
            .andExpect(status().isOk())
            .andReturn();

        Long playlistId = helper.extractLong(createResult, "data.id");

        helper.authGet("/api/v1/audio/playlists/" + playlistId, userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(playlistId))
            .andExpect(jsonPath("$.data.name").value("Detail Test"))
            .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void updatePlaylist_changesName() throws Exception {
        String createBody = """
            {"name":"Original Name","description":"Desc"}
            """;

        MvcResult createResult = helper.authPost("/api/v1/audio/playlists", userSession, createBody)
            .andExpect(status().isOk())
            .andReturn();

        Long playlistId = helper.extractLong(createResult, "data.id");

        String updateBody = """
            {"name":"Updated Name","description":"Updated Desc"}
            """;

        helper.authPut("/api/v1/audio/playlists/" + playlistId, userSession, updateBody)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Updated Name"));
    }

    @Test
    void addItem_thenRemoveItem() throws Exception {
        String createBody = """
            {"name":"Item Test","description":"Desc"}
            """;

        MvcResult createResult = helper.authPost("/api/v1/audio/playlists", userSession, createBody)
            .andExpect(status().isOk())
            .andReturn();

        Long playlistId = helper.extractLong(createResult, "data.id");

        // Add asset 1 to the playlist
        String addItemBody = """
            {"audioAssetId":1}
            """;

        helper.authPost("/api/v1/audio/playlists/" + playlistId + "/items", userSession, addItemBody)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Verify item is present
        helper.authGet("/api/v1/audio/playlists/" + playlistId, userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.items[0].id").value(1));

        // Remove the item (by assetId)
        helper.authDelete("/api/v1/audio/playlists/" + playlistId + "/items/1", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Verify items are empty
        helper.authGet("/api/v1/audio/playlists/" + playlistId, userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void deletePlaylist_removesIt() throws Exception {
        String createBody = """
            {"name":"Delete Me","description":"Desc"}
            """;

        MvcResult createResult = helper.authPost("/api/v1/audio/playlists", userSession, createBody)
            .andExpect(status().isOk())
            .andReturn();

        Long playlistId = helper.extractLong(createResult, "data.id");

        helper.authDelete("/api/v1/audio/playlists/" + playlistId, userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Verify the playlist is gone (should return 400)
        helper.authGet("/api/v1/audio/playlists/" + playlistId, userSession)
            .andExpect(status().isBadRequest());
    }

    @Test
    void getPlaylists_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/audio/playlists"))
            .andExpect(status().isForbidden());
    }
}
