package com.fraudwatch.transaction.repository;

import com.fraudwatch.transaction.domain.Transaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @EntityGraph(attributePaths = "account")
    Optional<Transaction> findDetailedById(Long id);

    @EntityGraph(attributePaths = "account")
    List<Transaction> findAllByAccountIdOrderByCreatedAtDesc(Long accountId);

    Optional<Transaction> findByTransactionReference(String transactionReference);
}

