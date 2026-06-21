# Local Runbook

## Start Everything

```powershell
docker compose up --build
```

## Validate Compose File

```powershell
docker compose -f .\compose.yml config
```

## Run Stack Smoke Check

After the containers are up, verify service and infrastructure readiness:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\dev\smoke-check.ps1
```

## Clean Rebuild The Stack

To reset local state, rebuild the stack, and rerun the smoke check:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\dev\clean-start.ps1
```

This command removes Docker Compose volumes, so it should be used only when a local reset is intended.

## Stop Everything

```powershell
docker compose down
```

## Useful Checks

### Health endpoints

- `http://localhost:8080/actuator/health`
- `http://localhost:8081/actuator/health`
- `http://localhost:8082/actuator/health`
- `http://localhost:8083/actuator/health`
- `http://localhost:8084/actuator/health`
- `http://localhost:8085/actuator/health`
- `http://localhost:8086/actuator/health`

### Gateway-routed APIs

- `http://localhost:8080/api/auth/register`
- `http://localhost:8080/api/accounts`
- `http://localhost:8080/api/transactions`
- `http://localhost:8080/api/reviews/cases`
- `http://localhost:8080/api/fraud/decisions`
- `http://localhost:8080/api/audit/records`
- `http://localhost:8080/api/notifications`

## Demo Scenario

Run the scripted end-to-end flow after all containers are healthy:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\demo\full-flow.ps1
```

What the script does:

- registers a unique demo user through `api-gateway`
- logs in with the seeded analyst account `analyst.demo` / `AnalystPass123!`
- creates a funded account
- runs one of the supported scenarios: `approved`, `review-approve`, `review-block`, `direct-block`
- waits for `api-gateway` health before starting requests
- waits for either a direct fraud outcome or a manual review branch
- fetches the related audit records and notifications

Example variants:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\demo\full-flow.ps1 -Scenario approved
powershell -ExecutionPolicy Bypass -File .\scripts\demo\full-flow.ps1 -Scenario review-approve
powershell -ExecutionPolicy Bypass -File .\scripts\demo\full-flow.ps1 -Scenario review-block
powershell -ExecutionPolicy Bypass -File .\scripts\demo\full-flow.ps1 -Scenario direct-block
```

### Infrastructure UIs

- RabbitMQ: `http://localhost:15672`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

### Grafana dashboard

- Sign in with `admin` / `admin`
- Open the preprovisioned `FraudWatch Overview` dashboard
- Confirm service availability, HTTP throughput, latency, JVM memory, and RabbitMQ activity

## Troubleshooting

### Build failures

- Ensure Docker can access the repository directory
- Ensure Java 17 is used inside container builds

### Port collisions

- Check whether ports `8080-8086`, `5433-5438`, `5672`, `6379`, `9090`, `3000`, `15672`, `15692` are already occupied

### Service startup order

- Databases, RabbitMQ, and Redis must become healthy before dependent services stabilize
- Application containers also expose `/actuator/health`, and Compose waits for healthy upstream services before starting `api-gateway` and `prometheus`

### Invalid event handling

- Review, transaction, fraud, audit, and notification consumers are configured with DLQ-backed queues
- Invalid messages are rejected without requeue and can be inspected in RabbitMQ dead-letter queues
- Example DLQ name: `fraudwatch.review.transaction-review-required.dlq`
