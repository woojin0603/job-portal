import { defineConfig } from 'vite'
import { VitePWA } from 'vite-plugin-pwa'

// 앱 화면만 오프라인용으로 사전 캐시한다. 로그인·공고 API 응답은 캐시에 저장하지 않는다.
const pwa = VitePWA({
  registerType: 'autoUpdate',
  manifestFilename: 'manifest.json',
  includeAssets: ['icons/icon-192.png', 'icons/icon-512.png', 'icons/icon-maskable-512.png'],
  manifest: {
    id: '/',
    name: 'JOB HUB KOREA - 공공기관 채용정보',
    short_name: 'JOB HUB',
    description: '공공기관 채용공고를 확인하고 스크랩하는 앱',
    lang: 'ko-KR',
    start_url: '/',
    scope: '/',
    display: 'standalone',
    display_override: ['window-controls-overlay', 'standalone'],
    orientation: 'portrait-primary',
    categories: ['business', 'productivity'],
    theme_color: '#143d71',
    background_color: '#f5f8fc',
    icons: [
      { src: '/icons/icon-192.png', sizes: '192x192', type: 'image/png' },
      { src: '/icons/icon-512.png', sizes: '512x512', type: 'image/png' },
      {
        src: '/icons/icon-maskable-512.png',
        sizes: '512x512',
        type: 'image/png',
        purpose: 'maskable',
      },
    ],
    shortcuts: [
      {
        name: '채용정보 보기',
        short_name: '채용정보',
        url: '/',
        icons: [{ src: '/icons/icon-192.png', sizes: '192x192' }],
      },
    ],
  },
  workbox: {
    globPatterns: ['**/*.{js,css,html,png,svg}'],
    navigateFallback: '/index.html',
    navigateFallbackDenylist: [/^\/api\//],
  },
})

// 로컬 React 개발 서버에서 /api 요청을 Java 서버로 전달해 세션 쿠키를 유지한다.
export default defineConfig({
  plugins: [pwa],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
