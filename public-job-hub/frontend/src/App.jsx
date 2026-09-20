import { useCallback, useEffect, useState } from 'react'

/** 모든 API 요청에서 JSON 응답을 읽고 HTTP 오류를 화면에서 처리할 예외로 변환한다. */
async function getJson(url, options = {}) {
  const response = await fetch(url, { credentials: 'same-origin', ...options })
  const data = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(data.error || data.detail || `요청 실패 (${response.status})`)
  return data
}

/** 공고 한 건의 정보와 개인별 스크랩·지원 완료 상태를 카드로 보여준다. */
function JobCard({ job, busy, onPreview, onScrap, onApplied }) {
  return (
    <article className={`card ${job.applied ? 'applied' : job.scrapped ? 'saved' : ''}`}>
      <div className="card-top">
        <span className="tag">채용공고</span>
        <span className="source">{job.source}</span>
      </div>
      <button className="title-button" onClick={() => onPreview(job.id)}>
        <h2>{job.title}</h2>
      </button>
      <div className="details">
        <span>채용기관</span>
        <strong>{job.organization}</strong>
        <div className="pills">
          {job.region && <span>{job.region}</span>}
          {job.employmentType && <span>{job.employmentType}</span>}
        </div>
      </div>
      <div className="date">
        <span aria-hidden="true">▦</span>
        <div>
          <small>접수 기간</small>
          <strong>
            {job.postedAt || '미확인'} ~ {job.deadline || '미확인'}
          </strong>
        </div>
      </div>
      <div className="actions">
        <button className="preview-link" onClick={() => onPreview(job.id)}>
          미니탭에서 보기 ↗
        </button>
        <button disabled={busy} onClick={() => onScrap(job.id)}>
          {job.scrapped ? '스크랩 해제' : '＋ 스크랩'}
        </button>
      </div>
      {job.scrapped && (
        <button className="applied-button" disabled={busy} onClick={() => onApplied(job.id)}>
          {job.applied ? '✓ 지원 완료 · 취소' : '지원 완료로 표시'}
        </button>
      )}
    </article>
  )
}

/** 로그인·회원가입을 같은 입력 패널에서 처리하는 대화상자. */
function AuthDialog({ mode, onClose, onSubmit, busy, error }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  return (
    <div
      className="overlay"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <section
        className="auth-dialog"
        role="dialog"
        aria-modal="true"
        aria-label={mode === 'register' ? '회원가입' : '로그인'}
      >
        <button className="close" onClick={onClose} aria-label="닫기">
          ×
        </button>
        <p className="eyebrow">JOB HUB KOREA</p>
        <h2>{mode === 'register' ? '회원가입' : '로그인'}</h2>
        <form
          onSubmit={(e) => {
            e.preventDefault()
            onSubmit({ email, password, displayName })
          }}
        >
          {mode === 'register' && (
            <label>
              이름
              <input
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                required
                maxLength={80}
              />
            </label>
          )}
          <label>
            이메일
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoComplete="email"
            />
          </label>
          <label>
            비밀번호
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={8}
              autoComplete={mode === 'register' ? 'new-password' : 'current-password'}
            />
          </label>
          {error && (
            <p className="error" role="alert">
              {error}
            </p>
          )}
          <button className="primary" disabled={busy}>
            {busy ? '처리 중...' : mode === 'register' ? '가입하기' : '로그인'}
          </button>
        </form>
      </section>
    </div>
  )
}

/** 원문의 표·이미지·스타일을 스크립트 실행이 차단된 프레임에 표시한다. */
function PreviewPanel({ job, preview, loading, error, onClose }) {
  return (
    <div
      className="preview-backdrop"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <aside
        className="preview-panel"
        role="dialog"
        aria-modal="true"
        aria-label="공고 원문 미리보기"
      >
        <div className="preview-head">
          <span>공고 미니탭</span>
          <button onClick={onClose} aria-label="닫기">
            ×
          </button>
        </div>
        <div className="preview-body">
          <p className="eyebrow">{job.source}</p>
          <h2>{job.title}</h2>
          <p className="preview-org">{job.organization}</p>
          {loading && <p>원문을 불러오는 중...</p>}
          {error && (
            <p className="error" role="alert">
              {error}
            </p>
          )}
          {preview?.html && (
            <iframe
              className="preview-frame"
              title={`${job.title} 원문`}
              srcDoc={preview.html}
              sandbox="allow-popups"
              referrerPolicy="no-referrer"
            />
          )}
        </div>
      </aside>
    </div>
  )
}

/** 검색·페이지 이동·인증·개인 상태와 원문 미리보기를 조합하는 메인 화면. */
export default function App() {
  // 현재 회원과 CSRF 토큰은 세션 변경 요청에 공통으로 사용한다.
  const [user, setUser] = useState(null)
  const [csrf, setCsrf] = useState(null)
  const [authMode, setAuthMode] = useState(null)
  const [authError, setAuthError] = useState('')
  const [authBusy, setAuthBusy] = useState(false)
  // 공고 검색, 마이페이지 필터, 페이지 번호 및 API 결과를 보관한다.
  const [mine, setMine] = useState(false)
  const [queryInput, setQueryInput] = useState('')
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const [data, setData] = useState({ items: [], total: 0, hasNext: false, crawlStatus: null })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)
  // 우측 미니탭의 선택 공고와 원문 조회 상태를 별도로 관리한다.
  const [previewJob, setPreviewJob] = useState(null)
  const [preview, setPreview] = useState(null)
  const [previewError, setPreviewError] = useState('')
  const [previewLoading, setPreviewLoading] = useState(false)
  // 설치 가능 여부와 네트워크 상태는 PWA 경험을 안내하는 데만 사용한다.
  const [installPrompt, setInstallPrompt] = useState(null)
  const [online, setOnline] = useState(navigator.onLine)

  /** 브라우저의 설치 제안을 보관하고 온라인·오프라인 전환을 화면에 반영한다. */
  useEffect(() => {
    const onInstall = (event) => {
      event.preventDefault()
      setInstallPrompt(event)
    }
    const onInstalled = () => setInstallPrompt(null)
    const onOnline = () => setOnline(true)
    const onOffline = () => setOnline(false)
    window.addEventListener('beforeinstallprompt', onInstall)
    window.addEventListener('appinstalled', onInstalled)
    window.addEventListener('online', onOnline)
    window.addEventListener('offline', onOffline)
    return () => {
      window.removeEventListener('beforeinstallprompt', onInstall)
      window.removeEventListener('appinstalled', onInstalled)
      window.removeEventListener('online', onOnline)
      window.removeEventListener('offline', onOffline)
    }
  }, [])

  /** 설치를 지원하는 브라우저에서는 저장한 설치 대화상자를 연다. */
  async function installApp() {
    if (!installPrompt) return
    await installPrompt.prompt()
    setInstallPrompt(null)
  }

  /** 새로고침·로그인·로그아웃 뒤 서버 세션과 CSRF 토큰을 다시 읽는다. */
  const refreshAuth = useCallback(async () => {
    const [session, token] = await Promise.all([getJson('/api/auth/me'), getJson('/api/auth/csrf')])
    setUser(session.user)
    setCsrf(token)
  }, [])
  useEffect(() => {
    refreshAuth().catch(() => setError('서버에 연결할 수 없습니다.'))
  }, [refreshAuth])

  /** 현재 검색 조건에 해당하는 20개 공고를 로드하고 요청 오류를 표시한다. */
  const load = useCallback(
    async (signal) => {
      try {
        const params = new URLSearchParams({ q: query, page: String(page), mine: String(mine) })
        setData(await getJson(`/api/postings?${params}`, { signal }))
        setError('')
      } catch (e) {
        if (e.name !== 'AbortError')
          setError('공고를 불러오지 못했습니다. Spring Boot 서버를 확인해 주세요.')
      } finally {
        setLoading(false)
      }
    },
    [query, page, mine],
  )
  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    load(controller.signal)
    return () => controller.abort()
  }, [load])

  /** 모든 변경 요청에 서버 세션의 CSRF 토큰을 헤더로 포함한다. */
  async function securePost(url, body, contentType) {
    const token = csrf || (await getJson('/api/auth/csrf'))
    return getJson(url, {
      method: 'POST',
      headers: {
        [token.headerName]: token.token,
        ...(contentType ? { 'Content-Type': contentType } : {}),
      },
      body,
    })
  }
  /** 회원가입 후 자동 로그인하거나 기존 계정으로 로그인한다. */
  async function submitAuth(form) {
    setAuthBusy(true)
    setAuthError('')
    try {
      if (authMode === 'register')
        await securePost('/api/auth/register', JSON.stringify(form), 'application/json')
      await securePost(
        '/api/auth/login',
        new URLSearchParams({ username: form.email, password: form.password }),
        'application/x-www-form-urlencoded',
      )
      await refreshAuth()
      setAuthMode(null)
      await load()
    } catch (e) {
      setAuthError(e.message)
    } finally {
      setAuthBusy(false)
    }
  }
  /** 서버 세션을 종료하고 공고 목록 화면으로 돌아간다. */
  async function logout() {
    try {
      await securePost('/api/auth/logout')
      await refreshAuth()
      setMine(false)
      setPage(0)
      await load()
    } catch (e) {
      setError(e.message)
    }
  }
  /** 스크랩·지원 완료 상태를 토글한 뒤 최신 목록을 다시 읽는다. */
  async function mutate(id, action) {
    if (!user) {
      setAuthMode('login')
      return
    }
    setBusyId(id)
    try {
      await securePost(`/api/postings/${id}/${action}`)
      await load()
    } catch (e) {
      setError(e.message)
    } finally {
      setBusyId(null)
    }
  }
  /** 선택 공고의 상세 텍스트를 서버에서 가져와 미니탭을 연다. */
  async function openPreview(job) {
    setPreviewJob(job)
    setPreview(null)
    setPreviewError('')
    setPreviewLoading(true)
    try {
      setPreview(await getJson(`/api/postings/${job.id}/preview`))
    } catch (e) {
      setPreviewError('원문을 미니탭에서 불러오지 못했습니다.')
    } finally {
      setPreviewLoading(false)
    }
  }
  /** 마이페이지는 로그인한 회원에게만 열고 필터 변경 시 첫 페이지로 이동한다. */
  function navigate(nextMine) {
    if (nextMine && !user) {
      setAuthMode('login')
      return
    }
    setMine(nextMine)
    setPage(0)
  }
  /** 검색어를 확정하고 결과의 첫 페이지를 다시 조회한다. */
  function search(e) {
    e.preventDefault()
    setQuery(queryInput.trim())
    setPage(0)
  }
  const status = data.crawlStatus

  return (
    <>
      <header>
        <div className="bar">
          <button className="brand" onClick={() => navigate(false)}>
            JOB HUB KOREA
          </button>
          <span>한눈에 확인하는 공공기관 채용공고</span>
          <nav>
            {installPrompt && <button onClick={installApp}>앱 설치</button>}
            <button className={!mine ? 'active' : ''} onClick={() => navigate(false)}>
              채용정보
            </button>
            <button className={mine ? 'active' : ''} onClick={() => navigate(true)}>
              마이페이지
            </button>
            {user ? (
              <>
                <span className="user-name">{user.displayName}님</span>
                <button onClick={logout}>로그아웃</button>
              </>
            ) : (
              <>
                <button onClick={() => setAuthMode('login')}>로그인</button>
                <button onClick={() => setAuthMode('register')}>회원가입</button>
              </>
            )}
          </nav>
        </div>
      </header>
      <main>
        <div className="heading">
          <div>
            <p className="eyebrow">PUBLIC CAREERS · LIVE BOARD</p>
            <h1>{mine ? '스크랩한 채용공고' : '최신 채용공고 현황'}</h1>
            <p className="sub">20개씩 · 5열 × 4행 · 매일 자정 수집</p>
          </div>
          <form className="search" onSubmit={search}>
            <input
              value={queryInput}
              onChange={(e) => setQueryInput(e.target.value)}
              placeholder="직무, 기관 검색..."
              aria-label="공고 검색"
            />
            <button>검색</button>
          </form>
        </div>
        <div className="meta">
          <span>검색 결과 {data.total}건</span>
          <span>파란 외곽선: 스크랩 · 초록 외곽선: 지원 완료</span>
        </div>
        {!online && (
          <p className="offline-notice" role="status">
            오프라인 상태입니다. 앱 화면은 열리지만 공고 조회·로그인·스크랩에는 인터넷 연결이
            필요합니다.
          </p>
        )}
        {status && (
          <p className="status">
            {status.lastSuccess
              ? `마지막 수집: ${new Date(status.lastSuccess).toLocaleString('ko-KR')} · 처리 ${status.lastCount}건`
              : '첫 자정 수집을 기다리는 중입니다.'}
            {status.error && <span className="status-error"> · 수집 오류: {status.error}</span>}
          </p>
        )}
        {error && (
          <p className="error" role="alert">
            {error}
          </p>
        )}
        {loading ? (
          <p className="empty">불러오는 중...</p>
        ) : (
          <section className="grid" aria-label="채용공고 목록">
            {data.items.map((job) => (
              <JobCard
                key={job.id}
                job={job}
                busy={busyId === job.id}
                onPreview={() => openPreview(job)}
                onScrap={(id) => mutate(id, 'scrap')}
                onApplied={(id) => mutate(id, 'applied')}
              />
            ))}
          </section>
        )}
        {!loading && data.items.length === 0 && <p className="empty">표시할 공고가 없습니다.</p>}
        <div className="pagination">
          <button disabled={page === 0} onClick={() => setPage(page - 1)}>
            ← 이전
          </button>
          <span>{page + 1} 페이지</span>
          <button disabled={!data.hasNext} onClick={() => setPage(page + 1)}>
            다음 →
          </button>
        </div>
      </main>
      {authMode && (
        <AuthDialog
          key={authMode}
          mode={authMode}
          onClose={() => {
            setAuthMode(null)
            setAuthError('')
          }}
          onSubmit={submitAuth}
          busy={authBusy}
          error={authError}
        />
      )}
      {previewJob && (
        <PreviewPanel
          job={previewJob}
          preview={preview}
          loading={previewLoading}
          error={previewError}
          onClose={() => setPreviewJob(null)}
        />
      )}
    </>
  )
}
