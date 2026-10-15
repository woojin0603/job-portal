#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${PROJECT_DIR}/compose-tidb.yml"
ENV_FILE="${PROJECT_DIR}/.env"
BACKUP_DIR="${PROJECT_DIR}/backups/tidb"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-7}"

umask 077
mkdir -p -- "${BACKUP_DIR}"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "ERROR: ${ENV_FILE} does not exist." >&2
  exit 1
fi

APP_CONTAINER="$(docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" ps -q app)"
if [[ -z "${APP_CONTAINER}" ]]; then
  echo "ERROR: The app container is not running." >&2
  exit 1
fi

read_container_env() {
  local key="$1"
  docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' "${APP_CONTAINER}" \
    | awk -F= -v key="${key}" '$1 == key {sub(/^[^=]*=/, ""); print; exit}'
}

MYSQL_URL="$(read_container_env MYSQL_URL)"
MYSQL_USERNAME="$(read_container_env MYSQL_USERNAME)"
MYSQL_PASSWORD="$(read_container_env MYSQL_PASSWORD)"

if [[ -z "${MYSQL_URL}" || -z "${MYSQL_USERNAME}" || -z "${MYSQL_PASSWORD}" ]]; then
  echo "ERROR: TiDB connection settings are missing from the app container." >&2
  exit 1
fi

JDBC_TARGET="${MYSQL_URL#jdbc:mysql://}"
AUTHORITY="${JDBC_TARGET%%/*}"
DATABASE_AND_QUERY="${JDBC_TARGET#*/}"
DATABASE="${DATABASE_AND_QUERY%%\?*}"

if [[ "${AUTHORITY}" == *:* ]]; then
  MYSQL_HOST="${AUTHORITY%%:*}"
  MYSQL_PORT="${AUTHORITY##*:}"
else
  MYSQL_HOST="${AUTHORITY}"
  MYSQL_PORT="3306"
fi

if [[ -z "${MYSQL_HOST}" || -z "${MYSQL_PORT}" || -z "${DATABASE}" ]]; then
  echo "ERROR: MYSQL_URL could not be parsed." >&2
  exit 1
fi

TIMESTAMP="$(TZ=Asia/Seoul date +'%Y%m%d-%H%M%S')"
TARGET_FILE="${BACKUP_DIR}/public-job-hub-tidb-${TIMESTAMP}.sql.gz"
TEMP_FILE="${TARGET_FILE}.tmp"
trap 'rm -f -- "${TEMP_FILE}"' EXIT

docker run --rm \
  -v /etc/ssl/certs/ca-certificates.crt:/tmp/ca-certificates.crt:ro \
  -e MYSQL_PWD="${MYSQL_PASSWORD}" \
  mysql:8.4 \
  mysqldump \
  -h "${MYSQL_HOST}" \
  -P "${MYSQL_PORT}" \
  -u "${MYSQL_USERNAME}" \
  --ssl-mode=VERIFY_IDENTITY \
  --ssl-ca=/tmp/ca-certificates.crt \
  --skip-lock-tables \
  --no-tablespaces \
  --set-gtid-purged=OFF \
  --default-character-set=utf8mb4 \
  "${DATABASE}" \
  | gzip -9 > "${TEMP_FILE}"

if [[ ! -s "${TEMP_FILE}" ]]; then
  echo "ERROR: The backup file is empty." >&2
  exit 1
fi

mv -- "${TEMP_FILE}" "${TARGET_FILE}"
chmod 600 -- "${TARGET_FILE}"

find "${BACKUP_DIR}" -maxdepth 1 -type f \
  -name 'public-job-hub-tidb-*.sql.gz' -mtime "+${RETENTION_DAYS}" -delete

BACKUP_SIZE="$(du -h "${TARGET_FILE}" | awk '{print $1}')"
echo "TiDB backup completed: ${TARGET_FILE} (${BACKUP_SIZE})"
echo "Retention: ${RETENTION_DAYS} days"
