Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$ServiceRoot = Split-Path -Parent $PSScriptRoot
Push-Location $ServiceRoot
try {
  if (-not (Test-Path -LiteralPath ".env")) {
    Copy-Item -LiteralPath ".env.example" -Destination ".env"
  }
  podman network exists sentra-gateway_services
  if ($LASTEXITCODE -ne 0) {
    podman network create --internal sentra-gateway_services | Out-Host
  }
  $previousBuildahFormat = $env:BUILDAH_FORMAT
  try {
    $env:BUILDAH_FORMAT = "docker"
    podman compose --env-file .env -f compose.yaml -f compose.postman.yaml up --build -d
    podman compose -f compose.yaml -f compose.postman.yaml ps
  }
  finally {
    if ($null -eq $previousBuildahFormat) {
      Remove-Item Env:\BUILDAH_FORMAT -ErrorAction SilentlyContinue
    }
    else {
      $env:BUILDAH_FORMAT = $previousBuildahFormat
    }
  }
}
finally {
  Pop-Location
}
