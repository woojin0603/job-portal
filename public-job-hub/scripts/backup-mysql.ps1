$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$backupRoot = Join-Path $projectRoot 'backups'
New-Item -ItemType Directory -Force -Path $backupRoot | Out-Null
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$target = Join-Path $backupRoot "public-job-hub-$timestamp.sql"

Push-Location $projectRoot
try {
    docker compose exec -T mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers public_job_hub' |
        Set-Content -LiteralPath $target -Encoding utf8
    if ($LASTEXITCODE -ne 0) { throw 'MySQL 백업에 실패했습니다.' }
} finally {
    Pop-Location
}

Write-Host "백업 완료: $target"
