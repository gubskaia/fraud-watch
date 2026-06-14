package com.fraudwatch.transaction.mapper;

import com.fraudwatch.transaction.domain.Account;
import com.fraudwatch.transaction.domain.Transaction;
import com.fraudwatch.transaction.dto.AccountResponse;
import com.fraudwatch.transaction.dto.TransactionResponse;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public AccountResponse toAccountResponse(Account account) {
        return new AccountResponse(
            account.getId(),
            account.getAccountNumber(),
            account.getCustomerId(),
            account.getOwnerName(),
            account.getCurrency(),
            account.getBalance(),
            account.getStatus().name()
        );
    }

    public TransactionResponse toTransactionResponse(Transaction transaction) {
        return new TransactionResponse(
            transaction.getId(),
            transaction.getTransactionReference(),
            transaction.getAccount().getId(),
            transaction.getAccount().getAccountNumber(),
            transaction.getAmount(),
            transaction.getCurrency(),
            transaction.getDirection().name(),
            transaction.getStatus().name(),
            transaction.getMerchantName(),
            transaction.getMerchantCategory(),
            transaction.getDeviceId(),
            transaction.getIpAddress(),
            transaction.getDescription(),
            transaction.getCreatedAt()
        );
    }
}
