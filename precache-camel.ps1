$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$JbangExe = Join-Path $ScriptDir "route-builder\jbang.cmd"

if (-not (Test-Path $JbangExe)) {
    $JbangExe = Join-Path $ScriptDir "jbang.cmd"
}

if (-not (Test-Path $JbangExe)) {
    Write-Error "Error: Could not find JBang wrapper script in route-builder\jbang.cmd or .\jbang.cmd"
    exit 1
}

Write-Host "Pre-caching Camel JBang CLI locally..."
Write-Host "Executing: & '$JbangExe' --main=main.CamelJBang camel@apache/camel --help"
& $JbangExe --main=main.CamelJBang camel@apache/camel --help

Write-Host "Camel dependencies pre-cached successfully!"
