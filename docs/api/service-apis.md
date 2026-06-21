# Service APIs

## Gateway-facing APIs

### Auth

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `GET /api/users/me`

### Transaction

- `POST /api/accounts`
- `GET /api/accounts/{id}`
- `POST /api/transactions`
- `GET /api/transactions/{id}`
- `GET /api/transactions/account/{accountId}`

### Review

- `GET /api/reviews/cases`
- `GET /api/reviews/cases/{id}`
- `POST /api/reviews/cases/{id}/assign`
- `POST /api/reviews/cases/{id}/approve`
- `POST /api/reviews/cases/{id}/block`
- `POST /api/reviews/cases/{id}/comment`

### Fraud

- `GET /api/fraud/rules`
- `PUT /api/fraud/rules/{id}`
- `GET /api/fraud/decisions/{transactionId}`

### Audit And Notifications

- `GET /api/audit/records`
- `GET /api/audit/records/{aggregateType}/{aggregateId}`
- `GET /api/notifications`

## Authorization Model

- Customer flows require transaction-oriented permissions such as `TRANSACTION_CREATE` and `TRANSACTION_READ`.
- Analyst flows require `REVIEW_CASE_READ`, `REVIEW_CASE_DECIDE`, `AUDIT_READ`, and related read permissions.
- Admin users additionally receive `FRAUD_RULE_WRITE` for fraud rule management.
