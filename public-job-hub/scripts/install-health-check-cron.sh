#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
HEALTH_SCRIPT="${SCRIPT_DIR}/check-production-health.sh"
MONITOR_SCRIPT="${SCRIPT_DIR}/monitor-command.sh"
LOG_FILE="${PROJECT_DIR}/backups/production-health.log"
CRON_MARKER="# public-job-hub production health check"
CRON_SCHEDULE="${HEALTH_CHECK_CRON_SCHEDULE:-7 * * * *}"

chmod 700 -- "${HEALTH_SCRIPT}" "${MONITOR_SCRIPT}" "${SCRIPT_DIR}/send-ops-alert.sh"
mkdir -p -- "${PROJECT_DIR}/backups"

EXISTING_CRONTAB="$(crontab -l 2>/dev/null || true)"
FILTERED_CRONTAB="$(printf '%s\n' "${EXISTING_CRONTAB}" \
  | grep -Fv "${CRON_MARKER}" \
  | grep -Fv 'CRON_TZ=Asia/Seoul' || true)"

{
  printf '%s\n' "${FILTERED_CRONTAB}"
  printf 'CRON_TZ=Asia/Seoul\n'
  printf '%s %q health production-health %q >> %q 2>&1 %s\n' \
    "${CRON_SCHEDULE}" "${MONITOR_SCRIPT}" "${HEALTH_SCRIPT}" "${LOG_FILE}" "${CRON_MARKER}"
} | sed '/^[[:space:]]*$/d' | crontab -

echo "Production health check scheduled hourly at minute 7."
echo "Log: ${LOG_FILE}"
crontab -l | grep -F -B 1 "${CRON_MARKER}"
