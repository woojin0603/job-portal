const sentErrors = new Map()

/** 동일 오류 폭주를 막으면서 서버 운영 화면에 제한된 브라우저 진단 정보를 보낸다. */
export function reportClientError(error, info = {}, scope = 'UI') {
  const message = String(error?.message || error || '알 수 없는 화면 오류').slice(0, 500)
  const key = `${scope}:${message}:${window.location.pathname}`
  const now = Date.now()
  if (now - (sentErrors.get(key) || 0) < 60000) return
  sentErrors.set(key, now)
  if (sentErrors.size > 50) {
    for (const [storedKey, time] of sentErrors) {
      if (now - time > 300000) sentErrors.delete(storedKey)
    }
  }
  fetch('/api/client-errors', {
    method: 'POST',
    credentials: 'same-origin',
    keepalive: true,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      scope: String(scope).slice(0, 30),
      message,
      // Query strings may contain one-time authentication tokens. Never send them
      // to the diagnostic endpoint.
      path: window.location.pathname.slice(0, 500),
      userAgent: navigator.userAgent.slice(0, 500),
      stack: String(error?.stack || '').slice(0, 8000),
      componentStack: String(info?.componentStack || '').slice(0, 8000),
    }),
  }).catch(() => {})
}
