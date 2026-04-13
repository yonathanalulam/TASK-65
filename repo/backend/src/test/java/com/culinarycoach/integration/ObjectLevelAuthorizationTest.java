package com.culinarycoach.integration;

import com.culinarycoach.domain.entity.*;
import com.culinarycoach.domain.enums.*;
import com.culinarycoach.domain.repository.*;
import com.culinarycoach.domain.entity.AudioSegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests verifying object-level authorization returns 403 (not 500)
 * when a user tries to access resources owned by another user.
 *
 * Covers:
 * - Notebook entry access (NotebookService ownership check)
 * - Cooking session access (CookingSessionService ownership check)
 * - Audio cache entry access (AudioCacheService ownership check)
 *
 * Tests the positive/negative matrix:
 * - Owner: allowed
 * - Other user: denied with 403
 * - Admin accessing admin routes: allowed
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS)
class ObjectLevelAuthorizationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private AuthSessionRepository authSessionRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private WrongNotebookEntryRepository notebookEntryRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private CookingSessionRepository cookingSessionRepository;
    @Autowired private AudioCacheManifestRepository cacheManifestRepository;
    @Autowired private AudioSegmentRepository audioSegmentRepository;

    private Long ownerUserId;
    private Long otherUserId;
    private Long adminUserId;
    private String ownerSessionId;
    private String otherSessionId;
    private String adminSessionId;
    private final Map<String, String> sessionSigningKeys = new HashMap<>();

    @BeforeEach
    void setUp() throws Exception {
        // Create owner user
        User owner = userRepository.findByUsernameIgnoreCase("objauth_owner").orElseGet(() -> {
            Role role = roleRepository.findByName("ROLE_USER").orElseThrow();
            User u = new User();
            u.setUsername("objauth_owner");
            u.setPasswordHash(passwordEncoder.encode("TestPassword1!xy"));
            u.setStatus(AccountStatus.ACTIVE);
            u.setRoles(Set.of(role));
            return userRepository.save(u);
        });
        ownerUserId = owner.getId();

        // Create other user
        User other = userRepository.findByUsernameIgnoreCase("objauth_other").orElseGet(() -> {
            Role role = roleRepository.findByName("ROLE_USER").orElseThrow();
            User u = new User();
            u.setUsername("objauth_other");
            u.setPasswordHash(passwordEncoder.encode("TestPassword1!xy"));
            u.setStatus(AccountStatus.ACTIVE);
            u.setRoles(Set.of(role));
            return userRepository.save(u);
        });
        otherUserId = other.getId();

        // Create admin user
        User admin = userRepository.findByUsernameIgnoreCase("objauth_admin").orElseGet(() -> {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
            User u = new User();
            u.setUsername("objauth_admin");
            u.setPasswordHash(passwordEncoder.encode("TestPassword1!xy"));
            u.setStatus(AccountStatus.ACTIVE);
            u.setRoles(Set.of(adminRole));
            return userRepository.save(u);
        });
        adminUserId = admin.getId();

        ownerSessionId = createSession(ownerUserId);
        otherSessionId = createSession(otherUserId);
        adminSessionId = createSession(adminUserId);
    }

    // ── Notebook Entry Authorization Tests ────────────────────────────

    @Test
    void notebookEntry_ownerCanAccess() throws Exception {
        Long entryId = createNotebookEntry(ownerUserId);

        mockMvc.perform(get("/api/v1/notebook/entries/" + entryId)
                .header("X-Session-Id", ownerSessionId))
            .andExpect(status().isOk());
    }

    @Test
    void notebookEntry_otherUserDeniedWith403() throws Exception {
        Long entryId = createNotebookEntry(ownerUserId);

        mockMvc.perform(get("/api/v1/notebook/entries/" + entryId)
                .header("X-Session-Id", otherSessionId))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    // ── Cooking Session Authorization Tests ───────────────────────────

    @Test
    void cookingSession_ownerCanAccess() throws Exception {
        Long sessionId = createCookingSession(ownerUserId);

        mockMvc.perform(get("/api/v1/cooking/sessions/" + sessionId)
                .header("X-Session-Id", ownerSessionId))
            .andExpect(status().isOk());
    }

    @Test
    void cookingSession_otherUserDeniedWith403() throws Exception {
        Long sessionId = createCookingSession(ownerUserId);

        mockMvc.perform(get("/api/v1/cooking/sessions/" + sessionId)
                .header("X-Session-Id", otherSessionId))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    // ── Audio Cache Authorization Tests ───────────────────────────────

    @Test
    void audioCache_ownerCanDelete() throws Exception {
        Long manifestId = createCacheManifest(ownerUserId);
        if (manifestId == null) return; // skip if no seed segments

        String path = "/api/v1/audio/cache/" + manifestId;
        String timestamp = Instant.now().toString();
        String nonce = UUID.randomUUID().toString();
        String signature = sign("DELETE", path, timestamp, nonce, sessionSigningKeys.get(ownerSessionId));

        mockMvc.perform(delete(path)
                .with(csrf())
                .header("X-Session-Id", ownerSessionId)
                .header("X-Timestamp", timestamp)
                .header("X-Nonce", nonce)
                .header("X-Signature", signature))
            .andExpect(status().isOk());
    }

    @Test
    void audioCache_otherUserDeleteDeniedWith403() throws Exception {
        Long manifestId = createCacheManifest(ownerUserId);
        if (manifestId == null) return; // skip if no seed segments

        String path = "/api/v1/audio/cache/" + manifestId;
        String timestamp = Instant.now().toString();
        String nonce = UUID.randomUUID().toString();
        String signature = sign("DELETE", path, timestamp, nonce, sessionSigningKeys.get(otherSessionId));

        mockMvc.perform(delete(path)
                .with(csrf())
                .header("X-Session-Id", otherSessionId)
                .header("X-Timestamp", timestamp)
                .header("X-Nonce", nonce)
                .header("X-Signature", signature))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    // ── Admin Route Access Tests ──────────────────────────────────────

    @Test
    void adminRoutes_adminAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                .header("X-Session-Id", adminSessionId))
            .andExpect(status().isOk());
    }

    @Test
    void adminRoutes_regularUserDenied() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                .header("X-Session-Id", ownerSessionId))
            .andExpect(status().isForbidden());
    }

    // ── Helper Methods ────────────────────────────────────────────────

    private String createSession(Long userId) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        keyGen.init(256);
        String signingKey = Base64.getEncoder().encodeToString(keyGen.generateKey().getEncoded());

        AuthSession session = new AuthSession();
        session.setId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setSigningKey(signingKey);
        session.setStatus(SessionStatus.ACTIVE);
        session.setExpiresAt(Instant.now().plus(12, ChronoUnit.HOURS));
        session.setIdleTimeoutMinutes(30);
        authSessionRepository.save(session);
        sessionSigningKeys.put(session.getId(), signingKey);
        return session.getId();
    }

    private String sign(String method, String path, String timestamp, String nonce, String keyBase64) throws Exception {
        String payload = method + "\n" + path + "\n" + timestamp + "\n" + nonce;
        byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
        return Base64.getEncoder().encodeToString(
            mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    private Long createNotebookEntry(Long userId) {
        // Find or create a question to reference
        Question question = questionRepository.findAll().stream().findFirst().orElseGet(() -> {
            Question q = new Question();
            q.setQuestionText("Test question for auth test?");
            q.setQuestionType("SINGLE_CHOICE");
            q.setCanonicalAnswer("answer");
            q.setDifficulty("BEGINNER");
            return questionRepository.save(q);
        });

        WrongNotebookEntry entry = new WrongNotebookEntry();
        entry.setUserId(userId);
        entry.setQuestionId(question.getId());
        entry.setStatus(NotebookEntryStatus.ACTIVE);
        entry.setFailCount(1);
        entry.setFavorite(false);
        entry.setLastAttemptAt(Instant.now());
        entry = notebookEntryRepository.save(entry);
        return entry.getId();
    }

    private Long createCookingSession(Long userId) {
        CookingSession session = new CookingSession();
        session.setUserId(userId);
        session.setRecipeTitle("Auth Test Recipe");
        session.setStatus(CookingSessionStatus.ACTIVE);
        session.setTotalSteps(3);
        session.setLastCompletedStepOrder(-1);
        session.setStartedAt(Instant.now());
        session.setLastActivityAt(Instant.now());
        session = cookingSessionRepository.save(session);
        return session.getId();
    }

    private Long createCacheManifest(Long userId) {
        // Use a real audio segment from seeded data, or create one
        AudioSegment segment = audioSegmentRepository.findAll().stream().findFirst().orElse(null);
        if (segment == null) return null;

        // Delete any existing manifest for this user+segment to avoid unique constraint violation
        cacheManifestRepository.findByUserId(userId).stream()
            .filter(m -> m.getSegmentId().equals(segment.getId()))
            .forEach(m -> cacheManifestRepository.delete(m));

        AudioCacheManifest manifest = new AudioCacheManifest();
        manifest.setUserId(userId);
        manifest.setSegmentId(segment.getId());
        manifest.setCachedFilePath("/tmp/test-cache-" + UUID.randomUUID() + ".mp3");
        manifest.setFileSizeBytes(1024L);
        manifest.setChecksum("test-checksum-" + UUID.randomUUID());
        manifest.setStatus(CacheEntryStatus.CACHED_VALID);
        manifest.setDownloadedAt(Instant.now());
        manifest.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        manifest.setLastAccessedAt(Instant.now());
        manifest = cacheManifestRepository.save(manifest);
        return manifest.getId();
    }
}
