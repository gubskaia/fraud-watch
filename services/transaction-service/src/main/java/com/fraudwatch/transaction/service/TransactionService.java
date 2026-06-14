package com.fraudwatch.transaction.service;

import com.fraudwatch.transaction.domain.Account;
import com.fraudwatch.transaction.domain.AccountStatus;
import com.fraudwatch.transaction.domain.IdempotencyRecord;
import com.fraudwatch.transaction.domain.Transaction;
import com.fraudwatch.transaction.domain.TransactionDirection;
import com.fraudwatch.transaction.domain.TransactionStatus;
import com.fraudwatch.transaction.dto.CreateTransactionRequest;
import com.fraudwatch.transaction.dto.TransactionResponse;
import com.fraudwatch.transaction.exception.TransactionBusinessException;
import com.fraudwatch.transaction.mapper.TransactionMapper;
import com.fraudwatch.transaction.messaging.TransactionEventPublisher;
import com.fraudwatch.transaction.repository.AccountRepository;
import com.fraudwatch.transaction.repository.IdempotencyRecordRepository;
import com.fraudwatch.transaction.repository.TransactionRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final TransactionMapper transactionMapper;
    private final TransactionEventPublisher transactionEventPublisher;

    public TransactionService(
        AccountRepository accountRepository,
        TransactionRepository transactionRepository,
        IdempotencyRecordRepository idempotencyRecordRepository,
        TransactionMapper transactionMapper,
        TransactionEventPublisher transactionEventPublisher
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.transactionMapper = transactionMapper;
        this.transactionEventPublisher = transactionEventPublisher;
    }

    @Transactional
    public TransactionResponse createTransaction(
        String idempotencyKey,
        String correlationId,
        CreateTransactionRequest request,
        HttpServletRequest httpRequest
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new TransactionBusinessException(HttpStatus.BAD_REQUEST, "X-Idempotency-Key header is required");
        }

        String normalizedKey = idempotencyKey.trim();
        String requestHash = requestHash(request);
        IdempotencyRecord existingRecord = idempotencyRecordRepository.findByIdempotencyKey(normalizedKey).orElse(null);
        if (existingRecord != null) {
            if (!existingRecord.getRequestHash().equals(requestHash)) {
                throw new TransactionBusinessException(HttpStatus.CONFLICT, "Idempotency key was already used for a different request");
            }
            Transaction existingTransaction = transactionRepository.findDetailedById(existingRecord.getTransactionId())
                .orElseThrow(() -> new TransactionBusinessException(HttpStatus.CONFLICT, "Stored idempotent transaction was not found"));
            return transactionMapper.toTransactionResponse(existingTransaction);
        }

        Account account = accountRepository.findById(request.accountId())
            .orElseThrow(() -> new TransactionBusinessException(HttpStatus.NOT_FOUND, "Account was not found"));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new TransactionBusinessException(HttpStatus.CONFLICT, "Account is not active");
        }

        String currency = request.currency().trim().toUpperCase(Locale.ROOT);
        if (!account.getCurrency().equalsIgnoreCase(currency)) {
            throw new TransactionBusinessException(HttpStatus.BAD_REQUEST, "Transaction currency must match account currency");
        }

        TransactionDirection direction;
        try {
            direction = TransactionDirection.valueOf(request.direction().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new TransactionBusinessException(HttpStatus.BAD_REQUEST, "Unsupported transaction direction");
        }

        Transaction transaction = new Transaction();
        transaction.setTransactionReference(UUID.randomUUID().toString());
        transaction.setAccount(account);
        transaction.setAmount(request.amount());
        transaction.setCurrency(currency);
        transaction.setDirection(direction);
        transaction.setStatus(TransactionStatus.PENDING_REVIEW);
        transaction.setMerchantName(request.merchantName().trim());
        transaction.setMerchantCategory(request.merchantCategory().trim());
        transaction.setDeviceId(request.deviceId() == null ? null : request.deviceId().trim());
        transaction.setIpAddress(extractIpAddress(httpRequest));
        transaction.setDescription(request.description() == null ? null : request.description().trim());

        Transaction savedTransaction = transactionRepository.save(transaction);

        IdempotencyRecord idempotencyRecord = new IdempotencyRecord();
        idempotencyRecord.setIdempotencyKey(normalizedKey);
        idempotencyRecord.setRequestHash(requestHash);
        idempotencyRecord.setTransactionId(savedTransaction.getId());
        idempotencyRecord.setCompletedAt(Instant.now());
        idempotencyRecordRepository.save(idempotencyRecord);

        transactionEventPublisher.publishTransactionCreated(
            savedTransaction,
            normalizeCorrelationId(correlationId)
        );
        return transactionMapper.toTransactionResponse(savedTransaction);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findDetailedById(transactionId)
            .orElseThrow(() -> new TransactionBusinessException(HttpStatus.NOT_FOUND, "Transaction was not found"));
        return transactionMapper.toTransactionResponse(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByAccount(Long accountId) {
        return transactionRepository.findAllByAccountIdOrderByCreatedAtDesc(accountId)
            .stream()
            .map(transactionMapper::toTransactionResponse)
            .toList();
    }

    private String extractIpAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String requestHash(CreateTransactionRequest request) {
        String value = "%s|%s|%s|%s|%s|%s|%s".formatted(
            request.accountId(),
            request.amount(),
            request.currency(),
            request.direction(),
            request.merchantName(),
            request.merchantCategory(),
            request.description()
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new TransactionBusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to hash request");
        }
    }

    private String normalizeCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return correlationId.trim();
    }
}
