# System Diagram

```mermaid
flowchart LR
    Client["Client / Frontend"]
    Gateway["api-gateway"]
    Auth["auth-service"]
    Tx["transaction-service"]
    Fraud["fraud-service"]
    Review["review-service"]
    Audit["audit-service"]
    Notify["notification-service"]
    Rabbit["RabbitMQ"]
    Redis["Redis"]
    PgAuth["auth_db"]
    PgTx["transaction_db"]
    PgFraud["fraud_db"]
    PgReview["review_db"]
    PgAudit["audit_db"]
    PgNotify["notification_db"]
    Prom["Prometheus"]
    Grafana["Grafana"]

    Client --> Gateway
    Gateway --> Auth
    Gateway --> Tx
    Gateway --> Fraud
    Gateway --> Review
    Gateway --> Audit
    Gateway --> Notify

    Auth --> PgAuth
    Tx --> PgTx
    Fraud --> PgFraud
    Review --> PgReview
    Audit --> PgAudit
    Notify --> PgNotify

    Tx --> Rabbit
    Fraud --> Rabbit
    Review --> Rabbit
    Rabbit --> Fraud
    Rabbit --> Tx
    Rabbit --> Review
    Rabbit --> Audit
    Rabbit --> Notify

    Fraud --> Redis

    Gateway --> Prom
    Auth --> Prom
    Tx --> Prom
    Fraud --> Prom
    Review --> Prom
    Audit --> Prom
    Notify --> Prom
    Rabbit --> Prom
    Prom --> Grafana
```

## Notes

- `api-gateway` is the single external HTTP entry point.
- Each service owns its own PostgreSQL database.
- RabbitMQ carries domain events between services.
- Redis stores short-window fraud behavior signals.
- Prometheus scrapes service and infrastructure metrics; Grafana visualizes them.
