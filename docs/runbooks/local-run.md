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

### Infrastructure UIs

- RabbitMQ: `http://localhost:15672`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

## Troubleshooting

### Build failures

- Ensure Docker can access the repository directory
- Ensure Java 21 is used inside container builds

### Port collisions

- Check whether ports `8080-8086`, `5433-5438`, `5672`, `6379`, `9090`, `3000`, `15672`, `15692` are already occupied

### Service startup order

- Databases, RabbitMQ, and Redis must become healthy before dependent services stabilize

