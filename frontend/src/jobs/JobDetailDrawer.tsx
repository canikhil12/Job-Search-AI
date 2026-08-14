import { useEffect, useState } from 'react'
import type { AtsResult, Job } from '../types'
import { AiStreamPanel } from '../ai/AiStreamPanel'
import { getAts } from '../ats/atsApi'

type Tab = 'overview' | 'analysis' | 'cover-letter' | 'tailor'

function atsLevel(score: number): string {
  if (score >= 75) return 'good'
  if (score >= 50) return 'mid'
  return 'low'
}

export function JobDetailDrawer({
  job,
  activeResumeId,
  onClose,
}: {
  job: Job
  activeResumeId: string | null
  onClose: () => void
}) {
  const [tab, setTab] = useState<Tab>('overview')
  const [ats, setAts] = useState<AtsResult | null>(null)
  const [atsLoading, setAtsLoading] = useState(false)
  const [atsError, setAtsError] = useState<string | null>(null)

  // Compute the ATS score when the drawer opens for a job (needs an active résumé).
  useEffect(() => {
    if (!activeResumeId) return
    let cancelled = false
    setAts(null)
    setAtsError(null)
    setAtsLoading(true)
    getAts(activeResumeId, job.id)
      .then((r) => !cancelled && setAts(r))
      .catch(() => !cancelled && setAtsError('Could not score this job.'))
      .finally(() => !cancelled && setAtsLoading(false))
    return () => {
      cancelled = true
    }
  }, [job.id, activeResumeId])

  return (
    <div className="drawer-overlay" onClick={onClose}>
      <aside className="drawer" onClick={(e) => e.stopPropagation()}>
        <header className="drawer-head">
          <div className="drawer-title">
            <h2>{job.title}</h2>
            <p className="muted">{[job.company, job.location].filter(Boolean).join(' · ')}</p>
          </div>
          <button type="button" className="link" onClick={onClose}>
            Close
          </button>
        </header>

        {job.sourceUrl && (
          <a className="btn-apply" href={job.sourceUrl} target="_blank" rel="noopener noreferrer">
            Apply on the job site →
          </a>
        )}

        {activeResumeId && (
          <div className="ats">
            {atsLoading ? (
              <span className="muted small">Scoring your résumé against this job…</span>
            ) : atsError ? (
              <p className="error">{atsError}</p>
            ) : ats ? (
              <>
                <div className="ats-head">
                  <div className={`ats-gauge ${atsLevel(ats.score)}`}>
                    <strong>{ats.score}</strong>
                    <span>ATS</span>
                  </div>
                  <div className="ats-summary">
                    <p>{ats.summary}</p>
                    {ats.score < 75 && (
                      <button type="button" className="link strong" onClick={() => setTab('tailor')}>
                        Tailor résumé to close the gaps →
                      </button>
                    )}
                  </div>
                </div>
                {(ats.matchedKeywords.length > 0 || ats.missingKeywords.length > 0) && (
                  <div className="ats-keywords">
                    {ats.matchedKeywords.map((k) => (
                      <span key={`m-${k}`} className="kw matched">
                        {k}
                      </span>
                    ))}
                    {ats.missingKeywords.map((k) => (
                      <span key={`x-${k}`} className="kw missing">
                        {k}
                      </span>
                    ))}
                  </div>
                )}
              </>
            ) : null}
          </div>
        )}

        <nav className="tabs">
          <button type="button" className={tab === 'overview' ? 'active' : ''} onClick={() => setTab('overview')}>
            Overview
          </button>
          <button type="button" className={tab === 'analysis' ? 'active' : ''} onClick={() => setTab('analysis')}>
            Gap analysis
          </button>
          <button
            type="button"
            className={tab === 'cover-letter' ? 'active' : ''}
            onClick={() => setTab('cover-letter')}
          >
            Cover letter
          </button>
          <button type="button" className={tab === 'tailor' ? 'active' : ''} onClick={() => setTab('tailor')}>
            Tailor
          </button>
        </nav>

        <div className="drawer-body">
          {tab === 'overview' && <div className="job-description">{job.description}</div>}
          {tab !== 'overview' &&
            (activeResumeId ? (
              <AiStreamPanel resumeId={activeResumeId} jobId={job.id} kind={tab} />
            ) : (
              <p className="muted">Pick an active résumé (top-right) to generate this.</p>
            ))}
        </div>
      </aside>
    </div>
  )
}
