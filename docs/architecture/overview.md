# Architecture Overview

FraudWatch is organized as a microservice monorepo with clear bounded contexts.

## Core Principles

- Each service owns its own persistence boundary
- Cross-service communication is primarily event-driven
- The gateway is the main external entry point
- Fraud scoring is isolated from transaction storage
- Manual review is a separate analyst-oriented workflow
- Audit history is stored separately as immutable event-derived records

## Bounded Contexts

### Identity and access

- `auth-service`
- Users, roles, permissions, refresh tokens

### Transaction intake and lifecycle

- `transaction-service`
- Accounts, transactions, idempotency records

### Automated fraud scoring

- `fraud-service`
- Fraud rules, fraud decisions, Redis-backed behavioral checks

### Manual analyst workflow

- `review-service`
- Fraud cases, reason codes, comments, review actions

### Audit and operations

- `audit-service`
- Immutable event-driven audit trail

### User communications

- `notification-service`
- Notification templates, notifications, delivery attempts

## Infrastructure

- RabbitMQ for event routing
- Redis for short-window fraud behavior signals
- PostgreSQL per service
- Prometheus and Grafana for metrics

## Related Documents

- [System Diagram](system-diagram.md)
- [Transaction Review Sequence](../diagrams/transaction-review-sequence.md)
- [Service Data Models](../erd/service-data-models.md)
- [Trade-Offs And Scaling Path](trade-offs-and-scaling.md)
