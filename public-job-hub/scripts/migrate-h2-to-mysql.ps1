$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $projectRoot 'build/libs/public-job-hub-0.1.0.jar'
$dataRoot = Join-Path $projectRoot 'data'

if (-not $env:MYSQL_URL -or -not $env:MYSQL_USERNAME -or -not $env:MYSQL_PASSWORD) {
    throw 'Set MYSQL_URL, MYSQL_USERNAME, and MYSQL_PASSWORD before migration.'
}
if (-not (Test-Path -LiteralPath $jar)) {
    throw 'Application JAR not found. Run .\gradlew.bat bootJar first.'
}
if (-not (Test-Path -LiteralPath (Join-Path $dataRoot 'jobhub.mv.db'))) {
    throw 'H2 database file data/jobhub.mv.db was not found.'
}

$backupRoot = Join-Path $projectRoot 'backups'
New-Item -ItemType Directory -Force -Path $backupRoot | Out-Null
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$h2Backup = Join-Path $backupRoot "h2-before-mysql-$timestamp"
Copy-Item -LiteralPath $dataRoot -Destination $h2Backup -Recurse
$migrationLog = Join-Path $backupRoot "mysql-migration-$timestamp.log"
Write-Host "H2 backup completed: $h2Backup"
Write-Host "Migration log: $migrationLog"

Push-Location $projectRoot
try {
    & java -jar $jar `
        --spring.profiles.active=mysql `
        --server.port=0 `
        --jobhub.crawl.enabled=false `
        --jobhub.public-data.recruitment.enabled=false `
        --jobhub.migration.h2-to-mysql=true 2>&1 | Tee-Object -FilePath $migrationLog
    $migrationExitCode = $LASTEXITCODE
    if ($migrationExitCode -ne 0) {
        throw "H2 to MySQL migration failed (exit code $migrationExitCode). Review: $migrationLog"
    }
} finally {
    Pop-Location
}

Write-Host 'H2 -> MySQL migration and row-count verification completed.'
