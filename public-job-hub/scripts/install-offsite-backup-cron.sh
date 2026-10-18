#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
UPLOAD_SCRIPT="${SCRIPT_DIR}/upload-backup-object-storage.sh"
MONITOR_SCRIPT="${SCRIPT_DIR}/monitor-command.sh"
LOG_FILE="${PROJECT_DIR}/backups/offsite-backup.log"
CRON_MARKER="# public-job-hub encrypted off-site backup"
CRON_SCHEDULE="${OFFSITE_BACKUP_CRON_SCHEDULE:-0 5 * * 0}"

chmod 700 -- "${UPLOAD_SCRIPT}" "${MONITOR_SCRIPT}" "${SCRIPT_DIR}/send-ops-alert.sh"
mkdir -p -- "${PROJECT_DIR}/backups"

EXISTING_CRONTAB="$(crontab -l 2>/dev/null || true)"
FILTERED_CRONTAB="$(printf '%s\n' "${EXISTING_CRONTAB}" \
  | grep -Fv "${CRON_MARKER}" \
  | grep -Fv 'CRON_TZ=Asia/Seoul' || true)"

{
  printf '%s\n' "${FILTERED_CRONTAB}"
  printf 'CRON_TZ=Asia/Seoul\n'
  printf '%s %q offsite-backup offsite-backup %q >> %q 2>&1 %s\n' \
    "${CRON_SCHEDULE}" "${MONITOR_SCRIPT}" "${UPLOAD_SCRIPT}" "${LOG_FILE}" "${CRON_MARKER}"
} | sed '/^[[:space:]]*$/d' | crontab -

echo "Encrypted off-site backup scheduled for every Sunday at 05:00 Asia/Seoul."
echo "Log: ${LOG_FILE}"
crontab -l | grep -F "${CRON_MARKER}"
