#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
BACKUP_SCRIPT="${SCRIPT_DIR}/backup-tidb.sh"
LOG_FILE="${PROJECT_DIR}/backups/tidb-backup.log"
CRON_MARKER="# public-job-hub TiDB backup"
CRON_SCHEDULE="${BACKUP_CRON_SCHEDULE:-30 3 * * *}"

chmod 700 -- "${BACKUP_SCRIPT}"
mkdir -p -- "${PROJECT_DIR}/backups"

EXISTING_CRONTAB="$(crontab -l 2>/dev/null || true)"
FILTERED_CRONTAB="$(printf '%s\n' "${EXISTING_CRONTAB}" \
  | grep -Fv "${CRON_MARKER}" \
  | grep -Fv 'CRON_TZ=Asia/Seoul' || true)"

{
  printf '%s\n' "${FILTERED_CRONTAB}"
  printf 'CRON_TZ=Asia/Seoul\n'
  printf '%s %q >> %q 2>&1 %s\n' \
    "${CRON_SCHEDULE}" "${BACKUP_SCRIPT}" "${LOG_FILE}" "${CRON_MARKER}"
} | sed '/^[[:space:]]*$/d' | crontab -

echo "Daily TiDB backup scheduled for 03:30 Asia/Seoul."
echo "Log: ${LOG_FILE}"
crontab -l | grep -F -B 1 "${CRON_MARKER}"
