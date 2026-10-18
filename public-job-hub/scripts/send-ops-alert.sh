#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${PROJECT_DIR}/.env"

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
  printf '%s' "${value}"
}

LEVEL="${1:-INFO}"
SUBJECT="${2:-Public Job Hub 운영 알림}"
DETAIL="${3:-상세 내용이 없습니다.}"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "WARN: Alert skipped because ${ENV_FILE} does not exist." >&2
  exit 0
fi

BOT_TOKEN="$(read_env_value JOBHUB_TELEGRAM_BOT_TOKEN)"
CHAT_ID="$(read_env_value JOBHUB_TELEGRAM_CHAT_ID)"
if [[ -z "${BOT_TOKEN}" || -z "${CHAT_ID}" ]]; then
  echo "INFO: Telegram alert is not configured; notification skipped."
  exit 0
fi

HOST_NAME="$(hostname)"
NOW="$(TZ=Asia/Seoul date +'%Y-%m-%d %H:%M:%S KST')"
MESSAGE="[${LEVEL}] ${SUBJECT}
서버: ${HOST_NAME}
시각: ${NOW}

${DETAIL}"

curl --fail --silent --show-error --max-time 15 \
  --request POST "https://api.telegram.org/bot${BOT_TOKEN}/sendMessage" \
  --data-urlencode "chat_id=${CHAT_ID}" \
  --data-urlencode "text=${MESSAGE}" \
  --data-urlencode "disable_web_page_preview=true" \
  >/dev/null

echo "Telegram alert sent: ${SUBJECT}"
