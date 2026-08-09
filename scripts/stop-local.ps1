[CmdletBinding()]
param(
    [switch]$KeepInfrastructure
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$statePath = Join-Path $root "logs\local-processes.json"
$env:FINPAY_POSTGRES_PORT = "55432"
Set-Location $root

if (Test-Path -LiteralPath $statePath) {
    $state = Get-Content -Raw -LiteralPath $statePath | ConvertFrom-Json
    $processIds = @(@($state.portProcessIds) + @($state.launcherPids) |
        Where-Object { $_ } |
        Sort-Object -Unique)

    foreach ($processId in $processIds) {
        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    }

    Remove-Item -LiteralPath $statePath -Force
    Write-Host "FinPay application processes stopped."
} else {
    Write-Host "No FinPay process state file was found."
}

if (-not $KeepInfrastructure) {
    $docker = Get-Command docker.exe -ErrorAction SilentlyContinue
    $dockerPath = if ($docker) { $docker.Source } else { "C:\Program Files\Docker\Docker\resources\bin\docker.exe" }
    if (Test-Path -LiteralPath $dockerPath) {
        & $dockerPath compose stop kafka
        if ($LASTEXITCODE -eq 0) {
            Start-Sleep -Seconds 7
        }
        & $dockerPath compose stop postgres redis zookeeper
    }
}
