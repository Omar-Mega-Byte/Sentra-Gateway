[CmdletBinding()]
param(
    [switch]$WithPostman
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$serviceRoot = Split-Path -Parent $PSScriptRoot
$mvnw = Join-Path $serviceRoot "mvnw.cmd"

function Invoke-CheckedNative {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FilePath,

        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$FilePath failed with exit code $LASTEXITCODE"
    }
}

Push-Location $serviceRoot
try {
    Invoke-CheckedNative $mvnw @("-B", "-ntp", "clean", "verify")
    Invoke-CheckedNative $mvnw @("-B", "-ntp", "javadoc:javadoc")

    if ($WithPostman) {
        & (Join-Path $PSScriptRoot "postman-newman.ps1")
        if ($LASTEXITCODE -ne 0) {
            throw "postman-newman.ps1 failed with exit code $LASTEXITCODE"
        }
    }
}
finally {
    Pop-Location
}
