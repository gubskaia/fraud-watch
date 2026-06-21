# Service Data Models

## Auth Service

```mermaid
erDiagram
    USERS ||--o{ USER_ROLES : has
    ROLES ||--o{ USER_ROLES : assigned
    ROLES ||--o{ ROLE_PERMISSIONS : grants
    PERMISSIONS ||--o{ ROLE_PERMISSIONS : referenced_by
    USERS ||--o{ REFRESH_TOKENS : owns
```

## Transaction And Fraud Domains

```mermaid
erDiagram
    ACCOUNTS ||--o{ TRANSACTIONS : owns
    ACCOUNTS ||--o{ IDEMPOTENCY_RECORDS : correlates
    TRANSACTIONS ||--|| FRAUD_DECISIONS : evaluated_by
    FRAUD_RULES ||--o{ FRAUD_DECISION_RULES : contributes_to
```

Notes:

- `transaction-service` persists account and transaction lifecycle state.
- `fraud-service` keeps rule configuration and final fraud decisions separately.

## Review, Audit, And Notification Domains

```mermaid
erDiagram
    FRAUD_CASES ||--o{ REVIEW_ACTIONS : records
    FRAUD_CASES ||--o{ ANALYST_COMMENTS : contains
    REASON_CODES ||--o{ REVIEW_ACTIONS : classifies
    AUDIT_RECORDS {
        string event_id
        string aggregate_type
        string aggregate_id
    }
    NOTIFICATION_TEMPLATES ||--o{ NOTIFICATIONS : used_by
    NOTIFICATIONS ||--o{ DELIVERY_ATTEMPTS : produces
```

Notes:

- `audit-service` stores append-only event history.
- `notification-service` tracks rendered notifications and delivery attempts.
