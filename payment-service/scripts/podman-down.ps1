[CmdletBinding()]
param(
    [switch]$Postman,
    [switch]$RemoveVolumes
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$serviceRoot = Split-Path -Parent $PSScriptRoot

function Invoke-Podman {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)

    & podman @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "podman $($Arguments -join ' ') failed with exit code $LASTEXITCODE"
    }
}

Push-Location $serviceRoot
try {
    $composeArgs = @("compose", "--env-file", ".env", "-f", "compose.yaml")
    if ($Postman) {
        $composeArgs += @("-f", "compose.postman.yaml")
    }

    $composeArgs += "down"
    if ($RemoveVolumes) {
        $composeArgs += "--volumes"
    }

    Invoke-Podman $composeArgs
}
finally {
    Pop-Location
}
