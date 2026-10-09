$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $projectRoot 'build/libs/public-job-hub-0.1.0.jar'
$dataRoot = Join-Path $projectRoot 'data'

if (-not $env:MYSQL_URL -or -not $env:MYSQL_USERNAME -or -not $env:MYSQL_PASSWORD) {
    throw 'MYSQL_URL, MYSQL_USERNAME, MYSQL_PASSWORD 환경변수를 먼저 설정해 주세요.'
}
if (-not (Test-Path -LiteralPath $jar)) {
    throw '배포 JAR이 없습니다. 먼저 .\gradlew.bat bootJar를 실행해 주세요.'
}
if (-not (Test-Path -LiteralPath (Join-Path $dataRoot 'jobhub.mv.db'))) {
    throw 'data/jobhub.mv.db 파일을 찾을 수 없습니다.'
}

$backupRoot = Join-Path $projectRoot 'backups'
New-Item -ItemType Directory -Force -Path $backupRoot | Out-Null
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$h2Backup = Join-Path $backupRoot "h2-before-mysql-$timestamp"
Copy-Item -LiteralPath $dataRoot -Destination $h2Backup -Recurse
Write-Host "H2 백업 완료: $h2Backup"

Push-Location $projectRoot
try {
    & java -jar $jar `
        --spring.profiles.active=mysql `
        --server.port=0 `
        --jobhub.crawl.enabled=false `
        --jobhub.public-data.recruitment.enabled=false `
        --jobhub.migration.h2-to-mysql=true
    if ($LASTEXITCODE -ne 0) { throw 'H2에서 MySQL로 데이터 이관하는 데 실패했습니다.' }
} finally {
    Pop-Location
}

Write-Host 'H2 -> MySQL 데이터 이관과 테이블별 건수 검증이 완료되었습니다.'
