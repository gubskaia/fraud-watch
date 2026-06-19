# Local Runbook

## Start Everything

```powershell
docker compose up --build
```

## Validate Compose File

```powershell
docker compose -f .\compose.yml config
```

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
- creates a funded account
- submits a transaction designed to trigger `UNDER_REVIEW`
- waits for the review case, assigns it, and blocks it
- waits for the transaction to become `BLOCKED`
- fetches the related audit records and notifications

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
