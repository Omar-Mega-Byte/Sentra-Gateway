Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$ServiceRoot = Split-Path -Parent $PSScriptRoot
Push-Location $ServiceRoot
try {
  $Cache = Join-Path $ServiceRoot "target\npm-cache-newman"
  npx --yes --cache $Cache newman run `
    postman/Sentra_Notification_Service.postman_collection.json `
    -e postman/Sentra_Notification_Service_Local.postman_environment.json `
    --reporters cli
  if ($LASTEXITCODE -ne 0) {
    throw "Newman failed with exit code $LASTEXITCODE"
  }
}
finally {
  Pop-Location
}
