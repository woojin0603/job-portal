#!/usr/bin/env bash
set -Eeuo pipefail
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${PROJECT_DIR}/compose-tidb.yml"
ENV_FILE="${PROJECT_DIR}/.env"
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
JDBC_TARGET="${MYSQL_URL#jdbc:mysql://}"
AUTHORITY="${JDBC_TARGET%%/*}"
DATABASE_AND_QUERY="${JDBC_TARGET#*/}"
DATABASE="${DATABASE_AND_QUERY%%\?*}"
MYSQL_HOST="${AUTHORITY%%:*}"
MYSQL_PORT="${AUTHORITY##*:}"
docker run --rm \
  -v /etc/ssl/certs/ca-certificates.crt:/tmp/ca-certificates.crt:ro \
  -e MYSQL_PWD="${MYSQL_PASSWORD}" mysql:8.4 mysql \
  -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u "${MYSQL_USERNAME}" \
  --ssl-mode=VERIFY_IDENTITY --ssl-ca=/tmp/ca-certificates.crt \
  "${DATABASE}" -e "ALTER TABLE posting_changes MODIFY COLUMN old_value LONGTEXT NULL, MODIFY COLUMN new_value LONGTEXT NULL;"
echo "posting_changes old_value/new_value columns changed to LONGTEXT."