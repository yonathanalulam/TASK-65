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
 * Integration tests for the Audio Cache HTTP endpoints.
 * Supplements the existing AudioCacheIntegrationTest with endpoint-level testing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class AudioCacheEndpointTest {

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
        String uniqueName = "ac_" + testInfo.getTestMethod().orElseThrow().getName();
        userSession = helper.createUserWithSession(uniqueName, "ROLE_USER");
    }

    @Test
    void getCacheStatus_returnsListResponse() throws Exception {
        helper.authGet("/api/v1/audio/cache/status", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getStorageMeter_returnsMeterWithQuota() throws Exception {
        helper.authGet("/api/v1/audio/cache/storage-meter", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalQuotaBytes").exists())
            .andExpect(jsonPath("$.data.usedBytes").exists())
            .andExpect(jsonPath("$.data.percentUsed").exists());
    }

    @Test
    void downloadSegment_freeAssetSegment_returns200() throws Exception {
        // Segment 11 belongs to asset 11 ("Seasonal Salads"), which has no bundle (free)
        helper.authPostEmpty("/api/v1/audio/cache/download/11", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.segmentId").value(11))
            .andExpect(jsonPath("$.data.status").value("CACHED_VALID"));
    }

    @Test
    void getCacheStatus_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/audio/cache/status"))
            .andExpect(status().isForbidden());
    }

    // ── Download Asset (all segments) ───────────────────────────────

    @Test
    void downloadAsset_freeAsset_returnsCacheEntries() throws Exception {
        // Asset 11 ("Seasonal Salads") is free (no bundle requirement)
        helper.authPostEmpty("/api/v1/audio/cache/download-asset/11", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data").isNotEmpty());
    }

    @Test
    void downloadAsset_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/audio/cache/download-asset/11"))
            .andExpect(status().isForbidden());
    }

    // ── Stream Cached Segment ───────────────────────────────────────

    @Test
    void streamCachedSegment_afterDownload_returnsAudioStream() throws Exception {
        // First download a segment to create a cache entry
        MvcResult downloadResult = helper.authPostEmpty(
                "/api/v1/audio/cache/download/11", userSession)
            .andExpect(status().isOk())
            .andReturn();

        // Extract the manifest ID (field "id") from the cache entry response
        Long manifestId = helper.extractLong(downloadResult, "data.id");

        // Stream the cached manifest
        helper.authGet("/api/v1/audio/cache/" + manifestId + "/stream", userSession)
            .andExpect(status().isOk());
    }

    @Test
    void streamCachedSegment_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/audio/cache/999/stream"))
            .andExpect(status().isForbidden());
    }
}
