#!/usr/bin/env bash
set -u

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${PROJECT_DIR}/compose-tidb.yml"
ENV_FILE="${PROJECT_DIR}/.env"
BACKUP_DIR="${PROJECT_DIR}/backups/tidb"
MAX_BACKUP_AGE_HOURS="${MAX_BACKUP_AGE_HOURS:-30}"
MIN_AVAILABLE_MEMORY_MB="${MIN_AVAILABLE_MEMORY_MB:-128}"
MAX_DISK_USAGE_PERCENT="${MAX_DISK_USAGE_PERCENT:-85}"
ISSUES=0

ok() { printf '[OK] %s\n' "$1"; }
warn() { printf '[WARN] %s\n' "$1"; ISSUES=$((ISSUES + 1)); }
fail() { printf '[ERROR] %s\n' "$1"; ISSUES=$((ISSUES + 1)); }

container_env() {
  local container="$1"
  local key="$2"
  docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' "${container}" 2>/dev/null \
    | awk -F= -v key="${key}" '$1 == key {sub(/^[^=]*=/, ""); print; exit}'
}

printf '\n=== Public Job Hub health check: %s ===\n' "$(TZ=Asia/Seoul date +'%Y-%m-%d %H:%M:%S KST')"

if [[ ! -f "${ENV_FILE}" ]]; then
  fail "Deployment environment file is missing: ${ENV_FILE}"
  exit 1
fi

APP_CONTAINER="$(docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" ps -a -q app 2>/dev/null)"
CADDY_CONTAINER="$(docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" ps -a -q caddy 2>/dev/null)"

if [[ -z "${APP_CONTAINER}" ]]; then
  fail "App container does not exist."
else
  APP_HEALTH="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${APP_CONTAINER}" 2>/dev/null)"
  if [[ "${APP_HEALTH}" == "healthy" ]]; then
    ok "App container is healthy."
  else
    fail "App container state is ${APP_HEALTH:-unknown}."
  fi
fi

if [[ -z "${CADDY_CONTAINER}" ]]; then
  fail "Caddy container does not exist."
else
  CADDY_STATE="$(docker inspect --format '{{.State.Status}}' "${CADDY_CONTAINER}" 2>/dev/null)"
  if [[ "${CADDY_STATE}" == "running" ]]; then
    ok "Caddy container is running."
  else
    fail "Caddy container state is ${CADDY_STATE:-unknown}."
  fi
fi

if [[ -n "${CADDY_CONTAINER}" ]]; then
  JOBHUB_DOMAIN="$(container_env "${CADDY_CONTAINER}" JOBHUB_DOMAIN)"
  if [[ -n "${JOBHUB_DOMAIN}" ]] && curl --max-time 15 --retry 1 -fsS \
      -o /dev/null "https://${JOBHUB_DOMAIN}/"; then
    ok "Public HTTPS site is reachable."
  else
    fail "Public HTTPS site is not reachable."
  fi
fi

if [[ -n "${APP_CONTAINER}" ]]; then
  MYSQL_URL="$(container_env "${APP_CONTAINER}" MYSQL_URL)"
  MYSQL_USERNAME="$(container_env "${APP_CONTAINER}" MYSQL_USERNAME)"
  MYSQL_PASSWORD="$(container_env "${APP_CONTAINER}" MYSQL_PASSWORD)"
  JDBC_TARGET="${MYSQL_URL#jdbc:mysql://}"
  AUTHORITY="${JDBC_TARGET%%/*}"
  MYSQL_HOST="${AUTHORITY%%:*}"
  MYSQL_PORT="${AUTHORITY##*:}"
  if [[ "${AUTHORITY}" != *:* ]]; then MYSQL_PORT="3306"; fi

  if [[ -n "${MYSQL_HOST}" && -n "${MYSQL_USERNAME}" && -n "${MYSQL_PASSWORD}" ]] && \
      docker run --rm \
        -v /etc/ssl/certs/ca-certificates.crt:/tmp/ca-certificates.crt:ro \
        -e MYSQL_PWD="${MYSQL_PASSWORD}" mysql:8.4 mysql \
        -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u "${MYSQL_USERNAME}" \
        --ssl-mode=VERIFY_IDENTITY --ssl-ca=/tmp/ca-certificates.crt \
        --connect-timeout=10 -Nse 'SELECT 1' >/dev/null 2>&1; then
    ok "TiDB connection succeeded."
  else
    fail "TiDB connection failed."
  fi
fi

AVAILABLE_MEMORY_MB="$(free -m | awk '/^Mem:/ {print $7}')"
if [[ "${AVAILABLE_MEMORY_MB}" =~ ^[0-9]+$ && "${AVAILABLE_MEMORY_MB}" -ge "${MIN_AVAILABLE_MEMORY_MB}" ]]; then
  ok "Available memory: ${AVAILABLE_MEMORY_MB} MB."
else
  warn "Available memory is low: ${AVAILABLE_MEMORY_MB:-unknown} MB."
fi

DISK_USAGE="$(df -P "${PROJECT_DIR}" | awk 'NR==2 {gsub(/%/, "", $5); print $5}')"
if [[ "${DISK_USAGE}" =~ ^[0-9]+$ && "${DISK_USAGE}" -lt "${MAX_DISK_USAGE_PERCENT}" ]]; then
  ok "Disk usage: ${DISK_USAGE}%."
else
  warn "Disk usage is high: ${DISK_USAGE:-unknown}%."
fi

LATEST_BACKUP="$(find "${BACKUP_DIR}" -maxdepth 1 -type f -name 'public-job-hub-tidb-*.sql.gz' \
  -printf '%T@ %p\n' 2>/dev/null | sort -nr | head -n 1 | cut -d' ' -f2-)"
if [[ -z "${LATEST_BACKUP}" ]]; then
  fail "No TiDB backup file was found."
else
  BACKUP_AGE_HOURS=$(( ($(date +%s) - $(stat -c %Y "${LATEST_BACKUP}")) / 3600 ))
  if [[ "${BACKUP_AGE_HOURS}" -le "${MAX_BACKUP_AGE_HOURS}" ]]; then
    ok "Latest backup age: ${BACKUP_AGE_HOURS} hour(s)."
  else
    fail "Latest backup is too old: ${BACKUP_AGE_HOURS} hour(s)."
  fi
fi

if [[ "${ISSUES}" -eq 0 ]]; then
  echo "RESULT: HEALTHY"
  exit 0
fi

echo "RESULT: ATTENTION REQUIRED (${ISSUES} issue(s))"
exit 1
