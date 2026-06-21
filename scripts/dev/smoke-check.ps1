[CmdletBinding()]
param(
    [int]$PollAttempts = 30,
    [int]$PollDelaySeconds = 2
)

$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Wait-ForJson {
    param(
        [string]$Name,
        [string]$Uri,
        [scriptblock]$Condition
    )

    for ($attempt = 1; $attempt -le $PollAttempts; $attempt++) {
        try {
            $response = Invoke-RestMethod -Uri $Uri -Method GET
            if (& $Condition $response) {
                return $response
            }
        } catch {
        }

        Start-Sleep -Seconds $PollDelaySeconds
    }

    throw "Timed out while waiting for $Name at $Uri"
}

function Wait-ForStatusCode {
    param(
        [string]$Name,
        [string]$Uri,
        [int]$ExpectedStatusCode = 200
    )

    for ($attempt = 1; $attempt -le $PollAttempts; $attempt++) {
        try {
            $response = Invoke-WebRequest -Uri $Uri -Method GET -UseBasicParsing
            if ($response.StatusCode -eq $ExpectedStatusCode) {
                return
            }
        } catch {
        }

        Start-Sleep -Seconds $PollDelaySeconds
    }

    throw "Timed out while waiting for $Name at $Uri"
}

$healthEndpoints = @(
    @{ Name = "api-gateway"; Uri = "http://localhost:8080/actuator/health" },
    @{ Name = "auth-service"; Uri = "http://localhost:8081/actuator/health" },
    @{ Name = "transaction-service"; Uri = "http://localhost:8082/actuator/health" },
    @{ Name = "fraud-service"; Uri = "http://localhost:8083/actuator/health" },
    @{ Name = "review-service"; Uri = "http://localhost:8084/actuator/health" },
    @{ Name = "audit-service"; Uri = "http://localhost:8085/actuator/health" },
    @{ Name = "notification-service"; Uri = "http://localhost:8086/actuator/health" }
)

$infoEndpoints = @(
    @{ Name = "auth-service"; Uri = "http://localhost:8081/internal/info"; ExpectedService = "auth-service" },
    @{ Name = "transaction-service"; Uri = "http://localhost:8082/internal/info"; ExpectedService = "transaction-service" },
    @{ Name = "fraud-service"; Uri = "http://localhost:8083/internal/info"; ExpectedService = "fraud-service" },
    @{ Name = "review-service"; Uri = "http://localhost:8084/internal/info"; ExpectedService = "review-service" },
    @{ Name = "audit-service"; Uri = "http://localhost:8085/internal/info"; ExpectedService = "audit-service" },
    @{ Name = "notification-service"; Uri = "http://localhost:8086/internal/info"; ExpectedService = "notification-service" },
    @{ Name = "api-gateway"; Uri = "http://localhost:8080/internal/info"; ExpectedService = "api-gateway" }
)

$infraEndpoints = @(
    @{ Name = "Prometheus"; Uri = "http://localhost:9090/-/healthy" },
    @{ Name = "Grafana login page"; Uri = "http://localhost:3000/login" },
    @{ Name = "RabbitMQ management"; Uri = "http://localhost:15672" }
)

Write-Step "Waiting for application health endpoints"
$healthResults = foreach ($endpoint in $healthEndpoints) {
    Wait-ForJson -Name $endpoint.Name -Uri $endpoint.Uri -Condition {
        param($response)
        $response.status -eq "UP"
    }
}

Write-Step "Checking internal service info endpoints"
$infoResults = foreach ($endpoint in $infoEndpoints) {
    Wait-ForJson -Name $endpoint.Name -Uri $endpoint.Uri -Condition {
        param($response)
        $response.service -eq $endpoint.ExpectedService -and $response.status -eq "bootstrapped"
    }
}

Write-Step "Checking infrastructure HTTP endpoints"
foreach ($endpoint in $infraEndpoints) {
    Wait-ForStatusCode -Name $endpoint.Name -Uri $endpoint.Uri
}

$summary = [pscustomobject]@{
    checkedHealthEndpoints = @($healthResults).Count
    checkedInfoEndpoints = @($infoResults).Count
    checkedInfrastructureEndpoints = @($infraEndpoints).Count
    status = "ok"
}

Write-Step "Smoke check completed"
$summary | ConvertTo-Json -Depth 5
