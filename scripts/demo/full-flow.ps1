[CmdletBinding()]
param(
    [ValidateSet("approved", "review-approve", "review-block", "direct-block")]
    [string]$Scenario = "review-block",
    [string]$GatewayBaseUrl = "http://localhost:8080",
    [int]$PollAttempts = 30,
    [int]$PollDelaySeconds = 2
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

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

function Wait-ForGatewayHealth {
    param([string]$BaseUrl)

    Wait-Until `
        -Description "gateway health endpoint at $BaseUrl" `
        -Action {
            try {
                Invoke-RestMethod -Method GET -Uri "$BaseUrl/actuator/health"
            } catch {
                $null
            }
        } `
        -Condition {
            param($health)
            $null -ne $health -and $health.status -eq "UP"
        } | Out-Null
}

function New-ScenarioRequest {
    param(
        [string]$Name,
        [long]$AccountId,
        [string]$Suffix
    )

    switch ($Name) {
        "approved" {
            return @{
                amount           = 120.00
                merchantName     = "City Market"
                merchantCategory = "GROCERY"
                deviceId         = $null
                description      = "Low-risk approval scenario"
                expectedStatus   = "APPROVED"
                requiresReview   = $false
            }
        }
        "review-approve" {
            return @{
                amount           = 250.00
                merchantName     = "Crypto Voucher Shop"
                merchantCategory = "CRYPTO"
                deviceId         = "new-device-$Suffix"
                description      = "Manual review approval scenario"
                expectedStatus   = "APPROVED"
                requiresReview   = $true
                reviewAction     = "approve"
                reasonCode       = "LEGIT_ACTIVITY"
            }
        }
        "review-block" {
            return @{
                amount           = 250.00
                merchantName     = "Crypto Voucher Shop"
                merchantCategory = "CRYPTO"
                deviceId         = "new-device-$Suffix"
                description      = "Manual review block scenario"
                expectedStatus   = "BLOCKED"
                requiresReview   = $true
                reviewAction     = "block"
                reasonCode       = "CONFIRMED_FRAUD"
            }
        }
        "direct-block" {
            return @{
                amount           = 15000.00
                merchantName     = "Offshore Crypto Exchange"
                merchantCategory = "CRYPTO"
                deviceId         = $null
                description      = "Direct fraud block scenario"
                expectedStatus   = "BLOCKED"
                requiresReview   = $false
            }
        }
    }
}

$suffix = Get-Date -Format "yyyyMMddHHmmss"
$username = "demo-$suffix"
$email = "$username@fraudwatch.local"
$password = "DemoPass123!"
$analystUsername = "analyst.demo"
$analystPassword = "AnalystPass123!"
$accountNumber = "FW-DEMO-$suffix"
$correlationId = "demo-flow-$Scenario-$suffix"

Write-Step "Waiting for the API gateway to become healthy"
Wait-ForGatewayHealth -BaseUrl $GatewayBaseUrl

Write-Step "Using scenario '$Scenario'"

Write-Step "Registering a demo user through the gateway"
$authResponse = Invoke-JsonRequest -Method POST -Uri "$GatewayBaseUrl/api/auth/register" -Body @{
    username  = $username
    email     = $email
    password  = $password
    firstName = "Demo"
    lastName  = "Operator"
}

$authHeaders = @{
    Authorization      = "Bearer $($authResponse.accessToken)"
    "X-Correlation-Id" = $correlationId
}

Write-Step "Logging in as the seeded analyst user for review and audit operations"
$analystAuthResponse = Invoke-JsonRequest -Method POST -Uri "$GatewayBaseUrl/api/auth/login" -Body @{
    usernameOrEmail = $analystUsername
    password        = $analystPassword
}

$analystHeaders = @{
    Authorization      = "Bearer $($analystAuthResponse.accessToken)"
    "X-Correlation-Id" = $correlationId
}

Write-Step "Creating a demo account"
$account = Invoke-JsonRequest -Method POST -Uri "$GatewayBaseUrl/api/accounts" -Headers $authHeaders -Body @{
    accountNumber  = $accountNumber
    customerId     = "customer-$suffix"
    ownerName      = "Demo Operator"
    currency       = "USD"
    initialBalance = 25000.00
}

$scenarioRequest = New-ScenarioRequest -Name $Scenario -AccountId $account.id -Suffix $suffix
$transactionHeaders = @{
    Authorization       = $authHeaders.Authorization
    "X-Correlation-Id"  = $authHeaders["X-Correlation-Id"]
    "X-Idempotency-Key" = "idem-$Scenario-$suffix"
}

Write-Step "Creating a transaction for scenario '$Scenario'"
$transactionBody = @{
    accountId        = $account.id
    amount           = $scenarioRequest.amount
    currency         = "USD"
    direction        = "DEBIT"
    merchantName     = $scenarioRequest.merchantName
    merchantCategory = $scenarioRequest.merchantCategory
    description      = $scenarioRequest.description
}
if ($null -ne $scenarioRequest.deviceId) {
    $transactionBody.deviceId = $scenarioRequest.deviceId
}

$transaction = Invoke-JsonRequest -Method POST -Uri "$GatewayBaseUrl/api/transactions" -Headers $transactionHeaders -Body $transactionBody
$transactionId = $transaction.id

$reviewCase = $null
if ($scenarioRequest.requiresReview) {
    Write-Step "Waiting for the review case to be created asynchronously"
    $reviewCases = Wait-Until `
        -Description "review case for transaction $transactionId" `
        -Action {
            Invoke-JsonRequest -Method GET -Uri "$GatewayBaseUrl/api/reviews/cases" -Headers $analystHeaders
        } `
        -Condition {
            param($cases)
            @($cases | Where-Object { $_.transactionId -eq $transactionId }).Count -gt 0
        }
    $reviewCase = @($reviewCases | Where-Object { $_.transactionId -eq $transactionId } | Select-Object -First 1)[0]

    Write-Step "Assigning the review case"
    $null = Invoke-JsonRequest -Method POST -Uri "$GatewayBaseUrl/api/reviews/cases/$($reviewCase.id)/assign" -Headers $analystHeaders -Body @{
        analyst = "demo-analyst"
        details = "Assigned by demo flow script"
    }

    if ($scenarioRequest.reviewAction -eq "approve") {
        Write-Step "Approving the review case"
        $null = Invoke-JsonRequest -Method POST -Uri "$GatewayBaseUrl/api/reviews/cases/$($reviewCase.id)/approve" -Headers $analystHeaders -Body @{
            analyst    = "demo-analyst"
            reasonCode = $scenarioRequest.reasonCode
            details    = "Approved as part of the end-to-end demo scenario"
        }
    } else {
        Write-Step "Blocking the review case"
        $null = Invoke-JsonRequest -Method POST -Uri "$GatewayBaseUrl/api/reviews/cases/$($reviewCase.id)/block" -Headers $analystHeaders -Body @{
            analyst    = "demo-analyst"
            reasonCode = $scenarioRequest.reasonCode
            details    = "Blocked as part of the end-to-end demo scenario"
        }
    }
}

Write-Step "Waiting for transaction status '$($scenarioRequest.expectedStatus)'"
$finalTransaction = Wait-Until `
    -Description "final transaction status" `
    -Action {
        Invoke-JsonRequest -Method GET -Uri "$GatewayBaseUrl/api/transactions/$transactionId" -Headers $authHeaders
    } `
    -Condition {
        param($tx)
        $tx.status -eq $scenarioRequest.expectedStatus
    }

Write-Step "Fetching related audit records"
$auditRecords = Invoke-JsonRequest `
    -Method GET `
    -Uri "$GatewayBaseUrl/api/audit/records?aggregateType=TRANSACTION&aggregateId=$transactionId" `
    -Headers $analystHeaders

Write-Step "Fetching generated notifications"
$accountNotifications = Invoke-JsonRequest `
    -Method GET `
    -Uri "$GatewayBaseUrl/api/notifications?recipientRef=account-$($account.id)" `
    -Headers $analystHeaders

$reviewNotifications = @()
if ($null -ne $reviewCase) {
    $reviewNotifications = Invoke-JsonRequest `
        -Method GET `
        -Uri "$GatewayBaseUrl/api/notifications?recipientRef=review-case-$($reviewCase.id)" `
        -Headers $analystHeaders
}

Write-Step "Demo flow completed"
$summary = [pscustomobject]@{
    scenario = $Scenario
    correlationId = $correlationId
    user = [pscustomobject]@{
        username = $username
        email    = $email
    }
    account = [pscustomobject]@{
        id            = $account.id
        accountNumber = $account.accountNumber
    }
    transaction = [pscustomobject]@{
        id                   = $finalTransaction.id
        transactionReference = $finalTransaction.transactionReference
        status               = $finalTransaction.status
    }
    reviewCase = if ($null -eq $reviewCase) {
        $null
    } else {
        [pscustomobject]@{
            id         = $reviewCase.id
            action     = $scenarioRequest.reviewAction
            assignedTo = "demo-analyst"
        }
    }
    auditRecordCount = @($auditRecords).Count
    notificationCounts = [pscustomobject]@{
        accountNotifications = @($accountNotifications).Count
        reviewNotifications  = @($reviewNotifications).Count
    }
}

$summary | ConvertTo-Json -Depth 10
