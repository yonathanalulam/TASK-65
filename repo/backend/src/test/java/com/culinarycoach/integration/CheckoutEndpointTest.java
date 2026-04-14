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
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the Checkout endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class CheckoutEndpointTest {

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
        userSession = helper.createUserWithSession("checkout_user", "ROLE_USER");
    }

    @Test
    void listBundles_authenticated_returnsSeededBundles() throws Exception {
        helper.authGet("/api/v1/checkout/bundles", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data").isNotEmpty());
    }

    @Test
    void initiateCheckout_returns200WithTransaction() throws Exception {
        helper.authPost("/api/v1/checkout/initiate", userSession,
                """
                {"bundleIds":[1]}
                """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").exists())
            .andExpect(jsonPath("$.data.status").exists());
    }

    @Test
    void completeCheckout_returnsCompletedTransaction() throws Exception {
        MvcResult initiateResult = helper.authPost("/api/v1/checkout/initiate", userSession,
                """
                {"bundleIds":[1]}
                """)
            .andExpect(status().isOk())
            .andReturn();

        Long transactionId = helper.extractLong(initiateResult, "data.id");

        helper.authPostEmpty("/api/v1/checkout/complete/" + transactionId, userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.receiptNumber").exists());
    }

    @Test
    void listTransactions_afterCheckout_hasContent() throws Exception {
        // Create a completed transaction first
        MvcResult initiateResult = helper.authPost("/api/v1/checkout/initiate", userSession,
                """
                {"bundleIds":[2]}
                """)
            .andExpect(status().isOk())
            .andReturn();

        Long transactionId = helper.extractLong(initiateResult, "data.id");

        helper.authPostEmpty("/api/v1/checkout/complete/" + transactionId, userSession)
            .andExpect(status().isOk());

        helper.authGet("/api/v1/checkout/transactions", userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.content").isNotEmpty());
    }

    @Test
    void getTransactionDetail_returnsTransactionWithReceipt() throws Exception {
        MvcResult initiateResult = helper.authPost("/api/v1/checkout/initiate", userSession,
                """
                {"bundleIds":[3]}
                """)
            .andExpect(status().isOk())
            .andReturn();

        Long transactionId = helper.extractLong(initiateResult, "data.id");

        helper.authPostEmpty("/api/v1/checkout/complete/" + transactionId, userSession)
            .andExpect(status().isOk());

        helper.authGet("/api/v1/checkout/transactions/" + transactionId, userSession)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.receiptNumber").exists());
    }

    @Test
    void getTransactionDetail_otherUserDenied_returns403() throws Exception {
        MvcResult initiateResult = helper.authPost("/api/v1/checkout/initiate", userSession,
                """
                {"bundleIds":[1]}
                """)
            .andExpect(status().isOk())
            .andReturn();

        Long transactionId = helper.extractLong(initiateResult, "data.id");

        helper.authPostEmpty("/api/v1/checkout/complete/" + transactionId, userSession)
            .andExpect(status().isOk());

        // Create a different user
        TestHelper.SessionInfo otherSession = helper.createUserWithSession(
            "checkout_other_user", "ROLE_USER");

        helper.authGet("/api/v1/checkout/transactions/" + transactionId, otherSession)
            .andExpect(status().isForbidden());
    }

    @Test
    void listBundles_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/checkout/bundles"))
            .andExpect(status().isForbidden());
    }
}
