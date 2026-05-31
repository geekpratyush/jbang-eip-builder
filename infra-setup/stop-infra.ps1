# Stop infrastructure and clean up ports on Windows/Linux/macOS PowerShell

# Detect docker-compose command
$dockerCmd = "docker"
$dockerArgs = @("compose")

if (Get-Command "docker-compose" -ErrorAction SilentlyContinue) {
    $dockerCmd = "docker-compose"
    $dockerArgs = @()
}

Write-Host "=== Stopping Docker Containers & Volumes ===" -ForegroundColor Cyan
& $dockerCmd $dockerArgs down -v

Write-Host "`n=== Cleaning Up Remaining Port Clashes ===" -ForegroundColor Cyan
$ports = @(1414, 2181, 9094, 19093, 27017, 88, 749)

foreach ($port in $ports) {
    Write-Host ("Checking port {0}..." -f $port) -ForegroundColor Gray
    
    if ($IsWindows) {
        $netstat = netstat -ano | Select-String ":$port\s"
        if ($netstat) {
            $pid = ($netstat.Line -split '\s+')[-1]
            if ($pid -and $pid -ne "0") {
                Write-Host ("Found process {0} running on port {1}. Terminating..." -f $pid, $port) -ForegroundColor Yellow
                Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
            }
        } else {
            Write-Host ("Port {0} is free." -f $port)
        }
    } else {
        # Linux/macOS
        $pid = (lsof -t -i :$port 2>$null)
        if ($pid) {
            Write-Host ("Found process {0} running on port {1}. Terminating..." -f $pid, $port) -ForegroundColor Yellow
            kill -9 $pid 2>$null
        } else {
            Write-Host ("Port {0} is free." -f $port)
        }
    }
}

Write-Host "`n=== Clean Up Complete! ===" -ForegroundColor Green
