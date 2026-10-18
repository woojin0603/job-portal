#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
MAINTENANCE_SCRIPT="${SCRIPT_DIR}/maintain-server-storage.sh"
MONITOR_SCRIPT="${SCRIPT_DIR}/monitor-command.sh"
LOG_FILE="${PROJECT_DIR}/backups/storage-maintenance.log"
CRON_MARKER="# public-job-hub storage maintenance"
CRON_SCHEDULE="${STORAGE_MAINTENANCE_CRON_SCHEDULE:-30 4 * * 0}"

chmod 700 -- "${MAINTENANCE_SCRIPT}" "${MONITOR_SCRIPT}" "${SCRIPT_DIR}/send-ops-alert.sh"
mkdir -p -- "${PROJECT_DIR}/backups"

EXISTING_CRONTAB="$(crontab -l 2>/dev/null || true)"
FILTERED_CRONTAB="$(printf '%s\n' "${EXISTING_CRONTAB}" \
  | grep -Fv "${CRON_MARKER}" \
  | grep -Fv 'CRON_TZ=Asia/Seoul' || true)"

{
  printf 'CRON_TZ=Asia/Seoul\n'
  printf '%s\n' "${FILTERED_CRONTAB}"
  printf '%s %q storage storage-maintenance %q >> %q 2>&1 %s\n' \
    "${CRON_SCHEDULE}" "${MONITOR_SCRIPT}" "${MAINTENANCE_SCRIPT}" "${LOG_FILE}" "${CRON_MARKER}"
} | sed '/^[[:space:]]*$/d' | crontab -

echo "Storage maintenance scheduled for every Sunday at 04:30 Asia/Seoul."
echo "Log: ${LOG_FILE}"
crontab -l | grep -F "${CRON_MARKER}"
