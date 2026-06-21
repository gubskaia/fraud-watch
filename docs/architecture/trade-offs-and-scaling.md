# Trade-Offs And Scaling Path

## Current Trade-Offs

- The platform favors clarity and bounded contexts over operational simplicity.
- Event publication is service-owned and pragmatic rather than a fully generalized outbox framework.
- Notification delivery is intentionally mocked to keep the project demoable without external providers.
- RBAC is enforced at the gateway, while downstream services stay comparatively lightweight.

## Why This Shape Works Well

- Separate service databases make ownership and migration boundaries explicit.
- RabbitMQ allows the fraud, review, audit, and notification flows to evolve independently.
- Redis keeps short-window fraud signals fast without complicating transactional persistence.
- Shared libraries keep event contracts, observability, and testing utilities consistent across services.

## Natural Next Scaling Steps

- Introduce a dedicated outbox implementation for the publishing services.
- Export traces to a real OpenTelemetry backend such as Tempo or Jaeger.
- Add external delivery providers for email, SMS, or webhook notifications.
- Split gateway policy by endpoint groups or tenants as authorization needs grow.
- Add queue-specific dashboards and alert rules for DLQ growth and consumer lag.
