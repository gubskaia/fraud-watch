# Transaction Review Sequence

```mermaid
sequenceDiagram
    actor User
    participant Gateway as api-gateway
    participant Auth as auth-service
    participant Tx as transaction-service
    participant Fraud as fraud-service
    participant Review as review-service
    participant Audit as audit-service
    participant Notify as notification-service
    participant MQ as RabbitMQ

    User->>Gateway: POST /api/auth/login
    Gateway->>Auth: Forward login request
    Auth-->>Gateway: JWT access + refresh tokens
    Gateway-->>User: Tokens

    User->>Gateway: POST /api/transactions
    Gateway->>Tx: Create transaction + correlation id
    Tx->>Tx: Persist transaction as PENDING_REVIEW
    Tx->>MQ: Publish TransactionCreated

    MQ->>Fraud: TransactionCreated
    Fraud->>Fraud: Execute fraud rules in parallel
    Fraud->>MQ: Publish TransactionReviewRequired

    MQ->>Tx: TransactionReviewRequired
    Tx->>Tx: Update status to UNDER_REVIEW
    Tx->>MQ: Publish TransactionStatusChanged

    MQ->>Review: TransactionReviewRequired
    Review->>Review: Create FraudCase

    User->>Gateway: Analyst reviews case
    Gateway->>Review: Approve / block case
    Review->>Review: Store review action + reason code
    Review->>MQ: Publish ReviewDecisionMade

    MQ->>Tx: ReviewDecisionMade
    Tx->>Tx: Finalize transaction status
    Tx->>MQ: Publish TransactionStatusChanged

    MQ->>Audit: Key domain events
    Audit->>Audit: Persist immutable audit records

    MQ->>Notify: Fraud/review events
    Notify->>Notify: Persist notification + delivery attempt
```

## Covered Variants

- Low-risk transactions end at `APPROVED` without manual review.
- High-risk transactions can be `BLOCKED` directly by `fraud-service`.
- Medium-risk transactions follow the analyst review branch shown above.
