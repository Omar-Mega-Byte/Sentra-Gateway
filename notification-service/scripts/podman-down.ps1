Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$ServiceRoot = Split-Path -Parent $PSScriptRoot
Push-Location $ServiceRoot
try {
  podman compose -f compose.yaml -f compose.postman.yaml down
}
finally {
  Pop-Location
}
