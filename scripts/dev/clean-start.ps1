[CmdletBinding()]
param(
    [switch]$SkipSmokeCheck
)

$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$composeFile = Join-Path $repoRoot "compose.yml"
$smokeScript = Join-Path $repoRoot "scripts\dev\smoke-check.ps1"

Write-Step "Stopping the local stack and removing containers, networks, and volumes"
docker compose -f $composeFile down --volumes --remove-orphans

Write-Step "Rebuilding and starting the full stack in detached mode"
docker compose -f $composeFile up --build -d

if (-not $SkipSmokeCheck) {
    Write-Step "Running stack smoke check"
    powershell -ExecutionPolicy Bypass -File $smokeScript
}

Write-Step "Clean start completed"
