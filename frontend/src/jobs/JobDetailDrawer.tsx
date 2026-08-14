import { useState } from 'react'
import type { Job } from '../types'
import { AiStreamPanel } from '../ai/AiStreamPanel'

type Tab = 'overview' | 'analysis' | 'cover-letter'

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
