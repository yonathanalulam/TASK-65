package com.culinarycoach.integration;

import com.culinarycoach.domain.entity.AuthSession;
import com.culinarycoach.domain.entity.Role;
import com.culinarycoach.domain.entity.User;
import com.culinarycoach.domain.enums.AccountStatus;
import com.culinarycoach.domain.enums.SessionStatus;
import com.culinarycoach.domain.repository.AuthSessionRepository;
import com.culinarycoach.domain.repository.RoleRepository;
import com.culinarycoach.domain.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Shared helper for integration tests: creates users, sessions, and signs requests.
 */
public class TestHelper {

    private final MockMvc mockMvc;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthSessionRepository authSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TestHelper(MockMvc mockMvc, UserRepository userRepository,
                       RoleRepository roleRepository,
                       AuthSessionRepository authSessionRepository,
                       PasswordEncoder passwordEncoder) {
        this.mockMvc = mockMvc;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.authSessionRepository = authSessionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record SessionInfo(String sessionId, String signingKey, Long userId) {}

    /** Create a user with the given role and return a valid session. */
    public SessionInfo createUserWithSession(String username, String roleName) throws Exception {
        User user = userRepository.findByUsernameIgnoreCase(username).orElseGet(() -> {
            Role role = roleRepository.findByName(roleName).orElseThrow();
            User u = new User();
            u.setUsername(username);
            u.setPasswordHash(passwordEncoder.encode("TestPassword1!xy"));
            u.setStatus(AccountStatus.ACTIVE);
            u.setRoles(Set.of(role));
            return userRepository.save(u);
        });
        return createSession(user.getId());
    }

    /** Create an active session for a user. */
    public SessionInfo createSession(Long userId) throws Exception {
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
        return new SessionInfo(session.getId(), signingKey, userId);
    }

    /** Perform an authenticated GET. */
    public ResultActions authGet(String path, SessionInfo session) throws Exception {
        return mockMvc.perform(get(path)
            .header("X-Session-Id", session.sessionId()));
    }

    /** Perform an authenticated, signed POST with JSON body. */
    public ResultActions authPost(String path, SessionInfo session, String body) throws Exception {
        return mockMvc.perform(signedPost(path, session)
            .content(body));
    }

    /** Perform an authenticated, signed POST with no body. */
    public ResultActions authPostEmpty(String path, SessionInfo session) throws Exception {
        return mockMvc.perform(signedPost(path, session)
            .content("{}"));
    }

    /** Perform an authenticated, signed DELETE. */
    public ResultActions authDelete(String path, SessionInfo session) throws Exception {
        String timestamp = Instant.now().toString();
        String nonce = UUID.randomUUID().toString();
        String signature = sign("DELETE", path, timestamp, nonce, session.signingKey());
        return mockMvc.perform(delete(path)
            .with(csrf())
            .header("X-Session-Id", session.sessionId())
            .header("X-Timestamp", timestamp)
            .header("X-Nonce", nonce)
            .header("X-Signature", signature));
    }

    /** Perform an authenticated, signed PUT with JSON body. */
    public ResultActions authPut(String path, SessionInfo session, String body) throws Exception {
        String timestamp = Instant.now().toString();
        String nonce = UUID.randomUUID().toString();
        String signature = sign("PUT", path, timestamp, nonce, session.signingKey());
        return mockMvc.perform(put(path)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Session-Id", session.sessionId())
            .header("X-Timestamp", timestamp)
            .header("X-Nonce", nonce)
            .header("X-Signature", signature)
            .content(body));
    }

    /** Build a signed POST request builder. */
    public MockHttpServletRequestBuilder signedPost(String path, SessionInfo session) throws Exception {
        String timestamp = Instant.now().toString();
        String nonce = UUID.randomUUID().toString();
        String signature = sign("POST", path, timestamp, nonce, session.signingKey());
        return post(path)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Session-Id", session.sessionId())
            .header("X-Timestamp", timestamp)
            .header("X-Nonce", nonce)
            .header("X-Signature", signature);
    }

    /** Compute HMAC-SHA256 signature. */
    public String sign(String method, String path, String timestamp, String nonce, String keyBase64) throws Exception {
        String payload = method + "\n" + path + "\n" + timestamp + "\n" + nonce;
        byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
        return Base64.getEncoder().encodeToString(
            mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    /** Extract a field from JSON response. */
    public String extractField(MvcResult result, String jsonPath) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        String[] parts = jsonPath.replace("$.", "").split("\\.");
        JsonNode node = root;
        for (String part : parts) {
            node = node.get(part);
            if (node == null) return null;
        }
        return node.isNull() ? null : node.asText();
    }

    /** Extract a long from JSON response. */
    public Long extractLong(MvcResult result, String jsonPath) throws Exception {
        String val = extractField(result, jsonPath);
        return val == null ? null : Long.parseLong(val);
    }
}
