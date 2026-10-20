import { useCallback, useEffect, useState } from 'react'

/** 모든 API 요청에서 JSON 응답을 읽고 HTTP 오류를 화면에서 처리할 예외로 변환한다. */
async function getJson(url, options = {}) {
  const response = await fetch(url, { credentials: 'same-origin', ...options })
  const data = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(data.error || data.detail || `요청 실패 (${response.status})`)
  return data
}

const REGION_LEVELS = [
  { code: '서울', label: '서울특별시' },
  { code: '부산', label: '부산광역시' },
  { code: '대구', label: '대구광역시' },
  { code: '인천', label: '인천광역시' },
  { code: '광주', label: '광주광역시' },
  { code: '대전', label: '대전광역시' },
  { code: '울산', label: '울산광역시' },
  { code: '세종', label: '세종특별자치시' },
  { code: '경기', label: '경기도' },
  { code: '강원', label: '강원특별자치도' },
  { code: '충북', label: '충청북도' },
  { code: '충남', label: '충청남도' },
  { code: '전북', label: '전북특별자치도' },
  { code: '전남', label: '전라남도' },
  { code: '경북', label: '경상북도' },
  { code: '경남', label: '경상남도' },
  { code: '제주', label: '제주특별자치도' },
]

const REGION_DISTRICTS = {
  서울: [
    '종로구',
    '중구',
    '용산구',
    '성동구',
    '광진구',
    '동대문구',
    '중랑구',
    '성북구',
    '강북구',
    '도봉구',
    '노원구',
    '은평구',
    '서대문구',
    '마포구',
    '양천구',
    '강서구',
    '구로구',
    '금천구',
    '영등포구',
    '동작구',
    '관악구',
    '서초구',
    '강남구',
    '송파구',
    '강동구',
  ],
  부산: [
    '중구',
    '서구',
    '동구',
    '영도구',
    '부산진구',
    '동래구',
    '남구',
    '북구',
    '해운대구',
    '사하구',
    '금정구',
    '강서구',
    '연제구',
    '수영구',
    '사상구',
    '기장군',
  ],
  대구: ['중구', '동구', '서구', '남구', '북구', '수성구', '달서구', '달성군', '군위군'],
  인천: [
    '중구',
    '동구',
    '미추홀구',
    '연수구',
    '남동구',
    '부평구',
    '계양구',
    '서구',
    '강화군',
    '옹진군',
  ],
  광주: ['동구', '서구', '남구', '북구', '광산구'],
  대전: ['동구', '중구', '서구', '유성구', '대덕구'],
  울산: ['중구', '남구', '동구', '북구', '울주군'],
  세종: ['세종시'],
  경기: [
    '수원시',
    '고양시',
    '용인시',
    '성남시',
    '부천시',
    '화성시',
    '안산시',
    '남양주시',
    '안양시',
    '평택시',
    '시흥시',
    '파주시',
    '의정부시',
    '김포시',
    '광주시',
    '광명시',
    '군포시',
    '하남시',
    '오산시',
    '양주시',
    '이천시',
    '구리시',
    '안성시',
    '포천시',
    '의왕시',
    '여주시',
    '동두천시',
    '과천시',
    '양평군',
    '가평군',
    '연천군',
  ],
  강원: [
    '춘천시',
    '원주시',
    '강릉시',
    '동해시',
    '태백시',
    '속초시',
    '삼척시',
    '홍천군',
    '횡성군',
    '영월군',
    '평창군',
    '정선군',
    '철원군',
    '화천군',
    '양구군',
    '인제군',
    '고성군',
    '양양군',
  ],
  충북: [
    '청주시',
    '충주시',
    '제천시',
    '보은군',
    '옥천군',
    '영동군',
    '증평군',
    '진천군',
    '괴산군',
    '음성군',
    '단양군',
  ],
  충남: [
    '천안시',
    '공주시',
    '보령시',
    '아산시',
    '서산시',
    '논산시',
    '계룡시',
    '당진시',
    '금산군',
    '부여군',
    '서천군',
    '청양군',
    '홍성군',
    '예산군',
    '태안군',
  ],
  전북: [
    '전주시',
    '군산시',
    '익산시',
    '정읍시',
    '남원시',
    '김제시',
    '완주군',
    '진안군',
    '무주군',
    '장수군',
    '임실군',
    '순창군',
    '고창군',
    '부안군',
  ],
  전남: [
    '목포시',
    '여수시',
    '순천시',
    '나주시',
    '광양시',
    '담양군',
    '곡성군',
    '구례군',
    '고흥군',
    '보성군',
    '화순군',
    '장흥군',
    '강진군',
    '해남군',
    '영암군',
    '무안군',
    '함평군',
    '영광군',
    '장성군',
    '완도군',
    '진도군',
    '신안군',
  ],
  경북: [
    '포항시',
    '경주시',
    '김천시',
    '안동시',
    '구미시',
    '영주시',
    '영천시',
    '상주시',
    '문경시',
    '경산시',
    '의성군',
    '청송군',
    '영양군',
    '영덕군',
    '청도군',
    '고령군',
    '성주군',
    '칠곡군',
    '예천군',
    '봉화군',
    '울진군',
    '울릉군',
  ],
  경남: [
    '창원시',
    '진주시',
    '통영시',
    '사천시',
    '김해시',
    '밀양시',
    '거제시',
    '양산시',
    '의령군',
    '함안군',
    '창녕군',
    '고성군',
    '남해군',
    '하동군',
    '산청군',
    '함양군',
    '거창군',
    '합천군',
  ],
  제주: ['제주시', '서귀포시'],
}

function districtsIn(provinceCode) {
  return REGION_DISTRICTS[provinceCode] || []
}

/** 공고 한 건의 정보와 개인별 스크랩·지원 완료 상태를 카드로 보여준다. */
const STAGE_LABELS = {
  SAVED: '관심 공고',
  PREPARING: '지원 준비',
  SUBMITTED: '지원 완료',
  DOCUMENT_PASSED: '서류 합격',
  WRITTEN_TEST: '필기 예정',
  INTERVIEW: '면접 예정',
  PASSED: '최종 합격',
  REJECTED: '전형 종료',
}

/** API가 쉼표로 전달한 여러 항목을 카드 크기에 맞게 핵심 항목과 나머지 개수로 요약한다. */
function summarizeList(value, limit = 2) {
  if (!value) return ''
  const items = [
    ...new Set(
      String(value)
        .split(/[,，]/)
        .map((item) => item.trim())
        .filter(Boolean),
    ),
  ]
  if (items.length <= limit) return items.join(' · ')
  return `${items.slice(0, limit).join(' · ')} 외 ${items.length - limit}개`
}

function institutionLabel(job) {
  if (job.organizationType === 'PRIVATE') return '민간기업'
  if (job.publicInstitutionType === 'LOCAL_PUBLIC') return '지방공기업'
  if (job.publicInstitutionType === 'CENTRAL_PUBLIC') return '중앙 공공기관'
  return '공공기관'
}

function institutionClassification(job) {
  const scope =
    job.organizationType === 'PRIVATE'
      ? '민간'
      : job.publicInstitutionType === 'LOCAL_PUBLIC'
        ? '지방'
        : job.publicInstitutionType === 'CENTRAL_PUBLIC'
          ? '중앙'
          : '공공'
  const organization = job.organization || ''
  const form = ['공사', '공단', '재단', '병원', '연구원', '진흥원', '센터'].find((value) =>
    organization.includes(value),
  )
  return form ? `${scope} · ${form}` : scope
}

function compactPositionLabel(position, fallbackRegion) {
  const source = position.standardCategory || position.originalName || '직렬 미확인'
  const categories = [
    ...new Set(
      String(source)
        .split(/[,，.·/|>]+/)
        .map((value) => value.trim())
        .filter(Boolean),
    ),
  ]
  const category = categories.slice(0, 3).join(', ')
  const region = summarizeList(position.workRegion || fallbackRegion, 1)
  return region ? `${category} (${region})` : category
}

function sourceIconUrl(sourceUrl) {
  try {
    return `${new URL(sourceUrl).origin}/favicon.ico`
  } catch {
    return ''
  }
}

/** 현재 페이지가 속한 5개 묶음과 맨 앞·뒤 이동을 함께 제공한다. */
function Pagination({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null
  const groupStart = Math.floor(page / 5) * 5
  const pages = Array.from(
    { length: Math.min(5, totalPages - groupStart) },
    (_, index) => groupStart + index,
  )
  return (
    <div className="pagination" role="navigation" aria-label="페이지 이동">
      <button disabled={page === 0} onClick={() => onChange(0)}>
        « 맨앞
      </button>
      <button disabled={page === 0} onClick={() => onChange(page - 1)}>
        ‹ 이전
      </button>
      <div className="page-numbers">
        {pages.map((value) => (
          <button
            key={value}
            className={value === page ? 'active' : ''}
            aria-current={value === page ? 'page' : undefined}
            onClick={() => onChange(value)}
          >
            {value + 1}
          </button>
        ))}
      </div>
      <button disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
        다음 ›
      </button>
      <button disabled={page + 1 >= totalPages} onClick={() => onChange(totalPages - 1)}>
        맨뒤 »
      </button>
    </div>
  )
}

function JobCard({
  job,
  busy,
  onOpenSource,
  onSalary,
  onCompetition,
  onScrap,
  onTrack,
  onMatch,
  onShare,
  onInsight,
  onReport,
  matchBusy,
  memberActions = true,
}) {
  const institutionClass =
    job.organizationType === 'PRIVATE'
      ? 'institution-private'
      : job.publicInstitutionType === 'LOCAL_PUBLIC'
        ? 'institution-local'
        : job.publicInstitutionType === 'CENTRAL_PUBLIC'
          ? 'institution-central'
          : 'institution-public'
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const deadlineDate = job.deadline ? new Date(`${job.deadline}T00:00:00`) : null
  const daysLeft = deadlineDate
    ? Math.ceil((deadlineDate.getTime() - today.getTime()) / 86400000)
    : null
  const urgent = daysLeft !== null && daysLeft >= 0 && daysLeft <= 2
  const expired = daysLeft !== null && daysLeft < 0
  const [iconFailed, setIconFailed] = useState(false)
  const iconUrl = sourceIconUrl(job.sourceUrl)
  const institutionType = institutionLabel(job)
  const institutionClassificationText = institutionClassification(job)
  const visiblePositions = (job.positions || []).slice(0, 2)
  const hiddenPositionCount = Math.max(0, (job.positions?.length || 0) - visiblePositions.length)
  const [tracking, setTracking] = useState({
    stage: job.applicationStage || 'SAVED',
    nextStepDate: job.nextStepDate || '',
    memo: job.applicationMemo || '',
  })
  useEffect(() => {
    setTracking({
      stage: job.applicationStage || 'SAVED',
      nextStepDate: job.nextStepDate || '',
      memo: job.applicationMemo || '',
    })
  }, [job.applicationStage, job.nextStepDate, job.applicationMemo])
  return (
    <article
      className={`card ${institutionClass} ${urgent ? 'deadline-urgent' : ''} ${expired ? 'posting-expired' : ''} ${job.applied ? 'applied' : job.scrapped ? 'saved' : ''}`}
    >
      <div className="card-top">
        <div className="card-labels">
          <span className={`institution-badge ${institutionClass}`}>{institutionType}</span>
          {!expired && daysLeft !== null && (
            <span className={urgent ? 'deadline-tag urgent-tag' : 'deadline-tag'}>
              {daysLeft === 0 ? 'D-Day' : `D-${daysLeft}`}
            </span>
          )}
          {!expired && daysLeft === null && <span className="deadline-tag">마감일 미정</span>}
          {expired && <span className="expired-tag">접수마감</span>}
        </div>
        <div className="card-tools">
          <button
            type="button"
            className={job.scrapped ? 'scrap-tool active' : 'scrap-tool'}
            disabled={busy}
            onClick={() => onScrap(job.id)}
          >
            {job.scrapped ? '스크랩 완료' : '스크랩'}
          </button>
          <button type="button" onClick={() => onShare(job)} aria-label="공고 공유">
            공유
          </button>
        </div>
      </div>
      <button className="title-button" onClick={onOpenSource} title={job.title}>
        <h2>{job.title}</h2>
      </button>
      <div className="details">
        <div className="organization-row">
          <span className={`source-symbol ${iconFailed || !iconUrl ? 'fallback' : ''}`}>
            {!iconFailed && iconUrl ? (
              <img src={iconUrl} alt="" onError={() => setIconFailed(true)} />
            ) : (
              job.organization?.trim()?.charAt(0) || 'J'
            )}
          </span>
          <div>
            <span>{institutionClassificationText}</span>
            <strong className="organization-name" title={job.organization}>
              {job.organization}
            </strong>
          </div>
        </div>
        <div className="pills">
          {job.employmentType && (
            <span title={job.employmentType}>{summarizeList(job.employmentType)}</span>
          )}
        </div>
        {job.positions?.length > 0 && (
          <div className="position-list">
            {visiblePositions.map((position) => (
              <span
                key={`${position.standardCategory}-${position.originalName}`}
                title={[
                  position.standardCategory,
                  position.originalName,
                  position.headcount && `${position.headcount}명`,
                ]
                  .filter(Boolean)
                  .join(' · ')}
              >
                <strong>{summarizeList(position.standardCategory)}</strong>
                {position.originalName !== position.standardCategory &&
                  ` · ${summarizeList(position.originalName, 1)}`}
                {position.headcount && ` · ${position.headcount}명`}
              </span>
            ))}
            {hiddenPositionCount > 0 && (
              <span className="position-more">외 {hiddenPositionCount}개 직렬</span>
            )}
          </div>
        )}
      </div>
      <div className="date">
        <span aria-hidden="true">▦</span>
        <div>
          <small>접수 기간</small>
          <strong>
            {job.postedAt || '미확인'} ~ {job.deadline || '미확인'}
            {urgent && ` · D-${daysLeft}`}
          </strong>
        </div>
      </div>
      <div className="actions">
        <div className="job-info-actions">
          <button className="preview-link" onClick={() => onSalary(job)}>
            연봉정보
          </button>
          <button className="preview-link" onClick={() => onCompetition(job)}>
            이전 경쟁률 확인
          </button>
          <button className="preview-link" onClick={() => onInsight(job)}>
            과거 채용 분석
          </button>
        </div>
        {memberActions && (
          <>
            <button className="report-link" onClick={() => onReport(job)}>
              정보 오류 신고
            </button>
            <div className="career-actions">
              <button disabled={matchBusy} onClick={() => onMatch(job)}>
                {matchBusy ? '분석 중...' : '내 스펙 매칭'}
              </button>
            </div>
          </>
        )}
      </div>
      {job.scrapped && (
        <div className="tracking-box">
          <strong>지원 일정 관리</strong>
          <div className="tracking-fields">
            <select
              value={tracking.stage}
              onChange={(e) => setTracking({ ...tracking, stage: e.target.value })}
              aria-label="지원 단계"
            >
              {Object.entries(STAGE_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
            <input
              type="date"
              value={tracking.nextStepDate}
              onChange={(e) => setTracking({ ...tracking, nextStepDate: e.target.value })}
              aria-label="다음 일정"
            />
          </div>
          <textarea
            value={tracking.memo}
            onChange={(e) => setTracking({ ...tracking, memo: e.target.value })}
            placeholder="준비사항이나 전형 메모"
            maxLength={2000}
          />
          <button disabled={busy} onClick={() => onTrack(job.id, tracking)}>
            지원현황 저장
          </button>
        </div>
      )}
    </article>
  )
}

function PostingDetailPage({ id, user, busy, onBack, onScrap, onShare }) {
  const [job, setJob] = useState(null)
  const [preview, setPreview] = useState(null)
  const [message, setMessage] = useState('')

  useEffect(() => {
    let active = true
    Promise.all([getJson(`/api/postings/${id}`), getJson(`/api/postings/${id}/preview`)]).then(
      ([posting, original]) => {
        if (!active) return
        setJob(posting)
        setPreview(original)
      },
      (error) => active && setMessage(error.message),
    )
    return () => {
      active = false
    }
  }, [id])

  if (message) return <p className="error">{message}</p>
  if (!job) return <p className="empty">공고 상세 내용을 불러오는 중...</p>
  const compactPositions = [
    ...new Set(job.positions?.map((position) => compactPositionLabel(position, job.region)) || []),
  ]
  const visibleCompactPositions = compactPositions.slice(0, 12)
  return (
    <article className="posting-detail-page">
      <div className="detail-toolbar">
        <button onClick={onBack}>← 목록으로</button>
        <div>
          <button disabled={busy} onClick={() => onScrap(job.id)}>
            {job.scrapped ? '스크랩 해제' : user ? '스크랩' : '로그인하고 스크랩'}
          </button>
          <button onClick={() => onShare(job)}>공유</button>
          <a
            href={preview?.publisherUrl || preview?.sourceUrl || job.sourceUrl}
            target="_blank"
            rel="noreferrer"
          >
            원문 사이트 ↗
          </a>
        </div>
      </div>
      <header className="detail-heading">
        <span className="institution-badge">{institutionLabel(job)}</span>
        <h1>{job.title}</h1>
        <p>{job.organization}</p>
        <dl>
          <div>
            <dt>접수 기간</dt>
            <dd>
              {job.postedAt || '미확인'} ~ {job.deadline || '미확인'}
            </dd>
          </div>
          <div>
            <dt>근무 지역</dt>
            <dd>{job.region || '공고문 확인'}</dd>
          </div>
          <div>
            <dt>고용 형태</dt>
            <dd>{job.employmentType || '공고문 확인'}</dd>
          </div>
        </dl>
      </header>
      {job.positions?.length > 0 && (
        <section className="detail-positions">
          <h2>채용 직렬</h2>
          <div className="detail-position-chips">
            {visibleCompactPositions.map((position) => (
              <span key={position}>{position}</span>
            ))}
            {compactPositions.length > visibleCompactPositions.length && (
              <span className="detail-position-more">
                외 {compactPositions.length - visibleCompactPositions.length}개
              </span>
            )}
          </div>
        </section>
      )}
      {preview?.originalUrl && (
        <section className="original-preview">
          <div className="original-preview-heading">
            <h2>공고 원문</h2>
            <a href={preview.publisherUrl || preview.sourceUrl} target="_blank" rel="noreferrer">
              새 창에서 원문 보기 ↗
            </a>
          </div>
          {preview.originalUrl.startsWith('/api/') ? (
            <iframe title="채용공고 PDF 원문" src={preview.originalUrl} />
          ) : (
            <iframe
              title="채용공고 원문 내용"
              sandbox="allow-same-origin allow-popups allow-popups-to-escape-sandbox"
              srcDoc={preview.html}
            />
          )}
        </section>
      )}
    </article>
  )
}

function SalaryDialog({ value, onClose }) {
  const money = (amount) => (amount == null ? '—' : `${Number(amount).toLocaleString('ko-KR')}천원`)
  return (
    <div className="match-overlay" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <section className="salary-dialog" role="dialog" aria-modal="true" aria-label="신입사원 초임">
        <div className="match-dialog-head">
          <div>
            <p className="eyebrow">ALIO SALARY</p>
            <h2>신입사원 초임</h2>
          </div>
          <button className="close" onClick={onClose} aria-label="닫기">
            ×
          </button>
        </div>
        {value.loading ? (
          <p>연봉정보를 불러오는 중입니다...</p>
        ) : !value.available ? (
          <div className="salary-empty">
            <strong>공시된 초임 정보를 연결하지 못했습니다.</strong>
            <p>
              {value.alioInstitutionCode
                ? '이 기관의 보수 파일이 아직 적재되지 않았습니다.'
                : '공고에 알리오 기관코드가 없습니다.'}
            </p>
          </div>
        ) : (
          <>
            <p className="salary-organization">{value.organization}</p>
            <div className="salary-total">
              <span>
                {value.fiscalYear}년 {value.valueType === 'BUDGET' ? '예산' : '결산'} 기준
              </span>
              <strong>{money(value.totalAmount)}</strong>
            </div>
            <dl className="salary-breakdown">
              <div>
                <dt>기본급</dt>
                <dd>{money(value.baseSalary)}</dd>
              </div>
              <div>
                <dt>고정수당</dt>
                <dd>{money(value.fixedAllowance)}</dd>
              </div>
              <div>
                <dt>실적수당</dt>
                <dd>{money(value.variableAllowance)}</dd>
              </div>
              <div>
                <dt>급여성 복리후생비</dt>
                <dd>{money(value.welfareBenefit)}</dd>
              </div>
              <div>
                <dt>성과상여금</dt>
                <dd>{money(value.performanceBonus)}</dd>
              </div>
              <div>
                <dt>경영평가 성과급</dt>
                <dd>{money(value.managementEvaluationBonus)}</dd>
              </div>
              <div>
                <dt>기타</dt>
                <dd>{money(value.otherAmount)}</dd>
              </div>
            </dl>
            <p className="salary-note">
              알리오 공시 기준 참고값이며 실제 채용 직무·직급의 보수와 다를 수 있습니다. 단위는
              천원입니다.
            </p>
          </>
        )}
      </section>
    </div>
  )
}

function CompetitionDialog({ value, onClose }) {
  const ratio = (stage) => {
    if (stage.ratio != null && Number(stage.ratio) > 0)
      return `${Number(stage.ratio).toFixed(2)} : 1`
    if (stage.applicants != null && stage.selected)
      return `${(Number(stage.applicants) / Number(stage.selected)).toFixed(2)} : 1`
    return '공개되지 않음'
  }
  return (
    <div className="match-overlay" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <section
        className="salary-dialog competition-dialog"
        role="dialog"
        aria-modal="true"
        aria-label="이전 경쟁률"
      >
        <div className="match-dialog-head">
          <div>
            <p className="eyebrow">채용 경쟁률</p>
            <h2>이전 2개년 경쟁률</h2>
          </div>
          <button className="close" onClick={onClose} aria-label="닫기">
            ×
          </button>
        </div>
        {value.loading ? (
          <p>경쟁률 자료를 불러오는 중입니다...</p>
        ) : !value.available ? (
          <div className="salary-empty">
            <strong>비교 가능한 경쟁률 자료가 없습니다.</strong>
            <p>{value.note}</p>
          </div>
        ) : (
          <>
            <p className="salary-organization">{value.organization}</p>
            <p className="salary-note">{value.note}</p>
            <div className="competition-list">
              {value.items.map((item) => (
                <article key={item.postingId}>
                  <div className="competition-title">
                    <strong>{item.title}</strong>
                    <span>
                      {item.postedAt?.slice(0, 4) || '연도 미확인'} ·{' '}
                      {item.similarCategory ? '유사 직렬' : '기관 정규직 참고'}
                    </span>
                  </div>
                  {item.stages.map((stage, index) => (
                    <div className="competition-stage" key={`${stage.name}-${index}`}>
                      <div className="competition-stage-main">
                        <span>{stage.name}</span>
                        <small>
                          {stage.sourceType === 'API'
                            ? '공공데이터 자료'
                            : stage.sourceType === 'OFFICIAL_PDF'
                              ? '첨부 공고문'
                              : '채용 페이지'}
                          {stage.calculated && ' · 인원 기준 계산'}
                        </small>
                        {stage.evidenceText && <em>{stage.evidenceText}</em>}
                      </div>
                      <small>
                        지원 {stage.applicants ?? '—'}명 · 선발 {stage.selected ?? '—'}명
                      </small>
                      <strong>{ratio(stage)}</strong>
                      {stage.sourceUrl && (
                        <a href={stage.sourceUrl} target="_blank" rel="noreferrer">
                          근거 보기 ↗
                        </a>
                      )}
                    </div>
                  ))}
                  <a href={item.sourceUrl} target="_blank" rel="noreferrer">
                    과거 공고 원문 ↗
                  </a>
                </article>
              ))}
            </div>
          </>
        )}
      </section>
    </div>
  )
}

function MatchDialog({ value, onClose, onOpenSource }) {
  const { job, result } = value
  return (
    <div className="match-overlay" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <section className="match-dialog" role="dialog" aria-modal="true" aria-label="스펙 매칭 결과">
        <div className="match-dialog-head">
          <div>
            <p className="eyebrow">SPEC MATCHING</p>
            <h2>내 스펙 매칭</h2>
          </div>
          <button className="close" onClick={onClose} aria-label="닫기">
            ×
          </button>
        </div>
        <h3>{job.title}</h3>
        <p className="match-organization">{job.organization}</p>
        <div className="match-score">
          <strong>{result.score ?? 0}점</strong>
          <span>등록한 역량과 공고문을 비교한 참고 점수</span>
        </div>
        <div className="match-summary">{result.summary}</div>
        {result.strengths?.length > 0 && (
          <div className="match-points strengths">
            <strong>확인된 강점</strong>
            <ul>
              {result.strengths.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </div>
        )}
        {result.gaps?.length > 0 && (
          <div className="match-points gaps">
            <strong>추가 확인할 요건</strong>
            <ul>
              {result.gaps.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </div>
        )}
        {result.matchedCertifications?.length > 0 && (
          <div className="matched-certificates">
            <strong>일치 자격증</strong>
            <div>
              {result.matchedCertifications.map((item) => (
                <span key={item}>{item}</span>
              ))}
            </div>
          </div>
        )}
        <div className="match-evidence">
          <strong>공고문 자격·우대 근거</strong>
          {result.requirementEvidence?.length > 0 ? (
            result.requirementEvidence.map((line) => <p key={line}>{line}</p>)
          ) : (
            <p>구조화된 자격·우대 문구를 찾지 못했습니다. 원문에서 확인해 주세요.</p>
          )}
        </div>
        <button className="match-original" onClick={() => onOpenSource(job)}>
          원문보기 ↗
        </button>
      </section>
    </div>
  )
}

/** 저장된 학위·자격증·경력·대외활동을 입력 폼과 분리해 읽기 쉽게 보여준다. */
function CapabilityDialog({ profile, loading, onClose, onEdit }) {
  const period = (start, end) => [start, end].filter(Boolean).join(' ~ ') || '기간 미입력'
  const certifications = (profile.certifications || []).filter((item) => item.name)
  const activities = (profile.activities || []).filter((item) => item.name || item.description)
  const careers = (profile.careers || []).filter(
    (item) => item.companyName || item.position || item.duties,
  )
  const degrees = (profile.degrees || []).filter(
    (item) => item.schoolName || item.major || item.degreeType,
  )
  const empty =
    !certifications.length &&
    !activities.length &&
    !careers.length &&
    !degrees.length &&
    !profile.grades
  return (
    <div className="match-overlay" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <section className="capability-dialog" role="dialog" aria-modal="true" aria-label="내 역량">
        <div className="match-dialog-head">
          <div>
            <p className="eyebrow">MY CAPABILITIES</p>
            <h2>내 역량</h2>
          </div>
          <button className="close" onClick={onClose} aria-label="닫기">
            ×
          </button>
        </div>
        {loading ? (
          <p className="capability-empty">등록한 스펙을 불러오는 중입니다...</p>
        ) : empty ? (
          <p className="capability-empty">
            아직 등록한 스펙이 없습니다. 마이페이지에서 내 스펙을 입력해 주세요.
          </p>
        ) : (
          <div className="capability-sections">
            {degrees.length > 0 && (
              <section>
                <h3>학위</h3>
                {degrees.map((item, index) => (
                  <div className="capability-item" key={`degree-view-${index}`}>
                    <strong>{[item.schoolName, item.major].filter(Boolean).join(' · ')}</strong>
                    <span>
                      {[item.degreeType, item.status, period(item.startMonth, item.endMonth)]
                        .filter(Boolean)
                        .join(' · ')}
                    </span>
                  </div>
                ))}
              </section>
            )}
            {certifications.length > 0 && (
              <section>
                <h3>자격증</h3>
                {certifications.map((item, index) => (
                  <div className="capability-item" key={`cert-view-${index}`}>
                    <strong>{item.name}</strong>
                    {item.type && (
                      <span>
                        {QUALIFICATION_TYPE_LABELS[item.type] || item.type}
                        {item.issuer && ` · ${item.issuer}`}
                      </span>
                    )}
                    {item.grade && <span>급수 · {item.grade}</span>}
                    <span>
                      {item.acquiredDate
                        ? `${item.acquiredDate} 취득`
                        : item.acquiredMonth
                          ? `${item.acquiredMonth} 취득`
                          : '취득일 미입력'}
                    </span>
                  </div>
                ))}
              </section>
            )}
            {careers.length > 0 && (
              <section>
                <h3>경력</h3>
                {careers.map((item, index) => (
                  <div className="capability-item" key={`career-view-${index}`}>
                    <strong>{[item.companyName, item.position].filter(Boolean).join(' · ')}</strong>
                    <span>{period(item.startMonth, item.endMonth)}</span>
                    {item.duties && <p>{item.duties}</p>}
                  </div>
                ))}
              </section>
            )}
            {activities.length > 0 && (
              <section>
                <h3>대외활동</h3>
                {activities.map((item, index) => (
                  <div className="capability-item" key={`activity-view-${index}`}>
                    <strong>{item.name || '활동명 미입력'}</strong>
                    <span>{period(item.startMonth, item.endMonth)}</span>
                    {item.description && <p>{item.description}</p>}
                  </div>
                ))}
              </section>
            )}
            {profile.grades && (
              <section>
                <h3>성적</h3>
                <div className="capability-item">
                  <p>{profile.grades}</p>
                </div>
              </section>
            )}
          </div>
        )}
        <button className="capability-edit" onClick={onEdit}>
          마이페이지에서 수정
        </button>
      </section>
    </div>
  )
}

function CommunityBoard({ user, securePost, onLogin, onUnreadChange }) {
  const [tab, setTab] = useState('notices')
  const [notices, setNotices] = useState([])
  const [inquiries, setInquiries] = useState([])
  const [noticeForm, setNoticeForm] = useState({ title: '', content: '', pinned: false })
  const [editingNoticeId, setEditingNoticeId] = useState(null)
  const [questionForm, setQuestionForm] = useState({
    category: 'SERVICE',
    title: '',
    content: '',
    postingUrl: '',
  })
  const [answers, setAnswers] = useState({})
  const [inquiryQuery, setInquiryQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [categoryFilter, setCategoryFilter] = useState('')
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)

  const loadCommunity = async () => {
    const savedNotices = await getJson('/api/community/notices')
    setNotices(savedNotices)
    if (user) {
      const savedInquiries = await getJson('/api/community/inquiries')
      setInquiries(savedInquiries)
      if (!user.admin) onUnreadChange(savedInquiries.filter((item) => !item.answerRead).length)
    } else setInquiries([])
  }
  useEffect(() => {
    loadCommunity().catch((e) => setMessage(e.message))
    // 로그인 상태가 바뀌면 열람 가능한 문의 범위를 다시 읽는다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user])
  useEffect(() => {
    if (tab !== 'questions' || !user || user.admin) return
    securePost('/api/community/inquiries/read')
      .then(() => {
        setInquiries((items) => items.map((item) => ({ ...item, answerRead: true })))
        onUnreadChange(0)
      })
      .catch(() => {})
  }, [tab, user, securePost, onUnreadChange])

  const submitNotice = async (event) => {
    event.preventDefault()
    setBusy(true)
    try {
      await securePost(
        editingNoticeId
          ? `/api/admin/community/notices/${editingNoticeId}`
          : '/api/admin/community/notices',
        JSON.stringify(noticeForm),
        'application/json',
      )
      setNoticeForm({ title: '', content: '', pinned: false })
      setEditingNoticeId(null)
      await loadCommunity()
      setMessage(editingNoticeId ? '공지사항을 수정했습니다.' : '공지사항을 등록했습니다.')
    } catch (e) {
      setMessage(e.message)
    } finally {
      setBusy(false)
    }
  }
  const submitQuestion = async (event) => {
    event.preventDefault()
    setBusy(true)
    try {
      await securePost('/api/community/inquiries', JSON.stringify(questionForm), 'application/json')
      setQuestionForm({ category: 'SERVICE', title: '', content: '', postingUrl: '' })
      await loadCommunity()
      setMessage('문의가 접수되었습니다.')
    } catch (e) {
      setMessage(e.message)
    } finally {
      setBusy(false)
    }
  }
  const editNotice = (item) => {
    setEditingNoticeId(item.id)
    setNoticeForm({ title: item.title, content: item.content, pinned: item.pinned })
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
  const deleteNotice = async (id) => {
    if (!window.confirm('이 공지사항을 삭제할까요?')) return
    setBusy(true)
    try {
      await securePost(`/api/admin/community/notices/${id}/delete`)
      if (editingNoticeId === id) {
        setEditingNoticeId(null)
        setNoticeForm({ title: '', content: '', pinned: false })
      }
      await loadCommunity()
      setMessage('공지사항을 삭제했습니다.')
    } catch (e) {
      setMessage(e.message)
    } finally {
      setBusy(false)
    }
  }
  const changeInquiryStatus = async (id, status) => {
    setBusy(true)
    try {
      await securePost(
        `/api/admin/community/inquiries/${id}/status`,
        JSON.stringify({ status }),
        'application/json',
      )
      await loadCommunity()
      setMessage('문의 처리 상태를 변경했습니다.')
    } catch (e) {
      setMessage(e.message)
    } finally {
      setBusy(false)
    }
  }
  const submitAnswer = async (id) => {
    if (!answers[id]?.trim()) return
    setBusy(true)
    try {
      await securePost(
        `/api/admin/community/inquiries/${id}/answer`,
        JSON.stringify({ answer: answers[id] }),
        'application/json',
      )
      setAnswers((values) => ({ ...values, [id]: '' }))
      await loadCommunity()
      setMessage('답변을 등록했습니다.')
    } catch (e) {
      setMessage(e.message)
    } finally {
      setBusy(false)
    }
  }
  const formatDateTime = (value) =>
    value
      ? new Date(value).toLocaleString('ko-KR', { dateStyle: 'medium', timeStyle: 'short' })
      : ''
  const statusLabel = (status) =>
    ({ WAITING: '접수', IN_PROGRESS: '처리 중', ANSWERED: '답변 완료' })[status] || status
  const filteredInquiries = inquiries.filter((item) => {
    const query = inquiryQuery.trim().toLowerCase()
    return (
      (!statusFilter || item.status === statusFilter) &&
      (!categoryFilter || item.category === categoryFilter) &&
      (!query ||
        item.title.toLowerCase().includes(query) ||
        item.content.toLowerCase().includes(query) ||
        item.requesterName?.toLowerCase().includes(query) ||
        item.requesterEmail?.toLowerCase().includes(query))
    )
  })

  return (
    <section className="community-page">
      <div className="heading community-heading">
        <div>
          <p className="eyebrow">CUSTOMER SUPPORT</p>
          <h1>공지사항 · Q&amp;A</h1>
          <p className="sub">서비스 안내를 확인하고 관리자에게 비공개로 문의할 수 있습니다.</p>
        </div>
      </div>
      <div className="community-tabs" role="tablist">
        <button className={tab === 'notices' ? 'active' : ''} onClick={() => setTab('notices')}>
          공지사항
        </button>
        <button className={tab === 'questions' ? 'active' : ''} onClick={() => setTab('questions')}>
          Q&amp;A{' '}
          {user?.admin &&
            inquiries.filter((item) => item.status === 'WAITING').length > 0 &&
            `(${inquiries.filter((item) => item.status === 'WAITING').length})`}
        </button>
      </div>
      {message && <p className="community-message">{message}</p>}

      {tab === 'notices' && (
        <div className="community-content">
          {user?.admin && (
            <form className="community-form" onSubmit={submitNotice}>
              <h2>{editingNoticeId ? '공지 수정' : '공지 등록'}</h2>
              <input
                value={noticeForm.title}
                onChange={(e) => setNoticeForm({ ...noticeForm, title: e.target.value })}
                placeholder="공지 제목"
                maxLength={200}
                required
              />
              <textarea
                value={noticeForm.content}
                onChange={(e) => setNoticeForm({ ...noticeForm, content: e.target.value })}
                placeholder="공지 내용"
                maxLength={10000}
                required
              />
              <label className="community-check">
                <input
                  type="checkbox"
                  checked={noticeForm.pinned}
                  onChange={(e) => setNoticeForm({ ...noticeForm, pinned: e.target.checked })}
                />
                상단 고정
              </label>
              <div className="community-form-actions">
                <button disabled={busy}>{editingNoticeId ? '수정 저장' : '공지 등록'}</button>
                {editingNoticeId && (
                  <button
                    type="button"
                    className="secondary"
                    onClick={() => {
                      setEditingNoticeId(null)
                      setNoticeForm({ title: '', content: '', pinned: false })
                    }}
                  >
                    취소
                  </button>
                )}
              </div>
            </form>
          )}
          <div className="board-list">
            {notices.map((item) => (
              <details key={item.id} className={item.pinned ? 'pinned' : ''}>
                <summary>
                  <span>{item.pinned ? '필독' : '공지'}</span>
                  <strong>{item.title}</strong>
                  <time>{formatDateTime(item.createdAt)}</time>
                </summary>
                <p>{item.content}</p>
                {user?.admin && (
                  <div className="board-admin-actions">
                    <button type="button" onClick={() => editNotice(item)}>
                      수정
                    </button>
                    <button type="button" className="danger" onClick={() => deleteNotice(item.id)}>
                      삭제
                    </button>
                  </div>
                )}
              </details>
            ))}
            {!notices.length && <p className="empty">등록된 공지사항이 없습니다.</p>}
          </div>
        </div>
      )}

      {tab === 'questions' && !user && (
        <div className="community-login-guide">
          <p>문의 작성과 답변 확인은 로그인 후 이용할 수 있습니다.</p>
          <button onClick={onLogin}>로그인</button>
        </div>
      )}
      {tab === 'questions' && user && (
        <div className="community-content qna-layout">
          {!user.admin && (
            <form className="community-form" onSubmit={submitQuestion}>
              <h2>문의 등록</h2>
              <select
                value={questionForm.category}
                onChange={(e) => setQuestionForm({ ...questionForm, category: e.target.value })}
              >
                <option value="SERVICE">서비스 이용</option>
                <option value="POSTING">채용공고 오류</option>
                <option value="ACCOUNT">계정</option>
                <option value="SUGGESTION">기능 제안</option>
                <option value="OTHER">기타</option>
              </select>
              <input
                value={questionForm.title}
                onChange={(e) => setQuestionForm({ ...questionForm, title: e.target.value })}
                placeholder="문의 제목"
                maxLength={200}
                required
              />
              <textarea
                value={questionForm.content}
                onChange={(e) => setQuestionForm({ ...questionForm, content: e.target.value })}
                placeholder="개인정보를 제외하고 문의 내용을 작성해 주세요."
                maxLength={10000}
                required
              />
              {questionForm.category === 'POSTING' && (
                <input
                  type="url"
                  value={questionForm.postingUrl}
                  onChange={(e) => setQuestionForm({ ...questionForm, postingUrl: e.target.value })}
                  placeholder="오류가 있는 채용공고 원문 주소"
                  maxLength={1500}
                />
              )}
              <button disabled={busy}>문의 접수</button>
            </form>
          )}
          <div className="board-list inquiry-list">
            <h2>{user.admin ? '전체 문의 관리' : '내 문의'}</h2>
            <div className="inquiry-filters">
              <input
                value={inquiryQuery}
                onChange={(e) => setInquiryQuery(e.target.value)}
                placeholder={user.admin ? '제목, 내용, 문의자 검색' : '제목과 내용 검색'}
              />
              <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
                <option value="">전체 상태</option>
                <option value="WAITING">접수</option>
                <option value="IN_PROGRESS">처리 중</option>
                <option value="ANSWERED">답변 완료</option>
              </select>
              <select value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value)}>
                <option value="">전체 분류</option>
                <option value="SERVICE">서비스 이용</option>
                <option value="POSTING">채용공고 오류</option>
                <option value="ACCOUNT">계정</option>
                <option value="SUGGESTION">기능 제안</option>
                <option value="OTHER">기타</option>
              </select>
            </div>
            {filteredInquiries.map((item) => (
              <details key={item.id}>
                <summary>
                  <span className={item.status.toLowerCase()}>{statusLabel(item.status)}</span>
                  <strong>{item.title}</strong>
                  <time>{formatDateTime(item.createdAt)}</time>
                </summary>
                {user.admin && (
                  <small className="requester">
                    문의자: {item.requesterName} · {item.requesterEmail}
                  </small>
                )}
                <p>{item.content}</p>
                {item.postingUrl && (
                  <a
                    className="inquiry-posting-link"
                    href={item.postingUrl}
                    target="_blank"
                    rel="noreferrer"
                  >
                    연결된 채용공고 확인 ↗
                  </a>
                )}
                {item.answer && (
                  <div className="admin-answer">
                    <strong>관리자 답변</strong>
                    <p>{item.answer}</p>
                    <time>{formatDateTime(item.answeredAt)}</time>
                  </div>
                )}
                {user.admin && (
                  <div className="answer-form">
                    <label>
                      처리 상태
                      <select
                        value={item.status === 'ANSWERED' ? 'ANSWERED' : item.status}
                        disabled={item.status === 'ANSWERED' || busy}
                        onChange={(e) => changeInquiryStatus(item.id, e.target.value)}
                      >
                        <option value="WAITING">접수</option>
                        <option value="IN_PROGRESS">처리 중</option>
                        {item.status === 'ANSWERED' && <option value="ANSWERED">답변 완료</option>}
                      </select>
                    </label>
                    <textarea
                      value={answers[item.id] ?? item.answer ?? ''}
                      onChange={(e) => setAnswers({ ...answers, [item.id]: e.target.value })}
                      placeholder="답변을 입력하세요."
                      maxLength={10000}
                    />
                    <button disabled={busy} onClick={() => submitAnswer(item.id)}>
                      {item.answer ? '답변 수정' : '답변 등록'}
                    </button>
                  </div>
                )}
              </details>
            ))}
            {!filteredInquiries.length && <p className="empty">조건에 맞는 문의가 없습니다.</p>}
          </div>
        </div>
      )}
    </section>
  )
}

/** 공고 변경·마감 임박·관심 기관 신규 공고를 상단에서 함께 보여준다. */
function SecurityCenter({ user, securePost, onClose }) {
  const [value, setValue] = useState(null)
  const [message, setMessage] = useState('')
  const load = () =>
    getJson('/api/auth/security')
      .then(setValue)
      .catch((e) => setMessage(e.message))
  useEffect(() => {
    load()
  }, [])
  const logoutOthers = async () => {
    await securePost('/api/auth/security/logout-others')
    setMessage('현재 기기를 제외한 기존 로그인 세션을 종료했습니다.')
  }
  const setup = async () => {
    const setupValue = await securePost('/api/auth/security/2fa/setup')
    const code = window.prompt(
      `인증 앱에 아래 키를 등록한 뒤 6자리 코드를 입력하세요.\n\n${setupValue.secret}`,
    )
    if (!code) return
    await securePost('/api/auth/security/2fa/enable', JSON.stringify({ code }), 'application/json')
    setMessage('관리자 2단계 인증을 사용합니다.')
    load()
  }
  const disable = async () => {
    const password = window.prompt('현재 비밀번호를 입력하세요.')
    if (!password) return
    const code = window.prompt('인증 앱의 6자리 코드를 입력하세요.')
    if (!code) return
    await securePost(
      '/api/auth/security/2fa/disable',
      JSON.stringify({ password, code }),
      'application/json',
    )
    setMessage('관리자 2단계 인증을 해제했습니다.')
    load()
  }
  return (
    <div className="match-overlay" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <section className="security-dialog">
        <button className="close" onClick={onClose}>
          ×
        </button>
        <p className="eyebrow">ACCOUNT SECURITY</p>
        <h2>계정 보안</h2>
        <p>추가 개인정보 없이 로그인 기록과 접속 기기를 관리합니다.</p>
        <div className="security-actions">
          <button onClick={logoutOthers}>다른 기기 모두 로그아웃</button>
          {user.admin && !value?.twoFactorEnabled && (
            <button onClick={setup}>관리자 2단계 인증 설정</button>
          )}
          {user.admin && value?.twoFactorEnabled && (
            <>
              <b>2단계 인증 사용 중</b>
              <button onClick={disable}>2단계 인증 해제</button>
            </>
          )}
        </div>
        {message && <p className="status">{message}</p>}
        <h3>최근 로그인 기록</h3>
        <div className="login-history">
          {value?.loginHistory.map((item, index) => (
            <article key={`${item.createdAt}-${index}`}>
              <b>{item.success ? '로그인 성공' : '로그인 실패'}</b>
              <span>{new Date(item.createdAt).toLocaleString('ko-KR')}</span>
              <small>
                {item.ipAddress} · {item.userAgent || '기기 정보 없음'}
              </small>
            </article>
          ))}
        </div>
      </section>
    </div>
  )
}

function NotificationCenter({ user, securePost, onOpen, refreshVersion }) {
  const [open, setOpen] = useState(false)
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')

  const load = async () => {
    if (!user) return
    setLoading(true)
    try {
      const [alerts, notificationStates] = await Promise.all([
        getJson('/api/tools/alerts'),
        getJson('/api/tools/notification-states'),
      ])
      const stateByKey = new Map(notificationStates.map((item) => [item.key, item]))
      const merged = alerts
        .map((item) => ({
          ...item,
          key: `alert-${item.id}-${item.reason}`,
          postingId: item.id,
          category: '스크랩 마감',
          important: item.remainingDays != null && item.remainingDays <= 3,
          detail: item.region || '지역 미정',
          occurredAt: item.deadline ? `${item.deadline}T00:00:00` : null,
        }))
        .map((item) => ({
          ...item,
          fresh: item.fresh && !stateByKey.get(item.key)?.read,
          deleted: stateByKey.get(item.key)?.deleted,
        }))
        .filter((item) => !item.deleted)
        .sort((a, b) => {
          if (a.important !== b.important) return a.important ? -1 : 1
          return new Date(b.occurredAt || 0) - new Date(a.occurredAt || 0)
        })
      setItems(merged.slice(0, 60))
      setMessage('')
    } catch (e) {
      setMessage(e.message)
    } finally {
      setLoading(false)
    }
  }
  useEffect(() => {
    load()
    // 로그인 계정이 바뀔 때 해당 회원의 알림만 다시 읽는다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.email, refreshVersion])

  const readAll = async () => {
    try {
      await Promise.all([
        securePost(
          '/api/tools/notification-states/read',
          JSON.stringify({ keys: items.map((item) => item.key) }),
          'application/json',
        ),
        securePost('/api/tools/alerts/read'),
      ])
      setItems((values) => values.map((item) => ({ ...item, fresh: false })))
    } catch (e) {
      setMessage(e.message)
    }
  }
  const markRead = async (item) => {
    if (!item.fresh) return
    await securePost(
      '/api/tools/notification-states/read',
      JSON.stringify({ keys: [item.key] }),
      'application/json',
    )
    setItems((values) =>
      values.map((value) => (value.key === item.key ? { ...value, fresh: false } : value)),
    )
  }
  const openItem = (item) => {
    // 사용자 클릭 시 즉시 빈 원문 창을 열어 브라우저 팝업 차단을 피한다.
    onOpen({
      id: item.postingId || item.id,
      title: item.title,
      organization: item.organization,
      sourceUrl: item.sourceUrl,
    })
    markRead(item).catch((e) => setMessage(e.message))
  }
  const deleteItem = async (item) => {
    try {
      await securePost(
        '/api/tools/notification-states/delete',
        JSON.stringify({ keys: [item.key] }),
        'application/json',
      )
      setItems((values) => values.filter((value) => value.key !== item.key))
    } catch (e) {
      setMessage(e.message)
    }
  }
  const unread = items.filter((item) => item.fresh).length

  return (
    <div className="notification-center">
      <button
        className={open ? 'active notification-trigger' : 'notification-trigger'}
        type="button"
        aria-label={`알림센터${unread ? `, 새 알림 ${unread}건` : ''}`}
        aria-expanded={open}
        onClick={() => setOpen((value) => !value)}
      >
        <span aria-hidden="true">●</span> 알림
        {unread > 0 && <b className="nav-notification">{unread}</b>}
      </button>
      {open && (
        <section className="notification-popover">
          <div className="notification-popover-head">
            <div>
              <strong>알림센터</strong>
              <span>스크랩한 공고의 마감 일정만 보여드립니다.</span>
            </div>
            {unread > 0 && (
              <button type="button" onClick={readAll}>
                모두 확인
              </button>
            )}
          </div>
          {message && <p className="notification-message">{message}</p>}
          <div className="notification-list">
            {items.map((item) => (
              <article
                key={item.key}
                className={`${item.important ? 'important' : ''} ${item.fresh ? 'fresh' : ''}`}
              >
                <button className="notification-main" type="button" onClick={() => openItem(item)}>
                  <span className="notification-labels">
                    {item.important && <b>중요</b>}
                    <em>{item.category}</em>
                    {item.fresh && <i>NEW</i>}
                  </span>
                  <strong>{item.title}</strong>
                  <span>{item.organization}</span>
                  <small>{item.reason}</small>
                  {item.detail && <p>{item.detail}</p>}
                </button>
                <button
                  className="notification-delete"
                  type="button"
                  aria-label="이 알림 삭제"
                  onClick={() => deleteItem(item)}
                >
                  ×
                </button>
              </article>
            ))}
            {loading && <p className="empty">알림을 불러오는 중...</p>}
            {!loading && !items.length && <p className="empty">현재 확인할 알림이 없습니다.</p>}
          </div>
        </section>
      )}
    </div>
  )
}

function PostingChangesPanel({ securePost, onOpen, onUnreadChange }) {
  const [changes, setChanges] = useState([])
  const [open, setOpen] = useState(false)
  const [message, setMessage] = useState('')

  const loadChanges = async () => {
    try {
      const values = await getJson('/api/tools/posting-changes')
      setChanges(values)
      onUnreadChange(values.filter((item) => item.fresh).length)
    } catch (e) {
      setMessage(e.message)
    }
  }
  useEffect(() => {
    loadChanges()
    // 마이페이지 진입 시 최신 변경 이력을 가져온다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])
  const toggle = async () => {
    const next = !open
    setOpen(next)
    if (!next) return
    try {
      await securePost('/api/tools/posting-changes/read')
      setChanges((items) => items.map((item) => ({ ...item, fresh: false })))
      onUnreadChange(0)
    } catch (e) {
      setMessage(e.message)
    }
  }
  const displayValue = (value) => value || '정보 없음'
  const formatDateTime = (value) =>
    new Date(value).toLocaleString('ko-KR', { dateStyle: 'medium', timeStyle: 'short' })

  return (
    <section className="posting-changes-panel">
      <div className="posting-changes-head">
        <div>
          <p className="eyebrow">POSTING UPDATES</p>
          <h2>공고 변경 알림</h2>
          <p>스크랩하거나 관심 기관으로 등록한 공고의 주요 변경사항입니다.</p>
        </div>
        <button type="button" onClick={toggle}>
          {open ? '변경사항 닫기' : '변경사항 확인'}
          {!open && changes.some((item) => item.fresh) && (
            <b>{changes.filter((item) => item.fresh).length} NEW</b>
          )}
        </button>
      </div>
      {message && <p className="favorite-message">{message}</p>}
      {open && (
        <div className="posting-change-list">
          {changes.map((item) => (
            <article key={item.id} className={item.fresh ? 'fresh' : ''}>
              <div className="posting-change-title">
                <span>{item.organization}</span>
                <strong>{item.title}</strong>
                <time>{formatDateTime(item.detectedAt)}</time>
              </div>
              <div className="posting-change-badges">
                <b className="posting-change-field">{item.fieldLabel} 변경</b>
                <b
                  className={`change-importance ${item.importance === 'IMPORTANT' ? 'important' : ''}`}
                >
                  {item.importance === 'IMPORTANT' ? '중요' : '일반'}
                </b>
              </div>
              <div className="posting-change-values">
                <p>
                  <span>변경 전</span>
                  {displayValue(item.oldValue)}
                </p>
                <p>
                  <span>변경 후</span>
                  {displayValue(item.newValue)}
                </p>
              </div>
              <button
                type="button"
                onClick={() =>
                  onOpen({
                    id: item.postingId,
                    title: item.title,
                    organization: item.organization,
                    sourceUrl: item.sourceUrl,
                  })
                }
              >
                공고 원문 확인 ↗
              </button>
            </article>
          ))}
          {!changes.length && <p className="empty">아직 감지된 공고 변경사항이 없습니다.</p>}
        </div>
      )}
    </section>
  )
}

function FavoriteOrganizationsPanel({ securePost, onOpen }) {
  const [favorites, setFavorites] = useState([])
  const [postings, setPostings] = useState([])
  const [query, setQuery] = useState('')
  const [options, setOptions] = useState([])
  const [open, setOpen] = useState(false)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')

  const loadFavorites = () =>
    getJson('/api/tools/favorite-organizations')
      .then(setFavorites)
      .catch((e) => setMessage(e.message))
  useEffect(() => {
    loadFavorites()
  }, [])

  const searchOrganizations = async (event) => {
    event.preventDefault()
    if (!query.trim()) return
    setBusy(true)
    try {
      setOptions(
        await getJson(
          `/api/tools/favorite-organizations/search?q=${encodeURIComponent(query.trim())}`,
        ),
      )
    } catch (e) {
      setMessage(e.message)
    } finally {
      setBusy(false)
    }
  }
  const toggleFavorite = async (organization) => {
    setBusy(true)
    try {
      const result = await securePost(
        '/api/tools/favorite-organizations/toggle',
        JSON.stringify({ organization }),
        'application/json',
      )
      await loadFavorites()
      setOptions([])
      setQuery('')
      setMessage(result.followed ? '관심 기관으로 등록했습니다.' : '관심 기관에서 해제했습니다.')
      if (open) setPostings(await getJson('/api/tools/favorite-organizations/postings'))
    } catch (e) {
      setMessage(e.message)
    } finally {
      setBusy(false)
    }
  }
  const showPostings = async () => {
    setBusy(true)
    try {
      setPostings(await getJson('/api/tools/favorite-organizations/postings'))
      await securePost('/api/tools/favorite-organizations/read')
      setOpen(true)
      await loadFavorites()
    } catch (e) {
      setMessage(e.message)
    } finally {
      setBusy(false)
    }
  }
  const totalNew = favorites.reduce((sum, item) => sum + item.newPostings, 0)

  return (
    <section className="favorite-organizations">
      <div className="profile-heading favorite-heading">
        <div>
          <p className="eyebrow">FAVORITE ORGANIZATIONS</p>
          <h2>관심 기관</h2>
          <p>관심 기관을 등록하면 해당 기관의 진행 중 공고와 신규 공고를 모아볼 수 있습니다.</p>
        </div>
        <button type="button" disabled={busy || !favorites.length} onClick={showPostings}>
          관심 기관 공고 {totalNew > 0 && <b>{totalNew} NEW</b>}
        </button>
      </div>
      <form className="organization-search" onSubmit={searchOrganizations}>
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="기관명을 검색하세요. 예: 한국전력공사"
          maxLength={200}
        />
        <button disabled={busy || !query.trim()}>기관 검색</button>
      </form>
      {message && <p className="favorite-message">{message}</p>}
      {options.length > 0 && (
        <div className="organization-options">
          {options.map((item) => {
            const followed = favorites.some((favorite) =>
              favorite.aliases.includes(item.organization),
            )
            return (
              <button
                type="button"
                key={item.organization}
                disabled={busy}
                onClick={() => toggleFavorite(item.organization)}
              >
                <span>{item.organization}</span>
                <small>수집 공고 {item.postings}건</small>
                <b>{followed ? '등록 해제' : '＋ 관심 기관'}</b>
              </button>
            )
          })}
        </div>
      )}
      <div className="favorite-list">
        {favorites.map((item) => (
          <article key={item.id}>
            <div>
              <strong>{item.organization}</strong>
              {item.aliases.length > 1 && (
                <small title={item.aliases.join(', ')}>명칭 {item.aliases.length}개 통합</small>
              )}
            </div>
            <span>진행 중 {item.openPostings}건</span>
            <span>최근 마감 {item.nearestDeadline || '미정'}</span>
            {item.newPostings > 0 && <b>NEW {item.newPostings}</b>}
            <button type="button" disabled={busy} onClick={() => toggleFavorite(item.organization)}>
              해제
            </button>
          </article>
        ))}
        {!favorites.length && <p className="empty">등록한 관심 기관이 없습니다.</p>}
      </div>
      {open && (
        <div className="favorite-postings">
          <div className="favorite-postings-head">
            <h3>관심 기관 채용공고</h3>
            <button type="button" onClick={() => setOpen(false)}>
              닫기
            </button>
          </div>
          {postings.map((item) => (
            <button type="button" key={item.id} onClick={() => onOpen(item)}>
              <span>{item.organization}</span>
              <strong>{item.title}</strong>
              <small>
                {item.region || '지역 미정'} · 마감 {item.deadline || '미정'}
              </small>
            </button>
          ))}
          {!postings.length && <p className="empty">현재 진행 중인 관심 기관 공고가 없습니다.</p>}
        </div>
      )}
    </section>
  )
}

function ApplicationDashboard({ applications, onOpen }) {
  const [stageFilter, setStageFilter] = useState('')
  const [calendarMonth, setCalendarMonth] = useState(() => {
    const today = new Date()
    return new Date(today.getFullYear(), today.getMonth(), 1)
  })
  const [selectedDate, setSelectedDate] = useState('')
  const year = calendarMonth.getFullYear()
  const month = calendarMonth.getMonth()
  const firstWeekday = new Date(year, month, 1).getDay()
  const lastDay = new Date(year, month + 1, 0).getDate()
  const visible = stageFilter
    ? applications.filter((item) => item.stage === stageFilter)
    : applications
  const eventsByDate = {}
  applications.forEach((item) => {
    if (item.deadline) {
      eventsByDate[item.deadline] = [
        ...(eventsByDate[item.deadline] || []),
        { ...item, eventType: '마감' },
      ]
    }
    if (item.nextStepDate) {
      eventsByDate[item.nextStepDate] = [
        ...(eventsByDate[item.nextStepDate] || []),
        { ...item, eventType: STAGE_LABELS[item.stage] || '전형 일정' },
      ]
    }
  })
  const selectedEvents = selectedDate ? eventsByDate[selectedDate] || [] : []
  const dateKey = (day) =>
    `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  const changeMonth = (offset) => {
    setCalendarMonth(new Date(year, month + offset, 1))
    setSelectedDate('')
  }
  const exportCalendar = () => {
    const escapeIcs = (value) =>
      String(value || '')
        .replaceAll('\\', '\\\\')
        .replaceAll(';', '\\;')
        .replaceAll(',', '\\,')
        .replaceAll('\n', '\\n')
    const events = applications.flatMap((item) => {
      const values = []
      if (item.deadline) values.push({ date: item.deadline, label: '지원 마감' })
      if (item.nextStepDate)
        values.push({ date: item.nextStepDate, label: STAGE_LABELS[item.stage] || '채용 전형' })
      return values.map((event) => ({ ...event, item }))
    })
    if (!events.length) return
    const body = events.flatMap(({ date, label, item }, index) => [
      'BEGIN:VEVENT',
      `UID:jobhub-${item.id}-${date}-${index}@public-job-hub`,
      `DTSTART;VALUE=DATE:${date.replaceAll('-', '')}`,
      `SUMMARY:${escapeIcs(`[${label}] ${item.organization} ${item.title}`)}`,
      `DESCRIPTION:${escapeIcs(item.memo || 'JOB HUB KOREA 지원 일정')}`,
      `URL:${escapeIcs(item.sourceUrl)}`,
      'END:VEVENT',
    ])
    const blob = new Blob(
      [
        [
          'BEGIN:VCALENDAR',
          'VERSION:2.0',
          'PRODID:-//JOB HUB KOREA//KO',
          ...body,
          'END:VCALENDAR',
        ].join('\r\n'),
      ],
      { type: 'text/calendar;charset=utf-8' },
    )
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'job-hub-calendar.ics'
    link.click()
    URL.revokeObjectURL(url)
  }

  return (
    <section className="application-dashboard">
      <div className="profile-heading application-dashboard-head">
        <div>
          <p className="eyebrow">APPLICATION TRACKER</p>
          <h2>지원 현황과 일정</h2>
          <p>스크랩한 공고의 지원 단계와 다음 전형일을 한곳에서 관리하세요.</p>
        </div>
        <div className="application-dashboard-actions">
          <strong>{applications.length}건 관리 중</strong>
          <button type="button" disabled={!applications.length} onClick={exportCalendar}>
            캘린더 파일 받기
          </button>
        </div>
      </div>
      <div className="application-stage-summary">
        <button className={!stageFilter ? 'active' : ''} onClick={() => setStageFilter('')}>
          <span>전체</span>
          <b>{applications.length}</b>
        </button>
        {Object.entries(STAGE_LABELS).map(([stage, label]) => (
          <button
            key={stage}
            className={stageFilter === stage ? 'active' : ''}
            onClick={() => setStageFilter(stage)}
          >
            <span>{label}</span>
            <b>{applications.filter((item) => item.stage === stage).length}</b>
          </button>
        ))}
      </div>
      <div className="application-dashboard-grid">
        <div className="application-list">
          <h3>{stageFilter ? STAGE_LABELS[stageFilter] : '전체 지원 현황'}</h3>
          {visible.map((item) => (
            <button type="button" key={item.id} onClick={() => onOpen(item)}>
              <span>{item.organization}</span>
              <strong>{item.title}</strong>
              <small>
                {STAGE_LABELS[item.stage] || '관심 공고'} · 다음 일정 {item.nextStepDate || '미정'}{' '}
                · 마감 {item.deadline || '미정'}
              </small>
              {item.memo && <em>{item.memo}</em>}
            </button>
          ))}
          {visible.length === 0 && <p className="empty">해당 단계의 공고가 없습니다.</p>}
        </div>
        <section className="alerts-calendar application-calendar" aria-label="지원 일정 달력">
          <div className="calendar-head">
            <button type="button" onClick={() => changeMonth(-1)} aria-label="이전 달">
              ‹
            </button>
            <strong>
              {year}년 {month + 1}월
            </strong>
            <button type="button" onClick={() => changeMonth(1)} aria-label="다음 달">
              ›
            </button>
          </div>
          <div className="calendar-grid calendar-weekdays">
            {['일', '월', '화', '수', '목', '금', '토'].map((day) => (
              <span key={day}>{day}</span>
            ))}
          </div>
          <div className="calendar-grid calendar-days">
            {Array.from({ length: firstWeekday }, (_, index) => (
              <span key={`application-blank-${index}`} />
            ))}
            {Array.from({ length: lastDay }, (_, index) => {
              const day = index + 1
              const key = dateKey(day)
              const events = eventsByDate[key] || []
              return (
                <button
                  type="button"
                  key={key}
                  className={`${events.length ? 'has-events' : ''} ${selectedDate === key ? 'selected' : ''}`}
                  onClick={() => events.length && setSelectedDate(key)}
                >
                  <span>{day}</span>
                  {events.length > 0 && <small>{events.length}건</small>}
                </button>
              )
            })}
          </div>
          {selectedDate && (
            <div className="calendar-events">
              <strong>{selectedDate} 일정</strong>
              {selectedEvents.map((item, index) => (
                <button
                  type="button"
                  key={`${item.id}-${item.eventType}-${index}`}
                  onClick={() => onOpen(item)}
                >
                  <span>
                    {item.eventType} · {item.organization}
                  </span>
                  <b>{item.title}</b>
                </button>
              ))}
            </div>
          )}
        </section>
      </div>
    </section>
  )
}

function CareerToolsPanel({ preference, alerts, busy, message, onChange, onSave, onRead, onOpen }) {
  const [calendarMonth, setCalendarMonth] = useState(() => {
    const today = new Date()
    return new Date(today.getFullYear(), today.getMonth(), 1)
  })
  const [selectedDate, setSelectedDate] = useState('')
  const year = calendarMonth.getFullYear()
  const month = calendarMonth.getMonth()
  const firstWeekday = new Date(year, month, 1).getDay()
  const lastDay = new Date(year, month + 1, 0).getDate()
  const dateKey = (day) =>
    `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  const eventsByDate = alerts.reduce((map, item) => {
    if (item.deadline) map[item.deadline] = [...(map[item.deadline] || []), item]
    return map
  }, {})
  const selectedEvents = selectedDate ? eventsByDate[selectedDate] || [] : []
  const changeMonth = (offset) => {
    setCalendarMonth(new Date(year, month + offset, 1))
    setSelectedDate('')
  }
  return (
    <section className="career-tools">
      <div className="profile-heading">
        <div>
          <p className="eyebrow">PERSONAL JOB ASSISTANT</p>
          <h2>스크랩 공고 마감 알림</h2>
          <p>스크랩한 공고가 설정한 날짜만큼 남았을 때 알려드립니다.</p>
        </div>
        <label className="alert-toggle">
          <input
            type="checkbox"
            checked={preference.enabled}
            onChange={(e) =>
              onChange({
                ...preference,
                enabled: e.target.checked,
                deadlineAlerts: e.target.checked,
              })
            }
          />{' '}
          알림 사용
        </label>
      </div>
      <div className="notification-options">
        <label>
          알림 시점
          <input
            value={preference.deadlineDays || '7,3,1'}
            onChange={(e) => onChange({ ...preference, deadlineDays: e.target.value })}
            placeholder="7,3,1"
            aria-label="마감 알림 일수"
          />
          <small>일 전 · 쉼표로 구분</small>
        </label>
        <button disabled={busy} onClick={onSave}>
          {busy ? '저장 중...' : '알림 설정 저장'}
        </button>
      </div>
      {message && <p role="status">{message}</p>}
      <div className="alerts-head">
        <h3>마감 알림 대상 {alerts.length}건</h3>
        {alerts.some((item) => item.fresh) && <button onClick={onRead}>모두 확인</button>}
      </div>
      <section className="alerts-calendar" aria-label="스크랩 공고 마감 달력">
        <div className="calendar-head">
          <button type="button" onClick={() => changeMonth(-1)} aria-label="이전 달">
            ‹
          </button>
          <strong>
            {year}년 {month + 1}월
          </strong>
          <button type="button" onClick={() => changeMonth(1)} aria-label="다음 달">
            ›
          </button>
        </div>
        <div className="calendar-grid calendar-weekdays">
          {['일', '월', '화', '수', '목', '금', '토'].map((day) => (
            <span key={day}>{day}</span>
          ))}
        </div>
        <div className="calendar-grid calendar-days">
          {Array.from({ length: firstWeekday }, (_, index) => (
            <span key={`blank-${index}`} />
          ))}
          {Array.from({ length: lastDay }, (_, index) => {
            const day = index + 1
            const key = dateKey(day)
            const events = eventsByDate[key] || []
            return (
              <button
                type="button"
                key={key}
                className={`${events.length ? 'has-events' : ''} ${selectedDate === key ? 'selected' : ''}`}
                onClick={() => events.length && setSelectedDate(key)}
                aria-label={`${key}${events.length ? `, 마감 공고 ${events.length}건` : ''}`}
              >
                <span>{day}</span>
                {events.length > 0 && <small>{events.length}건</small>}
              </button>
            )
          })}
        </div>
        {selectedDate && (
          <div className="calendar-events">
            <strong>{selectedDate} 마감 공고</strong>
            {selectedEvents.map((item) => (
              <button type="button" key={item.id} onClick={() => onOpen(item)}>
                <span>{item.organization}</span>
                <b>{item.title}</b>
              </button>
            ))}
          </div>
        )}
      </section>
      <div className="alerts-list">
        {alerts.slice(0, 8).map((item) => (
          <button key={item.id} className={item.fresh ? 'fresh' : ''} onClick={() => onOpen(item)}>
            <span>
              {item.reason} · {item.fresh && 'NEW · '}
              {item.organization}
            </span>
            <strong>{item.title}</strong>
            <small>
              {item.region || '지역 미정'} · 마감 {item.deadline || '미정'}
            </small>
          </button>
        ))}
        {alerts.length === 0 && <p>저장한 조건에 맞는 진행 중 공고가 없습니다.</p>}
      </div>
    </section>
  )
}

const LEGAL_CONTENT = {
  terms: {
    title: '이용약관',
    sections: [
      [
        '제1조 목적',
        '본 약관은 Public Job Hub가 제공하는 채용공고 조회, 스크랩, 역량 관리, 알림 및 커뮤니티 서비스의 이용 조건을 정합니다.',
      ],
      [
        '제2조 서비스 성격',
        '채용정보는 공공 API와 공개 웹페이지를 바탕으로 제공됩니다. 지원 전 반드시 원문을 확인해야 하며, 서비스는 채용기관의 모집·선발 결과를 보증하지 않습니다.',
      ],
      [
        '제3조 계정 관리',
        '이용자는 정확한 이메일을 사용하고 계정 정보를 안전하게 관리해야 합니다. 타인의 권리를 침해하거나 서비스를 방해하는 이용은 제한될 수 있습니다.',
      ],
      [
        '제4조 게시물과 문의',
        '문의에는 주민등록번호, 전화번호, 자격증 번호 등 불필요한 개인정보를 작성하지 않아야 합니다. 불법·권리침해 내용은 제한될 수 있습니다.',
      ],
      [
        '제5조 서비스 변경',
        '외부 제공기관의 장애, 점검 또는 정책 변경에 따라 일부 정보가 지연되거나 서비스가 변경될 수 있습니다. 중요한 변경은 공지사항으로 안내합니다.',
      ],
      [
        '제6조 문의',
        '서비스 이용, 계정 삭제 및 권리 행사는 사이트의 공지·Q&A 게시판을 통해 운영자에게 요청할 수 있습니다.',
      ],
    ],
  },
  privacy: {
    title: '개인정보 처리방침',
    sections: [
      [
        '처리 목적',
        '회원 인증, 개인화된 채용공고·알림·스크랩·역량 관리, 문의 처리, 보안 및 서비스 안정화를 위해 개인정보를 처리합니다.',
      ],
      [
        '처리 항목',
        '필수 항목은 이메일, 표시 이름, 비밀번호 해시, 약관 동의 기록입니다. 선택 입력은 학위·성적·자격증·경력·대외활동입니다. 로그인 시 접속 IP, 브라우저 정보와 접속 시각이 보안 목적으로 기록될 수 있습니다.',
      ],
      [
        '입력 금지 정보',
        '주민등록번호, 전화번호, 자격증 번호, 학위번호 등 서비스에 필요하지 않은 고유·상세 개인정보는 입력하지 않습니다.',
      ],
      [
        '보유 기간',
        '회원 정보와 이용자가 저장한 역량·스크랩 정보는 회원 탈퇴 또는 삭제 요청 처리 시까지 보유합니다. 법령상 보존 의무가 있는 경우 해당 기간 동안 분리 보관할 수 있습니다.',
      ],
      [
        '제3자 제공과 국외 처리',
        '개인정보를 판매하지 않습니다. 운영 DB와 암호화 백업은 서비스 운영을 위해 클라우드 인프라에서 처리될 수 있으며, 실제 제공업체·지역 변경 시 본 방침과 공지로 안내합니다.',
      ],
      [
        '파기와 안전조치',
        '보유 목적이 끝난 개인정보는 복구가 어렵도록 삭제합니다. 비밀번호 해시, HTTPS, 접근권한 통제, 로그인 제한, 감사 기록 및 암호화 백업을 적용합니다.',
      ],
      [
        '이용자 권리',
        '이용자는 마이페이지에서 정보를 조회·수정할 수 있으며, 열람·정정·삭제·처리정지·탈퇴 요청은 공지·Q&A를 통해 신청할 수 있습니다.',
      ],
      [
        '자동 수집 장치',
        '로그인 세션 유지와 보안을 위해 필수 쿠키를 사용합니다. 맞춤 광고용 쿠키는 사용하지 않습니다. 브라우저에서 쿠키를 차단하면 로그인 기능이 제한될 수 있습니다.',
      ],
      [
        '보호책임 및 문의',
        '개인정보 보호책임자는 Public Job Hub 운영자이며, 개인정보 관련 문의와 권리 행사는 사이트의 공지·Q&A에서 접수합니다.',
      ],
    ],
  },
  contact: {
    title: '운영자 안내',
    sections: [
      ['서비스 운영', '서비스명: Public Job Hub · 운영자: Public Job Hub 운영자'],
      [
        '문의 방법',
        '사이트의 공지·Q&A 게시판에서 서비스 문의, 개인정보 권리 행사, 계정 삭제 요청을 접수합니다.',
      ],
      [
        '계정 삭제 방법',
        '로그인 후 마이페이지의 회원 탈퇴에서 비밀번호를 확인하면 계정과 연결 개인정보를 직접 삭제할 수 있습니다. 로그인할 수 없는 경우 공지·Q&A를 통해 본인 확인 절차를 요청할 수 있습니다.',
      ],
      [
        '출시 전 확인',
        '플레이스토어 공개 전 운영자의 실명 또는 사업자명, 연락 가능한 이메일, 소재지 등 적용 대상 표시사항을 최종 입력해야 합니다.',
      ],
    ],
  },
}

function LegalDialog({ type, onClose }) {
  const content = LEGAL_CONTENT[type]
  return (
    <div className="overlay">
      <section className="legal-dialog" role="dialog" aria-modal="true">
        <button className="close" onClick={onClose}>
          ×
        </button>
        <p className="eyebrow">PUBLIC JOB HUB</p>
        <h2>{content.title}</h2>
        <p className="legal-effective">시행일 2026년 10월 18일 · 버전 2026-10-18</p>
        <div className="legal-sections">
          {content.sections.map(([title, body]) => (
            <section key={title}>
              <h3>{title}</h3>
              <p>{body}</p>
            </section>
          ))}
        </div>
        <button className="primary" onClick={onClose}>
          확인
        </button>
      </section>
    </div>
  )
}

function ConsentFields({ terms, privacy, onTerms, onPrivacy, onOpenLegal }) {
  return (
    <div className="consent-fields">
      <label>
        <input
          type="checkbox"
          checked={terms}
          onChange={(e) => onTerms(e.target.checked)}
          required
        />
        <span>
          <b>[필수]</b> 이용약관에 동의합니다.
        </span>
        <button type="button" onClick={() => onOpenLegal('terms')}>
          보기
        </button>
      </label>
      <label>
        <input
          type="checkbox"
          checked={privacy}
          onChange={(e) => onPrivacy(e.target.checked)}
          required
        />
        <span>
          <b>[필수]</b> 개인정보 처리방침에 동의합니다.
        </span>
        <button type="button" onClick={() => onOpenLegal('privacy')}>
          보기
        </button>
      </label>
    </div>
  )
}

/** 로그인·회원가입을 같은 입력 패널에서 처리하는 대화상자. */
function AuthDialog({ mode, onClose, onSubmit, onSwitch, onOpenLegal, busy, error }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [preferredJobCategory, setPreferredJobCategory] = useState('')
  const [acceptedTerms, setAcceptedTerms] = useState(false)
  const [acceptedPrivacy, setAcceptedPrivacy] = useState(false)
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
            onSubmit({
              email,
              password,
              displayName,
              preferredJobCategory,
              acceptedTerms,
              acceptedPrivacy,
            })
          }}
        >
          {mode === 'register' && (
            <>
              <label>
                이름
                <input
                  value={displayName}
                  onChange={(e) => setDisplayName(e.target.value)}
                  required
                  maxLength={80}
                />
              </label>
              <label>
                희망 직무
                <select
                  value={preferredJobCategory}
                  onChange={(e) => setPreferredJobCategory(e.target.value)}
                  required
                >
                  <option value="">선택해 주세요</option>
                  {ADMIN_CATEGORIES.map((category) => (
                    <option key={category}>{category}</option>
                  ))}
                </select>
                <small>선택한 직무의 공고를 목록에서 먼저 보여드립니다.</small>
              </label>
            </>
          )}
          <label>
            {mode === 'register' ? '이메일' : '이메일 또는 관리자 아이디'}
            <input
              type={mode === 'register' ? 'email' : 'text'}
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoComplete={mode === 'register' ? 'email' : 'username'}
            />
          </label>
          {mode === 'register' && (
            <ConsentFields
              terms={acceptedTerms}
              privacy={acceptedPrivacy}
              onTerms={setAcceptedTerms}
              onPrivacy={setAcceptedPrivacy}
              onOpenLegal={onOpenLegal}
            />
          )}
          <label>
            비밀번호
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={mode === 'register' ? 10 : 8}
              autoComplete={mode === 'register' ? 'new-password' : 'current-password'}
            />
            {mode === 'register' && <small>영문과 숫자를 포함해 10자 이상 입력해 주세요.</small>}
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
        {mode === 'login' && (
          <div className="auth-switch">
            <span>계정이 없으신가요?</span>
            <button type="button" onClick={onSwitch}>
              회원가입
            </button>
          </div>
        )}
      </section>
    </div>
  )
}

function RequiredConsentDialog({ busy, error, onSubmit, onOpenLegal }) {
  const [terms, setTerms] = useState(false)
  const [privacy, setPrivacy] = useState(false)
  return (
    <div className="overlay">
      <section className="auth-dialog" role="dialog" aria-modal="true">
        <p className="eyebrow">POLICY UPDATE</p>
        <h2>서비스 이용 동의</h2>
        <p>서비스 이용을 계속하려면 현재 약관과 개인정보 처리방침을 확인해 주세요.</p>
        <ConsentFields
          terms={terms}
          privacy={privacy}
          onTerms={setTerms}
          onPrivacy={setPrivacy}
          onOpenLegal={onOpenLegal}
        />
        {error && <p className="error">{error}</p>}
        <button
          className="primary"
          disabled={busy || !terms || !privacy}
          onClick={() => onSubmit({ acceptedTerms: terms, acceptedPrivacy: privacy })}
        >
          {busy ? '처리 중...' : '동의하고 계속'}
        </button>
      </section>
    </div>
  )
}

function PasswordDialog({ required, busy, error, onClose, onSubmit }) {
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  return (
    <div className="overlay">
      <section className="auth-dialog" role="dialog" aria-modal="true" aria-label="비밀번호 변경">
        {!required && (
          <button className="close" onClick={onClose} aria-label="닫기">
            ×
          </button>
        )}
        <p className="eyebrow">계정 보안</p>
        <h2>비밀번호 변경</h2>
        {required && <p>안전한 이용을 위해 기본 관리자 비밀번호를 먼저 변경해 주세요.</p>}
        <form
          onSubmit={(event) => {
            event.preventDefault()
            if (newPassword !== confirmPassword) return
            onSubmit({ currentPassword, newPassword })
          }}
        >
          <label>
            현재 비밀번호
            <input
              type="password"
              required
              value={currentPassword}
              onChange={(event) => setCurrentPassword(event.target.value)}
              autoComplete="current-password"
            />
          </label>
          <label>
            새 비밀번호
            <input
              type="password"
              required
              minLength={10}
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
              autoComplete="new-password"
            />
            <small>영문과 숫자를 포함해 10자 이상 입력해 주세요.</small>
          </label>
          <label>
            새 비밀번호 확인
            <input
              type="password"
              required
              minLength={10}
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              autoComplete="new-password"
            />
          </label>
          {confirmPassword && newPassword !== confirmPassword && (
            <p className="error">새 비밀번호가 서로 다릅니다.</p>
          )}
          {error && (
            <p className="error" role="alert">
              {error}
            </p>
          )}
          <button className="primary" disabled={busy || newPassword !== confirmPassword}>
            {busy ? '변경 중...' : '비밀번호 변경'}
          </button>
        </form>
      </section>
    </div>
  )
}

function AccountDeletionDialog({ busy, error, onClose, onSubmit }) {
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  return (
    <div className="overlay">
      <form
        className="account-deletion-dialog"
        onSubmit={(event) => {
          event.preventDefault()
          onSubmit({ password, confirmation })
        }}
      >
        <button type="button" className="close" onClick={onClose}>
          ×
        </button>
        <p className="eyebrow">ACCOUNT DELETION</p>
        <h2>회원 탈퇴</h2>
        <div className="deletion-warning">
          <strong>탈퇴하면 다음 정보가 삭제됩니다.</strong>
          <p>계정·역량·스크랩·지원 일정·알림 설정·문의 및 로그인 기록</p>
          <small>
            삭제된 정보는 복구할 수 없습니다. 공고 원본과 익명 운영 감사 기록은 남을 수 있습니다.
          </small>
        </div>
        <label>
          현재 비밀번호
          <input
            type="password"
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="current-password"
          />
        </label>
        <label>
          확인 문구
          <input
            required
            value={confirmation}
            onChange={(event) => setConfirmation(event.target.value)}
            placeholder="탈퇴합니다"
          />
          <small>
            <b>탈퇴합니다</b>를 정확히 입력해 주세요.
          </small>
        </label>
        {error && <p className="error">{error}</p>}
        <button className="danger" disabled={busy || confirmation !== '탈퇴합니다'}>
          {busy ? '삭제 중...' : '계정과 개인정보 영구 삭제'}
        </button>
      </form>
    </div>
  )
}

const QUALIFICATION_TYPE_LABELS = {
  NATIONAL_TECHNICAL: '국가기술',
  NATIONAL_PROFESSIONAL: '국가전문',
  ACCREDITED_PRIVATE: '국가공인 민간',
}

function QualificationSearchInput({ item, onChange }) {
  const [results, setResults] = useState([])
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState('')
  useEffect(() => {
    if (!open || query.trim().length < 2) {
      setResults([])
      return
    }
    const controller = new AbortController()
    const timer = window.setTimeout(
      () =>
        getJson(`/api/qualifications?q=${encodeURIComponent(query.trim())}`, {
          signal: controller.signal,
        })
          .then(setResults)
          .catch(() => {}),
      250,
    )
    return () => {
      window.clearTimeout(timer)
      controller.abort()
    }
  }, [open, query])
  function showSearch() {
    setQuery(item.name || '')
    setResults([])
    setOpen(true)
  }
  function select(result) {
    onChange({
      ...item,
      catalogId: result.id,
      name: result.name,
      type: result.type,
      issuer: result.issuer,
    })
    setOpen(false)
  }
  return (
    <div className="qualification-search">
      <input
        value={item.name || ''}
        readOnly
        placeholder="눌러서 자격증 검색"
        onClick={showSearch}
        onFocus={showSearch}
        aria-haspopup="dialog"
      />
      {item.type && (
        <small className="qualification-selected">
          {QUALIFICATION_TYPE_LABELS[item.type] || item.type}
          {item.issuer && ` · ${item.issuer}`}
        </small>
      )}
      {open && (
        <div
          className="match-overlay qualification-overlay"
          onMouseDown={(e) => e.target === e.currentTarget && setOpen(false)}
          onClick={(e) => e.stopPropagation()}
        >
          <section
            className="qualification-dialog"
            role="dialog"
            aria-modal="true"
            aria-label="자격증 검색 및 등록"
          >
            <div className="match-dialog-head">
              <div>
                <p className="eyebrow">QUALIFICATION SEARCH</p>
                <h2>자격증 검색·등록</h2>
              </div>
              <button className="close" type="button" onClick={() => setOpen(false)}>
                ×
              </button>
            </div>
            <input
              className="qualification-dialog-input"
              value={query}
              maxLength={120}
              autoComplete="off"
              autoFocus
              placeholder="자격증명을 두 글자 이상 입력하세요"
              onChange={(e) => setQuery(e.target.value)}
            />
            <div className="qualification-dialog-results">
              {query.trim().length < 2 && <p>두 글자 이상 입력하면 공식 자격증을 검색합니다.</p>}
              {query.trim().length >= 2 && results.length === 0 && (
                <p>일치하는 공식 자격증이 없습니다.</p>
              )}
              {results.map((result) => (
                <button type="button" key={result.id} onClick={() => select(result)}>
                  <strong>{result.name}</strong>
                  <span>
                    {QUALIFICATION_TYPE_LABELS[result.type]}
                    {result.issuer && ` · ${result.issuer}`}
                  </span>
                  <em>등록</em>
                </button>
              ))}
            </div>
            {query.trim().length >= 2 && (
              <button
                className="qualification-manual"
                type="button"
                onClick={() => select({ id: null, name: query.trim(), type: '', issuer: '' })}
              >
                목록에 없으면 “{query.trim()}” 직접 등록
              </button>
            )}
          </section>
        </div>
      )}
    </div>
  )
}

/** 개인정보를 최소화한 회원 스펙 입력 폼. */
function ProfileEditor({ profile, busy, message, onSave }) {
  const blankCertification = () => ({
    catalogId: null,
    name: '',
    type: '',
    issuer: '',
    grade: '',
    acquiredDate: '',
    acquiredMonth: '',
  })
  const blankActivity = () => ({ name: '', description: '', startMonth: '', endMonth: '' })
  const blankCareer = () => ({
    companyName: '',
    position: '',
    duties: '',
    startMonth: '',
    endMonth: '',
  })
  const blankDegree = () => ({
    schoolName: '',
    major: '',
    degreeType: '',
    startMonth: '',
    endMonth: '',
    status: '',
  })
  const prepare = (value) => ({
    ...value,
    certifications: value.certifications?.length
      ? value.certifications.map((item) => ({ ...blankCertification(), ...item }))
      : [blankCertification()],
    activities: value.activities?.length ? value.activities : [blankActivity()],
    careers: value.careers?.length ? value.careers : [blankCareer()],
    degrees: value.degrees?.length ? value.degrees : [blankDegree()],
  })
  const [form, setForm] = useState(() => prepare(profile))
  useEffect(() => setForm(prepare(profile)), [profile])
  const change = (field) => (event) => setForm({ ...form, [field]: event.target.value })
  const itemChange = (group, index, field) => (event) => {
    const items = [...form[group]]
    items[index] = { ...items[index], [field]: event.target.value }
    setForm({ ...form, [group]: items })
  }
  const add = (group, blank) => setForm({ ...form, [group]: [...form[group], blank()] })
  const remove = (group, index, blank) => {
    const items = form[group].filter((_, itemIndex) => itemIndex !== index)
    setForm({ ...form, [group]: items.length ? items : [blank()] })
  }
  const sectionHead = (title, hint, group, blank) => (
    <div className="spec-section-head">
      <div>
        <h3>{title}</h3>
        {hint && <small>{hint}</small>}
      </div>
      <button type="button" onClick={() => add(group, blank)}>
        ＋ 추가
      </button>
    </div>
  )
  return (
    <section className="profile-editor" aria-labelledby="profile-title">
      <div className="profile-heading">
        <div>
          <p className="eyebrow">PRIVATE CAREER PROFILE</p>
          <h2 id="profile-title">내 스펙</h2>
        </div>
        <p className="privacy-note">
          전화번호·이메일·주민번호·자격증 번호·학위 번호·상세주소는 입력하지 마세요.
        </p>
      </div>
      <form
        onSubmit={(event) => {
          event.preventDefault()
          onSave(form)
        }}
      >
        <label>
          이름
          <input value={form.displayName || ''} disabled />
        </label>
        <label>
          생년월일 <small>선택</small>
          <input type="date" value={form.birthDate || ''} onChange={change('birthDate')} />
        </label>
        <div className="spec-section wide">
          {sectionHead(
            '자격증',
            '자격증 번호는 입력하지 마세요.',
            'certifications',
            blankCertification,
          )}
          {form.certifications.map((item, index) => (
            <div className="spec-item spec-grid-3" key={`certification-${index}`}>
              <label>
                1. 자격증 검색·등록
                <QualificationSearchInput
                  item={item}
                  onChange={(next) => {
                    const items = [...form.certifications]
                    items[index] = next
                    setForm({ ...form, certifications: items })
                  }}
                />
              </label>
              <label>
                2. 급수
                <input
                  value={item.grade || ''}
                  onChange={itemChange('certifications', index, 'grade')}
                  maxLength={50}
                  placeholder="예: 1급, 기사"
                />
              </label>
              <label>
                3. 취득연월일
                <input
                  type="date"
                  value={item.acquiredDate || ''}
                  onChange={itemChange('certifications', index, 'acquiredDate')}
                />
                {!item.acquiredDate && item.acquiredMonth && (
                  <small>기존 취득연월: {item.acquiredMonth} · 정확한 일자를 선택해 주세요.</small>
                )}
              </label>
              <button
                className="remove-spec"
                type="button"
                onClick={() => remove('certifications', index, blankCertification)}
              >
                삭제
              </button>
            </div>
          ))}
        </div>
        <div className="spec-section wide">
          {sectionHead('대외활동', '', 'activities', blankActivity)}
          {form.activities.map((item, index) => (
            <div className="spec-item spec-grid-2" key={`activity-${index}`}>
              <label>
                활동명
                <input
                  value={item.name || ''}
                  onChange={itemChange('activities', index, 'name')}
                  maxLength={160}
                />
              </label>
              <div className="month-range">
                <label>
                  시작연월
                  <input
                    type="month"
                    value={item.startMonth || ''}
                    onChange={itemChange('activities', index, 'startMonth')}
                  />
                </label>
                <label>
                  종료연월
                  <input
                    type="month"
                    value={item.endMonth || ''}
                    onChange={itemChange('activities', index, 'endMonth')}
                  />
                </label>
              </div>
              <label className="item-wide">
                활동내용
                <textarea
                  value={item.description || ''}
                  onChange={itemChange('activities', index, 'description')}
                  maxLength={1500}
                />
              </label>
              <button
                className="remove-spec"
                type="button"
                onClick={() => remove('activities', index, blankActivity)}
              >
                삭제
              </button>
            </div>
          ))}
        </div>
        <div className="spec-section wide">
          {sectionHead('경력', '', 'careers', blankCareer)}
          {form.careers.map((item, index) => (
            <div className="spec-item spec-grid-2" key={`career-${index}`}>
              <label>
                회사명
                <input
                  value={item.companyName || ''}
                  onChange={itemChange('careers', index, 'companyName')}
                  maxLength={160}
                />
              </label>
              <label>
                직책
                <input
                  value={item.position || ''}
                  onChange={itemChange('careers', index, 'position')}
                  maxLength={120}
                />
              </label>
              <div className="month-range item-wide">
                <label>
                  시작연월
                  <input
                    type="month"
                    value={item.startMonth || ''}
                    onChange={itemChange('careers', index, 'startMonth')}
                  />
                </label>
                <label>
                  종료연월
                  <input
                    type="month"
                    value={item.endMonth || ''}
                    onChange={itemChange('careers', index, 'endMonth')}
                  />
                </label>
              </div>
              <label className="item-wide">
                담당업무
                <textarea
                  value={item.duties || ''}
                  onChange={itemChange('careers', index, 'duties')}
                  maxLength={2000}
                />
              </label>
              <button
                className="remove-spec"
                type="button"
                onClick={() => remove('careers', index, blankCareer)}
              >
                삭제
              </button>
            </div>
          ))}
        </div>
        <div className="spec-section wide">
          {sectionHead('학위', '학위 번호는 입력하지 마세요.', 'degrees', blankDegree)}
          {form.degrees.map((item, index) => (
            <div className="spec-item spec-grid-3" key={`degree-${index}`}>
              <label>
                학교명
                <input
                  value={item.schoolName || ''}
                  onChange={itemChange('degrees', index, 'schoolName')}
                  maxLength={160}
                />
              </label>
              <label>
                전공
                <input
                  value={item.major || ''}
                  onChange={itemChange('degrees', index, 'major')}
                  maxLength={160}
                />
              </label>
              <label>
                학위 종류
                <input
                  value={item.degreeType || ''}
                  onChange={itemChange('degrees', index, 'degreeType')}
                  maxLength={100}
                  placeholder="예: 학사"
                />
              </label>
              <label>
                입학연월
                <input
                  type="month"
                  value={item.startMonth || ''}
                  onChange={itemChange('degrees', index, 'startMonth')}
                />
              </label>
              <label>
                졸업연월
                <input
                  type="month"
                  value={item.endMonth || ''}
                  onChange={itemChange('degrees', index, 'endMonth')}
                />
              </label>
              <label>
                졸업 상태
                <select value={item.status || ''} onChange={itemChange('degrees', index, 'status')}>
                  <option value="">선택</option>
                  <option>재학</option>
                  <option>졸업예정</option>
                  <option>졸업</option>
                  <option>수료</option>
                  <option>중퇴</option>
                </select>
              </label>
              <button
                className="remove-spec"
                type="button"
                onClick={() => remove('degrees', index, blankDegree)}
              >
                삭제
              </button>
            </div>
          ))}
        </div>
        <label>
          성적
          <textarea
            value={form.grades || ''}
            onChange={change('grades')}
            maxLength={1000}
            placeholder="예: 3.8 / 4.5"
          />
        </label>
        <div className="profile-submit wide">
          {message && <span role="status">{message}</span>}
          <button disabled={busy}>{busy ? '저장 중...' : '내 스펙 저장'}</button>
        </div>
      </form>
    </section>
  )
}

const ADMIN_CATEGORIES = [
  '행정·사무',
  '전산·IT',
  '회계·재무',
  '토목',
  '건축',
  '전기',
  '기계',
  '연구',
  '의료·보건',
  '사회복지',
]

const POSTING_CHANGE_TYPES = [
  ['TITLE', '공고 제목'],
  ['DEADLINE', '지원 마감일'],
  ['REGION', '근무 지역'],
  ['EMPLOYMENT_TYPE', '고용 형태'],
  ['SOURCE_URL', '원문·첨부 링크'],
  ['HEADCOUNT', '채용 인원'],
  ['REQUIREMENTS', '지원 자격'],
  ['POSITIONS', '채용 직렬'],
]

function AdminRetentionPanel({ securePost }) {
  const [policy, setPolicy] = useState({
    enabled: true,
    postingChangeDays: 180,
    notificationStateDays: 90,
  })
  const [dates, setDates] = useState({ postingChangesBefore: '', notificationStatesBefore: '' })
  const [preview, setPreview] = useState(null)
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    getJson('/api/admin/data-retention')
      .then(setPolicy)
      .catch((e) => setMessage(e.message))
  }, [])
  const save = async () => {
    setBusy(true)
    try {
      setPolicy(
        await securePost('/api/admin/data-retention', JSON.stringify(policy), 'application/json'),
      )
      setMessage('자동 정리 정책을 저장했습니다.')
    } catch (e) {
      setMessage(e.message)
    } finally {
      setBusy(false)
    }
  }
  const requestBody = () => JSON.stringify(dates)
  const inspect = async () => {
    if (!dates.postingChangesBefore || !dates.notificationStatesBefore) return
    try {
      const result = await securePost(
        '/api/admin/data-retention/preview',
        requestBody(),
        'application/json',
      )
      setPreview(result)
      setMessage('삭제 전 예상 건수를 확인했습니다.')
    } catch (e) {
      setMessage(e.message)
    }
  }
  const cleanup = async () => {
    if (!preview) return
    const total = preview.postingChanges + preview.notificationStates
    if (
      !window.confirm(`확인한 오래된 데이터 ${total}건을 삭제할까요? 이 작업은 되돌릴 수 없습니다.`)
    )
      return
    setBusy(true)
    try {
      const result = await securePost(
        '/api/admin/data-retention/cleanup',
        JSON.stringify({
          ...dates,
          expectedPostingChanges: preview.postingChanges,
          expectedNotificationStates: preview.notificationStates,
        }),
        'application/json',
      )
      setPreview(null)
      setMessage(
        `변경 이력 ${result.postingChanges}건, 알림 상태 ${result.notificationStates}건을 정리했습니다.`,
      )
      setPolicy(await getJson('/api/admin/data-retention'))
    } catch (e) {
      setMessage(e.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="admin-retention" aria-label="데이터 보관 정책">
      <div className="admin-retention-heading">
        <div>
          <h2>데이터 보관 정책</h2>
          <p>오래된 변경 이력과 알림 상태를 자동 또는 수동으로 정리합니다.</p>
        </div>
        <label>
          <input
            type="checkbox"
            checked={policy.enabled}
            onChange={(e) => setPolicy({ ...policy, enabled: e.target.checked })}
          />
          자동 정리
        </label>
      </div>
      <div className="admin-retention-policy">
        <label>
          공고 변경 이력 보관
          <input
            type="number"
            min="30"
            max="3650"
            value={policy.postingChangeDays}
            onChange={(e) => setPolicy({ ...policy, postingChangeDays: Number(e.target.value) })}
          />
          <span>일</span>
        </label>
        <label>
          알림 읽음·삭제 상태 보관
          <input
            type="number"
            min="30"
            max="3650"
            value={policy.notificationStateDays}
            onChange={(e) =>
              setPolicy({ ...policy, notificationStateDays: Number(e.target.value) })
            }
          />
          <span>일</span>
        </label>
        <button type="button" disabled={busy} onClick={save}>
          정책 저장
        </button>
      </div>
      {policy.lastRunAt && (
        <p className="retention-last-run">
          최근 정리 {new Date(policy.lastRunAt).toLocaleString('ko-KR')} · 변경 이력{' '}
          {policy.lastPostingChangesDeleted}건 · 알림 상태 {policy.lastNotificationStatesDeleted}건
        </p>
      )}
      <div className="admin-retention-manual">
        <strong>수동 정리</strong>
        <label>
          변경 이력 기준일
          <input
            type="date"
            value={dates.postingChangesBefore}
            onChange={(e) => {
              setDates({ ...dates, postingChangesBefore: e.target.value })
              setPreview(null)
            }}
          />
        </label>
        <label>
          알림 상태 기준일
          <input
            type="date"
            value={dates.notificationStatesBefore}
            onChange={(e) => {
              setDates({ ...dates, notificationStatesBefore: e.target.value })
              setPreview(null)
            }}
          />
        </label>
        <button type="button" onClick={inspect}>
          삭제 전 확인
        </button>
        <button
          className="danger-button"
          type="button"
          disabled={!preview || busy}
          onClick={cleanup}
        >
          확인한 데이터 정리
        </button>
      </div>
      {preview && (
        <div className="retention-preview">
          삭제 예정: 변경 이력 <strong>{preview.postingChanges}건</strong> · 알림 상태{' '}
          <strong>{preview.notificationStates}건</strong>
        </div>
      )}
      {message && <p className="status">{message}</p>}
    </section>
  )
}

/** 전체 변경 이력을 검색하고 실제 원문과 비교해 오탐 기록을 정리한다. */
function AdminPostingChanges({ securePost }) {
  const [data, setData] = useState({ items: [], total: 0, page: 0, totalPages: 0 })
  const [queryInput, setQueryInput] = useState('')
  const [query, setQuery] = useState('')
  const [type, setType] = useState('')
  const [importance, setImportance] = useState('')
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState([])
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState('')

  const load = useCallback(() => {
    setLoading(true)
    const params = new URLSearchParams({ q: query, type, importance, page: String(page) })
    getJson(`/api/admin/posting-changes?${params}`)
      .then((value) => {
        setData(value)
        setSelected([])
        setMessage('')
      })
      .catch((e) => setMessage(e.message))
      .finally(() => setLoading(false))
  }, [importance, page, query, type])
  useEffect(load, [load])

  const search = (event) => {
    event.preventDefault()
    setPage(0)
    setQuery(queryInput.trim())
  }
  const toggle = (id) =>
    setSelected((values) =>
      values.includes(id) ? values.filter((value) => value !== id) : [...values, id],
    )
  const toggleAll = () =>
    setSelected((values) =>
      values.length === data.items.length ? [] : data.items.map((item) => item.id),
    )
  const removeSelected = async () => {
    if (!selected.length || !window.confirm(`선택한 변경 이력 ${selected.length}건을 삭제할까요?`))
      return
    try {
      const result = await securePost(
        '/api/admin/posting-changes/delete',
        JSON.stringify({ ids: selected }),
        'application/json',
      )
      setMessage(`오탐 변경 이력 ${result.deleted}건을 삭제했습니다.`)
      load()
    } catch (e) {
      setMessage(e.message)
    }
  }
  const displayValue = (value) => value || '정보 없음'

  return (
    <section className="admin-change-history" aria-label="공고 변경 이력 관리">
      <div className="admin-change-heading">
        <div>
          <h2>공고 변경 이력</h2>
          <p>감지된 변경을 원문과 비교하고 오탐 기록을 정리합니다.</p>
        </div>
        <strong>전체 {data.total}건</strong>
      </div>
      <form className="admin-change-filters" onSubmit={search}>
        <input
          value={queryInput}
          onChange={(e) => setQueryInput(e.target.value)}
          placeholder="기관명, 공고명, 변경 항목 검색"
        />
        <select
          value={type}
          onChange={(e) => {
            setType(e.target.value)
            setPage(0)
          }}
        >
          <option value="">전체 변경 항목</option>
          {POSTING_CHANGE_TYPES.map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
        <select
          value={importance}
          onChange={(e) => {
            setImportance(e.target.value)
            setPage(0)
          }}
        >
          <option value="">전체 중요도</option>
          <option value="IMPORTANT">중요</option>
          <option value="NORMAL">일반</option>
        </select>
        <button>검색</button>
        <button
          className="danger-button"
          type="button"
          disabled={!selected.length}
          onClick={removeSelected}
        >
          선택 삭제{selected.length ? ` (${selected.length})` : ''}
        </button>
      </form>
      {message && <p className="status">{message}</p>}
      {!loading && data.items.length > 0 && (
        <label className="admin-change-select-all">
          <input
            type="checkbox"
            checked={selected.length === data.items.length}
            onChange={toggleAll}
          />
          현재 페이지 전체 선택
        </label>
      )}
      <div className="admin-change-list">
        {data.items.map((item) => (
          <article key={item.id}>
            <input
              type="checkbox"
              aria-label={`${item.title} 변경 이력 선택`}
              checked={selected.includes(item.id)}
              onChange={() => toggle(item.id)}
            />
            <div className="admin-change-summary">
              <span>{item.organization}</span>
              <strong>{item.title}</strong>
              <time>{new Date(item.detectedAt).toLocaleString('ko-KR')}</time>
            </div>
            <div className="admin-change-badges">
              <b>{item.fieldLabel}</b>
              <b
                className={`change-importance ${item.importance === 'IMPORTANT' ? 'important' : ''}`}
              >
                {item.importance === 'IMPORTANT' ? '중요' : '일반'}
              </b>
            </div>
            <div className="admin-change-compare">
              <p>
                <span>변경 전</span>
                {displayValue(item.oldValue)}
              </p>
              <p>
                <span>변경 후</span>
                {displayValue(item.newValue)}
              </p>
            </div>
            <a href={item.sourceUrl} target="_blank" rel="noreferrer">
              원문 비교 ↗
            </a>
          </article>
        ))}
        {loading && <p className="empty">변경 이력을 불러오는 중...</p>}
        {!loading && !data.items.length && (
          <p className="empty">조건에 맞는 변경 이력이 없습니다.</p>
        )}
      </div>
      {data.totalPages > 1 && (
        <div className="pagination">
          <button disabled={page === 0} onClick={() => setPage((value) => value - 1)}>
            ← 이전
          </button>
          <span>
            {page + 1} / {data.totalPages} 페이지
          </span>
          <button
            disabled={page + 1 >= data.totalPages}
            onClick={() => setPage((value) => value + 1)}
          >
            다음 →
          </button>
        </div>
      )}
    </section>
  )
}

/** 관리자가 자동 추출 결과를 원문과 대조하고 공고별 분류를 바로 정정하는 화면. */
function AdminOperationsLogs({ securePost }) {
  const [reports, setReports] = useState([])
  const [logs, setLogs] = useState([])
  const load = () =>
    Promise.all([getJson('/api/admin/posting-reports'), getJson('/api/admin/audit-logs')])
      .then(([nextReports, nextLogs]) => {
        setReports(nextReports)
        setLogs(nextLogs)
      })
      .catch(() => {})
  useEffect(() => {
    load()
  }, [])
  const resolve = async (item, status) => {
    await securePost(
      `/api/admin/posting-reports/${item.id}`,
      JSON.stringify({ status, adminNote: item.adminNote || '' }),
      'application/json',
    )
    load()
  }
  return (
    <section className="admin-operations-logs">
      <div>
        <h2>공고 신고 처리</h2>
        {reports.slice(0, 20).map((item) => (
          <article key={item.id}>
            <span>{item.status}</span>
            <strong>
              {item.organization} · {item.title}
            </strong>
            <p>{item.content}</p>
            <select value={item.status} onChange={(e) => resolve(item, e.target.value)}>
              <option value="RECEIVED">접수</option>
              <option value="CHECKING">확인 중</option>
              <option value="RESOLVED">처리 완료</option>
            </select>
          </article>
        ))}
        {!reports.length && <p className="empty">접수된 신고가 없습니다.</p>}
      </div>
      <div>
        <h2>관리자 감사 로그</h2>
        {logs.slice(0, 30).map((item) => (
          <article key={item.id}>
            <span>{item.action}</span>
            <strong>{item.target}</strong>
            <small>
              {item.administrator} · {new Date(item.createdAt).toLocaleString('ko-KR')} · HTTP{' '}
              {item.status}
            </small>
          </article>
        ))}
      </div>
    </section>
  )
}

function AdminTrendChart({ daily }) {
  const width = 760
  const height = 250
  const left = 42
  const right = 18
  const top = 18
  const bottom = 38
  const metrics = [
    ['postings', '공고 수집', '#1768b7'],
    ['users', '회원 가입', '#15825f'],
    ['scraps', '스크랩', '#d0711d'],
    ['inquiries', '문의', '#8b55b6'],
  ]
  const maximum = Math.max(1, ...daily.flatMap((point) => metrics.map(([key]) => point[key])))
  const x = (index) => left + (index * (width - left - right)) / Math.max(1, daily.length - 1)
  const y = (value) => top + (1 - value / maximum) * (height - top - bottom)
  return (
    <div className="admin-trend-chart">
      <div className="chart-legend">
        {metrics.map(([key, label, color]) => (
          <span key={key}>
            <i style={{ background: color }} />
            {label}
          </span>
        ))}
      </div>
      <svg viewBox={`0 0 ${width} ${height}`} role="img" aria-label="최근 14일 운영 추이">
        {[0, 0.5, 1].map((ratio) => (
          <g key={ratio}>
            <line x1={left} x2={width - right} y1={y(maximum * ratio)} y2={y(maximum * ratio)} />
            <text x={left - 8} y={y(maximum * ratio) + 4} textAnchor="end">
              {Math.round(maximum * ratio)}
            </text>
          </g>
        ))}
        {metrics.map(([key, label, color]) => (
          <polyline
            key={key}
            aria-label={label}
            fill="none"
            stroke={color}
            strokeWidth="3"
            strokeLinejoin="round"
            strokeLinecap="round"
            points={daily.map((point, index) => `${x(index)},${y(point[key])}`).join(' ')}
          />
        ))}
        {daily.map((point, index) => (
          <text key={point.date} x={x(index)} y={height - 12} textAnchor="middle">
            {index % 2 === 0 || index === daily.length - 1
              ? point.date.slice(5).replace('-', '/')
              : ''}
          </text>
        ))}
      </svg>
    </div>
  )
}

function AdminReviewPanel({ user, securePost }) {
  const [section, setSection] = useState('overview')
  const [items, setItems] = useState([])
  const [quality, setQuality] = useState(null)
  const [dashboard, setDashboard] = useState(null)
  const [operationsStatus, setOperationsStatus] = useState(null)
  const [statistics, setStatistics] = useState(null)
  const [securityOverview, setSecurityOverview] = useState(null)
  const [busy, setBusy] = useState(null)
  const [message, setMessage] = useState('')
  const [crawlInfo, setCrawlInfo] = useState(null)
  const [qualificationFile, setQualificationFile] = useState(null)
  const [qualificationType, setQualificationType] = useState('AUTO')
  const [qualificationEncoding, setQualificationEncoding] = useState('AUTO')
  const [compensationFile, setCompensationFile] = useState(null)
  const loadReviews = useCallback(
    () =>
      Promise.all([
        getJson('/api/admin/reviews'),
        getJson('/api/admin/reviews/quality'),
        getJson('/api/admin/dashboard'),
        getJson('/api/admin/operations/status'),
        getJson('/api/admin/statistics'),
        getJson('/api/admin/security/overview'),
      ])
        .then(
          ([
            reviews,
            report,
            dashboardReport,
            operationsReport,
            statisticsReport,
            securityReport,
          ]) => {
            setItems(reviews)
            setQuality(report)
            setDashboard(dashboardReport)
            setOperationsStatus(operationsReport)
            setStatistics(statisticsReport)
            setSecurityOverview(securityReport)
          },
        )
        .catch((e) => setMessage(e.message)),
    [],
  )
  useEffect(() => {
    loadReviews()
    let wasRunning = false
    const refresh = () =>
      getJson('/api/admin/crawl')
        .then((next) => {
          if (wasRunning && !next.running) loadReviews()
          wasRunning = next.running
          setCrawlInfo(next)
        })
        .catch((e) => setMessage(e.message))
    refresh()
    const timer = window.setInterval(refresh, 3000)
    return () => window.clearInterval(timer)
  }, [loadReviews])
  async function startCrawl() {
    setBusy('crawl')
    setMessage('')
    try {
      setCrawlInfo(await securePost('/api/admin/crawl'))
      setMessage('공고 수집을 시작했습니다. 완료될 때까지 이 화면에서 상태를 확인할 수 있습니다.')
    } catch (e) {
      setMessage(e.message)
    } finally {
      setBusy(null)
    }
  }
  async function retryPublicRecruitment() {
    setBusy('public-recruitment')
    setMessage('')
    try {
      const apiStatus = await securePost('/api/admin/public-recruitment')
      setCrawlInfo((current) => ({ ...(current || {}), apiStatus }))
      await loadReviews()
      setMessage(
        apiStatus.error
          ? `공식 공고 재처리를 마쳤지만 확인이 필요합니다: ${apiStatus.error}`
          : `공식 공고 ${apiStatus.lastCount || 0}건을 다시 반영했습니다.`,
      )
    } catch (e) {
      setMessage(e.message)
    } finally {
      setBusy(null)
    }
  }
  async function importQualifications(event) {
    event.preventDefault()
    if (!qualificationFile) return
    const formElement = event.currentTarget
    setMessage('')
    const form = new FormData()
    form.append('file', qualificationFile)
    try {
      const result = await securePost(
        `/api/admin/qualifications/import?type=${qualificationType}&encoding=${qualificationEncoding}`,
        form,
      )
      setMessage(`자격증 ${result.imported}건을 반영했고 ${result.skipped}건을 건너뛰었습니다.`)
      setQualificationFile(null)
      formElement.reset()
    } catch (e) {
      setMessage(e.message)
    }
  }
  async function importCompensations(event) {
    event.preventDefault()
    if (!compensationFile) return
    const formElement = event.currentTarget
    const form = new FormData()
    form.append('file', compensationFile)
    setMessage('')
    try {
      const result = await securePost('/api/admin/compensations/upload', form)
      const unmatched = result.unmatchedNames?.length
        ? ` 미매칭: ${result.unmatchedNames.join(', ')}`
        : ''
      setMessage(
        `초임 ${result.savedRows}건, 기관 ${result.matchedInstitutions}곳을 저장했습니다.${unmatched}`,
      )
      setCompensationFile(null)
      formElement.reset()
    } catch (e) {
      setMessage(e.message)
    }
  }
  const change = (id, field, value) =>
    setItems((rows) => rows.map((row) => (row.id === id ? { ...row, [field]: value } : row)))
  const changePosition = (id, index, field, value) =>
    setItems((rows) =>
      rows.map((row) =>
        row.id === id
          ? {
              ...row,
              positions: row.positions.map((position, i) =>
                i === index ? { ...position, [field]: value } : position,
              ),
            }
          : row,
      ),
    )
  const addPosition = (id) =>
    change(id, 'positions', [
      ...items.find((row) => row.id === id).positions,
      {
        standardCategory: '행정·사무',
        originalName: '',
        headcount: '',
        workRegion: '',
        requirements: '',
      },
    ])
  const removePosition = (id, index) =>
    change(
      id,
      'positions',
      items.find((row) => row.id === id).positions.filter((_, i) => i !== index),
    )
  async function save(item) {
    setBusy(item.id)
    setMessage('')
    try {
      const saved = await securePost(
        `/api/admin/reviews/${item.id}`,
        JSON.stringify({
          region: item.region || null,
          employmentType: item.employmentType || null,
          organizationType: item.organizationType,
          mobilityType: item.mobilityType,
          positions: item.positions.map(
            ({ standardCategory, originalName, headcount, workRegion, requirements }) => ({
              standardCategory,
              originalName,
              headcount: headcount === '' ? null : Number(headcount),
              workRegion,
              requirements,
            }),
          ),
        }),
        'application/json',
      )
      setItems((rows) => rows.map((row) => (row.id === item.id ? saved : row)))
      setMessage(`${item.organization} 공고를 저장했습니다.`)
    } catch (e) {
      setMessage(e.message)
    } finally {
      setBusy(null)
    }
  }
  const operationIssues = [
    ...(crawlInfo?.status?.error
      ? [
          {
            key: 'crawl',
            title: '전체 공고 수집 오류',
            detail: crawlInfo.status.error,
            action: '전체 수집을 다시 실행하고 완료 상태를 확인하세요.',
          },
        ]
      : []),
    ...(crawlInfo?.apiStatus?.error
      ? [
          {
            key: 'public-api',
            title: '공식 채용정보 처리 오류',
            detail: crawlInfo.apiStatus.error,
            action: '공식 공고 재처리 버튼으로 해당 수집만 다시 실행하세요.',
          },
        ]
      : []),
    ...(operationsStatus?.jobs || [])
      .filter((job) => job.state !== 'healthy' || job.stale)
      .map((job) => ({
        key: job.key,
        title: `${job.label} ${job.state === 'failed' ? '실패' : '점검 필요'}`,
        detail: job.summary,
        action:
          job.state === 'failed'
            ? 'Discord 알림의 오류 내용과 서버 로그를 확인한 뒤 해당 스크립트를 다시 실행하세요.'
            : '예약 작업이 정상 등록되어 있는지 확인하거나 서버에서 해당 점검을 한 번 실행하세요.',
      })),
  ]
  return (
    <section className="admin-review">
      <div className="admin-title">
        <div>
          <p className="eyebrow">운영 관리</p>
          <h1>관리자 통합 운영센터</h1>
        </div>
        <div className="admin-crawl-actions">
          <span>
            {crawlInfo?.running
              ? '공고 수집 중…'
              : crawlInfo?.status?.lastSuccess
                ? `마지막 수집 ${new Date(crawlInfo.status.lastSuccess).toLocaleString('ko-KR')}`
                : '수집 기록 없음'}
          </span>
          {crawlInfo?.apiStatus?.error && (
            <span className="status-error">확인 필요: {crawlInfo.apiStatus.error}</span>
          )}
          {crawlInfo?.apiStatus?.lastSuccess && (
            <span>채용정보 {crawlInfo.apiStatus.lastCount}건 업데이트</span>
          )}
          <button type="button" disabled={crawlInfo?.running} onClick={startCrawl}>
            {crawlInfo?.running ? '수집 중…' : '공고 지금 수집'}
          </button>
        </div>
      </div>
      <nav className="admin-console-tabs" aria-label="관리자 운영 메뉴">
        {[
          ['overview', '운영 현황'],
          ['statistics', '운영 통계'],
          [
            'security',
            `운영 보안${securityOverview?.failedLogins24h || securityOverview?.passwordChangeRequired ? ' !' : ''}`,
          ],
          ['incidents', `장애 대응${operationIssues.length ? ` ${operationIssues.length}` : ''}`],
          ['collection', '수집 관리'],
          [
            'review',
            `공고 검수${dashboard?.qualityAttention ? ` ${dashboard.qualityAttention}` : ''}`,
          ],
          ['changes', '변경·신고'],
          [
            'support',
            `공지·문의${dashboard?.waitingInquiries ? ` ${dashboard.waitingInquiries}` : ''}`,
          ],
          ['data', '데이터 관리'],
        ].map(([value, label]) => (
          <button
            key={value}
            type="button"
            className={section === value ? 'active' : ''}
            aria-current={section === value ? 'page' : undefined}
            onClick={() => setSection(value)}
          >
            {label}
          </button>
        ))}
      </nav>
      {section === 'overview' && dashboard && (
        <section className="operations-dashboard" aria-label="서비스 운영 현황">
          <div className="operations-heading">
            <div>
              <h2>운영 현황</h2>
              <span>{new Date(dashboard.checkedAt).toLocaleString('ko-KR')} 기준</span>
            </div>
            <button type="button" onClick={loadReviews}>
              새로고침
            </button>
          </div>
          <div className="operations-stats">
            <div>
              <span>전체 공고</span>
              <strong>{dashboard.totalPostings}</strong>
              <small>진행 중 {dashboard.openPostings}건</small>
            </div>
            <div>
              <span>오늘 수집</span>
              <strong>{dashboard.collectedToday}</strong>
              <small>새로 발견한 공고</small>
            </div>
            <div className={dashboard.qualityAttention ? 'attention' : ''}>
              <span>검수 필요</span>
              <strong>{dashboard.qualityAttention}</strong>
              <small>정보 누락·미분류</small>
            </div>
            <div>
              <span>전체 회원</span>
              <strong>{dashboard.totalUsers}</strong>
              <small>오늘 가입 {dashboard.newUsersToday}명</small>
            </div>
            <div className={dashboard.waitingInquiries ? 'attention' : ''}>
              <span>답변 대기</span>
              <strong>{dashboard.waitingInquiries}</strong>
              <small>완료 {dashboard.answeredInquiries}건</small>
            </div>
            <div>
              <span>공지사항</span>
              <strong>{dashboard.notices}</strong>
              <small>등록된 운영 공지</small>
            </div>
          </div>
          <div className="operations-detail-grid">
            <section className="source-health">
              <h3>수집 출처 현황</h3>
              <div className="source-health-list">
                {dashboard.sources.map((source) => (
                  <div key={source.source}>
                    <strong>{source.source}</strong>
                    <span>
                      전체 {source.total}건 · 진행 중 {source.open}건
                    </span>
                    <small>
                      최근 갱신{' '}
                      {source.lastUpdatedAt
                        ? new Date(source.lastUpdatedAt).toLocaleString('ko-KR')
                        : '기록 없음'}
                    </small>
                  </div>
                ))}
                {!dashboard.sources.length && <p className="empty">수집된 공고가 없습니다.</p>}
              </div>
            </section>
            <section className="admin-activity">
              <h3>최근 운영 기록</h3>
              <div>
                {dashboard.recentActivities.map((activity, index) => (
                  <article key={`${activity.type}-${activity.occurredAt}-${index}`}>
                    <span>{activity.type === 'NOTICE' ? '공지' : '문의답변'}</span>
                    <strong>{activity.detail}</strong>
                    <time>{new Date(activity.occurredAt).toLocaleString('ko-KR')}</time>
                  </article>
                ))}
                {!dashboard.recentActivities.length && (
                  <p className="empty">아직 기록된 운영 작업이 없습니다.</p>
                )}
              </div>
            </section>
          </div>
        </section>
      )}
      {section === 'overview' && operationsStatus && (
        <section className="infrastructure-status" aria-label="서버 및 백업 상태">
          <div className="operations-heading">
            <div>
              <h2>서버·백업 상태</h2>
              <span>
                {new Date(operationsStatus.checkedAt).toLocaleString('ko-KR')} 화면 확인 기준
              </span>
            </div>
          </div>
          <div className="infrastructure-metrics">
            {[
              ['애플리케이션', operationsStatus.server.appHealthy, '컨테이너'],
              ['Caddy', operationsStatus.server.caddyRunning, '리버스 프록시'],
              ['HTTPS', operationsStatus.server.httpsReachable, '외부 접속'],
              ['TiDB', operationsStatus.server.databaseReachable, '데이터베이스'],
              [
                '가용 메모리',
                operationsStatus.server.availableMemoryMb == null
                  ? null
                  : operationsStatus.server.availableMemoryMb >= 128,
                operationsStatus.server.availableMemoryMb == null
                  ? '확인 전'
                  : `${operationsStatus.server.availableMemoryMb}MB`,
              ],
              [
                '디스크 사용량',
                operationsStatus.server.diskUsagePercent == null
                  ? null
                  : operationsStatus.server.diskUsagePercent < 85,
                operationsStatus.server.diskUsagePercent == null
                  ? '확인 전'
                  : `${operationsStatus.server.diskUsagePercent}%`,
              ],
              [
                '최근 백업',
                operationsStatus.server.latestBackupAgeHours == null
                  ? null
                  : operationsStatus.server.latestBackupAgeHours <= 30,
                operationsStatus.server.latestBackupAgeHours == null
                  ? '확인 전'
                  : `${operationsStatus.server.latestBackupAgeHours}시간 전`,
              ],
            ].map(([label, healthy, detail]) => (
              <div className={healthy === false ? 'attention' : ''} key={label}>
                <span>{label}</span>
                <strong>{healthy == null ? '—' : healthy ? '정상' : '확인 필요'}</strong>
                <small>{detail}</small>
              </div>
            ))}
          </div>
          <div className="operations-job-status">
            {operationsStatus.jobs.map((job) => (
              <article
                key={job.key}
                className={job.state !== 'healthy' || job.stale ? 'attention' : ''}
              >
                <div>
                  <strong>{job.label}</strong>
                  <span className={`job-state ${job.state}`}>
                    {job.state === 'healthy'
                      ? job.stale
                        ? '점검 필요'
                        : '정상'
                      : job.state === 'failed'
                        ? '실패'
                        : '기록 없음'}
                  </span>
                </div>
                <p>{job.summary}</p>
                <time>
                  {job.updatedAt
                    ? new Date(job.updatedAt).toLocaleString('ko-KR')
                    : '아직 실행되지 않음'}
                </time>
              </article>
            ))}
          </div>
          <p className="infrastructure-note">
            이 화면은 서버가 발행한 읽기 전용 상태만 표시하며 비밀번호와 접속 주소는 저장하지
            않습니다.
          </p>
        </section>
      )}
      {section === 'statistics' && statistics && (
        <section className="admin-statistics" aria-label="운영 통계와 추이">
          <div className="operations-heading">
            <div>
              <h2>최근 {statistics.periodDays}일 운영 통계</h2>
              <span>{new Date(statistics.checkedAt).toLocaleString('ko-KR')} 기준</span>
            </div>
            <button type="button" onClick={loadReviews}>
              새로고침
            </button>
          </div>
          <div className="statistics-summary">
            <div>
              <span>수집 공고</span>
              <strong>{statistics.collectedInPeriod}</strong>
              <small>최근 {statistics.periodDays}일</small>
            </div>
            <div>
              <span>신규 회원</span>
              <strong>{statistics.joinedInPeriod}</strong>
              <small>가입 완료</small>
            </div>
            <div>
              <span>신규 스크랩</span>
              <strong>{statistics.scrapsInPeriod}</strong>
              <small>사용자 관심 공고</small>
            </div>
            <div>
              <span>신규 문의</span>
              <strong>{statistics.inquiriesInPeriod}</strong>
              <small>Q&amp;A 접수</small>
            </div>
            <div>
              <span>진행 공고 비율</span>
              <strong>{statistics.openRate}%</strong>
              <small>전체 저장 공고 기준</small>
            </div>
            <div>
              <span>문의 답변률</span>
              <strong>{statistics.inquiryAnswerRate}%</strong>
              <small>누적 문의 기준</small>
            </div>
          </div>
          <div className="statistics-grid">
            <section>
              <h3>일별 활동 추이</h3>
              <AdminTrendChart daily={statistics.daily} />
            </section>
            <section>
              <h3>수집 출처별 공고</h3>
              <div className="source-share-chart">
                {statistics.sources.map((source) => {
                  const maximum = Math.max(1, ...statistics.sources.map((item) => item.total))
                  return (
                    <div key={source.source}>
                      <div>
                        <strong>{source.source}</strong>
                        <span>
                          {source.total}건 · 진행 {source.open}건
                        </span>
                      </div>
                      <div className="source-share-track">
                        <i style={{ width: `${(source.total / maximum) * 100}%` }} />
                      </div>
                    </div>
                  )
                })}
                {!statistics.sources.length && <p className="empty">집계할 공고가 없습니다.</p>}
              </div>
            </section>
          </div>
        </section>
      )}
      {section === 'security' && securityOverview && (
        <section className="admin-security-overview" aria-label="관리자 운영 보안">
          <div className="operations-heading">
            <div>
              <h2>운영 보안</h2>
              <span>{new Date(securityOverview.checkedAt).toLocaleString('ko-KR')} 기준</span>
            </div>
            <button type="button" onClick={loadReviews}>
              새로고침
            </button>
          </div>
          <div className="security-summary">
            <div>
              <span>관리자 계정</span>
              <strong>{securityOverview.adminAccounts}</strong>
              <small>등록된 운영 계정</small>
            </div>
            <div
              className={
                securityOverview.twoFactorEnabled < securityOverview.adminAccounts
                  ? 'attention'
                  : ''
              }
            >
              <span>2단계 인증</span>
              <strong>
                {securityOverview.twoFactorEnabled}/{securityOverview.adminAccounts}
              </strong>
              <small>관리자 적용 계정</small>
            </div>
            <div className={securityOverview.passwordChangeRequired ? 'attention' : ''}>
              <span>비밀번호 변경 필요</span>
              <strong>{securityOverview.passwordChangeRequired}</strong>
              <small>관리자 계정</small>
            </div>
            <div className={securityOverview.failedLogins24h ? 'attention' : ''}>
              <span>로그인 실패</span>
              <strong>{securityOverview.failedLogins24h}</strong>
              <small>최근 24시간</small>
            </div>
            <div>
              <span>로그인 성공</span>
              <strong>{securityOverview.successfulLogins24h}</strong>
              <small>최근 24시간</small>
            </div>
            <div className={securityOverview.failedAdminRequests24h ? 'attention' : ''}>
              <span>관리 요청 실패</span>
              <strong>{securityOverview.failedAdminRequests24h}</strong>
              <small>최근 24시간</small>
            </div>
          </div>
          <div className="security-detail-grid">
            <section>
              <h3>보안 설정 점검</h3>
              <div className="security-check-list">
                {securityOverview.checks.map((check) => (
                  <article className={check.enabled ? 'enabled' : 'attention'} key={check.key}>
                    <span>{check.enabled ? '적용' : '확인 필요'}</span>
                    <div>
                      <strong>{check.label}</strong>
                      <small>{check.detail}</small>
                    </div>
                  </article>
                ))}
              </div>
            </section>
            <section>
              <h3>최근 7일 로그인 실패</h3>
              <div className="security-failure-chart">
                {securityOverview.loginFailures.map((point) => {
                  const maximum = Math.max(
                    1,
                    ...securityOverview.loginFailures.map((item) => item.failures),
                  )
                  return (
                    <div key={point.date}>
                      <span>{point.date.slice(5).replace('-', '/')}</span>
                      <i>
                        <b
                          style={{ height: `${Math.max(4, (point.failures / maximum) * 100)}%` }}
                        />
                      </i>
                      <strong>{point.failures}</strong>
                    </div>
                  )
                })}
              </div>
              <p className="security-privacy-note">
                실패 출처 {securityOverview.distinctFailureSources24h}개 · IP 주소와 계정 식별자는
                이 화면에 표시하지 않습니다.
              </p>
            </section>
          </div>
        </section>
      )}
      {message && (
        <p className="status" role="status">
          {message}
        </p>
      )}
      {section === 'incidents' && (
        <section className="incident-center" aria-label="운영 장애 대응">
          <div className="incident-center-head">
            <div>
              <p className="eyebrow">INCIDENT RESPONSE</p>
              <h2>장애 대응</h2>
              <p>현재 감지된 오류와 오래된 점검 결과를 우선순위대로 확인합니다.</p>
            </div>
            <button type="button" disabled={busy != null} onClick={loadReviews}>
              상태 다시 확인
            </button>
          </div>
          {operationIssues.length ? (
            <div className="incident-list">
              {operationIssues.map((issue) => (
                <article key={issue.key}>
                  <div>
                    <span>확인 필요</span>
                    <strong>{issue.title}</strong>
                  </div>
                  <p>{issue.detail}</p>
                  <small>{issue.action}</small>
                  {issue.key === 'crawl' && (
                    <button
                      type="button"
                      disabled={busy != null || crawlInfo?.running}
                      onClick={startCrawl}
                    >
                      {busy === 'crawl' || crawlInfo?.running ? '수집 중…' : '전체 수집 재시도'}
                    </button>
                  )}
                  {issue.key === 'public-api' && (
                    <button type="button" disabled={busy != null} onClick={retryPublicRecruitment}>
                      {busy === 'public-recruitment' ? '처리 중…' : '공식 공고 재처리'}
                    </button>
                  )}
                </article>
              ))}
            </div>
          ) : (
            <div className="incident-empty">
              <strong>현재 확인이 필요한 장애가 없습니다.</strong>
              <p>실패가 발생하면 Discord 알림과 이 화면에 함께 표시됩니다.</p>
            </div>
          )}
          <div className="incident-policy">
            <strong>자동 처리 기준</strong>
            <p>
              실패 상태는 다음 예약 작업이 성공하면 자동으로 정상 처리됩니다. 서버 백업과 저장공간
              작업은 보안을 위해 웹에서 직접 실행하지 않습니다.
            </p>
          </div>
        </section>
      )}
      {section === 'collection' && (
        <section className="admin-collection-control" aria-label="채용공고 수집 관리">
          <div>
            <h2>수집 작업</h2>
            <p>전체 출처를 수집하거나 공식 채용정보만 즉시 다시 처리할 수 있습니다.</p>
          </div>
          <div className="admin-collection-actions">
            <button
              type="button"
              disabled={busy != null || crawlInfo?.running}
              onClick={startCrawl}
            >
              {crawlInfo?.running ? '전체 수집 중…' : '전체 공고 수집'}
            </button>
            <button
              type="button"
              className="secondary"
              disabled={busy != null}
              onClick={retryPublicRecruitment}
            >
              {busy === 'public-recruitment' ? '처리 중…' : '공식 공고 재처리'}
            </button>
          </div>
          <dl>
            <div>
              <dt>전체 수집</dt>
              <dd>
                {crawlInfo?.status?.lastSuccess
                  ? new Date(crawlInfo.status.lastSuccess).toLocaleString('ko-KR')
                  : '성공 기록 없음'}
              </dd>
            </div>
            <div>
              <dt>공식 채용정보</dt>
              <dd>
                {crawlInfo?.apiStatus?.lastSuccess
                  ? `${new Date(crawlInfo.apiStatus.lastSuccess).toLocaleString('ko-KR')} · ${crawlInfo.apiStatus.lastCount || 0}건`
                  : '성공 기록 없음'}
              </dd>
            </div>
          </dl>
          {crawlInfo?.apiStatus?.error && (
            <p className="status-error">최근 오류: {crawlInfo.apiStatus.error}</p>
          )}
        </section>
      )}
      {section === 'collection' && crawlInfo?.apiStatus?.validation?.pagesScanned > 0 && (
        <section className="collection-audit" aria-label="최근 공고 수집 검증 결과">
          <div className="collection-audit-head">
            <div>
              <strong>최근 수집 검증</strong>
              <span>{crawlInfo.apiStatus.validation.pagesScanned}페이지 확인</span>
            </div>
            <b className={crawlInfo.apiStatus.validation.complete ? 'audit-ok' : 'audit-warning'}>
              {crawlInfo.apiStatus.validation.complete ? '전체 확인 완료' : '추가 확인 필요'}
            </b>
          </div>
          <div className="collection-audit-counts">
            <span>
              원천 공고 <strong>{crawlInfo.apiStatus.validation.sourceCount}</strong>
            </span>
            <span>
              확인 <strong>{crawlInfo.apiStatus.validation.inspectedCount}</strong>
            </span>
            <span>
              저장 <strong>{crawlInfo.apiStatus.validation.savedCount}</strong>
            </span>
            <span>
              누락·실패 <strong>{crawlInfo.apiStatus.validation.missingCount}</strong>
            </span>
          </div>
          {crawlInfo.apiStatus.validation.failures?.length > 0 && (
            <details>
              <summary>확인이 필요한 공고 {crawlInfo.apiStatus.validation.failedCount}건</summary>
              <ul>
                {crawlInfo.apiStatus.validation.failures.map((failure, index) => (
                  <li key={`${failure}-${index}`}>{failure}</li>
                ))}
              </ul>
            </details>
          )}
        </section>
      )}
      {section === 'overview' && quality && (
        <section className="quality-overview" aria-label="채용정보 품질 현황">
          <div>
            <strong>{quality.total}</strong>
            <span>전체 공고</span>
          </div>
          <div>
            <strong>{quality.missingDeadline}</strong>
            <span>마감일 미확인</span>
          </div>
          <div>
            <strong>{quality.missingRegion}</strong>
            <span>지역 미확인</span>
          </div>
          <div>
            <strong>{quality.missingPositions}</strong>
            <span>직렬 미분류</span>
          </div>
          <div>
            <strong>{quality.unknownMobility}</strong>
            <span>근무형태 확인 필요</span>
          </div>
          <div>
            <strong>{quality.invalidSourceUrl}</strong>
            <span>원문 연결 확인</span>
          </div>
          <div>
            <strong>{quality.duplicateSourceUrls}</strong>
            <span>중복 의심</span>
          </div>
        </section>
      )}
      {section === 'changes' && (
        <>
          <AdminOperationsLogs securePost={securePost} />
          <AdminPostingChanges securePost={securePost} />
        </>
      )}
      {section === 'support' && (
        <CommunityBoard
          user={user}
          securePost={securePost}
          onLogin={() => {}}
          onUnreadChange={() => loadReviews()}
        />
      )}
      {section === 'data' && <AdminRetentionPanel securePost={securePost} />}
      {section === 'data' && (
        <form className="qualification-import" onSubmit={importQualifications}>
          <strong>자격증 목록 관리</strong>
          <select value={qualificationType} onChange={(e) => setQualificationType(e.target.value)}>
            <option value="AUTO">파일 내용으로 자동 구분</option>
            <option value="NATIONAL_TECHNICAL">국가기술자격</option>
            <option value="NATIONAL_PROFESSIONAL">국가전문자격</option>
            <option value="ACCREDITED_PRIVATE">국가공인 민간자격</option>
          </select>
          <select
            value={qualificationEncoding}
            onChange={(e) => setQualificationEncoding(e.target.value)}
          >
            <option value="AUTO">인코딩 자동 감지</option>
            <option value="UTF-8">UTF-8</option>
            <option value="MS949">한글 파일</option>
          </select>
          <input
            type="file"
            accept=".csv,text/csv"
            required
            onChange={(e) => setQualificationFile(e.target.files?.[0] || null)}
          />
          <button>자격증 목록 등록</button>
        </form>
      )}
      {section === 'data' && (
        <form className="qualification-import" onSubmit={importCompensations}>
          <strong>기관 초임 자료 관리</strong>
          <span>직원 평균보수 파일에서 신입사원 초임을 가져옵니다.</span>
          <input
            type="file"
            accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            required
            onChange={(e) => setCompensationFile(e.target.files?.[0] || null)}
          />
          <button>초임 자료 등록</button>
        </form>
      )}
      {section === 'review' &&
        items.map((item) => (
          <article className="admin-card" key={item.id}>
            <div className="admin-card-head">
              <div>
                <strong>{item.title}</strong>
                <span>{item.organization}</span>
              </div>
              <a href={item.sourceUrl} target="_blank" rel="noreferrer">
                원문 보기
              </a>
            </div>
            <div className="admin-fields">
              <label>
                기관 유형
                <select
                  value={item.organizationType || 'PUBLIC'}
                  onChange={(e) => change(item.id, 'organizationType', e.target.value)}
                >
                  <option value="PUBLIC">공공기관</option>
                  <option value="PRIVATE">민간기업</option>
                </select>
              </label>
              <label>
                근무 형태
                <select
                  value={item.mobilityType || 'UNKNOWN'}
                  onChange={(e) => change(item.id, 'mobilityType', e.target.value)}
                >
                  <option value="ROTATIONAL">순환근무</option>
                  <option value="FIXED">지역고정</option>
                  <option value="UNKNOWN">확인 필요</option>
                </select>
              </label>
              <label>
                근무지역
                <input
                  value={item.region || ''}
                  onChange={(e) => change(item.id, 'region', e.target.value)}
                />
              </label>
              <label>
                고용형태
                <input
                  value={item.employmentType || ''}
                  onChange={(e) => change(item.id, 'employmentType', e.target.value)}
                />
              </label>
            </div>
            <div className="admin-position-head">
              <strong>채용 직렬</strong>
              <button type="button" onClick={() => addPosition(item.id)}>
                + 직렬 추가
              </button>
            </div>
            {item.positions.map((position, index) => (
              <div className="admin-position" key={position.id || index}>
                <select
                  value={position.standardCategory}
                  onChange={(e) =>
                    changePosition(item.id, index, 'standardCategory', e.target.value)
                  }
                >
                  {ADMIN_CATEGORIES.map((value) => (
                    <option key={value}>{value}</option>
                  ))}
                </select>
                <input
                  aria-label="원문 직렬명"
                  placeholder="원문 직렬명"
                  value={position.originalName}
                  onChange={(e) => changePosition(item.id, index, 'originalName', e.target.value)}
                />
                <input
                  aria-label="채용인원"
                  type="number"
                  min="0"
                  placeholder="인원"
                  value={position.headcount ?? ''}
                  onChange={(e) => changePosition(item.id, index, 'headcount', e.target.value)}
                />
                <input
                  aria-label="직렬 근무지역"
                  placeholder="근무지역"
                  value={position.workRegion || ''}
                  onChange={(e) => changePosition(item.id, index, 'workRegion', e.target.value)}
                />
                <button
                  className="danger-text"
                  type="button"
                  onClick={() => removePosition(item.id, index)}
                >
                  삭제
                </button>
              </div>
            ))}
            <div className="admin-save">
              <button disabled={busy === item.id} onClick={() => save(item)}>
                {busy === item.id ? '저장 중...' : '검수 내용 저장'}
              </button>
            </div>
          </article>
        ))}
      {section === 'review' && !items.length && !message && (
        <p className="empty">검수할 공고를 불러오는 중...</p>
      )}
    </section>
  )
}

function InsightDialog({ value, onClose, onOpen }) {
  return (
    <div className="match-overlay" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <section className="insight-dialog" role="dialog" aria-modal="true">
        <button className="close" onClick={onClose}>
          ×
        </button>
        <p className="eyebrow">RECRUITMENT INSIGHT</p>
        <h2>{value.organization} 채용 분석</h2>
        <div className="insight-stats">
          <div>
            <span>누적 공고</span>
            <strong>{value.stats.totalPostings}건</strong>
          </div>
          <div>
            <span>정규직 공고</span>
            <strong>{value.stats.regularPostings}건</strong>
          </div>
          <div>
            <span>평균 채용인원</span>
            <strong>{value.stats.averageHeadcount ?? '—'}명</strong>
          </div>
          <div>
            <span>평균 공고 주기</span>
            <strong>{value.stats.averageCycleDays ?? '—'}일</strong>
          </div>
        </div>
        <p>자주 채용하는 직렬: {value.stats.frequentCategories.join(' · ') || '자료 없음'}</p>
        <p>
          채용이 잦은 월:{' '}
          {value.stats.frequentMonths.map((month) => `${month}월`).join(' · ') || '자료 없음'}
        </p>
        <h3>유사·과거 공고</h3>
        <div className="similar-list">
          {value.similar.items.map((item) => (
            <button key={item.id} onClick={() => onOpen(item)}>
              {item.sameCategory && <b>유사 직렬</b>}
              <strong>{item.title}</strong>
              <span>
                {item.postedAt || '등록일 미확인'} · {item.headcount || '인원 미정'}명 ·{' '}
                {item.categories.join(', ')}
              </span>
            </button>
          ))}
          {!value.similar.items.length && <p className="empty">연결할 과거 공고가 없습니다.</p>}
        </div>
      </section>
    </div>
  )
}

function ReportDialog({ job, busy, onClose, onSubmit }) {
  const [category, setCategory] = useState('WRONG_INFO')
  const [content, setContent] = useState('')
  return (
    <div className="match-overlay" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <form
        className="report-dialog"
        onSubmit={(e) => {
          e.preventDefault()
          onSubmit(category, content)
        }}
      >
        <button type="button" className="close" onClick={onClose}>
          ×
        </button>
        <p className="eyebrow">REPORT</p>
        <h2>공고 정보 신고</h2>
        <strong>
          {job.organization} · {job.title}
        </strong>
        <label>
          신고 유형
          <select value={category} onChange={(e) => setCategory(e.target.value)}>
            <option value="WRONG_INFO">잘못된 정보</option>
            <option value="MISSING_CONTENT">누락된 내용</option>
            <option value="BROKEN_LINK">원문 연결 오류</option>
            <option value="DUPLICATE">중복 공고</option>
          </select>
        </label>
        <label>
          확인할 내용
          <textarea
            required
            minLength="5"
            maxLength="2000"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="어떤 정보가 잘못되었거나 누락되었는지 알려주세요."
          />
        </label>
        <button className="primary" disabled={busy}>
          {busy ? '접수 중...' : '신고 접수'}
        </button>
      </form>
    </div>
  )
}

function ArchivedPostings({ handlers, jobCategories }) {
  const currentYear = new Date().getFullYear()
  const [queryInput, setQueryInput] = useState('')
  const [query, setQuery] = useState('')
  const [region, setRegion] = useState('')
  const [jobCategory, setJobCategory] = useState('')
  const [employmentType, setEmploymentType] = useState('')
  const [year, setYear] = useState('')
  const [page, setPage] = useState(0)
  const [data, setData] = useState({ items: [], total: 0, totalPages: 0, hasNext: false })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadArchive = useCallback(() => {
    setLoading(true)
    const params = new URLSearchParams({
      q: query,
      page: String(page),
      region,
      jobCategory,
      employmentType,
    })
    if (year) params.set('year', year)
    getJson(`/api/postings/archive?${params}`)
      .then((value) => {
        setData(value)
        setError('')
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [employmentType, jobCategory, page, query, region, year])
  useEffect(loadArchive, [loadArchive])
  const changeFilter = (setter) => (event) => {
    setter(event.target.value)
    setPage(0)
  }

  return (
    <section className="archive-page">
      <div className="archive-heading">
        <div>
          <p className="eyebrow">RECRUITMENT ARCHIVE</p>
          <h1>종료 공고 보관함</h1>
          <p className="sub">과거 채용 조건과 지원 기록을 다시 확인할 수 있습니다.</p>
        </div>
        <form
          className="search"
          onSubmit={(event) => {
            event.preventDefault()
            setQuery(queryInput.trim())
            setPage(0)
          }}
        >
          <input
            value={queryInput}
            onChange={(event) => setQueryInput(event.target.value)}
            placeholder="기관명, 공고명 검색"
          />
          <button>검색</button>
        </form>
      </div>
      <div className="archive-filters">
        <label>
          지역
          <select value={region} onChange={changeFilter(setRegion)}>
            <option value="">전국</option>
            {REGION_LEVELS.map((item) => (
              <option key={item.code} value={item.label}>
                {item.label}
              </option>
            ))}
          </select>
        </label>
        <label>
          채용 직렬
          <select value={jobCategory} onChange={changeFilter(setJobCategory)}>
            <option value="">전체 직렬</option>
            {jobCategories.map((item) => (
              <option key={item}>{item}</option>
            ))}
          </select>
        </label>
        <label>
          채용연도
          <select value={year} onChange={changeFilter(setYear)}>
            <option value="">전체 연도</option>
            {Array.from({ length: 15 }, (_, index) => currentYear - index).map((value) => (
              <option key={value} value={value}>
                {value}년
              </option>
            ))}
          </select>
        </label>
        <label>
          고용형태
          <select value={employmentType} onChange={changeFilter(setEmploymentType)}>
            <option value="">전체</option>
            <option value="정규직">정규직</option>
            <option value="무기계약직">무기계약직</option>
            <option value="계약직">계약직</option>
            <option value="인턴">인턴</option>
          </select>
        </label>
        {(region || jobCategory || year || employmentType) && (
          <button
            type="button"
            onClick={() => {
              setRegion('')
              setJobCategory('')
              setYear('')
              setEmploymentType('')
              setPage(0)
            }}
          >
            필터 초기화
          </button>
        )}
      </div>
      <div className="meta">
        <span>보관된 공고 {data.total}건</span>
        <span>최신 마감일순</span>
      </div>
      {error && <p className="error">{error}</p>}
      {loading ? (
        <p className="empty">종료 공고를 불러오는 중...</p>
      ) : (
        <section className="grid archive-grid" aria-label="종료된 채용공고 목록">
          {data.items.map((job) => (
            <JobCard
              key={job.id}
              job={job}
              busy={handlers.busyId === job.id}
              onOpenSource={() => handlers.onOpen(job)}
              onSalary={handlers.onSalary}
              onCompetition={handlers.onCompetition}
              onScrap={async (id) => {
                await handlers.onScrap(id)
                loadArchive()
              }}
              onTrack={handlers.onTrack}
              onMatch={handlers.onMatch}
              onShare={handlers.onShare}
              onInsight={handlers.onInsight}
              onReport={handlers.onReport}
              matchBusy={handlers.matchBusyId === job.id}
            />
          ))}
        </section>
      )}
      {!loading && !data.items.length && <p className="empty">조건에 맞는 종료 공고가 없습니다.</p>}
      <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />
    </section>
  )
}

function LandingPage({ user, onPostings, onMyPage, onCommunity }) {
  return (
    <section className="landing-page">
      <div className="landing-intro">
        <p className="eyebrow">PUBLIC JOB HUB</p>
        <h1>공공기관 채용, 필요한 화면부터 시작하세요</h1>
        <p>최신 공고를 확인하고, 나의 역량과 지원 일정을 관리할 수 있습니다.</p>
      </div>
      <div className="landing-menu">
        <button className="landing-card postings" onClick={onPostings}>
          <span aria-hidden="true">⌕</span>
          <strong>채용공고 확인</strong>
          <small>로그인 없이 최신 채용공고를 검색하고 원문을 확인합니다.</small>
        </button>
        <button className="landing-card mypage" onClick={onMyPage}>
          <span aria-hidden="true">◎</span>
          <strong>마이페이지</strong>
          <small>
            {user
              ? '내 역량·스크랩·지원 일정을 관리합니다.'
              : '로그인 후 내 역량과 지원 일정을 관리합니다.'}
          </small>
        </button>
        <button className="landing-card notice" onClick={onCommunity}>
          <span aria-hidden="true">i</span>
          <strong>공지사항</strong>
          <small>서비스 공지와 자주 묻는 내용을 로그인 없이 확인합니다.</small>
        </button>
      </div>
    </section>
  )
}

/** 검색·페이지 이동·인증·개인 상태와 원문 창을 조합하는 메인 화면. */
export default function App() {
  // 현재 회원과 CSRF 토큰은 세션 변경 요청에 공통으로 사용한다.
  const [user, setUser] = useState(null)
  const [csrf, setCsrf] = useState(null)
  const [authMode, setAuthMode] = useState(null)
  const [authReturn, setAuthReturn] = useState(null)
  const [authError, setAuthError] = useState('')
  const [authBusy, setAuthBusy] = useState(false)
  const [pendingScrapId, setPendingScrapId] = useState(null)
  const [legalView, setLegalView] = useState(null)
  const [passwordOpen, setPasswordOpen] = useState(false)
  const [passwordError, setPasswordError] = useState('')
  const [passwordBusy, setPasswordBusy] = useState(false)
  const [accountDeletionOpen, setAccountDeletionOpen] = useState(false)
  const [accountDeletionError, setAccountDeletionError] = useState('')
  const [accountDeletionBusy, setAccountDeletionBusy] = useState(false)
  // 공고 검색, 마이페이지 필터, 페이지 번호 및 API 결과를 보관한다.
  const [mine, setMine] = useState(false)
  const [scrapsOpen, setScrapsOpen] = useState(false)
  const [adminView, setAdminView] = useState(false)
  const [communityView, setCommunityView] = useState(false)
  const [landingView, setLandingView] = useState(true)
  const [detailId, setDetailId] = useState(null)
  const [inquiryUnreadCount, setInquiryUnreadCount] = useState(0)
  const [postingChangeUnread, setPostingChangeUnread] = useState(0)
  const [queryInput, setQueryInput] = useState('')
  const [query, setQuery] = useState('')
  const [regionProvince, setRegionProvince] = useState('')
  const [regionDistrict, setRegionDistrict] = useState('')
  const [mobility, setMobility] = useState('')
  const [institutionScope, setInstitutionScope] = useState('')
  const [organizationForm, setOrganizationForm] = useState('')
  const [jobCategory, setJobCategory] = useState('')
  const [showClosed, setShowClosed] = useState(false)
  const [filterOptions, setFilterOptions] = useState({
    regions: [],
    mobilityTypes: [],
    jobCategories: [],
  })
  const [page, setPage] = useState(0)
  const [data, setData] = useState({
    items: [],
    total: 0,
    totalPages: 0,
    hasNext: false,
    crawlStatus: null,
  })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)
  const [profile, setProfile] = useState({
    displayName: '',
    birthDate: '',
    certifications: [],
    activities: [],
    careers: [],
    degrees: [],
    grades: '',
  })
  const [profileBusy, setProfileBusy] = useState(false)
  const [profileMessage, setProfileMessage] = useState('')
  const [preference, setPreference] = useState({
    keywords: '',
    regions: '',
    mobilityTypes: '',
    enabled: true,
    newPostingAlerts: true,
    deadlineAlerts: true,
    deadlineDays: '7,3,1',
    postingChangeAlerts: true,
    importantChangeOnly: false,
  })
  const [notificationRefresh, setNotificationRefresh] = useState(0)
  const [alerts, setAlerts] = useState([])
  const [applications, setApplications] = useState([])
  const [toolsBusy, setToolsBusy] = useState(false)
  const [toolsMessage, setToolsMessage] = useState('')
  const [matchDialog, setMatchDialog] = useState(null)
  const [capabilityOpen, setCapabilityOpen] = useState(false)
  const [capabilityLoading, setCapabilityLoading] = useState(false)
  const [matchBusyId, setMatchBusyId] = useState(null)
  const [salaryDialog, setSalaryDialog] = useState(null)
  const [competitionDialog, setCompetitionDialog] = useState(null)
  const [insightDialog, setInsightDialog] = useState(null)
  const [reportDialog, setReportDialog] = useState(null)
  const [reportBusy, setReportBusy] = useState(false)
  const [securityOpen, setSecurityOpen] = useState(false)
  // 설치 가능 여부와 네트워크 상태는 PWA 경험을 안내하는 데만 사용한다.
  const [installPrompt, setInstallPrompt] = useState(null)
  const [online, setOnline] = useState(navigator.onLine)
  const [updateAvailable, setUpdateAvailable] = useState(false)

  /** 브라우저 주소와 화면 상태를 동기화해 새로고침·뒤로가기를 지원한다. */
  const applyRoute = useCallback(() => {
    const path = window.location.pathname
    const postingMatch = path.match(/^\/postings\/(\d+)$/)
    setDetailId(postingMatch ? Number(postingMatch[1]) : null)
    setLandingView(path === '/')
    setMine(path === '/mypage')
    setCommunityView(path === '/community')
    setAdminView(path === '/admin')
  }, [])
  useEffect(() => {
    applyRoute()
    window.addEventListener('popstate', applyRoute)
    return () => window.removeEventListener('popstate', applyRoute)
  }, [applyRoute])

  /** 브라우저의 설치 제안을 보관하고 온라인·오프라인 전환을 화면에 반영한다. */
  useEffect(() => {
    const onInstall = (event) => {
      event.preventDefault()
      setInstallPrompt(event)
    }
    const onInstalled = () => setInstallPrompt(null)
    const onOnline = () => setOnline(true)
    const onOffline = () => setOnline(false)
    const onUpdate = () => setUpdateAvailable(true)
    window.addEventListener('beforeinstallprompt', onInstall)
    window.addEventListener('appinstalled', onInstalled)
    window.addEventListener('online', onOnline)
    window.addEventListener('offline', onOffline)
    window.addEventListener('jobhub:update-available', onUpdate)
    return () => {
      window.removeEventListener('beforeinstallprompt', onInstall)
      window.removeEventListener('appinstalled', onInstalled)
      window.removeEventListener('online', onOnline)
      window.removeEventListener('offline', onOffline)
      window.removeEventListener('jobhub:update-available', onUpdate)
    }
  }, [])

  /** 설치를 지원하는 브라우저에서는 저장한 설치 대화상자를 연다. */
  async function installApp() {
    if (!installPrompt) return
    await installPrompt.prompt()
    setInstallPrompt(null)
  }
  /** 지원 브라우저에서는 시스템 공유창을 열고, 그 외에는 원문 주소를 복사한다. */
  async function shareJob(job) {
    const shareData = { title: job.title, text: `${job.organization} 채용공고`, url: job.sourceUrl }
    try {
      if (navigator.share) await navigator.share(shareData)
      else {
        await navigator.clipboard.writeText(job.sourceUrl)
        setError('공고 원문 주소를 복사했습니다.')
      }
    } catch (e) {
      if (e.name !== 'AbortError') setError('공고를 공유하지 못했습니다.')
    }
  }

  /** 새로고침·로그인·로그아웃 뒤 서버 세션과 CSRF 토큰을 다시 읽는다. */
  const refreshAuth = useCallback(async () => {
    const [session, token] = await Promise.all([getJson('/api/auth/me'), getJson('/api/auth/csrf')])
    setUser(session.user)
    if (session.user?.mustChangePassword && !session.user?.consentRequired) setPasswordOpen(true)
    setCsrf(token)
  }, [])
  useEffect(() => {
    refreshAuth().catch(() => setError('서버에 연결할 수 없습니다.'))
  }, [refreshAuth])
  useEffect(() => {
    if (!user || user.admin) {
      setInquiryUnreadCount(0)
      return
    }
    getJson('/api/community/inquiries/unread-count')
      .then((value) => setInquiryUnreadCount(value.count))
      .catch(() => {})
  }, [user])
  useEffect(() => {
    if (!user) {
      setPostingChangeUnread(0)
      return
    }
    getJson('/api/tools/posting-changes/unread-count')
      .then((value) => setPostingChangeUnread(value.count))
      .catch(() => {})
  }, [user])
  useEffect(() => {
    getJson('/api/tools/filters')
      .then(setFilterOptions)
      .catch(() => {})
  }, [])

  /** 현재 검색 조건에 해당하는 20개 공고를 로드하고 요청 오류를 표시한다. */
  const load = useCallback(
    async (signal) => {
      try {
        const params = new URLSearchParams({
          q: mine ? '' : query,
          page: String(page),
          mine: String(mine && scrapsOpen),
          includeClosed: String(!mine && showClosed),
          region: mine ? '' : regionProvince,
          regionFull: mine
            ? ''
            : REGION_LEVELS.find((item) => item.code === regionProvince)?.label || '',
          district: mine ? '' : regionDistrict,
          mobility: mine ? '' : mobility,
          institutionScope: mine ? '' : institutionScope,
          organizationForm: mine ? '' : organizationForm,
          jobCategory: mine ? '' : jobCategory,
        })
        setData(await getJson(`/api/postings?${params}`, { signal }))
        setError('')
      } catch (e) {
        if (e.name !== 'AbortError')
          setError('공고를 불러오지 못했습니다. Spring Boot 서버를 확인해 주세요.')
      } finally {
        setLoading(false)
      }
    },
    [
      query,
      page,
      mine,
      scrapsOpen,
      regionProvince,
      regionDistrict,
      mobility,
      institutionScope,
      organizationForm,
      jobCategory,
      showClosed,
    ],
  )
  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    load(controller.signal)
    return () => controller.abort()
  }, [load])

  /** 마이페이지에 들어오면 현재 회원의 비공개 스펙을 읽는다. */
  useEffect(() => {
    if (!mine || !user) return
    Promise.all([
      getJson('/api/profile'),
      getJson('/api/tools/preferences'),
      getJson('/api/tools/alerts'),
      getJson('/api/tools/applications'),
    ])
      .then(([saved, savedPreference, savedAlerts, savedApplications]) => {
        setProfile(saved)
        setPreference(savedPreference)
        setAlerts(savedAlerts)
        setApplications(savedApplications)
        setProfileMessage('')
      })
      .catch((e) => setProfileMessage(e.message))
  }, [mine, user])

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
      if (pendingScrapId) {
        await securePost(`/api/postings/${pendingScrapId}/scrap`)
        setPendingScrapId(null)
      }
      if (authReturn === 'mypage') {
        setMine(true)
        setLandingView(false)
        window.history.pushState({}, '', '/mypage')
      }
      setAuthReturn(null)
      await load()
    } catch (e) {
      setAuthError(e.message)
    } finally {
      setAuthBusy(false)
    }
  }
  async function submitConsent(consent) {
    setAuthBusy(true)
    setAuthError('')
    try {
      await securePost('/api/auth/consent', JSON.stringify(consent), 'application/json')
      await refreshAuth()
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
  async function changePassword(form) {
    setPasswordBusy(true)
    setPasswordError('')
    try {
      const updated = await securePost(
        '/api/auth/password',
        JSON.stringify(form),
        'application/json',
      )
      setUser(updated)
      setPasswordOpen(false)
      setError('비밀번호를 변경했습니다.')
    } catch (e) {
      setPasswordError(e.message)
    } finally {
      setPasswordBusy(false)
    }
  }
  async function deleteAccount(form) {
    setAccountDeletionBusy(true)
    setAccountDeletionError('')
    try {
      await securePost('/api/account/delete', JSON.stringify(form), 'application/json')
      setAccountDeletionOpen(false)
      setUser(null)
      setMine(false)
      setLandingView(true)
      setError('회원 탈퇴와 개인정보 삭제가 완료되었습니다.')
      await refreshAuth()
    } catch (e) {
      setAccountDeletionError(e.message)
    } finally {
      setAccountDeletionBusy(false)
    }
  }
  /** 스크랩·지원 완료 상태를 토글한 뒤 최신 목록을 다시 읽는다. */
  async function mutate(id, action) {
    if (!user) {
      if (action === 'scrap') setPendingScrapId(id)
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
  /** 서버 검증을 거쳐 현재 회원의 스펙을 저장한다. */
  async function saveProfile(nextProfile) {
    setProfileBusy(true)
    setProfileMessage('')
    try {
      const saved = await securePost(
        '/api/profile',
        JSON.stringify({
          birthDate: nextProfile.birthDate || null,
          certifications: nextProfile.certifications,
          activities: nextProfile.activities,
          careers: nextProfile.careers,
          degrees: nextProfile.degrees,
          grades: nextProfile.grades,
        }),
        'application/json',
      )
      setProfile(saved)
      setProfileMessage('저장되었습니다.')
    } catch (e) {
      setProfileMessage(e.message)
    } finally {
      setProfileBusy(false)
    }
  }
  async function openCapabilities() {
    if (!user) {
      setAuthMode('login')
      return
    }
    setCapabilityOpen(true)
    setCapabilityLoading(true)
    try {
      setProfile(await getJson('/api/profile'))
    } catch (e) {
      setError(e.message)
      setCapabilityOpen(false)
    } finally {
      setCapabilityLoading(false)
    }
  }
  async function savePreference() {
    setToolsBusy(true)
    setToolsMessage('')
    try {
      const saved = await securePost(
        '/api/tools/preferences',
        JSON.stringify(preference),
        'application/json',
      )
      setPreference(saved)
      setAlerts(await getJson('/api/tools/alerts'))
      setNotificationRefresh((value) => value + 1)
      setToolsMessage('맞춤 알림 조건을 저장했습니다.')
    } catch (e) {
      setToolsMessage(e.message)
    } finally {
      setToolsBusy(false)
    }
  }
  async function readAlerts() {
    try {
      await securePost('/api/tools/alerts/read')
      setAlerts((items) => items.map((item) => ({ ...item, fresh: false })))
    } catch (e) {
      setToolsMessage(e.message)
    }
  }
  async function saveTracking(id, tracking) {
    setBusyId(id)
    try {
      await securePost(
        `/api/tools/postings/${id}/application`,
        JSON.stringify({ ...tracking, nextStepDate: tracking.nextStepDate || null }),
        'application/json',
      )
      const [savedApplications] = await Promise.all([getJson('/api/tools/applications'), load()])
      setApplications(savedApplications)
    } catch (e) {
      setError(e.message)
    } finally {
      setBusyId(null)
    }
  }
  async function analyzeMatch(job) {
    if (!user) {
      setAuthMode('login')
      return
    }
    setMatchBusyId(job.id)
    try {
      const result = await getJson(`/api/tools/postings/${job.id}/match`)
      setMatchDialog({ job, result })
    } catch (e) {
      setError(e.message)
    } finally {
      setMatchBusyId(null)
    }
  }
  async function openSalary(job) {
    setSalaryDialog({ loading: true, organization: job.organization })
    try {
      setSalaryDialog(await getJson(`/api/postings/${job.id}/salary`))
    } catch (e) {
      setSalaryDialog(null)
      setError(e.message)
    }
  }
  async function openCompetition(job) {
    setCompetitionDialog({ loading: true, organization: job.organization })
    try {
      setCompetitionDialog(await getJson(`/api/postings/${job.id}/competition`))
    } catch (e) {
      setCompetitionDialog(null)
      setError(e.message)
    }
  }
  async function openInsight(job) {
    try {
      const [similar, stats] = await Promise.all([
        getJson(`/api/postings/${job.id}/similar`),
        getJson(`/api/postings/${job.id}/organization-stats`),
      ])
      setInsightDialog({ organization: job.organization, similar, stats })
    } catch (e) {
      setError(e.message)
    }
  }
  function openReport(job) {
    if (!user) {
      setAuthMode('login')
      return
    }
    setReportDialog(job)
  }
  async function submitReport(category, content) {
    setReportBusy(true)
    try {
      await securePost(
        `/api/postings/${reportDialog.id}/reports`,
        JSON.stringify({ category, content }),
        'application/json',
      )
      setReportDialog(null)
      setError('공고 정보 신고가 접수되었습니다. 관리자가 확인하겠습니다.')
    } catch (e) {
      setError(e.message)
    } finally {
      setReportBusy(false)
    }
  }
  async function openAdmin() {
    try {
      const security = await getJson('/api/auth/security')
      if (security.twoFactorEnabled && !security.twoFactorVerified) {
        const code = window.prompt('인증 앱의 6자리 코드를 입력하세요.')
        if (!code) return
        await securePost(
          '/api/auth/security/2fa/verify',
          JSON.stringify({ code }),
          'application/json',
        )
      }
      setAdminView(true)
      setMine(false)
      setCommunityView(false)
      setDetailId(null)
      window.history.pushState({}, '', '/admin')
    } catch (e) {
      setError(e.message)
    }
  }
  /** 카드 선택 시 새로고침 가능한 내부 상세 페이지로 이동한다. */
  function openSource(job) {
    setDetailId(job.id)
    setLandingView(false)
    window.history.pushState({}, '', `/postings/${job.id}`)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
  /** 마이페이지는 로그인한 회원에게만 열고 필터 변경 시 첫 페이지로 이동한다. */
  function navigate(nextMine) {
    if (nextMine && !user) {
      setAuthReturn('mypage')
      setAuthMode('login')
      return
    }
    setMine(nextMine)
    setLandingView(false)
    setScrapsOpen(false)
    setAdminView(false)
    setCommunityView(false)
    setDetailId(null)
    setPage(0)
    window.history.pushState({}, '', nextMine ? '/mypage' : '/jobs')
  }
  /** 서비스 배너를 누르면 검색·필터·개인 화면 상태를 모두 지우고 첫 화면으로 돌아간다. */
  function resetHome() {
    setLandingView(true)
    setMine(false)
    setScrapsOpen(false)
    setAdminView(false)
    setCommunityView(false)
    setDetailId(null)
    setQueryInput('')
    setQuery('')
    setRegionProvince('')
    setRegionDistrict('')
    setMobility('')
    setInstitutionScope('')
    setOrganizationForm('')
    setJobCategory('')
    setShowClosed(false)
    setPage(0)
    setMatchDialog(null)
    setError('')
    window.history.pushState({}, '', '/')
  }
  /** 검색어를 확정하고 결과의 첫 페이지를 다시 조회한다. */
  function search(e) {
    e.preventDefault()
    setQuery(queryInput.trim())
    setPage(0)
  }
  const status = data.crawlStatus
  const districtOptions = districtsIn(regionProvince)

  return (
    <>
      <header>
        <div className="bar">
          <button
            className="brand"
            onClick={resetHome}
            aria-label="검색과 필터를 초기화하고 처음 화면으로 이동"
          >
            JOB HUB KOREA
          </button>
          <span>한눈에 확인하는 공공기관 채용공고</span>
          <nav>
            {installPrompt && <button onClick={installApp}>앱 설치</button>}
            <button
              className={
                !landingView && !mine && !communityView && !adminView && !detailId ? 'active' : ''
              }
              onClick={() => navigate(false)}
            >
              채용정보
            </button>
            <button className={mine && !landingView ? 'active' : ''} onClick={() => navigate(true)}>
              마이페이지
              {postingChangeUnread > 0 && (
                <span
                  className="nav-notification"
                  aria-label={`확인하지 않은 공고 변경 ${postingChangeUnread}건`}
                >
                  {postingChangeUnread}
                </span>
              )}
            </button>
            {user && (
              <button className={capabilityOpen ? 'active' : ''} onClick={openCapabilities}>
                내 역량
              </button>
            )}
            {user && (
              <NotificationCenter
                user={user}
                securePost={securePost}
                onOpen={openSource}
                refreshVersion={notificationRefresh}
              />
            )}
            {user && <button onClick={() => setSecurityOpen(true)}>보안</button>}
            <button
              className={communityView ? 'active' : ''}
              onClick={() => {
                setLandingView(false)
                setCommunityView(true)
                setMine(false)
                setAdminView(false)
                setDetailId(null)
                window.history.pushState({}, '', '/community')
              }}
            >
              공지·Q&amp;A
              {inquiryUnreadCount > 0 && (
                <span className="nav-notification" aria-label={`새 답변 ${inquiryUnreadCount}건`}>
                  {inquiryUnreadCount}
                </span>
              )}
            </button>
            {user?.admin && (
              <button className={adminView ? 'active' : ''} onClick={openAdmin}>
                관리자 운영
              </button>
            )}
            {user ? (
              <>
                <span className="user-name">{user.displayName}님</span>
                <button onClick={logout}>로그아웃</button>
              </>
            ) : (
              <>
                <button
                  onClick={() => {
                    setAuthReturn(null)
                    setAuthMode('login')
                  }}
                >
                  로그인
                </button>
                <button
                  onClick={() => {
                    setAuthReturn(null)
                    setAuthMode('register')
                  }}
                >
                  회원가입
                </button>
              </>
            )}
          </nav>
        </div>
      </header>
      <main>
        {updateAvailable && (
          <div className="update-banner" role="status">
            <span>새 버전이 준비되었습니다.</span>
            <button onClick={() => window.dispatchEvent(new CustomEvent('jobhub:apply-update'))}>
              지금 업데이트
            </button>
          </div>
        )}
        {detailId ? (
          <PostingDetailPage
            id={detailId}
            user={user}
            busy={busyId === detailId}
            onBack={() => window.history.back()}
            onScrap={(id) => mutate(id, 'scrap')}
            onShare={shareJob}
          />
        ) : landingView ? (
          <LandingPage
            user={user}
            onPostings={() => navigate(false)}
            onMyPage={() => navigate(true)}
            onCommunity={() => {
              setLandingView(false)
              setCommunityView(true)
              setMine(false)
              setAdminView(false)
              setDetailId(null)
              window.history.pushState({}, '', '/community')
            }}
          />
        ) : adminView ? (
          <AdminReviewPanel user={user} securePost={securePost} />
        ) : communityView ? (
          <CommunityBoard
            user={user}
            securePost={securePost}
            onLogin={() => setAuthMode('login')}
            onUnreadChange={setInquiryUnreadCount}
          />
        ) : (
          <>
            <div className="heading">
              <div>
                <p className="eyebrow">공공기관 채용정보</p>
                <h1>{mine ? '마이페이지' : '최신 채용공고 현황'}</h1>
                <p className="sub">
                  {mine ? '내 스펙과 맞춤 알림을 관리하세요.' : '마감 임박순 · 12개씩 조회'}
                </p>
                {mine && (
                  <div className="mypage-heading-actions">
                    <button
                      type="button"
                      className={`scrap-list-toggle ${scrapsOpen ? 'active' : ''}`}
                      onClick={() => {
                        setScrapsOpen((open) => !open)
                        setPage(0)
                      }}
                    >
                      {scrapsOpen ? '스크랩 공고 닫기' : '스크랩 공고 확인'}
                    </button>
                    <button
                      type="button"
                      className="password-change-button"
                      onClick={() => setPasswordOpen(true)}
                    >
                      비밀번호 변경
                    </button>
                    {!user?.admin && (
                      <button
                        type="button"
                        className="account-delete-button"
                        onClick={() => setAccountDeletionOpen(true)}
                      >
                        회원 탈퇴
                      </button>
                    )}
                  </div>
                )}
              </div>
              {!mine && (
                <form className="search" onSubmit={search}>
                  <input
                    value={queryInput}
                    onChange={(e) => setQueryInput(e.target.value)}
                    placeholder="직무, 기관 검색..."
                    aria-label="공고 검색"
                  />
                  <button>검색</button>
                </form>
              )}
            </div>
            {!mine && (
              <>
                <div className="filter-row">
                  <label>
                    도·광역시·특별시
                    <select
                      value={regionProvince}
                      onChange={(e) => {
                        setRegionProvince(e.target.value)
                        setRegionDistrict('')
                        setPage(0)
                      }}
                    >
                      <option value="">전국</option>
                      {REGION_LEVELS.map((item) => (
                        <option key={item.code} value={item.code}>
                          {item.label}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    시·군·구
                    <select
                      value={regionDistrict}
                      disabled={!regionProvince}
                      onChange={(e) => {
                        setRegionDistrict(e.target.value)
                        setPage(0)
                      }}
                    >
                      <option value="">
                        {regionProvince ? '전체 시·군·구' : '상위 지역을 먼저 선택'}
                      </option>
                      {districtOptions.map((value) => (
                        <option key={value}>{value}</option>
                      ))}
                    </select>
                  </label>
                  <label>
                    근무 범위
                    <select
                      value={mobility}
                      onChange={(e) => {
                        setMobility(e.target.value)
                        setPage(0)
                      }}
                    >
                      <option value="">전체</option>
                      <option value="ROTATIONAL">순환근무 가능</option>
                      <option value="FIXED">지역고정</option>
                      <option value="UNKNOWN">확인 필요</option>
                    </select>
                  </label>
                  <label>
                    기관 구분
                    <select
                      value={institutionScope}
                      onChange={(e) => {
                        setInstitutionScope(e.target.value)
                        setPage(0)
                      }}
                    >
                      <option value="">중앙·지방 전체</option>
                      <option value="CENTRAL_PUBLIC">중앙 공공기관</option>
                      <option value="LOCAL_PUBLIC">지방공기업</option>
                    </select>
                  </label>
                  <label>
                    기관 형태
                    <select
                      value={organizationForm}
                      onChange={(e) => {
                        setOrganizationForm(e.target.value)
                        setPage(0)
                      }}
                    >
                      <option value="">전체 형태</option>
                      {['공사', '공단', '재단', '병원', '연구원', '진흥원', '센터'].map((value) => (
                        <option key={value}>{value}</option>
                      ))}
                    </select>
                  </label>
                  <label>
                    채용 직렬
                    <select
                      value={jobCategory}
                      onChange={(e) => {
                        setJobCategory(e.target.value)
                        setPage(0)
                      }}
                    >
                      <option value="">전체 직렬</option>
                      {filterOptions.jobCategories
                        ?.filter((value) => ADMIN_CATEGORIES.includes(value))
                        .map((value) => (
                          <option key={value}>{value}</option>
                        ))}
                    </select>
                  </label>
                  <label className="closed-posting-toggle">
                    <input
                      type="checkbox"
                      checked={showClosed}
                      onChange={(e) => {
                        setShowClosed(e.target.checked)
                        setPage(0)
                      }}
                    />
                    마감 공고도 확인
                  </label>
                  {(regionProvince ||
                    regionDistrict ||
                    mobility ||
                    institutionScope ||
                    organizationForm ||
                    jobCategory ||
                    showClosed) && (
                    <button
                      onClick={() => {
                        setRegionProvince('')
                        setRegionDistrict('')
                        setMobility('')
                        setInstitutionScope('')
                        setOrganizationForm('')
                        setJobCategory('')
                        setShowClosed(false)
                        setPage(0)
                      }}
                    >
                      필터 초기화
                    </button>
                  )}
                </div>
                <div className="meta">
                  <span>검색 결과 {data.total}건</span>
                  <span>
                    중앙 공공기관 · 지방공기업 · 민간기업을 기관 소속 기준으로 구분합니다. 이내
                  </span>
                </div>
              </>
            )}
            {!mine && !online && (
              <p className="offline-notice" role="status">
                오프라인 상태입니다. 앱 화면은 열리지만 공고 조회·로그인·스크랩에는 인터넷 연결이
                필요합니다.
              </p>
            )}
            {!mine && status && (
              <p className="status">
                {status.lastSuccess
                  ? `마지막 수집: ${new Date(status.lastSuccess).toLocaleString('ko-KR')} · 처리 ${status.lastCount}건`
                  : '첫 자정 수집을 기다리는 중입니다.'}
                {status.error && (
                  <span className="status-error"> · 일부 공고 수집을 확인하고 있습니다.</span>
                )}
              </p>
            )}
            {error && (
              <p className="error" role="alert">
                {error}
              </p>
            )}
            {mine &&
              scrapsOpen &&
              (loading ? (
                <p className="empty">불러오는 중...</p>
              ) : (
                <section className="grid scrap-list-grid" aria-label="스크랩한 채용공고 목록">
                  {data.items.map((job) => (
                    <JobCard
                      key={job.id}
                      job={job}
                      busy={busyId === job.id}
                      onOpenSource={() => openSource(job)}
                      onSalary={openSalary}
                      onCompetition={openCompetition}
                      onScrap={(id) => mutate(id, 'scrap')}
                      onTrack={saveTracking}
                      onMatch={analyzeMatch}
                      onShare={shareJob}
                      onInsight={openInsight}
                      onReport={openReport}
                      matchBusy={matchBusyId === job.id}
                      memberActions={Boolean(user)}
                    />
                  ))}
                </section>
              ))}
            {mine && scrapsOpen && !loading && data.items.length === 0 && (
              <p className="empty">스크랩한 공고가 없습니다.</p>
            )}
            {mine && scrapsOpen && (
              <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />
            )}
            {mine && user && (
              <div className="mypage-dashboard">
                <ApplicationDashboard applications={applications} onOpen={openSource} />
                <FavoriteOrganizationsPanel securePost={securePost} onOpen={openSource} />
                <PostingChangesPanel
                  securePost={securePost}
                  onOpen={openSource}
                  onUnreadChange={setPostingChangeUnread}
                />
                <CareerToolsPanel
                  preference={preference}
                  alerts={alerts}
                  busy={toolsBusy}
                  message={toolsMessage}
                  onChange={setPreference}
                  onSave={savePreference}
                  onRead={readAlerts}
                  onOpen={openSource}
                />
                <ProfileEditor
                  profile={profile}
                  busy={profileBusy}
                  message={profileMessage}
                  onSave={saveProfile}
                />
              </div>
            )}
            {!mine &&
              (loading ? (
                <p className="empty">불러오는 중...</p>
              ) : (
                <section className="grid" aria-label="채용공고 목록">
                  {data.items.map((job) => (
                    <JobCard
                      key={job.id}
                      job={job}
                      busy={busyId === job.id}
                      onOpenSource={() => openSource(job)}
                      onSalary={openSalary}
                      onCompetition={openCompetition}
                      onScrap={(id) => mutate(id, 'scrap')}
                      onTrack={saveTracking}
                      onMatch={analyzeMatch}
                      onShare={shareJob}
                      onInsight={openInsight}
                      onReport={openReport}
                      matchBusy={matchBusyId === job.id}
                      memberActions={Boolean(user)}
                    />
                  ))}
                </section>
              ))}
            {!mine && !loading && data.items.length === 0 && (
              <p className="empty">
                {regionProvince || regionDistrict
                  ? '선택한 지역에는 공고가 없습니다.'
                  : '공고가 없습니다.'}
              </p>
            )}
            {!mine && <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />}
          </>
        )}
      </main>
      <footer className="site-footer">
        <div>
          <strong>Public Job Hub</strong>
          <span>공공기관 채용정보와 지원 준비를 한곳에서 관리합니다.</span>
        </div>
        <nav aria-label="서비스 정책">
          <button onClick={() => setLegalView('terms')}>이용약관</button>
          <button onClick={() => setLegalView('privacy')}>개인정보 처리방침</button>
          <button onClick={() => setLegalView('contact')}>운영자 안내</button>
        </nav>
      </footer>
      <div className="page-jump-buttons" aria-label="페이지 빠른 이동">
        <button
          onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
          aria-label="맨 위로"
        >
          ↑
        </button>
        <button
          onClick={() =>
            window.scrollTo({ top: document.documentElement.scrollHeight, behavior: 'smooth' })
          }
          aria-label="맨 아래로"
        >
          ↓
        </button>
      </div>
      {matchDialog && (
        <MatchDialog
          value={matchDialog}
          onClose={() => setMatchDialog(null)}
          onOpenSource={openSource}
        />
      )}
      {salaryDialog && <SalaryDialog value={salaryDialog} onClose={() => setSalaryDialog(null)} />}
      {competitionDialog && (
        <CompetitionDialog value={competitionDialog} onClose={() => setCompetitionDialog(null)} />
      )}
      {insightDialog && (
        <InsightDialog
          value={insightDialog}
          onClose={() => setInsightDialog(null)}
          onOpen={openSource}
        />
      )}
      {reportDialog && (
        <ReportDialog
          job={reportDialog}
          busy={reportBusy}
          onClose={() => setReportDialog(null)}
          onSubmit={submitReport}
        />
      )}
      {securityOpen && (
        <SecurityCenter
          user={user}
          securePost={securePost}
          onClose={() => setSecurityOpen(false)}
        />
      )}
      {capabilityOpen && (
        <CapabilityDialog
          profile={profile}
          loading={capabilityLoading}
          onClose={() => setCapabilityOpen(false)}
          onEdit={() => {
            setCapabilityOpen(false)
            navigate(true)
          }}
        />
      )}
      {authMode && (
        <AuthDialog
          key={authMode}
          mode={authMode}
          onClose={() => {
            setAuthMode(null)
            setAuthError('')
            setPendingScrapId(null)
          }}
          onSubmit={submitAuth}
          onSwitch={() => {
            setAuthMode('register')
            setAuthError('')
          }}
          onOpenLegal={setLegalView}
          busy={authBusy}
          error={authError}
        />
      )}
      {user?.consentRequired && !authMode && (
        <RequiredConsentDialog
          busy={authBusy}
          error={authError}
          onSubmit={submitConsent}
          onOpenLegal={setLegalView}
        />
      )}
      {legalView && <LegalDialog type={legalView} onClose={() => setLegalView(null)} />}
      {passwordOpen && user && (
        <PasswordDialog
          required={Boolean(user.mustChangePassword)}
          busy={passwordBusy}
          error={passwordError}
          onClose={() => {
            setPasswordOpen(false)
            setPasswordError('')
          }}
          onSubmit={changePassword}
        />
      )}
      {accountDeletionOpen && user && !user.admin && (
        <AccountDeletionDialog
          busy={accountDeletionBusy}
          error={accountDeletionError}
          onClose={() => {
            setAccountDeletionOpen(false)
            setAccountDeletionError('')
          }}
          onSubmit={deleteAccount}
        />
      )}
    </>
  )
}
