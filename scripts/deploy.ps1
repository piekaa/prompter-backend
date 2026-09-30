# Deploy prompter-backend to production (Windows/PowerShell version of deploy.sh)
# Build, scp image, restart compose stack on server
[CmdletBinding()]
param(
    [string]$TargetHost = "root@piekoszek.pl",
    [string]$AppDir = "/opt/prompter-app",
    [string]$ImageName = "prompter-backend",
    [int]$HostPort = 8081
)
$ErrorActionPreference = "Stop"
$BuildContext = Split-Path -Parent $PSScriptRoot
$TempTar = Join-Path $env:TEMP "prompter-backend-docker-$([guid]::NewGuid().ToString('n').Substring(0,5)).tar"

function Invoke-Remote {
    param([string]$Cmd)
    # compose/certbot write progress to stderr — keep EAP=Continue locally so
    # PS 5.1 does not treat stderr lines as terminating errors; judge by exit code
    $prev = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $out = & ssh $TargetHost $Cmd 2>&1 | ForEach-Object { "$_" }
    $ErrorActionPreference = $prev
    if ($LASTEXITCODE -ne 0) {
        Write-Error "remote command failed (exit $LASTEXITCODE): $Cmd`n$out"
        exit 1
    }
    return $out
}

try {
    Write-Host "=== Building Docker image ==="
    & docker build --tag "${ImageName}:latest" $BuildContext
    if ($LASTEXITCODE -ne 0) { throw "docker build failed" }

    Write-Host "`n=== Saving image to tarball ==="
    & docker save -o $TempTar "${ImageName}:latest"
    if ($LASTEXITCODE -ne 0) { throw "docker save failed" }

    Write-Host "`n=== Sending image to server ($TargetHost) ==="
    Invoke-Remote "mkdir -p $AppDir" | Out-Null
    & scp $TempTar "${TargetHost}:$AppDir/image.tar"
    if ($LASTEXITCODE -ne 0) { throw "scp image failed" }

    Write-Host "`n=== Loading image on server ==="
    Invoke-Remote "docker load -i $AppDir/image.tar && rm -f $AppDir/image.tar" | Out-Null

    Write-Host "`n=== Updating compose file ==="
    & scp (Join-Path $BuildContext "docker-compose.prod.yml") "${TargetHost}:$AppDir/docker-compose.prod.yml"
    if ($LASTEXITCODE -ne 0) { throw "scp compose file failed" }

    Write-Host "`n=== Restarting stack ==="
    Invoke-Remote "cd $AppDir && docker compose -f docker-compose.prod.yml down" | Out-Null
    Invoke-Remote "cd $AppDir && docker compose -f docker-compose.prod.yml up -d"

    Write-Host "`n=== Waiting for health check ==="
    $isUp = $false
    for ($i = 1; $i -le 60; $i++) {
        $health = & ssh $TargetHost "curl -sf http://localhost:$HostPort/actuator/health" 2>&1
        if ($health -match '"status":"UP"') { $isUp = $true; break }
        Write-Host "  waiting... ($i)"
        Start-Sleep -Seconds 2
    }
    if (-not $isUp) { throw "health check did not go UP within 120s" }
    Write-Host "App is UP!"

    Write-Host "`n=== Done ==="
}
catch {
    Write-Error $_
    exit 1
}
finally {
    Remove-Item $TempTar -ErrorAction SilentlyContinue
}
