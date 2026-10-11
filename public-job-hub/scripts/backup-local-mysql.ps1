$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$backupRoot = Join-Path $projectRoot 'backups'
$dumpCandidates = @(
    'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe',
    'mysqldump.exe'
)
$dump = $dumpCandidates | Where-Object {
    if ([System.IO.Path]::IsPathRooted($_)) { Test-Path -LiteralPath $_ }
    else { $null -ne (Get-Command $_ -ErrorAction SilentlyContinue) }
} | Select-Object -First 1

if (-not $dump) {
    throw 'mysqldump was not found. Install MySQL Server 8 or add its bin directory to PATH.'
}

$username = Read-Host 'MySQL application username (default: jobhub)'
if ([string]::IsNullOrWhiteSpace($username)) { $username = 'jobhub' }
$securePassword = Read-Host 'MySQL password' -AsSecureString
$pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
New-Item -ItemType Directory -Force -Path $backupRoot | Out-Null
$target = Join-Path $backupRoot "public-job-hub-local-$timestamp.sql"

try {
    $env:MYSQL_PWD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    & $dump -h 127.0.0.1 -P 3306 -u $username --default-character-set=utf8mb4 --single-transaction --no-tablespaces --routines --triggers --result-file=$target public_job_hub
    if ($LASTEXITCODE -ne 0) { throw 'Local MySQL backup failed.' }
} finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
}

$file = Get-Item -LiteralPath $target
if ($file.Length -eq 0) { throw 'The backup file is empty.' }
Write-Host "Backup completed: $($file.FullName) ($($file.Length) bytes)"
