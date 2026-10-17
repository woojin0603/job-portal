#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${PROJECT_DIR}/compose-tidb.yml"
ENV_FILE="${PROJECT_DIR}/.env"
BACKUP_DIR="${PROJECT_DIR}/backups/tidb"
RESTORE_DATABASE="public_job_hub_restore_test"

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
SOURCE_DATABASE="${DATABASE_AND_QUERY%%\?*}"
if [[ "${AUTHORITY}" == *:* ]]; then
  MYSQL_HOST="${AUTHORITY%%:*}"
  MYSQL_PORT="${AUTHORITY##*:}"
else
  MYSQL_HOST="${AUTHORITY}"
  MYSQL_PORT="3306"
fi

if [[ ! "${SOURCE_DATABASE}" =~ ^[A-Za-z0-9_]+$ ]]; then
  echo "ERROR: The source database name in MYSQL_URL is invalid." >&2
  exit 1
fi

LATEST_BACKUP="$(find "${BACKUP_DIR}" -maxdepth 1 -type f \
  -name 'public-job-hub-tidb-*.sql.gz' -printf '%T@ %p\n' \
  | sort -nr | head -n 1 | cut -d' ' -f2-)"

if [[ -z "${LATEST_BACKUP}" || ! -s "${LATEST_BACKUP}" ]]; then
  echo "ERROR: No usable TiDB backup was found in ${BACKUP_DIR}." >&2
  exit 1
fi

if ! gzip -t -- "${LATEST_BACKUP}"; then
  echo "ERROR: The latest backup is not a valid gzip file." >&2
  exit 1
fi

mysql_client() {
  docker run --rm -i \
    -v /etc/ssl/certs/ca-certificates.crt:/tmp/ca-certificates.crt:ro \
    -e MYSQL_PWD="${MYSQL_PASSWORD}" \
    mysql:8.4 \
    mysql \
    -h "${MYSQL_HOST}" \
    -P "${MYSQL_PORT}" \
    -u "${MYSQL_USERNAME}" \
    --ssl-mode=VERIFY_IDENTITY \
    --ssl-ca=/tmp/ca-certificates.crt \
    --batch --skip-column-names \
    "$@"
}

cleanup() {
  mysql_client -e "DROP DATABASE IF EXISTS \`${RESTORE_DATABASE}\`;" >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "Verifying backup: ${LATEST_BACKUP}"
mysql_client -e "DROP DATABASE IF EXISTS \`${RESTORE_DATABASE}\`; CREATE DATABASE \`${RESTORE_DATABASE}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;"

gzip -dc -- "${LATEST_BACKUP}" | mysql_client "${RESTORE_DATABASE}"

TABLE_COUNT="$(mysql_client -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${RESTORE_DATABASE}';")"
BASE_TABLE_COUNT="$(mysql_client -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${RESTORE_DATABASE}' AND table_type='BASE TABLE';")"
SOURCE_TABLE_COUNT="$(mysql_client -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${SOURCE_DATABASE}';")"

if [[ ! "${TABLE_COUNT}" =~ ^[0-9]+$ || "${TABLE_COUNT}" -eq 0 ]]; then
  echo "ERROR: Restore completed without any tables." >&2
  exit 1
fi

if [[ "${TABLE_COUNT}" != "${SOURCE_TABLE_COUNT}" ]]; then
  echo "ERROR: Restored object count (${TABLE_COUNT}) does not match the source (${SOURCE_TABLE_COUNT})." >&2
  exit 1
fi

echo "TiDB restore verification completed successfully."
echo "Restored objects: ${TABLE_COUNT} (base tables: ${BASE_TABLE_COUNT})"
echo "The temporary database ${RESTORE_DATABASE} will now be removed."
