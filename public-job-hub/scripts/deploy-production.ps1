$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$environmentFile = Join-Path $projectRoot '.env'

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker is not installed or is not available in PATH.'
}
if (-not (Test-Path -LiteralPath $environmentFile)) {
    throw 'Create .env from .env.example and replace every change-* value first.'
}

$required = @(
    'JOBHUB_DOMAIN',
    'MYSQL_USERNAME',
    'MYSQL_PASSWORD',
    'MYSQL_ROOT_PASSWORD',
    'JOBHUB_ADMIN_USERNAME',
    'JOBHUB_ADMIN_PASSWORD'
)
$values = @{}
Get-Content -LiteralPath $environmentFile | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]*)=(.*)$') {
        $values[$matches[1].Trim()] = $matches[2].Trim()
    }
}
foreach ($name in $required) {
    $value = $values[$name]
    if ([string]::IsNullOrWhiteSpace($value) -or $value -like 'change-*' -or $value -eq 'jobs.example.com') {
        throw "Set a production value for $name in .env."
    }
}
if (($values['MYSQL_PASSWORD'].Length -lt 16) -or ($values['MYSQL_ROOT_PASSWORD'].Length -lt 16) -or ($values['JOBHUB_ADMIN_PASSWORD'].Length -lt 16)) {
    throw 'MySQL and administrator passwords must be at least 16 characters.'
}

Push-Location $projectRoot
try {
    docker compose --env-file .env config --quiet
    if ($LASTEXITCODE -ne 0) { throw 'Docker Compose configuration validation failed.' }
    docker compose --env-file .env up -d --build --remove-orphans
    if ($LASTEXITCODE -ne 0) { throw 'Production deployment failed.' }
    docker compose --env-file .env ps
} finally {
    Pop-Location
}

Write-Host 'Deployment started. Caddy will issue the HTTPS certificate after DNS reaches this server.'
