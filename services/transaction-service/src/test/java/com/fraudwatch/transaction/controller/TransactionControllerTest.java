package com.fraudwatch.transaction.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudwatch.transaction.config.SecurityConfig;
import com.fraudwatch.transaction.dto.AccountRequest;
import com.fraudwatch.transaction.dto.AccountResponse;
import com.fraudwatch.transaction.dto.CreateTransactionRequest;
import com.fraudwatch.transaction.dto.TransactionResponse;
import com.fraudwatch.transaction.exception.TransactionBusinessException;
import com.fraudwatch.transaction.exception.TransactionExceptionHandler;
import com.fraudwatch.transaction.service.AccountService;
import com.fraudwatch.transaction.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {AccountController.class, TransactionController.class})
@Import({SecurityConfig.class, TransactionExceptionHandler.class})
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @MockBean
    private TransactionService transactionService;

    @Test
    void shouldCreateAccount() throws Exception {
        when(accountService.createAccount(any(AccountRequest.class))).thenReturn(accountResponse());

        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AccountRequest(
                    "KZ123456789012345678",
                    "customer-1",
                    "Alice Doe",
                    "KZT",
                    new BigDecimal("150000.00")
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accountNumber").value("KZ123456789012345678"))
            .andExpect(jsonPath("$.balance").value(150000.00))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldRejectInvalidAccountRequest() throws Exception {
        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AccountRequest(
                    "",
                    "customer-1",
                    "Alice Doe",
                    "KZ",
                    new BigDecimal("-1.00")
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.details.fields.accountNumber").exists())
            .andExpect(jsonPath("$.details.fields.currency").exists())
            .andExpect(jsonPath("$.details.fields.initialBalance").exists());
    }

    @Test
    void shouldReturnAccountById() throws Exception {
        when(accountService.getAccount(15L)).thenReturn(accountResponse());

        mockMvc.perform(get("/api/accounts/15"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(15))
            .andExpect(jsonPath("$.customerId").value("customer-1"));
    }

    @Test
    void shouldCreateTransactionAndForwardHeaders() throws Exception {
        when(transactionService.createTransaction(
            eq("idem-123"),
            eq("corr-456"),
            any(CreateTransactionRequest.class),
            any(HttpServletRequest.class)
        )).thenReturn(transactionResponse());

        mockMvc.perform(post("/api/transactions")
                .header("X-Idempotency-Key", "idem-123")
                .header("X-Correlation-Id", "corr-456")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateTransactionRequest(
                    15L,
                    new BigDecimal("2500.00"),
                    "KZT",
                    "DEBIT",
                    "Mega Store",
                    "ELECTRONICS",
                    "device-1",
                    "Phone purchase"
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transactionReference").value("tx-11"))
            .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
            .andExpect(jsonPath("$.merchantCategory").value("ELECTRONICS"));

        verify(transactionService).createTransaction(
            eq("idem-123"),
            eq("corr-456"),
            any(CreateTransactionRequest.class),
            any(HttpServletRequest.class)
        );
    }

    @Test
    void shouldRejectInvalidTransactionRequest() throws Exception {
        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateTransactionRequest(
                    null,
                    new BigDecimal("0.00"),
                    "KZ",
                    "",
                    "",
                    "",
                    null,
                    "desc"
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.details.fields.accountId").exists())
            .andExpect(jsonPath("$.details.fields.amount").exists())
            .andExpect(jsonPath("$.details.fields.currency").exists())
            .andExpect(jsonPath("$.details.fields.direction").exists());
    }

    @Test
    void shouldReturnTransactionsByAccount() throws Exception {
        when(transactionService.getTransactionsByAccount(15L)).thenReturn(List.of(transactionResponse()));

        mockMvc.perform(get("/api/transactions/account/15"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(71))
            .andExpect(jsonPath("$[0].accountId").value(15));
    }

    @Test
    void shouldTranslateNotFoundBusinessError() throws Exception {
        when(transactionService.getTransaction(999L))
            .thenThrow(new TransactionBusinessException(HttpStatus.NOT_FOUND, "Transaction was not found"));

        mockMvc.perform(get("/api/transactions/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Transaction was not found"))
            .andExpect(jsonPath("$.path").value("/api/transactions/999"));
    }

    @Test
    void shouldTranslateConflictBusinessError() throws Exception {
        when(transactionService.createTransaction(any(), any(), any(), any()))
            .thenThrow(new TransactionBusinessException(HttpStatus.CONFLICT, "Idempotency key is already used"));

        mockMvc.perform(post("/api/transactions")
                .header("X-Idempotency-Key", "idem-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateTransactionRequest(
                    15L,
                    new BigDecimal("2500.00"),
                    "KZT",
                    "DEBIT",
                    "Mega Store",
                    "ELECTRONICS",
                    "device-1",
                    "Phone purchase"
                ))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Idempotency key is already used"));
    }

    private AccountResponse accountResponse() {
        return new AccountResponse(
            15L,
            "KZ123456789012345678",
            "customer-1",
            "Alice Doe",
            "KZT",
            new BigDecimal("150000.00"),
            "ACTIVE"
        );
    }

    private TransactionResponse transactionResponse() {
        return new TransactionResponse(
            71L,
            "tx-11",
            15L,
            "KZ123456789012345678",
            new BigDecimal("2500.00"),
            "KZT",
            "DEBIT",
            "PENDING_REVIEW",
            "Mega Store",
            "ELECTRONICS",
            "device-1",
            "127.0.0.1",
            "Phone purchase",
            Instant.parse("2026-06-18T12:00:00Z")
        );
    }
}
