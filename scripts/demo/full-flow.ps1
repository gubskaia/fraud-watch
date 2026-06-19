[CmdletBinding()]
param(
    [string]$GatewayBaseUrl = "http://localhost:8080",
    [int]$PollAttempts = 30,
    [int]$PollDelaySeconds = 2
)

$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Invoke-JsonRequest {
    param(
        [ValidateSet("GET", "POST")]
        [string]$Method,
        [string]$Uri,
        [hashtable]$Headers = @{},
        [object]$Body = $null
    )

    $request = @{
        Method      = $Method
        Uri         = $Uri
        Headers     = $Headers
        ContentType = "application/json"
    }

    if ($null -ne $Body) {
        $request.Body = ($Body | ConvertTo-Json -Depth 10)
    }

    Invoke-RestMethod @request
}

function Wait-Until {
    param(
        [scriptblock]$Action,
        [scriptblock]$Condition,
        [string]$Description
    )

    for ($attempt = 1; $attempt -le $PollAttempts; $attempt++) {
        $result = & $Action
        if (& $Condition $result) {
            return $result
        }

        Start-Sleep -Seconds $PollDelaySeconds
    }

    throw "Timed out while waiting for $Description"
}

$suffix = Get-Date -Format "yyyyMMddHHmmss"
$username = "demo-$suffix"
$email = "$username@fraudwatch.local"
$password = "DemoPass123!"
$accountNumber = "FW-DEMO-$suffix"
$correlationId = "demo-flow-$suffix"

Write-Step "Registering a demo user through the gateway"
$authResponse = Invoke-JsonRequest -Method POST -Uri "$GatewayBaseUrl/api/auth/register" -Body @{
    username  = $username
    email     = $email
    password  = $password
    firstName = "Demo"
    lastName  = "Operator"
}

$accessToken = $authResponse.accessToken
$authHeaders = @{
    Authorization      = "Bearer $accessToken"
    "X-Correlation-Id" = $correlationId
}

Write-Step "Creating a demo account"
$account = Invoke-JsonRequest -Method POST -Uri "$GatewayBaseUrl/api/accounts" -Headers $authHeaders -Body @{
    accountNumber   = $accountNumber
    customerId      = "customer-$suffix"
    ownerName       = "Demo Operator"
    currency        = "USD"
    initialBalance  = 25000.00
}

Write-Step "Creating a transaction that should be routed to manual review"
$idempotencyKey = "idem-$suffix"
$transactionHeaders = @{
    Authorization       = $authHeaders.Authorization
    "X-Correlation-Id"  = $authHeaders["X-Correlation-Id"]
    "X-Idempotency-Key" = $idempotencyKey
}
$transaction = Invoke-JsonRequest -Method POST -Uri "$GatewayBaseUrl/api/transactions" -Headers $transactionHeaders -Body @{
    accountId         = $account.id
    amount            = 250.00
    currency          = "USD"
    direction         = "DEBIT"
    merchantName      = "Crypto Voucher Shop"
    merchantCategory  = "CRYPTO"
    deviceId          = "new-device-$suffix"
    description       = "Demo fraud review scenario"
}

$transactionId = $transaction.id

Write-Step "Waiting for the review case to be created asynchronously"
$reviewCases = Wait-Until `
    -Description "review case for transaction $transactionId" `
    -Action {
        Invoke-JsonRequest -Method GET -Uri "$GatewayBaseUrl/api/reviews/cases" -Headers $authHeaders
    } `
    -Condition {
        param($cases)
        @($cases | Where-Object { $_.transactionId -eq $transactionId }).Count -gt 0
    }
$reviewCase = @($reviewCases | Where-Object { $_.transactionId -eq $transactionId } | Select-Object -First 1)[0]

Write-Step "Assigning the review case"
$null = Invoke-JsonRequest -Method POST -Uri "$GatewayBaseUrl/api/reviews/cases/$($reviewCase.id)/assign" -Headers $authHeaders -Body @{
    analyst = "demo-analyst"
    details = "Assigned by demo flow script"
}

Write-Step "Finalizing the case with a blocking decision"
$null = Invoke-JsonRequest -Method POST -Uri "$GatewayBaseUrl/api/reviews/cases/$($reviewCase.id)/block" -Headers $authHeaders -Body @{
    analyst    = "demo-analyst"
    reasonCode = "CONFIRMED_FRAUD"
    details    = "Blocked as part of the end-to-end demo scenario"
}

Write-Step "Waiting for transaction status to reflect the final review decision"
$finalTransaction = Wait-Until `
    -Description "final transaction status" `
    -Action {
        Invoke-JsonRequest -Method GET -Uri "$GatewayBaseUrl/api/transactions/$transactionId" -Headers $authHeaders
    } `
    -Condition {
        param($tx)
        $tx.status -eq "BLOCKED"
    }

Write-Step "Fetching related audit records"
$auditRecords = Invoke-JsonRequest `
    -Method GET `
    -Uri "$GatewayBaseUrl/api/audit/records?aggregateType=TRANSACTION&aggregateId=$transactionId" `
    -Headers $authHeaders

Write-Step "Fetching generated notifications"
$notifications = Invoke-JsonRequest `
    -Method GET `
    -Uri "$GatewayBaseUrl/api/notifications?recipientRef=review-case-$($reviewCase.id)" `
    -Headers $authHeaders

Write-Step "Demo flow completed"
$summary = [pscustomobject]@{
    user = [pscustomobject]@{
        username = $username
        email    = $email
    }
    account = [pscustomobject]@{
        id            = $account.id
        accountNumber = $account.accountNumber
    }
    transaction = [pscustomobject]@{
        id                 = $finalTransaction.id
        transactionReference = $finalTransaction.transactionReference
        status             = $finalTransaction.status
    }
    reviewCase = [pscustomobject]@{
        id         = $reviewCase.id
        status     = "BLOCKED"
        assignedTo = "demo-analyst"
    }
    auditRecordCount = @($auditRecords).Count
    notificationCount = @($notifications).Count
}

$summary | ConvertTo-Json -Depth 10
