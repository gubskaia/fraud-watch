# FraudWatch

FraudWatch is a Java 17 / Spring Boot microservice platform for real-time banking fraud detection. The system is built as an event-driven monorepo with isolated services, per-service databases, RabbitMQ-based communication, Redis-backed behavioral checks, and an observability stack built around Actuator, Prometheus, and Grafana.

## What Is Implemented

- Root Maven monorepo with shared library modules
- `api-gateway` with routing for all user-facing APIs, JWT validation, correlation id propagation, and request logging
- `auth-service` with user, role, permission, refresh token model and auth API
- `transaction-service` with account creation, idempotent transaction creation, and transaction lifecycle updates
- `fraud-service` with seeded fraud rules, Redis-backed rule checks, scoring, and decision publishing
- `review-service` with fraud case creation, analyst decisions, comments, and review events
- `audit-service` with immutable audit record storage and read-only API
- `notification-service` with event-driven notifications, persisted delivery attempts, and mock delivery
- Local Docker Compose stack for all services, databases, RabbitMQ, Redis, Prometheus, and Grafana

## Monorepo Layout

```text
fraudwatch/
  services/
    api-gateway/
    auth-service/
    transaction-service/
    fraud-service/
    review-service/
    audit-service/
    notification-service/

  libs/
    common-events/
    common-security/
    common-observability/
    common-test/

  infrastructure/
    prometheus/
    grafana/

  docs/
    architecture/
    events/
    runbooks/

  compose.yml
  pom.xml
```

## Service Overview

| Service | Port | Responsibility |
| --- | --- | --- |
| `api-gateway` | `8080` | Entry point, route forwarding to all public service APIs, JWT validation, correlation id propagation |
| `auth-service` | `8081` | Register/login/refresh, users, roles, permissions |
| `transaction-service` | `8082` | Accounts, transactions, idempotency, transaction events |
| `fraud-service` | `8083` | Fraud rule execution, scoring, fraud decisions |
| `review-service` | `8084` | Manual analyst review workflow |
| `audit-service` | `8085` | Immutable audit trail |
| `notification-service` | `8086` | Notification persistence and mock delivery |

## Key Event Flow

1. A client authenticates through `auth-service` and receives JWT tokens.
2. A transaction is created through `api-gateway` into `transaction-service`.
3. `transaction-service` stores the transaction in `PENDING_REVIEW` and publishes `TransactionCreated`.
4. `fraud-service` consumes the event, executes enabled rules, persists a fraud decision, and publishes:
   - `TransactionApproved`
   - `TransactionBlocked`
   - `TransactionReviewRequired`
5. `transaction-service` consumes fraud decisions and updates transaction status.
6. If the decision is `UNDER_REVIEW`, `review-service` creates a case for an analyst.
7. Analyst actions publish `ReviewDecisionMade`.
8. `transaction-service` consumes the final review decision and updates the transaction.
9. `audit-service` stores key domain events as immutable audit records.
10. `notification-service` stores and mock-delivers notifications for fraud and review outcomes.

More detail is available in [docs/events/event-flow.md](/D:/fraudwatch/docs/events/event-flow.md).

## Local Development

### Prerequisites

- Java 17
- Docker Desktop with Compose
- Optional: Maven installed globally, though the repo includes `mvnw`

### Validate the Monorepo

```powershell
.\mvnw.cmd -q validate
```

### Start the Full Stack

```powershell
docker compose up --build
```

### Stop the Stack

```powershell
docker compose down
```

### Useful Endpoints

- Gateway Swagger: `http://localhost:8080/swagger-ui.html`
- Gateway Audit API: `http://localhost:8080/api/audit/records`
- Gateway Notification API: `http://localhost:8080/api/notifications`
- Auth Swagger: `http://localhost:8081/swagger-ui.html`
- Transaction Swagger: `http://localhost:8082/swagger-ui.html`
- Fraud Swagger: `http://localhost:8083/swagger-ui.html`
- Review Swagger: `http://localhost:8084/swagger-ui.html`
- Audit API: `http://localhost:8085/api/audit/records`
- Notification API: `http://localhost:8086/api/notifications`
- RabbitMQ UI: `http://localhost:15672`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

## Current Notes

- The repository is configured for Java 17, so the local shell should use JDK 17 or newer.
- Dockerfiles build each service from the root monorepo using Maven.
- Prometheus scraping is wired through `/actuator/prometheus`.
- Grafana provisioning is included; dashboards can be added under `infrastructure/grafana/dashboards`.

## Next Recommended Steps

- Add end-to-end integration tests for the transaction to fraud to review lifecycle
- Add root-level demo scripts and seed scenarios
- Add richer Grafana dashboards and runbooks
- Harden security between services and external clients
