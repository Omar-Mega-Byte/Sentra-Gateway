Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Import-DotEnv {
  param([Parameter(Mandatory = $true)][string]$Path)

  foreach ($rawLine in Get-Content -LiteralPath $Path) {
    $line = $rawLine.Trim()
    if ($line.Length -eq 0 -or $line.StartsWith("#")) {
      continue
    }

    $separator = $line.IndexOf("=")
    if ($separator -le 0) {
      continue
    }

    $key = $line.Substring(0, $separator).Trim()
    $value = $line.Substring($separator + 1).Trim()
    if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
      $value = $value.Substring(1, $value.Length - 2)
    }

    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($key, "Process"))) {
      Set-Item -Path "Env:$key" -Value $value
    }
  }
}

$ServiceRoot = Split-Path -Parent $PSScriptRoot
Push-Location $ServiceRoot
try {
  if (-not (Test-Path -LiteralPath ".env")) {
    Copy-Item -LiteralPath ".env.example" -Destination ".env"
  }

  Import-DotEnv -Path ".env"

  $defaults = [ordered]@{
    SPRING_PROFILES_ACTIVE = "local"
    SERVER_PORT = "8084"
    SENTRA_ENVIRONMENT = "local"
    NOTIFICATION_SEED_ENABLED = "true"
    FAULT_CONTROLS_ENABLED = "true"
    OPENAPI_ENABLED = "true"
    SWAGGER_UI_ENABLED = "true"
  }

  foreach ($entry in $defaults.GetEnumerator()) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($entry.Key, "Process"))) {
      Set-Item -Path "Env:$($entry.Key)" -Value $entry.Value
    }
  }

  .\mvnw.cmd spring-boot:run
}
finally {
  Pop-Location
}
