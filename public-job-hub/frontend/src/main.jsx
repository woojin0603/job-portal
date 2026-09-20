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

// 새 배포가 있으면 서비스 워커가 정적 화면 파일을 자동 갱신한다.
registerSW({ immediate: true })
