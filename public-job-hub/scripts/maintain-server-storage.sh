#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
LOG_LIMIT_BYTES="${LOG_LIMIT_BYTES:-5242880}"
LOG_KEEP_LINES="${LOG_KEEP_LINES:-2000}"

compact_log() {
  local file="$1"
  [[ -f "${file}" ]] || return 0
  local size
  size="$(stat -c %s "${file}")"
  if [[ "${size}" -le "${LOG_LIMIT_BYTES}" ]]; then return 0; fi

  local temporary
  temporary="$(mktemp "${file}.XXXXXX")"
  tail -n "${LOG_KEEP_LINES}" "${file}" > "${temporary}"
  chmod --reference="${file}" "${temporary}"
  mv -- "${temporary}" "${file}"
  echo "Compacted log: ${file}"
}

printf '\n=== Public Job Hub storage maintenance: %s ===\n' \
  "$(TZ=Asia/Seoul date +'%Y-%m-%d %H:%M:%S KST')"

if ! docker info >/dev/null 2>&1; then
  echo "ERROR: Docker is not available." >&2
  exit 1
fi

echo "Disk usage before maintenance:"
df -h "${PROJECT_DIR}" | tail -n 1

# 실행 중인 컨테이너, 연결된 이미지와 모든 볼륨은 보존한다.
docker container prune --force --filter 'until=168h'
docker image prune --force --filter 'until=168h'
docker builder prune --force --filter 'until=168h'

compact_log "${PROJECT_DIR}/backups/tidb-backup.log"
compact_log "${PROJECT_DIR}/backups/production-health.log"

echo "Docker storage after maintenance:"
docker system df
echo "Disk usage after maintenance:"
df -h "${PROJECT_DIR}" | tail -n 1
echo "RESULT: STORAGE MAINTENANCE COMPLETED"
