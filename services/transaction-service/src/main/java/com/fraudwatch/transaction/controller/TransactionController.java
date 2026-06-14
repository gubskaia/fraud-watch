package com.fraudwatch.transaction.controller;

import com.fraudwatch.transaction.dto.CreateTransactionRequest;
import com.fraudwatch.transaction.dto.TransactionResponse;
import com.fraudwatch.transaction.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(
        @RequestHeader(name = "X-Idempotency-Key", required = false) String idempotencyKey,
        @RequestHeader(name = "X-Correlation-Id", required = false) String correlationId,
        @Valid @RequestBody CreateTransactionRequest request,
        HttpServletRequest httpRequest
    ) {
        return transactionService.createTransaction(idempotencyKey, correlationId, request, httpRequest);
    }

    @GetMapping("/{id}")
    public TransactionResponse getTransaction(@PathVariable Long id) {
        return transactionService.getTransaction(id);
    }

    @GetMapping("/account/{accountId}")
    public List<TransactionResponse> getTransactionsByAccount(@PathVariable Long accountId) {
        return transactionService.getTransactionsByAccount(accountId);
    }
}
