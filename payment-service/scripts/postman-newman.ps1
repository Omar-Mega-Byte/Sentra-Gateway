[CmdletBinding()]
param(
    [string]$BaseUrl = "http://localhost:8083",
    [int]$ReadinessTimeoutSeconds = 90
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$serviceRoot = Split-Path -Parent $PSScriptRoot
$collection = Join-Path $serviceRoot "postman\Sentra_Payment_Service.postman_collection.json"
$environment = Join-Path $serviceRoot "postman\Sentra_Payment_Service_Local.postman_environment.json"

$deadline = (Get-Date).AddSeconds($ReadinessTimeoutSeconds)
$lastReadinessError = $null

do {
    try {
        $health = Invoke-RestMethod -Uri "$BaseUrl/actuator/health/readiness" -TimeoutSec 5
        if ($health.status -eq "UP") {
            $lastReadinessError = $null
            break
        }
        $lastReadinessError = "Payment service readiness is $($health.status)"
    }
    catch {
        $lastReadinessError = $_.Exception.Message
    }

    Start-Sleep -Seconds 2
} while ((Get-Date) -lt $deadline)

if ($lastReadinessError) {
    throw "Payment service was not ready within $ReadinessTimeoutSeconds seconds: $lastReadinessError"
}

& npx --yes newman run $collection -e $environment --env-var "baseUrl=$BaseUrl" --reporters cli
if ($LASTEXITCODE -ne 0) {
    throw "newman failed with exit code $LASTEXITCODE"
}
