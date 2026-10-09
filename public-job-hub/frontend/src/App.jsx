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
  matchBusy,
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
  const urgent = daysLeft !== null && daysLeft >= 0 && daysLeft <= 7
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
      className={`card ${institutionClass} ${urgent ? 'deadline-urgent' : ''} ${job.applied ? 'applied' : job.scrapped ? 'saved' : ''}`}
    >
      <div className="card-top">
        <div className="card-labels">
          <span className="tag">채용공고</span>
          {urgent && <span className="urgent-tag">마감임박</span>}
        </div>
        <div className="card-tools">
          <button type="button" onClick={() => onShare(job)} aria-label="공고 공유">
            공유
          </button>
        </div>
      </div>
      <button className="title-button" onClick={onOpenSource} title={job.title}>
        <h2>{job.title}</h2>
      </button>
      <div className="details">
        <span>채용기관</span>
        <strong className="organization-name" title={job.organization}>
          {job.organization}
        </strong>
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
          <button className="preview-link" onClick={onOpenSource}>
            원문보기 ↗
          </button>
          <button className="preview-link" onClick={() => onSalary(job)}>
            연봉정보
          </button>
          <button className="preview-link" onClick={() => onCompetition(job)}>
            이전 경쟁률 확인
          </button>
        </div>
        <div className="career-actions">
          <button disabled={busy} onClick={() => onScrap(job.id)}>
            {job.scrapped ? '스크랩 해제' : '＋ 스크랩'}
          </button>
          <button disabled={matchBusy} onClick={() => onMatch(job)}>
            {matchBusy ? '분석 중...' : '내 스펙 매칭'}
          </button>
        </div>
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

function CommunityBoard({ user, securePost, onLogin }) {
  const [tab, setTab] = useState('notices')
  const [notices, setNotices] = useState([])
  const [inquiries, setInquiries] = useState([])
  const [noticeForm, setNoticeForm] = useState({ title: '', content: '', pinned: false })
  const [questionForm, setQuestionForm] = useState({ category: 'SERVICE', title: '', content: '' })
  const [answers, setAnswers] = useState({})
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)

  const loadCommunity = async () => {
    const savedNotices = await getJson('/api/community/notices')
    setNotices(savedNotices)
    if (user) setInquiries(await getJson('/api/community/inquiries'))
    else setInquiries([])
  }
  useEffect(() => {
    loadCommunity().catch((e) => setMessage(e.message))
    // 로그인 상태가 바뀌면 열람 가능한 문의 범위를 다시 읽는다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user])

  const submitNotice = async (event) => {
    event.preventDefault()
    setBusy(true)
    try {
      await securePost(
        '/api/admin/community/notices',
        JSON.stringify(noticeForm),
        'application/json',
      )
      setNoticeForm({ title: '', content: '', pinned: false })
      await loadCommunity()
      setMessage('공지사항을 등록했습니다.')
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
      setQuestionForm({ category: 'SERVICE', title: '', content: '' })
      await loadCommunity()
      setMessage('문의가 접수되었습니다.')
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
              <h2>공지 등록</h2>
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
              <button disabled={busy}>공지 등록</button>
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
              <button disabled={busy}>문의 접수</button>
            </form>
          )}
          <div className="board-list inquiry-list">
            <h2>{user.admin ? '전체 문의 관리' : '내 문의'}</h2>
            {inquiries.map((item) => (
              <details key={item.id}>
                <summary>
                  <span className={item.status === 'ANSWERED' ? 'answered' : 'waiting'}>
                    {item.status === 'ANSWERED' ? '답변완료' : '답변대기'}
                  </span>
                  <strong>{item.title}</strong>
                  <time>{formatDateTime(item.createdAt)}</time>
                </summary>
                {user.admin && (
                  <small className="requester">
                    문의자: {item.requesterName} · {item.requesterEmail}
                  </small>
                )}
                <p>{item.content}</p>
                {item.answer && (
                  <div className="admin-answer">
                    <strong>관리자 답변</strong>
                    <p>{item.answer}</p>
                    <time>{formatDateTime(item.answeredAt)}</time>
                  </div>
                )}
                {user.admin && (
                  <div className="answer-form">
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
            {!inquiries.length && <p className="empty">등록된 문의가 없습니다.</p>}
          </div>
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
          <h2>맞춤 공고 알림</h2>
        </div>
        <label className="alert-toggle">
          <input
            type="checkbox"
            checked={preference.enabled}
            onChange={(e) => onChange({ ...preference, enabled: e.target.checked })}
          />{' '}
          알림 사용
        </label>
      </div>
      <div className="preference-fields">
        <label>
          관심 키워드
          <input
            value={preference.keywords}
            onChange={(e) => onChange({ ...preference, keywords: e.target.value })}
            placeholder="예: 행정, 전산, 인턴 (쉼표로 구분)"
          />
        </label>
        <label>
          관심 지역
          <input
            value={preference.regions}
            onChange={(e) => onChange({ ...preference, regions: e.target.value })}
            placeholder="예: 서울, 부산 (쉼표로 구분)"
          />
        </label>
        <label>
          근무 유형
          <select
            value={preference.mobilityTypes}
            onChange={(e) => onChange({ ...preference, mobilityTypes: e.target.value })}
          >
            <option value="">전체</option>
            <option value="ROTATIONAL">순환근무 가능</option>
            <option value="FIXED">지역고정</option>
            <option value="UNKNOWN">확인 필요</option>
          </select>
        </label>
        <button disabled={busy} onClick={onSave}>
          {busy ? '저장 중...' : '알림 조건 저장'}
        </button>
      </div>
      <div className="notification-options">
        <label>
          <input
            type="checkbox"
            checked={preference.newPostingAlerts ?? true}
            onChange={(e) => onChange({ ...preference, newPostingAlerts: e.target.checked })}
          />
          새로운 맞춤 공고
        </label>
        <label>
          <input
            type="checkbox"
            checked={preference.deadlineAlerts ?? true}
            onChange={(e) => onChange({ ...preference, deadlineAlerts: e.target.checked })}
          />
          마감 알림
        </label>
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
      </div>
      {message && <p role="status">{message}</p>}
      <div className="alerts-head">
        <h3>조건에 맞는 공고 {alerts.length}건</h3>
        {alerts.some((item) => item.fresh) && <button onClick={onRead}>모두 확인</button>}
      </div>
      <section className="alerts-calendar" aria-label="맞춤 공고 마감 달력">
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

/** 로그인·회원가입을 같은 입력 패널에서 처리하는 대화상자. */
function AuthDialog({ mode, onClose, onSubmit, onSwitch, busy, error }) {
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
            {mode === 'register' ? '이메일' : '이메일 또는 관리자 아이디'}
            <input
              type={mode === 'register' ? 'email' : 'text'}
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoComplete={mode === 'register' ? 'email' : 'username'}
            />
          </label>
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

/** 관리자가 자동 추출 결과를 원문과 대조하고 공고별 분류를 바로 정정하는 화면. */
function AdminReviewPanel({ securePost }) {
  const [items, setItems] = useState([])
  const [quality, setQuality] = useState(null)
  const [busy, setBusy] = useState(null)
  const [message, setMessage] = useState('')
  const [crawlInfo, setCrawlInfo] = useState(null)
  const [qualificationFile, setQualificationFile] = useState(null)
  const [qualificationType, setQualificationType] = useState('AUTO')
  const [qualificationEncoding, setQualificationEncoding] = useState('AUTO')
  const [compensationFile, setCompensationFile] = useState(null)
  const loadReviews = useCallback(
    () =>
      Promise.all([getJson('/api/admin/reviews'), getJson('/api/admin/reviews/quality')])
        .then(([reviews, report]) => {
          setItems(reviews)
          setQuality(report)
        })
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
    setMessage('')
    try {
      setCrawlInfo(await securePost('/api/admin/crawl'))
      setMessage('공고 수집을 시작했습니다. 완료될 때까지 이 화면에서 상태를 확인할 수 있습니다.')
    } catch (e) {
      setMessage(e.message)
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
  return (
    <section className="admin-review">
      <div className="admin-title">
        <div>
          <p className="eyebrow">운영 관리</p>
          <h1>공고 분류 검수</h1>
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
      {message && (
        <p className="status" role="status">
          {message}
        </p>
      )}
      {crawlInfo?.apiStatus?.validation?.pagesScanned > 0 && (
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
      {quality && (
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
      {items.map((item) => (
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
                onChange={(e) => changePosition(item.id, index, 'standardCategory', e.target.value)}
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
      {!items.length && !message && <p className="empty">검수할 공고를 불러오는 중...</p>}
    </section>
  )
}

/** 검색·페이지 이동·인증·개인 상태와 원문 창을 조합하는 메인 화면. */
export default function App() {
  // 현재 회원과 CSRF 토큰은 세션 변경 요청에 공통으로 사용한다.
  const [user, setUser] = useState(null)
  const [csrf, setCsrf] = useState(null)
  const [authMode, setAuthMode] = useState(null)
  const [authError, setAuthError] = useState('')
  const [authBusy, setAuthBusy] = useState(false)
  const [passwordOpen, setPasswordOpen] = useState(false)
  const [passwordError, setPasswordError] = useState('')
  const [passwordBusy, setPasswordBusy] = useState(false)
  // 공고 검색, 마이페이지 필터, 페이지 번호 및 API 결과를 보관한다.
  const [mine, setMine] = useState(false)
  const [scrapsOpen, setScrapsOpen] = useState(false)
  const [adminView, setAdminView] = useState(false)
  const [communityView, setCommunityView] = useState(false)
  const [queryInput, setQueryInput] = useState('')
  const [query, setQuery] = useState('')
  const [regionProvince, setRegionProvince] = useState('')
  const [regionDistrict, setRegionDistrict] = useState('')
  const [mobility, setMobility] = useState('')
  const [jobCategory, setJobCategory] = useState('')
  const [filterOptions, setFilterOptions] = useState({
    regions: [],
    mobilityTypes: [],
    jobCategories: [],
  })
  const [page, setPage] = useState(0)
  const [data, setData] = useState({ items: [], total: 0, hasNext: false, crawlStatus: null })
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
  })
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
  // 설치 가능 여부와 네트워크 상태는 PWA 경험을 안내하는 데만 사용한다.
  const [installPrompt, setInstallPrompt] = useState(null)
  const [online, setOnline] = useState(navigator.onLine)
  const [updateAvailable, setUpdateAvailable] = useState(false)

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
    if (session.user?.mustChangePassword) setPasswordOpen(true)
    setCsrf(token)
  }, [])
  useEffect(() => {
    refreshAuth().catch(() => setError('서버에 연결할 수 없습니다.'))
  }, [refreshAuth])
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
          region: mine ? '' : regionProvince,
          regionFull: mine
            ? ''
            : REGION_LEVELS.find((item) => item.code === regionProvince)?.label || '',
          district: mine ? '' : regionDistrict,
          mobility: mine ? '' : mobility,
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
    [query, page, mine, scrapsOpen, regionProvince, regionDistrict, mobility, jobCategory],
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
  /** 개별 채용 공고문 PDF를 우선해 작은 창으로 열고 통합 채용 홈페이지 이동을 피한다. */
  async function openSource(job) {
    const width = Math.min(1100, Math.max(720, window.screen.availWidth - 160))
    const height = Math.min(820, Math.max(600, window.screen.availHeight - 120))
    const left = Math.max(0, Math.round((window.screen.availWidth - width) / 2))
    const top = Math.max(0, Math.round((window.screen.availHeight - height) / 2))
    const popup = window.open(
      'about:blank',
      `job-posting-${job.id}`,
      `popup=yes,width=${width},height=${height},left=${left},top=${top},resizable=yes,scrollbars=yes`,
    )
    if (!popup) {
      setError('원문 창이 차단되었습니다. 브라우저에서 이 사이트의 팝업을 허용해 주세요.')
      return
    }
    popup.document.title = '채용공고 원문 불러오는 중'
    popup.document.body.innerHTML =
      '<p style="font:16px sans-serif;padding:32px">채용공고 원문을 불러오는 중입니다...</p>'
    popup.focus()
    try {
      const detail = await getJson(`/api/postings/${job.id}/preview`)
      popup.location.replace(detail.originalUrl || job.sourceUrl)
    } catch (e) {
      popup.location.replace(job.sourceUrl)
    }
  }
  /** 마이페이지는 로그인한 회원에게만 열고 필터 변경 시 첫 페이지로 이동한다. */
  function navigate(nextMine) {
    if (nextMine && !user) {
      setAuthMode('login')
      return
    }
    setMine(nextMine)
    setScrapsOpen(false)
    setAdminView(false)
    setCommunityView(false)
    setPage(0)
  }
  /** 서비스 배너를 누르면 검색·필터·개인 화면 상태를 모두 지우고 첫 화면으로 돌아간다. */
  function resetHome() {
    setMine(false)
    setScrapsOpen(false)
    setAdminView(false)
    setCommunityView(false)
    setQueryInput('')
    setQuery('')
    setRegionProvince('')
    setRegionDistrict('')
    setMobility('')
    setJobCategory('')
    setPage(0)
    setMatchDialog(null)
    setError('')
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
            <button className={!mine ? 'active' : ''} onClick={() => navigate(false)}>
              채용정보
            </button>
            <button className={mine ? 'active' : ''} onClick={() => navigate(true)}>
              마이페이지
            </button>
            <button className={capabilityOpen ? 'active' : ''} onClick={openCapabilities}>
              내 역량
            </button>
            <button
              className={communityView ? 'active' : ''}
              onClick={() => {
                setCommunityView(true)
                setMine(false)
                setAdminView(false)
              }}
            >
              공지·Q&amp;A
            </button>
            {user?.admin && (
              <button
                className={adminView ? 'active' : ''}
                onClick={() => {
                  setAdminView(true)
                  setMine(false)
                  setCommunityView(false)
                }}
              >
                관리자 검수
              </button>
            )}
            {user ? (
              <>
                <span className="user-name">{user.displayName}님</span>
                <button onClick={() => setPasswordOpen(true)}>비밀번호 변경</button>
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
        {updateAvailable && (
          <div className="update-banner" role="status">
            <span>새 버전이 준비되었습니다.</span>
            <button onClick={() => window.dispatchEvent(new CustomEvent('jobhub:apply-update'))}>
              지금 업데이트
            </button>
          </div>
        )}
        {adminView ? (
          <AdminReviewPanel securePost={securePost} />
        ) : communityView ? (
          <CommunityBoard
            user={user}
            securePost={securePost}
            onLogin={() => setAuthMode('login')}
          />
        ) : (
          <>
            <div className="heading">
              <div>
                <p className="eyebrow">공공기관 채용정보</p>
                <h1>{mine ? '마이페이지' : '최신 채용공고 현황'}</h1>
                <p className="sub">
                  {mine
                    ? '내 스펙과 맞춤 알림을 관리하세요.'
                    : '마감 임박순 · 20개씩 · 매일 자정 수집'}
                </p>
                {mine && (
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
                    채용 직렬
                    <select
                      value={jobCategory}
                      onChange={(e) => {
                        setJobCategory(e.target.value)
                        setPage(0)
                      }}
                    >
                      <option value="">전체 직렬</option>
                      {filterOptions.jobCategories?.map((value) => (
                        <option key={value}>{value}</option>
                      ))}
                    </select>
                  </label>
                  {(regionProvince || regionDistrict || mobility || jobCategory) && (
                    <button
                      onClick={() => {
                        setRegionProvince('')
                        setRegionDistrict('')
                        setMobility('')
                        setJobCategory('')
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
                    파란색: 중앙 공공기관 · 주황색: 지방공기업 · 청록색: 민간기업 · 빨간 강조: 마감
                    7일 이내
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
                {status.error && <span className="status-error"> · 수집 오류: {status.error}</span>}
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
                      matchBusy={matchBusyId === job.id}
                    />
                  ))}
                </section>
              ))}
            {mine && scrapsOpen && !loading && data.items.length === 0 && (
              <p className="empty">스크랩한 공고가 없습니다.</p>
            )}
            {mine && scrapsOpen && (
              <div className="pagination">
                <button disabled={page === 0} onClick={() => setPage(page - 1)}>
                  ← 이전
                </button>
                <span>{page + 1} 페이지</span>
                <button disabled={!data.hasNext} onClick={() => setPage(page + 1)}>
                  다음 →
                </button>
              </div>
            )}
            {mine && user && (
              <div className="mypage-dashboard">
                <ApplicationDashboard applications={applications} onOpen={openSource} />
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
                      matchBusy={matchBusyId === job.id}
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
            {!mine && (
              <div className="pagination">
                <button disabled={page === 0} onClick={() => setPage(page - 1)}>
                  ← 이전
                </button>
                <span>{page + 1} 페이지</span>
                <button disabled={!data.hasNext} onClick={() => setPage(page + 1)}>
                  다음 →
                </button>
              </div>
            )}
          </>
        )}
      </main>
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
          }}
          onSubmit={submitAuth}
          onSwitch={() => {
            setAuthMode('register')
            setAuthError('')
          }}
          busy={authBusy}
          error={authError}
        />
      )}
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
    </>
  )
}
