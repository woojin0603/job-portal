import { getApp, getApps, initializeApp } from '@firebase/app'
import { deleteToken, getMessaging, getToken, isSupported, onMessage } from '@firebase/messaging'

let messagingInstance = null
let foregroundListenerRegistered = false

async function publicConfig() {
  const response = await fetch('/api/push/config', { credentials: 'same-origin' })
  if (!response.ok) throw new Error('푸시 알림 설정을 불러오지 못했습니다.')
  return response.json()
}

function workerUrl(config) {
  const query = new URLSearchParams({
    apiKey: config.apiKey,
    authDomain: config.authDomain || '',
    projectId: config.projectId,
    storageBucket: config.storageBucket || '',
    messagingSenderId: config.messagingSenderId,
    appId: config.appId,
  })
  return `/firebase-messaging-sw.js?${query}`
}

async function messaging(config) {
  if (messagingInstance) return messagingInstance
  const app = getApps().length
    ? getApp()
    : initializeApp({
        apiKey: config.apiKey,
        authDomain: config.authDomain,
        projectId: config.projectId,
        storageBucket: config.storageBucket,
        messagingSenderId: config.messagingSenderId,
        appId: config.appId,
      })
  messagingInstance = getMessaging(app)
  return messagingInstance
}

export async function registerPushNotifications(securePost, requestPermission = true) {
  if (!(await isSupported()) || !('serviceWorker' in navigator) || !('Notification' in window)) {
    throw new Error('이 브라우저에서는 푸시 알림을 지원하지 않습니다.')
  }
  const config = await publicConfig()
  if (!config.enabled) throw new Error('푸시 알림 서버 설정이 아직 완료되지 않았습니다.')
  let permission = Notification.permission
  if (permission === 'default' && requestPermission) permission = await Notification.requestPermission()
  if (permission !== 'granted') throw new Error('브라우저 알림 권한이 필요합니다.')

  const registration = await navigator.serviceWorker.register(workerUrl(config), {
    scope: '/firebase-cloud-messaging-push-scope/',
  })
  const instance = await messaging(config)
  const token = await getToken(instance, {
    vapidKey: config.vapidKey,
    serviceWorkerRegistration: registration,
  })
  if (!token) throw new Error('푸시 알림 기기 토큰을 발급하지 못했습니다.')
  await securePost(
    '/api/tools/push-subscriptions',
    JSON.stringify({ deviceToken: token, platform: 'WEB' }),
    'application/json',
  )
  window.localStorage.setItem('jobhub-fcm-token', token)
  if (!foregroundListenerRegistered) {
    onMessage(instance, async (payload) => {
      const data = payload.data || {}
      if (Notification.permission === 'granted') {
        // Android 설치형 PWA는 Notification 생성자를 지원하지 않을 수 있으므로
        // 전면 실행 중에도 서비스워커를 통해 시스템 알림을 표시한다.
        try {
          await registration.showNotification(data.title || 'JOB HUB KOREA', {
            body: data.body || '새로운 채용 알림이 있습니다.',
            icon: '/icons/icon-192.png',
            badge: '/icons/icon-192.png',
            tag: `${data.category || 'jobhub'}-${data.link || '/'}`,
            data: { link: data.link || '/' },
          })
        } catch (notificationError) {
          console.warn('시스템 푸시 알림을 표시하지 못했습니다.', notificationError)
        }
      }
      window.dispatchEvent(new CustomEvent('jobhub:push-received', { detail: data }))
    })
    foregroundListenerRegistered = true
  }
  return token
}

export async function unregisterPushNotifications(securePost) {
  const token = window.localStorage.getItem('jobhub-fcm-token')
  if (!token) return
  try {
    await securePost(
      '/api/tools/push-subscriptions/remove',
      JSON.stringify({ deviceToken: token, platform: 'WEB' }),
      'application/json',
    )
    if (messagingInstance) await deleteToken(messagingInstance)
  } finally {
    window.localStorage.removeItem('jobhub-fcm-token')
  }
}
