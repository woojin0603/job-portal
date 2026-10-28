import React, { Component } from 'react'
import { createRoot } from 'react-dom/client'
import { registerSW } from 'virtual:pwa-register'
import App from './App.jsx'
import './style.css'
import './card-redesign.css'

class RootErrorBoundary extends Component {
  constructor(props) { super(props); this.state = { error: null } }
  static getDerivedStateFromError(error) { return { error } }
  componentDidCatch(error, info) {
    console.error('화면 렌더링 오류', error, info)
    window.sessionStorage.setItem('jobhub-last-ui-error', String(error?.message || error))
  }
  render() {
    if (!this.state.error) return this.props.children
    return (
      <main style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', padding: 24, background: '#f5f7f5', fontFamily: 'system-ui, sans-serif' }}>
        <section style={{ width: 'min(560px, 100%)', padding: 28, border: '1px solid #d9e1dc', borderRadius: 16, background: '#fff', boxShadow: '0 14px 44px rgba(25,45,35,.1)' }}>
          <small style={{ color: '#a44b3e', fontWeight: 800 }}>화면 복구</small>
          <h1 style={{ margin: '10px 0', color: '#193b2d', fontSize: 24 }}>화면을 불러오는 중 문제가 발생했습니다.</h1>
          <p style={{ color: '#65736c', lineHeight: 1.7 }}>저장된 서버 데이터에는 영향이 없습니다. 화면을 새로 불러오면 대부분 정상적으로 복구됩니다.</p>
          <button type="button" onClick={() => window.location.reload()} style={{ minHeight: 44, marginTop: 12, padding: '0 18px', border: 0, borderRadius: 9, background: '#285b46', color: '#fff', fontWeight: 750, cursor: 'pointer' }}>화면 새로 불러오기</button>
        </section>
      </main>
    )
  }
}

// React 메인 화면을 index.html의 root 요소에 마운트한다.
// StrictMode는 개발 중 렌더링 부작용을 발견하도록 돕는다.
createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <RootErrorBoundary><App /></RootErrorBoundary>
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
