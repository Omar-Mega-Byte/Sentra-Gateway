Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$ServiceRoot = Split-Path -Parent $PSScriptRoot
Push-Location $ServiceRoot
try {
  .\mvnw.cmd -B -ntp test
}
finally {
  Pop-Location
}
