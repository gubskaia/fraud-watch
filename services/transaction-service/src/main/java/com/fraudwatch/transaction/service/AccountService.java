package com.fraudwatch.transaction.service;

import com.fraudwatch.transaction.domain.Account;
import com.fraudwatch.transaction.dto.AccountRequest;
import com.fraudwatch.transaction.dto.AccountResponse;
import com.fraudwatch.transaction.exception.TransactionBusinessException;
import com.fraudwatch.transaction.mapper.TransactionMapper;
import com.fraudwatch.transaction.repository.AccountRepository;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionMapper transactionMapper;

    public AccountService(AccountRepository accountRepository, TransactionMapper transactionMapper) {
        this.accountRepository = accountRepository;
        this.transactionMapper = transactionMapper;
    }

    @Transactional
    public AccountResponse createAccount(AccountRequest request) {
        accountRepository.findByAccountNumber(request.accountNumber().trim())
            .ifPresent(account -> {
                throw new TransactionBusinessException(HttpStatus.CONFLICT, "Account number already exists");
            });

        Account account = new Account();
        account.setAccountNumber(request.accountNumber().trim());
        account.setCustomerId(request.customerId().trim());
        account.setOwnerName(request.ownerName().trim());
        account.setCurrency(request.currency().trim().toUpperCase(Locale.ROOT));
        account.setBalance(request.initialBalance());

        return transactionMapper.toAccountResponse(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new TransactionBusinessException(HttpStatus.NOT_FOUND, "Account was not found"));
        return transactionMapper.toAccountResponse(account);
    }
}

