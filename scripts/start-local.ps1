[CmdletBinding()]
param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$logs = Join-Path $root "logs"
$statePath = Join-Path $logs "local-processes.json"
$env:FINPAY_POSTGRES_PORT = "55432"
Set-Location $root

function Resolve-Docker {
    $command = Get-Command docker.exe -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $desktopDocker = "C:\Program Files\Docker\Docker\resources\bin\docker.exe"
    if (Test-Path -LiteralPath $desktopDocker) {
        return $desktopDocker
    }

    throw "Docker CLI was not found. Start Docker Desktop and try again."
}

function Invoke-DockerProbe([string[]]$Arguments) {
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        & $docker @Arguments 1>$null 2>$null
        return $LASTEXITCODE -eq 0
    } finally {
        $ErrorActionPreference = $previousPreference
    }
}

function Test-HttpEndpoint([string]$Url) {
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 3
        return $response.StatusCode -eq 200
    } catch {
        return $false
    }
}

function Assert-PortAvailable([int]$Port, [string]$Name) {
    $listener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
    if ($listener) {
        throw "Port $Port is already occupied, so $Name cannot start."
    }
}

$docker = Resolve-Docker
& $docker info --format "{{.ServerVersion}}" | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Docker is installed, but its engine is not running."
}

Write-Host "Starting FinPay PostgreSQL, Redis, and ZooKeeper..."
& $docker compose up -d postgres redis zookeeper
if ($LASTEXITCODE -ne 0) {
    throw "FinPay infrastructure failed to start."
}

$postgresReady = $false
for ($attempt = 1; $attempt -le 30; $attempt++) {
    if (Invoke-DockerProbe -Arguments @("exec", "finpay-postgres", "pg_isready", "-U", "finpay", "-d", "finpay")) {
        $postgresReady = $true
        break
    }
    Start-Sleep -Seconds 2
}
if (-not $postgresReady) {
    throw "FinPay PostgreSQL did not become ready."
}

$redisReady = $false
for ($attempt = 1; $attempt -le 30; $attempt++) {
    if (Invoke-DockerProbe -Arguments @("exec", "finpay-redis", "redis-cli", "ping")) {
        $redisReady = $true
        break
    }
    Start-Sleep -Seconds 2
}
if (-not $redisReady) {
    throw "FinPay Redis did not become ready."
}

Write-Host "Starting FinPay Kafka..."
$kafkaReady = $false
for ($startAttempt = 1; $startAttempt -le 6 -and -not $kafkaReady; $startAttempt++) {
    & $docker compose up -d kafka
    if ($LASTEXITCODE -ne 0) {
        throw "FinPay Kafka container failed to start."
    }

    for ($healthAttempt = 1; $healthAttempt -le 15; $healthAttempt++) {
        if (Invoke-DockerProbe -Arguments @(
                "exec", "finpay-kafka", "kafka-topics",
                "--bootstrap-server", "localhost:9092", "--list"
            )) {
            $kafkaReady = $true
            break
        }
        Start-Sleep -Seconds 2
    }

    if (-not $kafkaReady) {
        Write-Host "Kafka is not ready yet; retrying after ZooKeeper clears the previous broker session..."
        Start-Sleep -Seconds 5
    }
}
if (-not $kafkaReady) {
    throw "FinPay Kafka did not become ready. Check the finpay-kafka container logs."
}

& $docker exec finpay-kafka kafka-topics --bootstrap-server localhost:9092 `
    --create --if-not-exists --topic payment-events --partitions 1 --replication-factor 1 *> $null
if ($LASTEXITCODE -ne 0) {
    throw "FinPay Kafka is running, but the payment-events topic could not be created."
}

$maven = Join-Path $root "mvnw.cmd"
if (-not $SkipBuild) {
    Write-Host "Building FinPay modules..."
    & $maven -q "-DskipTests" install
    if ($LASTEXITCODE -ne 0) {
        throw "The FinPay Maven build failed."
    }
}

$frontend = Join-Path $root "frontend"
$npm = (Get-Command npm.cmd -ErrorAction Stop).Source
if (-not (Test-Path -LiteralPath (Join-Path $frontend "node_modules"))) {
    Write-Host "Installing frontend dependencies..."
    & $npm install --prefix $frontend
    if ($LASTEXITCODE -ne 0) {
        throw "Frontend dependency installation failed."
    }
}

@(
    "VITE_API_BASE_URL=http://localhost:8180"
    "VITE_GATEWAY_PROXY_TARGET=http://localhost:8180"
) | Set-Content -LiteralPath (Join-Path $frontend ".env.development.local") -Encoding ascii

New-Item -ItemType Directory -Force -Path $logs | Out-Null
$launcherPids = @()
$startedAt = (Get-Date).ToString("o")

function Save-ProcessState {
    $trackedPorts = 5173, 8180, 8181, 8182, 8183, 8184
    $portProcessIds = @(Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
        Where-Object { $_.LocalPort -in $trackedPorts } |
        Select-Object -ExpandProperty OwningProcess -Unique)

    @{
        launcherPids = $launcherPids
        portProcessIds = $portProcessIds
        createdAt = $startedAt
    } | ConvertTo-Json | Set-Content -LiteralPath $statePath -Encoding ascii
}

$services = @(
    @{ Name = "auth-service"; Port = 8181 },
    @{ Name = "wallet-service"; Port = 8182 },
    @{ Name = "payment-service"; Port = 8183 },
    @{ Name = "notification-service"; Port = 8184 },
    @{ Name = "api-gateway"; Port = 8180 }
)

foreach ($service in $services) {
    $healthUrl = "http://localhost:$($service.Port)/actuator/health"
    if (Test-HttpEndpoint $healthUrl) {
        Write-Host "$($service.Name) is already running."
        continue
    }

    Assert-PortAvailable $service.Port $service.Name
    $stdout = Join-Path $logs "$($service.Name).out.log"
    $stderr = Join-Path $logs "$($service.Name).err.log"
    $process = Start-Process -WindowStyle Hidden -WorkingDirectory $root -FilePath $maven `
        -ArgumentList @("-pl", $service.Name, "spring-boot:run", "-Dspring-boot.run.profiles=ui-local") `
        -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru
    $launcherPids += $process.Id
    Write-Host "Starting $($service.Name)..."
}

$frontendUrl = "http://localhost:5173"
if (Test-HttpEndpoint $frontendUrl) {
    Write-Host "React frontend is already running."
} else {
    Assert-PortAvailable 5173 "React frontend"
    $frontendProcess = Start-Process -WindowStyle Hidden -WorkingDirectory $frontend -FilePath $npm `
        -ArgumentList @("run", "dev") `
        -RedirectStandardOutput (Join-Path $logs "frontend.out.log") `
        -RedirectStandardError (Join-Path $logs "frontend.err.log") -PassThru
    $launcherPids += $frontendProcess.Id
    Write-Host "Starting React frontend..."
}

Save-ProcessState

$endpoints = @($services | ForEach-Object {
    @{ Name = $_.Name; Url = "http://localhost:$($_.Port)/actuator/health" }
}) + @(@{ Name = "frontend"; Url = $frontendUrl })

$deadline = (Get-Date).AddSeconds(120)
do {
    $pending = @($endpoints | Where-Object { -not (Test-HttpEndpoint $_.Url) })
    if ($pending.Count -eq 0) {
        break
    }
    Start-Sleep -Seconds 2
} while ((Get-Date) -lt $deadline)

if ($pending.Count -gt 0) {
    $names = ($pending | ForEach-Object { $_.Name }) -join ", "
    throw "These FinPay processes did not become healthy: $names. Check the logs directory."
}

Save-ProcessState

Write-Host ""
Write-Host "FinPay is ready: http://localhost:5173"
Write-Host "API Gateway: http://localhost:8180"
