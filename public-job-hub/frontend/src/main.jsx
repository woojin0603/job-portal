import React from 'react'
import { createRoot } from 'react-dom/client'
import { registerSW } from 'virtual:pwa-register'
import App from './App.jsx'
import './style.css'

// React 메인 화면을 index.html의 root 요소에 마운트한다.
// StrictMode는 개발 중 렌더링 부작용을 발견하도록 돕는다.
createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)

// 새 배포를 발견하면 화면에 갱신 안내를 띄우고 사용자가 선택한 시점에 교체한다.
const updateSW = registerSW({
  immediate: true,
  onNeedRefresh() {
    window.dispatchEvent(new CustomEvent('jobhub:update-available'))
  },
})
window.addEventListener('jobhub:apply-update', () => updateSW(true))
