$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    docker compose --env-file .env ps
    docker compose --env-file .env logs --tail 80 app caddy
} finally {
    Pop-Location
}
