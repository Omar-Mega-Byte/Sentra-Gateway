[CmdletBinding()]
param(
    [switch]$Postman,
    [switch]$ForceRecreate,
    [switch]$NoBuild
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$serviceRoot = Split-Path -Parent $PSScriptRoot
$networkName = if ($env:SENTRA_SERVICES_NETWORK) { $env:SENTRA_SERVICES_NETWORK } else { "sentra-gateway_services" }

function Invoke-Podman {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)

    & podman @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "podman $($Arguments -join ' ') failed with exit code $LASTEXITCODE"
    }
}

Push-Location $serviceRoot
try {
    if (-not (Test-Path ".env")) {
        Copy-Item ".env.example" ".env"
    }

    & podman network exists $networkName *> $null
    if ($LASTEXITCODE -ne 0) {
        Invoke-Podman @("network", "create", "--internal", $networkName)
    }

    $composeArgs = @("compose", "--env-file", ".env", "-f", "compose.yaml")
    if ($Postman) {
        $composeArgs += @("-f", "compose.postman.yaml")
    }

    $composeArgs += "up"
    if (-not $NoBuild) {
        $composeArgs += "--build"
    }
    if ($ForceRecreate) {
        $composeArgs += "--force-recreate"
    }
    $composeArgs += "-d"

    Invoke-Podman $composeArgs
}
finally {
    Pop-Location
}
