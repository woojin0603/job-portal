/* Firebase Cloud Messaging 전용 서비스 워커. 공개 웹 설정은 등록 URL에서 전달된다. */
importScripts('https://www.gstatic.com/firebasejs/12.6.0/firebase-app-compat.js')
importScripts('https://www.gstatic.com/firebasejs/12.6.0/firebase-messaging-compat.js')

const values = new URL(self.location.href).searchParams
firebase.initializeApp({
  apiKey: values.get('apiKey'),
  authDomain: values.get('authDomain'),
  projectId: values.get('projectId'),
  storageBucket: values.get('storageBucket'),
  messagingSenderId: values.get('messagingSenderId'),
  appId: values.get('appId'),
})

firebase.messaging().onBackgroundMessage((payload) => {
  const data = payload.data || {}
  self.registration.showNotification(data.title || 'JOB HUB KOREA', {
    body: data.body || '새로운 채용 알림이 있습니다.',
    icon: '/icons/icon-192.png',
    badge: '/icons/icon-192.png',
    tag: `${data.category || 'jobhub'}-${data.link || '/'}`,
    data: { link: data.link || '/' },
  })
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const link = new URL(event.notification.data?.link || '/', self.location.origin).href
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((windows) => {
      const opened = windows.find((client) => client.url.startsWith(self.location.origin))
      if (opened) {
        opened.navigate(link)
        return opened.focus()
      }
      return clients.openWindow(link)
    }),
  )
})
