# Interactive Infrastructure Selector and Startup Script for PowerShell
# Supports Windows, Linux, and macOS

$choices = @(
    @{ Name = "IBM MQ with mTLS"; Service = "ibmmq" }
    @{ Name = "Apache Kafka with Kerberos"; Service = "zookeeper kerberos-kdc apache-kafka" }
    @{ Name = "Confluent Kafka with mTLS"; Service = "zookeeper confluent-kafka" }
    @{ Name = "MongoDB with TLS CA"; Service = "mongodb" }
)

Write-Host "`n=== EIP Builder Infrastructure Selector ===" -ForegroundColor Cyan
Write-Host "Select which services to start (enter numbers separated by commas, e.g. 1,3 or press Enter to start ALL):" -ForegroundColor Gray

for ($i = 0; $i -lt $choices.Length; $i++) {
    Write-Host (" [{0}] {1}" -f ($i + 1), $choices[$i].Name)
}

$inputSelection = Read-Host "Your Selection"
$selectedServices = @()
$startApacheKafka = $false

if ([string]::IsNullOrWhiteSpace($inputSelection)) {
    # Default to starting all services
    foreach ($c in $choices) {
        $selectedServices += $c.Service.Split(" ")
        if ($c.Service -like "*apache-kafka*") { $startApacheKafka = $true }
    }
} else {
    $nums = $inputSelection.Split(",")
    foreach ($num in $nums) {
        if ([int]::TryParse($num.Trim(), [ref]$idx)) {
            $idx = $idx - 1
            if ($idx -ge 0 -and $idx -lt $choices.Length) {
                $selectedServices += $choices[$idx].Service.Split(" ")
                if ($choices[$idx].Service -like "*apache-kafka*") { $startApacheKafka = $true }
            }
        }
    }
}

# Unique elements only
$selectedServices = $selectedServices | Select-Object -Unique

if ($selectedServices.Count -eq 0) {
    Write-Host "No valid services selected. Exiting." -ForegroundColor Yellow
    exit
}

# Detect docker-compose command
$dockerCmd = "docker"
$dockerArgs = @("compose")

if (Get-Command "docker-compose" -ErrorAction SilentlyContinue) {
    $dockerCmd = "docker-compose"
    $dockerArgs = @()
}

Write-Host ("`nStarting selected services: {0}..." -f ($selectedServices -join ", ")) -ForegroundColor Green
& $dockerCmd $dockerArgs up -d $selectedServices

if ($startApacheKafka) {
    Write-Host "`n=== Configuring Kerberos Principals and Keytabs ===" -ForegroundColor Cyan
    Write-Host "Waiting for Kerberos KDC to boot..." -ForegroundColor Gray
    Start-Sleep -Seconds 6

    Write-Host "Creating principal: kafka/localhost@EXAMPLE.COM"
    docker exec kerberos-kdc kadmin.local -q "addprinc -randkey kafka/localhost@EXAMPLE.COM"
    Write-Host "Creating principal: client@EXAMPLE.COM"
    docker exec kerberos-kdc kadmin.local -q "addprinc -randkey client@EXAMPLE.COM"

    Write-Host "Exporting keytabs..."
    docker exec kerberos-kdc kadmin.local -q "xst -k /var/keytabs/kafka.keytab kafka/localhost@EXAMPLE.COM"
    docker exec kerberos-kdc kadmin.local -q "xst -k /var/keytabs/client.keytab client@EXAMPLE.COM"

    # Fix permissions via the KDC container so they are readable on the host and by apache-kafka container
    docker exec kerberos-kdc chown -R 1000:1000 /var/keytabs
    docker exec kerberos-kdc chmod 644 /var/keytabs/kafka.keytab /var/keytabs/client.keytab

    # Copy the JAAS configuration into the mapped directory
    Copy-Item "kafka_server_jaas.conf" "certs/apache-kafka/kafka_server_jaas.conf" -Force

    Write-Host "Restarting apache-kafka to apply Kerberos keytabs..." -ForegroundColor Gray
    & $dockerCmd $dockerArgs restart apache-kafka
}

Write-Host "`n=== Selected Infrastructure is Running! ===" -ForegroundColor Green
& $dockerCmd $dockerArgs ps
