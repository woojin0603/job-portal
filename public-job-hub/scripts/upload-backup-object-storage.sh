#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${PROJECT_DIR}/.env"
BACKUP_DIR="${PROJECT_DIR}/backups/tidb"

read_env_value() {
  local key="$1"
  local value
  value="$(sed -n -E "s/^${key}=(.*)$/\\1/p" "${ENV_FILE}" 2>/dev/null | tail -n 1)"
  value="${value%$'\r'}"
  if [[ "${value}" == \"*\" && "${value}" == *\" ]]; then
    value="${value:1:${#value}-2}"
  elif [[ "${value}" == \'*\' && "${value}" == *\' ]]; then
    value="${value:1:${#value}-2}"
  fi
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  printf '%s' "${value}"
}

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "ERROR: ${ENV_FILE} does not exist." >&2
  exit 1
fi
if ! command -v openssl >/dev/null 2>&1; then
  echo "ERROR: openssl is required to encrypt the backup." >&2
  exit 1
fi

UPLOAD_URL="$(read_env_value JOBHUB_BACKUP_OBJECT_STORAGE_URL)"
ENCRYPTION_PASSWORD="$(read_env_value JOBHUB_BACKUP_ENCRYPTION_PASSWORD)"
if [[ ! "${UPLOAD_URL}" =~ ^https://objectstorage\.[a-z0-9-]+\.oraclecloud\.com/p/[^[:space:]]+/n/[^[:space:]]+/b/[^[:space:]]+/o/?$ ]]; then
  echo "ERROR: JOBHUB_BACKUP_OBJECT_STORAGE_URL is not a valid bucket write PAR URL." >&2
  exit 1
fi
if [[ "${#ENCRYPTION_PASSWORD}" -lt 24 ]]; then
  echo "ERROR: JOBHUB_BACKUP_ENCRYPTION_PASSWORD must be at least 24 characters." >&2
  exit 1
fi

LATEST_BACKUP="$(find "${BACKUP_DIR}" -maxdepth 1 -type f -name 'public-job-hub-tidb-*.sql.gz' \
  -printf '%T@ %p\n' 2>/dev/null | sort -nr | head -n 1 | cut -d' ' -f2-)"
if [[ -z "${LATEST_BACKUP}" || ! -s "${LATEST_BACKUP}" ]]; then
  echo "ERROR: No usable TiDB backup was found in ${BACKUP_DIR}." >&2
  exit 1
fi
if ! gzip -t -- "${LATEST_BACKUP}"; then
  echo "ERROR: The latest TiDB backup is not a valid gzip file." >&2
  exit 1
fi

OBJECT_NAME="$(basename -- "${LATEST_BACKUP}").aes256"
TEMP_FILE="$(mktemp "${BACKUP_DIR}/.${OBJECT_NAME}.XXXXXX")"
trap 'rm -f -- "${TEMP_FILE}"; unset ENCRYPTION_PASSWORD' EXIT

export ENCRYPTION_PASSWORD
openssl enc -aes-256-cbc -salt -pbkdf2 -iter 200000 \
  -in "${LATEST_BACKUP}" -out "${TEMP_FILE}" -pass env:ENCRYPTION_PASSWORD
chmod 600 -- "${TEMP_FILE}"

CHECKSUM="$(sha256sum "${TEMP_FILE}" | awk '{print $1}')"
TARGET_URL="${UPLOAD_URL%/}/${OBJECT_NAME}"
curl --fail-with-body --silent --show-error --max-time 120 --retry 3 \
  --request PUT \
  --header 'Content-Type: application/octet-stream' \
  --header "opc-meta-sha256:${CHECKSUM}" \
  --data-binary "@${TEMP_FILE}" \
  "${TARGET_URL}" \
  >/dev/null

SIZE="$(du -h "${TEMP_FILE}" | awk '{print $1}')"
echo "Encrypted off-site backup uploaded: ${OBJECT_NAME} (${SIZE})"
echo "SHA-256: ${CHECKSUM}"
