Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$ServiceRoot = Split-Path -Parent $PSScriptRoot
Push-Location $ServiceRoot
try {
  .\mvnw.cmd -B -ntp clean verify
  .\mvnw.cmd -B -ntp javadoc:javadoc
}
finally {
  Pop-Location
}
