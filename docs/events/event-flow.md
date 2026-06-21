# Event Flow

## Implemented Events

### Transaction domain

- `TransactionCreated`
- `TransactionStatusChanged`

### Fraud domain

- `TransactionApproved`
- `TransactionBlocked`
- `TransactionReviewRequired`

### Review domain

- `ReviewDecisionMade`

## Flow Sequence

1. `transaction-service` publishes `TransactionCreated`
2. `fraud-service` consumes `TransactionCreated`
3. `fraud-service` evaluates enabled rules and calculates a score
4. `fraud-service` publishes one of:
   - `TransactionApproved`
   - `TransactionBlocked`
   - `TransactionReviewRequired`
5. `transaction-service` consumes the fraud decision and updates status
6. `review-service` consumes `TransactionReviewRequired` and creates a case
7. Analyst approves or blocks the case in `review-service`
8. `review-service` publishes `ReviewDecisionMade`
9. `transaction-service` consumes `ReviewDecisionMade` and finalizes transaction status
10. `audit-service` consumes key events and stores them
11. `notification-service` consumes fraud/review events and stores notifications

## Exchanges and Routing Keys

### `fraudwatch.transaction.exchange`

- `transaction.created`
- `transaction.status-changed`

### `fraudwatch.fraud.exchange`

- `transaction.approved`
- `transaction.blocked`
- `transaction.review-required`

### `fraudwatch.review.exchange`

- `review.decision-made`

## Reliability Notes

- Consumer queues use dead-letter routing for invalid or non-processable messages
- Listener containers are configured with `default-requeue-rejected=false`
- Review flow integration coverage verifies that an invalid `transaction.review-required` message is routed to `fraudwatch.review.transaction-review-required.dlq`
