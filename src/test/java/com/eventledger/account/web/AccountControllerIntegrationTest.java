package com.eventledger.account.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the Account Service covering the core requirements:
 * balance computation, idempotency, out-of-order tolerance and validation.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String tx(String eventId, String type, String amount, String timestamp) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "eventId", eventId,
                "type", type,
                "amount", amount,
                "eventTimestamp", timestamp));
    }

    @Test
    void creditsAndDebits_computeNetBalance() throws Exception {
        String account = "acc-balance";
        apply(account, tx("e1", "CREDIT", "100.00", "2026-07-13T10:00:00Z")).andExpect(status().isOk());
        apply(account, tx("e2", "CREDIT", "50.00", "2026-07-13T10:01:00Z")).andExpect(status().isOk());
        apply(account, tx("e3", "DEBIT", "30.00", "2026-07-13T10:02:00Z")).andExpect(status().isOk());

        // 100 + 50 - 30 = 120
        mockMvc.perform(get("/accounts/{id}/balance", account))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", comparesEqualTo(120.0)));
    }

    @Test
    void duplicateEventId_doesNotDoubleApply() throws Exception {
        String account = "acc-idempotent";
        String body = tx("dup-1", "CREDIT", "75.00", "2026-07-13T10:00:00Z");

        apply(account, body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicate", is(false)));

        // Re-submit the exact same event id
        apply(account, body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicate", is(true)));

        mockMvc.perform(get("/accounts/{id}/balance", account))
                .andExpect(jsonPath("$.balance", comparesEqualTo(75.0)));
    }

    @Test
    void outOfOrderArrival_yieldsSameBalance() throws Exception {
        // Apply an earlier-timestamped event AFTER a later one; balance must be order-independent.
        String account = "acc-ooo";
        apply(account, tx("late", "CREDIT", "200.00", "2026-07-13T12:00:00Z")).andExpect(status().isOk());
        apply(account, tx("early", "DEBIT", "50.00", "2026-07-13T08:00:00Z")).andExpect(status().isOk());

        mockMvc.perform(get("/accounts/{id}/balance", account))
                .andExpect(jsonPath("$.balance", comparesEqualTo(150.0)));

        // recent transactions are returned most-recent-timestamp first
        mockMvc.perform(get("/accounts/{id}", account))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionCount", is(2)))
                .andExpect(jsonPath("$.recentTransactions[0].eventId", is("late")))
                .andExpect(jsonPath("$.recentTransactions[1].eventId", is("early")));
    }

    @Test
    void zeroAmount_isRejected() throws Exception {
        apply("acc-x", tx("z1", "CREDIT", "0.00", "2026-07-13T10:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void negativeAmount_isRejected() throws Exception {
        apply("acc-x", tx("n1", "DEBIT", "-5.00", "2026-07-13T10:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownType_isRejected() throws Exception {
        apply("acc-x", tx("u1", "TRANSFER", "5.00", "2026-07-13T10:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void balanceForUnknownAccount_is404() throws Exception {
        mockMvc.perform(get("/accounts/{id}/balance", "no-such-account"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    private org.springframework.test.web.servlet.ResultActions apply(String account, String body) throws Exception {
        return mockMvc.perform(post("/accounts/{id}/transactions", account)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
