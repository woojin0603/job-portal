#!/usr/bin/env bash
set -u

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
STATE_DIR="${PROJECT_DIR}/backups/monitor-state"
ALERT_SCRIPT="${SCRIPT_DIR}/send-ops-alert.sh"

if [[ "$#" -lt 3 ]]; then
  echo "Usage: $0 <state-key> <display-name> <command> [args...]" >&2
  exit 2
fi

STATE_KEY="$1"
DISPLAY_NAME="$2"
shift 2

case "${DISPLAY_NAME}" in
  production-health) DISPLAY_NAME="운영 상태 점검" ;;
  tidb-backup) DISPLAY_NAME="TiDB 백업" ;;
  storage-maintenance) DISPLAY_NAME="저장공간 정리" ;;
esac

if [[ ! "${STATE_KEY}" =~ ^[a-zA-Z0-9_-]+$ ]]; then
  echo "ERROR: Invalid state key: ${STATE_KEY}" >&2
  exit 2
fi

mkdir -p -- "${STATE_DIR}"
chmod 700 -- "${STATE_DIR}"
STATE_FILE="${STATE_DIR}/${STATE_KEY}.state"
OUTPUT_FILE="$(mktemp)"
trap 'rm -f -- "${OUTPUT_FILE}"' EXIT

set +e
"$@" >"${OUTPUT_FILE}" 2>&1
COMMAND_STATUS=$?
set -e

cat "${OUTPUT_FILE}"

PREVIOUS_STATE="$(cat "${STATE_FILE}" 2>/dev/null || true)"
if [[ "${COMMAND_STATUS}" -eq 0 ]]; then
  CURRENT_STATE="healthy"
else
  CURRENT_STATE="failed"
fi

if [[ "${CURRENT_STATE}" == "failed" && "${PREVIOUS_STATE}" != "failed" ]]; then
  DETAILS="$(tail -n 20 "${OUTPUT_FILE}")"
  "${ALERT_SCRIPT}" "ERROR" "${DISPLAY_NAME} 실패" "${DETAILS}" \
    || echo "WARN: Failure alert could not be sent." >&2
elif [[ "${CURRENT_STATE}" == "healthy" && "${PREVIOUS_STATE}" == "failed" ]]; then
  DETAILS="$(tail -n 10 "${OUTPUT_FILE}")"
  "${ALERT_SCRIPT}" "RECOVERED" "${DISPLAY_NAME} 복구" "${DETAILS}" \
    || echo "WARN: Recovery alert could not be sent." >&2
fi

printf '%s\n' "${CURRENT_STATE}" >"${STATE_FILE}"
chmod 600 -- "${STATE_FILE}"
exit "${COMMAND_STATUS}"
