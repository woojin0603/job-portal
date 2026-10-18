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

WEBHOOK_URL="$(read_env_value JOBHUB_DISCORD_WEBHOOK_URL)"
if [[ -z "${WEBHOOK_URL}" ]]; then
  echo "INFO: Discord alert is not configured; notification skipped."
  exit 0
fi

HOST_NAME="$(hostname)"
NOW="$(TZ=Asia/Seoul date +'%Y-%m-%d %H:%M:%S KST')"
MESSAGE="[${LEVEL}] ${SUBJECT}
서버: ${HOST_NAME}
시각: ${NOW}

${DETAIL}"

escape_json() {
  awk 'BEGIN { ORS=""; first=1 }
    {
      gsub(/\\/, "\\\\");
      gsub(/\"/, "\\\"");
      gsub(/\r/, "");
      if (!first) printf "\\n";
      printf "%s", $0;
      first=0;
    }'
}

# Discord content 제한과 운영 로그의 민감 정보 노출을 줄이기 위해 메시지를 1,800자로 제한한다.
MESSAGE="${MESSAGE:0:1800}"
ESCAPED_MESSAGE="$(printf '%s' "${MESSAGE}" | escape_json)"
PAYLOAD="{\"content\":\"${ESCAPED_MESSAGE}\",\"allowed_mentions\":{\"parse\":[]}}"

curl --fail --silent --show-error --max-time 15 \
  --header 'Content-Type: application/json' \
  --request POST \
  --data "${PAYLOAD}" \
  "${WEBHOOK_URL}" \
  >/dev/null

echo "Discord alert sent: ${SUBJECT}"
